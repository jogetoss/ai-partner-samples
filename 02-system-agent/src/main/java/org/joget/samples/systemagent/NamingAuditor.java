package org.joget.samples.systemagent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joget.ai.agent.core.AgentFactory;
import org.joget.ai.agent.model.AgentComponent;
import org.joget.ai.agent.model.AgentLLMAbstract;
import org.joget.ai.agent.model.AgentPromptAbstract;
import org.joget.ai.agent.model.AgentToolAbstract;
import org.joget.ai.agent.model.Response;
import org.joget.ai.agent.model.SystemAgentRequest;
import org.joget.ai.config.config.LLMConfig;
import org.joget.ai.config.llm.LLMConfigFactory;
import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;

/**
 * Builds the request and runs the agent. This is the only class that touches the LLM; call it from anything that
 * can run Java in Joget (a console page as here, a process tool, a scheduler, a servlet).
 */
public final class NamingAuditor {

    /** Outcome of one run. {@code ok} is false for an empty answer or an error text from the AI service. */
    public static final class Result {
        public final boolean ok;
        public final String runId;
        public final String report;

        Result(boolean ok, String runId, String report) {
            this.ok = ok;
            this.runId = runId;
            this.report = report;
        }
    }

    private NamingAuditor() {
    }

    public static Result audit(String appId, String language) {
        PluginManager pm = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");

        // 1. The model: whatever AI service is active in AI Central Config (provider, key and model live there).
        AgentLLMAbstract llm = activeLlm(pm);

        // 2. Prompts: a persona for the whole agent, then the task text.
        AgentPromptAbstract persona = AgentFactory.create(pm, AgentComponent.PersonaAgentPrompt, Map.of(
                "role", "Joget implementation reviewer",
                "backstory", "You review Joget apps against the naming standard. Always call check_app_naming first and report "
                        + "only what it returns. Never invent ids. Write a short Markdown report: a one-line verdict, "
                        + "a table of violations (id, problem, suggested id) and the top three recommendations."));
        String lang = "id".equals(language) ? "Bahasa Indonesia" : "English";
        AgentPromptAbstract task = AgentFactory.create(pm, AgentComponent.TextPromptAgentPrompt, Map.of(
                "msgRole", "USER", "prompt", "Audit the naming of app " + appId + ". Write the report in " + lang + "."));

        // 3. Tools: our own plugin; only its class name travels, Agent Builder re-creates it from the plugin manager.
        List<AgentToolAbstract> tools = List.of(new NamingCheckTool());

        SystemAgentRequest req = new SystemAgentRequest(llm, List.of(persona), List.of(task), tools, Collections.emptyList(), false);
        String runId = "RUN-" + UUID.randomUUID();
        req.setRunId(runId);

        // 4. Properties tell Agent Builder who the run is for; they appear in Agent Execution Audit Trail.
        Map<String, Object> props = new HashMap<>();
        props.put("username", nz(WorkflowUtil.getCurrentUsername()));
        props.put("fullName", nz(WorkflowUtil.getCurrentUserFullName()));
        props.put("appId", appId);
        props.put("triggerSource", "NAMING_AUDITOR_PAGE");

        Response r = new NamingAuditorAgent().run(req, props);
        String text = r == null || r.getContent() == null ? "" : r.getContent().trim();
        // run() returns normally even when the provider failed, so check the response itself.
        boolean ok = r != null && !r.isFailed() && !text.isEmpty() && !text.startsWith("API Error");
        return new Result(ok, runId, text);
    }

    /** The active AI Central Config service as an Agent Builder LLM plugin. */
    static AgentLLMAbstract activeLlm(PluginManager pm) {
        LLMConfig c;
        try {
            c = LLMConfigFactory.createByServiceOrActive(null);
        } catch (Exception e) {
            throw new IllegalStateException("No active AI service in AI Central Config: " + e.getMessage(), e);
        }
        Map<String, Object> p = new HashMap<>();
        if (c.getAdditionalConfig() != null) {
            p.putAll(c.getAdditionalConfig());
        }
        p.put("apiKey", c.getApiKey());
        p.put("model", c.getModelName());
        String custom = c.getCustomEndpointUrl();
        p.put("apiUrl", custom == null || custom.isEmpty() ? c.getApiEndpoint() : custom);
        p.put("maxTokens", "3000");
        return AgentFactory.createLLM(pm, c.getProviderName(), p);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
