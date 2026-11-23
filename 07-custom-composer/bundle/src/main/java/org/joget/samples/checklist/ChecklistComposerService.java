package org.joget.samples.checklist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import jakarta.websocket.Session;
import org.joget.ai.agent.core.AgentFactory;
import org.joget.ai.agent.lib.composer.AbstractComposerChat;
import org.joget.ai.agent.model.AgentComponent;
import org.joget.ai.agent.model.AgentLLMAbstract;
import org.joget.ai.agent.model.AgentPromptAbstract;
import org.joget.ai.agent.model.AgentToolAbstract;
import org.joget.ai.agent.model.Response;
import org.joget.ai.agent.model.SystemAgentAbstract;
import org.joget.ai.agent.model.SystemAgentRequest;
import org.joget.ai.config.config.LLMConfig;
import org.joget.ai.config.llm.LLMConfigFactory;
import org.joget.ai.config.utils.CredentialUtil;
import org.joget.ai.config.utils.SkillUtil;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;
import org.json.JSONObject;

/** One method, executeHeadless, does the work. The WebSocket path and the Orchestrator path both call it. */
public class ChecklistComposerService {

    private final SystemAgentAbstract agent;
    private final ExecutorService executor;

    public ChecklistComposerService(SystemAgentAbstract agent, ExecutorService executor) {
        this.agent = agent;
        this.executor = executor;
    }

    /** "init": a status frame first (it releases the panel), then a greeting on a fresh conversation. */
    public void handleInit(Session session, JSONObject message) {
        if (!CredentialUtil.hasActiveAiService()) {
            send(session, new JSONObject().put("type", "error").put("message", text("checklist.error.noAiServices"))
                    .put("errorCode", AbstractComposerChat.ERROR_CODE_AI_SERVICES_NOT_CONFIGURED));
            return;
        }
        send(session, new JSONObject().put("type", "status").put("message", text("checklist.status.initialized")));
        if (!message.optBoolean("reconnect", false)) {
            send(session, AbstractComposerChat.createWelcomeMessage(text("checklist.welcome")));
        }
    }

    /** "agentic_edit": run on a worker thread that carries the composer context (and the user) from this thread. */
    public void handleAgenticEdit(Session session, JSONObject incoming) {
        final String composerClass = AbstractComposerChat.getActiveComposerClass(ChecklistComposerChat.class);
        executor.submit(AbstractComposerChat.withComposerContext(composerClass, () -> executeHeadless(incoming, payload -> {
            if ("run_started".equals(payload.optString("type"))) {
                if (session.isOpen()) {
                    session.getUserProperties().put("agentRunId", payload.optString("runId"));
                }
            } else {
                if ("agentic_complete".equals(payload.optString("type")) && payload.optBoolean("changed", true)) {
                    payload.put("actions", new JSONArray().put("apply").put("cancel")); // draws Confirm and Revert
                }
                send(session, payload);
            }
        })));
    }

    /** The Stop button. */
    public void handleAbort(Session session) {
        Object runId = session.isOpen() ? session.getUserProperties().get("agentRunId") : null;
        if (runId != null) {
            ChecklistComposerAgentTool.stop(runId.toString());
        }
    }

    /**
     * Runs the composer without a browser. Events: run_started, agentic_trace,
     * agentic_complete {message, json, changed}, agentic_aborted, agentic_error {message}.
     */
    public void executeHeadless(JSONObject incoming, Consumer<JSONObject> sink) {
        final JSONObject json = incoming.optJSONObject("json");
        if (json == null) {
            sink.accept(new JSONObject().put("type", "agentic_error").put("message", text("checklist.error.noData")));
            return;
        }
        final String runId = "RUN-" + UUID.randomUUID();
        UUID traceListener = null;
        try {
            sink.accept(new JSONObject().put("type", "run_started").put("runId", runId));
            ChecklistComposerAgentTool.begin(runId);
            ChecklistComposerAgentTool.RUN_ID.set(runId);
            traceListener = agent.subscribe(runId, p -> { // forward each step; a bad event must not break the run
                try {
                    sink.accept(new JSONObject().put("type", "agentic_trace").put("runId", runId).put("payload", new JSONObject(p)));
                } catch (RuntimeException ignored) {
                    // skip this event
                }
            });

            PluginManager pm = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");

            // 1. The model: the AI service chosen in the panel, else the active one.
            LLMConfig cfg = LLMConfigFactory.createByServiceOrActive(incoming.optString("selectedAiService"));
            Map<String, Object> llmProps = new HashMap<>();
            llmProps.put("apiKey", cfg.getApiKey());
            llmProps.put("model", cfg.getModelName());
            llmProps.put("apiUrl", cfg.getCustomEndpointUrl().isEmpty() ? cfg.getApiEndpoint() : cfg.getCustomEndpointUrl());
            if (cfg.getAdditionalConfig() != null) {
                llmProps.putAll(cfg.getAdditionalConfig());
            }
            llmProps.put("maxTokens", "16000");
            AgentLLMAbstract llm = AgentFactory.createLLM(pm, cfg.getProviderName(), llmProps);

            // 2. System prompt, and the instruction with the current JSON.
            String skills = SkillUtil.buildSkillsPromptHint(AbstractComposerChat.getActiveComposerClass());
            AgentPromptAbstract system = AgentFactory.create(pm, AgentComponent.PersonaAgentPrompt, Map.of(
                    "role", "Checklist Builder Assistant",
                    "backstory", "You are a Joget checklist builder. Change the checklist ONLY by calling the tool functions; "
                            + "never write JSON yourself. Ids are snake_case, at most 20 characters. Finish with one or two "
                            + "plain sentences that say what changed." + (skills == null || skills.isEmpty() ? "" : "\n" + skills)));
            AgentPromptAbstract request = AgentFactory.create(pm, AgentComponent.TextPromptAgentPrompt, Map.of("msgRole", "USER",
                    "prompt", "Instruction: " + incoming.optString("message")
                            + "\n\nCurrent checklist JSON:\n" + json));

            // 3. Tools: ours first (the first tool that declares a function name wins), then the skill invoker.
            AgentComponent mine = AgentComponent.register(ChecklistComposerAgentTool.NAME,
                    ChecklistComposerAgentTool.class.getName(), AgentToolAbstract.class);
            ChecklistComposerAgentTool.CURRENT.set(new JSONObject(json.toString()));
            ChecklistComposerAgentTool.RESULT_HOLDER.remove();
            List<AgentToolAbstract> tools = new ArrayList<>();
            tools.add(AgentFactory.create(pm, mine, Map.of()));
            tools.add(AgentFactory.create(pm, AgentComponent.SkillComposerTool, Map.of()));

            // 4. Run. This blocks the current thread until the model has finished.
            SystemAgentRequest req = new SystemAgentRequest(llm, Collections.singletonList(system), Collections.singletonList(request), tools,
                    Collections.emptyList(), false);
            req.setRunId(runId);
            Response response = agent.run(req, new HashMap<>());

            JSONObject result = ChecklistComposerAgentTool.RESULT_HOLDER.get();
            String said = response != null && response.getContent() != null ? response.getContent().trim() : "";
            if (ChecklistComposerAgentTool.wasStopped(runId)) {
                sink.accept(new JSONObject().put("type", "agentic_aborted"));
            } else if (result == null) { // the model answered in words and called no function: nothing to apply
                sink.accept(new JSONObject().put("type", "agentic_complete").put("message", said).put("json", json).put("changed", false));
            } else {
                sink.accept(new JSONObject().put("type", "agentic_complete")
                        .put("message", said.isEmpty() ? text("checklist.updated") : said).put("json", result));
            }
        } catch (Exception e) {
            LogUtil.error(ChecklistComposerService.class.getName(), e, "Checklist composer run failed");
            sink.accept(new JSONObject().put("type", "agentic_error").put("message", String.valueOf(e.getMessage())));
        } finally { // pool threads are reused: leave nothing behind
            ChecklistComposerAgentTool.CURRENT.remove();
            ChecklistComposerAgentTool.RESULT_HOLDER.remove();
            ChecklistComposerAgentTool.RUN_ID.remove();
            ChecklistComposerAgentTool.end(runId);
            if (traceListener != null) {
                agent.unsubscribe(runId, traceListener);
            }
        }
    }

    /** Writes one frame. Agent Builder's WebsocketUtil is not available to other bundles: write to the session, under its lock. */
    static void send(Session session, JSONObject payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        synchronized (session) {
            try {
                session.getBasicRemote().sendText(payload.toString());
            } catch (Exception e) {
                LogUtil.debug(ChecklistComposerService.class.getName(), "Frame not sent: " + e.getMessage());
            }
        }
    }

    private static String text(String key) {
        return AppPluginUtil.getMessage(key, ChecklistComposerChat.class.getName(), "messages/checklistComposer");
    }
}
