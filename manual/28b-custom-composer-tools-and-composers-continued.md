# Custom Composer Tools and Building Your Own Composer (continued)

This page continues [Custom Composer Tools and Building Your Own Composer](28-custom-composer-tools-and-composers.md). It has the complete minimal example of Part 2 (a checklist composer: the build route for Joget DX 9, tool, service and composer class, with installation, what was tested and limits), Part 3 (what is possible for Composer MCP clients) and three ideas for an implementation partner. It is for plugin developers. Read Part 2 on the first page before you read the example.

## Part 2: The complete minimal example, part 1

A checklist composer: it edits `{id, name, items:[{id,label}]}` on a custom builder of type `checklist`. Package `org.joget.samples.checklist`. The complete project is the folder `07-custom-composer` of the examples repository; the classes on this page are its source files.

### The build route for Joget DX 9

A composer cannot be compiled directly against `agent-builder-api-8.2.5-ALPHA.jar`. The API jar is compiled with the `javax.servlet` and `javax.websocket` packages. Joget DX 9 uses `jakarta.servlet` and `jakarta.websocket`, and converts the Agent Builder plugin to `jakarta` when the plugin is installed. A class that extends `AbstractComposerChat` overrides methods with servlet and websocket types, so the direct build stops with `package javax.websocket does not exist` and `does not override abstract method getHtml(jakarta.servlet.http.HttpServletRequest)`. Tools, guards, enhancers and LLM providers are not affected: their methods have no such types.

The sample solves this with two Maven modules and one command:

1. **`api-jakarta`** makes a `jakarta` copy of the API jar with the Eclipse Transformer, the tool Joget DX 9 itself uses at install. The module has no source code. Its pom has packaging `jar`, artifact `org.joget.samples:agent-builder-api-jakarta:1.0-SNAPSHOT`, and this plugin:

```xml
<plugin>
    <groupId>org.eclipse.transformer</groupId>
    <artifactId>transformer-maven-plugin</artifactId>
    <version>1.0.0</version>
    <extensions>true</extensions>
    <configuration>
        <rules>
            <jakartaDefaults>true</jakartaDefaults>
        </rules>
    </configuration>
    <executions>
        <execution>
            <id>default-jar</id>
            <goals><goal>jar</goal></goals>
            <configuration>
                <artifact>
                    <groupId>org.joget.ai.agent</groupId>
                    <artifactId>agent-builder-api</artifactId>
                    <version>8.2.5-ALPHA</version>
                </artifact>
            </configuration>
        </execution>
    </executions>
</plugin>
```

2. **`bundle`** is the pom of Part 1 (`03-composer-custom-tool/bundle/pom.xml`) with these changes: `artifactId` `checklist-composer`, `Bundle-SymbolicName` `org.joget.samples.checklist`, `Private-Package` `org.joget.samples.checklist.*`, `Bundle-Activator` `org.joget.samples.checklist.Activator`; `Import-Package` `!*,jakarta.servlet;version="0",jakarta.servlet.http;version="0",org.osgi.framework;version="1.3.0"` (the classes already use `jakarta`, so Joget installs the jar as it is); and these dependencies in place of `agent-builder-api`:

```xml
<dependency>
    <groupId>org.joget.samples</groupId>
    <artifactId>agent-builder-api-jakarta</artifactId>
    <version>${project.version}</version>
    <scope>provided</scope>
    <exclusions><exclusion><groupId>*</groupId><artifactId>*</artifactId></exclusion></exclusions>
</dependency>
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.0.0</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>jakarta.websocket</groupId>
    <artifactId>jakarta.websocket-api</artifactId>
    <version>2.1.1</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>jakarta.websocket</groupId>
    <artifactId>jakarta.websocket-client-api</artifactId>
    <version>2.1.1</version>
    <scope>provided</scope>
</dependency>
```

3. A parent pom in `07-custom-composer` (packaging `pom`) names the modules `api-jakarta` and `bundle` in this order. Build there, not in `bundle`:

```bash
mvn clean package "-Djoget.version=9.1.2"
```

The result is `bundle/target/checklist-composer-1.0-SNAPSHOT.jar`. The first build downloads the transformer plugin from Maven Central. Never install or embed the `jakarta` copy: at run time the classes come from the installed Agent Builder plugin. In your source, import `jakarta.websocket.Session`.

### The message file

`bundle/src/main/resources/messages/checklistComposer.properties`:

```properties
checklist.welcome=Hello! I can create or change this checklist. Tell me which items it needs.
checklist.status.initialized=Checklist composer ready.
checklist.updated=Checklist updated.
checklist.error.noData=No checklist data was received from the builder.
checklist.error.noAiServices=No AI service is configured and activated. Add one in AI Central Config.
```

### Activator.java and Registrar.java

Like the Activator and the Registrar of Part 1 (the probe class is `org.joget.ai.agent.lib.composer.AbstractComposerChat`), with two registrations in `Registrar`: `context.registerService(ChecklistComposerChat.class.getName(), new ChecklistComposerChat(), null)` and the same for `ChecklistComposerAgentTool`. `stop` unregisters both. The tool must be registered too, because the runtime re-creates it through the plugin manager.

### ChecklistComposerAgentTool.java

```java
package org.joget.samples.checklist;

import java.util.concurrent.ConcurrentHashMap;
import org.joget.ai.agent.model.AgentAbortedException;
import org.joget.ai.agent.model.AgentLLM;
import org.joget.ai.agent.model.AgentToolAbstract;
import org.joget.ai.agent.model.Function;
import org.joget.ai.agent.model.Functions;
import org.joget.ai.agent.model.LLMConfig;
import org.joget.ai.agent.model.ToolExecution;
import org.json.JSONArray;
import org.json.JSONObject;

/** The only thing the model may touch. Artifact: {"id", "name", "items": [{"id", "label"}]}; the Orchestrator reads "id" and "name". */
public class ChecklistComposerAgentTool extends AgentToolAbstract {

    public static final String NAME = "ChecklistComposerAgentTool";
    public static final ThreadLocal<JSONObject> CURRENT = new ThreadLocal<>();       // the artifact the run starts from
    public static final ThreadLocal<JSONObject> RESULT_HOLDER = new ThreadLocal<>(); // the last good result
    public static final ThreadLocal<String> RUN_ID = new ThreadLocal<>();

    /** Active run ids, mapped to "was Stop pressed" (Agent Builder's own stop service is not exported). */
    private static final ConcurrentHashMap<String, Boolean> RUNS = new ConcurrentHashMap<>();

    static void begin(String runId)         { RUNS.put(runId, false); }
    static void stop(String runId)          { RUNS.replace(runId, true); } // ignored for a run that has ended
    static boolean wasStopped(String runId) { return runId != null && Boolean.TRUE.equals(RUNS.get(runId)); }
    static void end(String runId)           { RUNS.remove(runId); }

    @Override public String getName()            { return NAME; }
    @Override public String getVersion()         { return "1.0.0"; }
    @Override public String getClassName()       { return ChecklistComposerAgentTool.class.getName(); }
    @Override public String getLabel()           { return "Checklist Composer"; }
    @Override public String getDescription()     { return "Creates and changes checklist definitions."; }
    @Override public String getPropertyOptions() { return "[]"; }
    @Override public String getIcon()            { return "<i class=\"fas fa-tasks\"></i>"; }
    @Override public String getInfoTemplate()    { return "<div>" + NAME + "</div>"; }
    @Override public Boolean isHidden()          { return true; }  // only for the composer: out of the palette and the Tool Type list
    @Override public boolean isParallelSafe()    { return false; } // the state is in ThreadLocals

    @Override
    public Functions getFunctions() {
        Functions functions = new Functions();
        Function init = new Function("set_checklist", "Create the checklist or rename it. Call once, first, for a new checklist.");
        init.addParameter("id", Function.ParameterType.STRING, "snake_case id, at most 20 characters. Empty keeps the current id.", false);
        init.addParameter("name", Function.ParameterType.STRING, "Display name.", true);
        functions.add(init);
        Function add = new Function("add_item", "Append one item to the checklist.");
        add.addParameter("id", Function.ParameterType.STRING, "snake_case item id, unique in the checklist.", true);
        add.addParameter("label", Function.ParameterType.STRING, "What the person has to do or confirm.", true);
        functions.add(add);
        return functions;
    }

    @Override
    public String execute(AgentLLM llm, LLMConfig config, ToolExecution call) {
        if (wasStopped(RUN_ID.get())) {
            throw new AgentAbortedException("Stopped by the user"); // Agent Builder ends the run
        }
        JSONObject args = call.getArguments() == null ? new JSONObject() : call.getArguments();
        JSONObject doc = RESULT_HOLDER.get() != null ? RESULT_HOLDER.get()
                : new JSONObject(CURRENT.get() != null ? CURRENT.get().toString() : "{}");
        if (!doc.has("items")) {
            doc.put("items", new JSONArray());
        }
        switch (call.getName()) {
            case "set_checklist":
                if (!args.optString("id", "").isEmpty()) {
                    doc.put("id", args.getString("id"));
                }
                doc.put("name", args.optString("name", doc.optString("name", "")));
                break;
            case "add_item":
                JSONArray items = doc.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    if (args.optString("id").equals(items.getJSONObject(i).optString("id"))) {
                        return error("item '" + args.optString("id") + "' already exists; use another id");
                    }
                }
                items.put(new JSONObject().put("id", args.optString("id")).put("label", args.optString("label")));
                break;
            default:
                return error("Unknown function: " + call.getName());
        }
        RESULT_HOLDER.set(doc);
        return new JSONObject().put("status", "success").put("items", doc.getJSONArray("items").length()).toString();
    }

    private static String error(String message) {
        return new JSONObject().put("status", "error").put("message", message).toString();
    }
}
```

## Part 2: The complete minimal example, part 2

### ChecklistComposerService.java

```java
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
```

## Part 2: The example composer class, installation and limits

### ChecklistComposerChat.java

```java
package org.joget.samples.checklist;

import java.util.concurrent.Executors;
import java.util.function.Consumer;
import jakarta.websocket.Session;
import org.joget.ai.agent.lib.composer.AbstractComposerChat;
import org.joget.ai.agent.model.SystemAgentAbstract;
import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.dao.BuilderDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.BuilderDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.JSONObject;

/** A composer for a "checklist" artifact. The class name must end in "ComposerChat". */
public class ChecklistComposerChat extends AbstractComposerChat {

    public static final String BUILDER_TYPE = "checklist"; // the type key of the custom builder

    // ---- identity and place ----
    @Override public String getName()         { return "AI Composer - Checklist Builder Agent"; } // shown in Central Config and AI Session Monitor
    @Override public String getVersion()      { return "1.0.0"; }
    @Override public String getDescription()  { return "Chat window for AI Composer - Checklist Builder Agent"; }
    @Override public String getComposerUuid() { return "7b0d6a52-3f4e-4c8e-9a31-2d6f1c9b0a01"; } // generate your own once
    @Override public String getAgentName()    { return "Checklist Composer"; } // the panel header

    @Override
    public String[] getInjectUrlPatterns() { // the first entry also gives the Orchestrator its builder path
        return new String[] {"/web/console/app/*/*/cbuilder/checklist/design/*"};
    }

    @Override protected String getPlatformVersion() { return "9"; } // Joget DX 9 (the panel CSS uses it)

    // The panel reads its own texts from the Agent Builder bundle: return Agent Builder's class and message path.
    @Override protected String getAgentBuilderClassName() { return "org.joget.ai.agent.AgentBuilder"; }
    @Override protected String getMessagePath()           { return "agent/messages/agentPlugins"; }

    @Override protected String getBuilderLabel()         { return "checklist"; }
    @Override protected boolean showSelectedElementBar() { return false; } // no element selection in this builder

    // ---- messages from the browser ----
    @Override
    protected void handleMessage(String message, Session session) {
        try {
            ChecklistComposerService service = new ChecklistComposerService(new ChecklistAgent(), executorService);
            JSONObject incoming = new JSONObject(message);
            switch (incoming.optString("type", "agentic_edit")) {
                case "init":
                    service.handleInit(session, incoming);
                    break;
                case "abort_run":
                    service.handleAbort(session);
                    break;
                default: // agentic_edit
                    if (ensureAiServiceConfigured(session, incoming.optString("type"))) {
                        service.handleAgenticEdit(session, incoming);
                    }
            }
        } catch (Exception e) {
            ChecklistComposerService.send(session, new JSONObject().put("type", "error").put("message", String.valueOf(e.getMessage())));
        }
    }

    // ---- Orchestrator ----
    @Override public String getOrchestratorFunctionName() { return "edit_checklist"; } // non-null = opt in

    @Override
    public String getOrchestratorFunctionDescription() {
        return "Create or edit a checklist (a list of items that a person ticks off). Pass the user's instruction.";
    }

    @Override
    public String getOrchestratorRules() {
        return "edit_checklist rules:\n- Call it only when the user asks for a checklist.\n"
             + "- A checklist does not depend on forms or lists: no earlier artifact is needed.";
    }

    @Override public String getOrchestratorBuilderType() { return BUILDER_TYPE; } // the default would be "cbuilder"

    @Override
    public void executeHeadless(JSONObject incoming, Consumer<JSONObject> eventSink) {
        new ChecklistComposerService(new ChecklistAgent(), Executors.newSingleThreadExecutor()).executeHeadless(incoming, eventSink);
    }

    /** Called in Create mode with Confirm or Autopilot. Return true only when the artifact is saved. */
    @Override
    public boolean persistArtifact(String appId, String appVersion, JSONObject artifactJson) {
        try {
            String id = artifactJson.optString("id", "");
            if (id.isEmpty()) {
                return false;
            }
            AppDefinitionDao appDefDao = (AppDefinitionDao) AppUtil.getApplicationContext().getBean("appDefinitionDao");
            AppDefinition appDef = appDefDao.loadVersion(appId, Long.parseLong(appVersion.isEmpty() ? "1" : appVersion));
            if (appDef == null) {
                return false;
            }
            BuilderDefinitionDao dao = (BuilderDefinitionDao) AppUtil.getApplicationContext().getBean("builderDefinitionDao");
            BuilderDefinition definition = dao.loadById(id, appDef);
            boolean exists = definition != null;
            if (!exists) {
                definition = new BuilderDefinition();
                definition.setId(id);
                definition.setType(BUILDER_TYPE);
                definition.setAppDefinition(appDef);
            }
            definition.setName(artifactJson.optString("name", id));
            definition.setJson(artifactJson.toString());
            return exists ? dao.update(definition) : dao.add(definition);
        } catch (Exception e) {
            LogUtil.warn(ChecklistComposerChat.class.getName(), "persistArtifact failed: " + e.getMessage());
            return false;
        }
    }

    /** Names the run in Agent Execution Audit Trail. Class literals, never getClass(): on Joget Cloud an instance can be a proxy. */
    private static class ChecklistAgent extends SystemAgentAbstract {
        @Override protected String getAgentId()    { return ChecklistComposerChat.class.getName(); }
        @Override protected String getAgentName()  { return "Checklist Composer Agent"; }
        @Override protected String getLoggedName() { return "AI Composer"; }
    }
}
```

### Install and check

1. Upload `bundle/target/checklist-composer-1.0-SNAPSHOT.jar` (an AI service must be active). **Manage Plugins** lists **AI Composer - Checklist Builder Agent** and **Checklist Composer**. **Central Instructions** (on the **Skills** tab of AI Central Config) lists **AI Composer - Checklist Builder Agent**; AI Session Monitor lists it under **AI Composer Availability**; the Orchestrator's greeting has the line `Create or edit checklists`. Once a skill is scoped to it, the log shows `invoke_skill for <your class>: n skill(s) offered`.
2. Open a page of your custom builder of type `checklist` and send `Add three items for a new starter onboarding.` You get steps, then **Confirm** and **Revert**. Pressing Stop gives `agentic_aborted`.
3. In the Orchestrator choose **Create** with **Confirm** and ask `Make me an onboarding checklist.` The app then has a builder definition of type `checklist`.

### What was tested

The example was built with the command above and installed on Joget DX 9.1.2 Enterprise with Agent Builder 8.2.5-ALPHA.

- **Tested:** the build and the five unit tests of the tool; the start of the bundle; every listing in step 1; a WebSocket conversation with the composer (`init`, then `agentic_edit` with an empty checklist), which returned `status`, the greeting, the `agentic_trace` steps and `agentic_complete` with the new JSON and the actions `apply` and `cancel`; and the chat panel in a browser on a test page that supplied the builder container (an element with the id `cbuilder`) and a `ComposerBridge` object, where the panel opened with the header **Checklist Composer**, showed **Confirm** and **Revert**, and **Confirm** handed the JSON to the bridge.
- **Not tested:** the panel on a real custom builder page (the sample has no builder), Stop, an Orchestrator run that calls `edit_checklist` (step 3), and `persistArtifact`. Test these with your own builder before you rely on them.

### Limitations and troubleshooting

Stop is cooperative. The panel icon appears only on a page that has an element with the id `cbuilder`, which Joget builder pages have. After each Agent Builder update, install the new API jar, set its version in `api-jakarta/pom.xml`, rebuild and repeat the checks above.

- **The build stops with `does not override abstract method getHtml(jakarta.servlet.http.HttpServletRequest)`:** the composer was compiled against the original API jar. Use the build route above.

- **No panel icon:** the URL pattern does not match, the composer is disabled under **AI Composer Availability**, or the bundle is not active.
- **Raw `@@composer...@@` text, or the first message never sends:** `getMessagePath()` or `getAgentBuilderClassName()` points to your bundle, or `init` was not answered with `status`.
- **No `invoke_skill` and no central instructions:** the composer context was not carried to the worker thread; use `withComposerContext`.
- **The Orchestrator ignores the composer or reports "built but not saved":** the function name is `null` or the composer is disabled; or `persistArtifact` returned `false` (no id, unknown app version, DAO error: read the log warning).

## Part 3: Making your tool or composer available to MCP clients

**Composer MCP (9.1.0-ALPHA) cannot be extended from outside.** Another bundle can neither add a tool nor register a handler for a new artifact type:

- The plugin registers its five browser composer handlers (form, list, ui, process, theme) itself at start-up. No service lookup, plugin lookup or setting lets another bundle add one.
- It exports no packages, so another bundle cannot load the handler interface; reflection is not a supported route.
- The page script and injector that register builder tabs name a fixed set of builder pages, and the headless `builder_*` tools are a fixed list.

A handler for a new artifact type, or a tool callable directly from an MCP client, is a change to Composer MCP itself. Your tool from Part 1 exists only inside Agent Builder runs. To give MCP clients your conventions and data, write a skill in AI Central Config that makes sense outside a composer; for the data of a tool, add a second skill without `tools:` (`04-custom-skills` has `acme-standards-reference`). How Central Config content reaches an MCP client:

- `joget_skills_list`, `joget_skill_read` and `joget_skill_search` serve the skills of AI Central Config together with the plugin's own skills (the prompts `skill_<name>` too). Central skills show `source: central`; one with the same name as a built-in skill appears as `central:<name>`.
- Only **Active** skills are listed. **Draft** skills are never served. **Deprecated** skills can be read by exact name.
- **Available For** is listed but not applied (a client sees every active skill), and **Associated Tool Templates** have no effect.
- Text entered per composer in **Central Instructions** reaches the client as the prompts `joget_form_instructions`, `joget_list_instructions`, `joget_ui_instructions`, `joget_process_instructions` and `joget_theme_instructions`. The **Global** text is not included in this version: put rules every client must see into a skill.

See [Composer MCP: Skills, Prompts and the App Generator](20-composer-mcp-skills-and-prompts.md).

## Three ideas for an implementation partner

1. **Live standards lookup (Part 1, small).** Replace the class `StandardsCatalogue` of the sample `03-composer-custom-tool` with a read from the customer's ERP or a Joget form table, with a short cache, and add a skill per customer. Generated forms, lists and processes then carry the real cost centres, departments and approval limits.
2. **Label and glossary checker (Part 1, small).** A tool `check_label(text)` compares a proposed label with the customer's Bahasa glossary and returns the approved term or a correction. A skill scoped to the Form, List and UI Composers tells the model to call it for every label it writes.
3. **Approval Matrix Composer (Part 2, large).** A custom builder that edits approval levels and limits as one JSON artifact, and a composer with `set_matrix`, `add_level` and `remove_level`. `getOrchestratorRules()` runs it after `edit_form` and before `edit_process`; `persistArtifact` stores it as a builder definition that a process tool reads at run time. The builder is most of the work.

## Where to go next

- [Custom Composer Tools and Building Your Own Composer](28-custom-composer-tools-and-composers.md)
- [Skills, Central Instructions and Tool Templates](22-skills-and-tool-templates.md)
- [How the Composers Work](15-how-composers-work.md)
- [The Orchestrator: Building Whole Apps](13-orchestrator.md)
- [Composer MCP: Skills, Prompts and the App Generator](20-composer-mcp-skills-and-prompts.md)
