# System Agents (continued)

This page continues [System Agents](27-system-agents.md). It covers the ways to start a system agent (a Settings page, a process tool, a timer, an API call), how to show progress in a user interface, an outline of a second agent (an implementation reviewer), testing and packaging, limitations and troubleshooting. It is for plugin developers. The working sample is the folder `02-system-agent` of the examples repository (see [The Examples Repository](29-examples-repository.md)).

## Triggers

A system agent has no user interface, schedule or endpoint of its own. Code that you write builds a request and calls `run`.

| Trigger | How | Watch for |
|---|---|---|
| Settings page | A `ConsolePagePluginAbstract` in **Settings** (the walkthrough on [System Agents](27-system-agents.md)) | `isAuthorized()` decides who can open it and call its endpoints. A run takes seconds to minutes. The sample page waits for the answer in one request; for longer runs start the run on a background thread and poll (see "Streaming progress to a UI"). |
| Process tool | A `DefaultApplicationPlugin` that calls your code and stores the result in a workflow variable | The process waits until the run ends |
| Timer | A `ScheduledExecutorService` started in the activator and stopped in `stop()` | A timer thread has no user and no app. In a cluster every node runs its own timer. |
| API call | An endpoint of your console page, for example `/jw/web/console/plugin/FormNamingAuditor/api/audit?appId=...&lang=en` called with an administrator session | It is a GET in the sample; use POST with the CSRF token for work that costs money |

The process tool and the timer below are not part of the sample. They call `NamingAuditor.audit(appId, language)` of the sample, which records the trigger source `NAMING_AUDITOR_PAGE`; give `audit` a parameter for the trigger source when you add triggers. Register the process tool in the activator with `registrations.add(context.registerService(NamingAuditProcessTool.class.getName(), new NamingAuditProcessTool(), null));`.

```java
package org.joget.samples.systemagent;

import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;

/** A process tool: runs the audit and stores "OK" or "FAILED" in a workflow variable. */
public class NamingAuditProcessTool extends DefaultApplicationPlugin {

    @Override
    public String getName() {
        return "NamingAuditProcessTool";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Runs the form naming audit for an app and stores the verdict in a workflow variable";
    }

    @Override
    public String getLabel() {
        return "Form Naming Audit";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "[{\"title\":\"Form Naming Audit\",\"properties\":["
                + "{\"name\":\"appId\",\"label\":\"App ID\",\"type\":\"textfield\",\"required\":\"true\"},"
                + "{\"name\":\"resultVariable\",\"label\":\"Workflow Variable for the Verdict\",\"type\":\"textfield\"}"
                + "]}]";
    }

    @Override
    public Object execute(Map properties) {
        NamingAuditor.Result result = NamingAuditor.audit(getPropertyString("appId"), "en");
        WorkflowAssignment assignment = (WorkflowAssignment) properties.get("workflowAssignment");
        String variable = getPropertyString("resultVariable");
        if (assignment != null && variable != null && !variable.isEmpty()) {
            WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
            workflowManager.activityVariable(assignment.getActivityId(), variable, result.ok ? "OK" : "FAILED");
        }
        return result.ok ? result.report : null;
    }
}
```

A timer. It sets the user for the thread, because a timer thread has none and the run would be recorded for `roleAnonymous`. In a cluster run it on one node only, or check **Agent Execution Audit Trail** for a recent run before you start one.

```java
package org.joget.samples.systemagent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.model.service.WorkflowUserManager;

/** Audits one app every day. Call start() in Activator.start() and stop() in Activator.stop(). */
final class AuditScheduler {

    private ScheduledExecutorService timer;

    void start(String appId, String runAsUser) {
        timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "naming-audit-timer");
            t.setDaemon(true);
            return t;
        });
        timer.scheduleAtFixedRate(() -> {
            try {
                WorkflowUserManager users = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
                users.setCurrentThreadUser(runAsUser);
                NamingAuditor.audit(appId, "en");
            } catch (RuntimeException | LinkageError e) {
                LogUtil.warn(AuditScheduler.class.getName(), "Scheduled audit failed: " + e);
            }
        }, 1, 24, TimeUnit.HOURS);
    }

    void stop() {
        if (timer != null) {
            timer.shutdownNow();
        }
    }
}
```

## Streaming progress to a UI

A run publishes a trace event whenever a task, prompt, model call or tool call starts or ends. `agent.subscribe(runId, consumer)` delivers each event as JSON text to your consumer.

| Field | Content |
|---|---|
| `type` | `Task`, `Prompt`, `LLM`, `Tool`, `Decision`, `Enhancer`, `LLMMessage` or `End` |
| `label` | The name of the element, for example the tool name; for `LLMMessage` the model's text |
| `status` | `RUNNING`, `SUCCESS`, `FAILED`, `ABORTED`, `END` or `WAITING_INPUT` |
| `nodeId`, `parentId`, `runId`, `createdAt` | Identify and order the event |
| `request`, `response` | The input and output of the element. They can be large: send only what the page needs. |

Rules: subscribe **before** `run` (no replay). Events arrive on the thread that runs the agent, so keep the consumer short. Unsubscribe in a `finally` block; `unsubscribe` also removes every other listener of the run, so use one subscriber per run. Each task of a `runAll` ends with its own `End` event: the end of the run is the return of `runAll`. The bus lives in the memory of one node. A system agent run does not stream model tokens.

The pattern for a page: start the run on a background thread (`PluginThread`), collect readable lines in a list, and let the page poll an endpoint for lines after the last index. The sample page does not do this: it waits for the complete answer. To add progress to the sample, change `NamingAuditor.audit` so that the caller supplies the run id and a consumer, and subscribe around `run`:

```java
// the signature (keep a two-argument method that passes "RUN-" + UUID.randomUUID() and event -> { })
public static Result audit(String appId, String language, String runId, Consumer<String> progress) {

// use the run id of the caller
req.setRunId(runId);

// subscribe before run, unsubscribe after it
NamingAuditorAgent agent = new NamingAuditorAgent();
UUID subscription = agent.subscribe(runId, progress);
Response r;
try {
    r = agent.run(req, props);
} finally {
    agent.unsubscribe(runId, subscription);
}
```

A class such as `AuditRuns` then starts the audit on a `PluginThread`, keeps the progress lines and drops finished runs after ten minutes:

```java
package org.joget.samples.systemagent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.json.JSONObject;

/**
 * Runs audits on a background thread and keeps the progress lines for polling. The state is in the memory of
 * this node only: a poll must reach the node that started the run.
 */
final class AuditRuns {

    static final class Run {
        final List<String> lines = Collections.synchronizedList(new ArrayList<String>());
        volatile boolean done;
        volatile NamingAuditor.Result result;
        volatile String error = "";
        volatile long finishedAt;
    }

    private static final Map<String, Run> RUNS = new ConcurrentHashMap<>();
    private static final long KEEP_MILLIS = 10 * 60 * 1000L;

    private AuditRuns() {
    }

    static Run get(String runId) {
        return runId == null ? null : RUNS.get(runId);
    }

    /** Starts the audit and returns at once. */
    static String start(String appId, String language) {
        long limit = System.currentTimeMillis() - KEEP_MILLIS;
        RUNS.values().removeIf(r -> r.done && r.finishedAt < limit);
        final String runId = "RUN-" + UUID.randomUUID();
        final Run run = new Run();
        RUNS.put(runId, run);
        new PluginThread(() -> {
            try {
                run.result = NamingAuditor.audit(appId, language, runId, json -> {
                    String line = describe(json);
                    if (line != null) {
                        run.lines.add(line);
                    }
                });
            } catch (RuntimeException | LinkageError e) {
                LogUtil.warn(AuditRuns.class.getName(), "Audit " + runId + " failed: " + e);
                run.error = String.valueOf(e.getMessage());
            } finally {
                run.finishedAt = System.currentTimeMillis();
                run.done = true;
            }
        }).start();
        return runId;
    }

    /** One readable line for the events a user cares about; null for the rest. */
    static String describe(String eventJson) {
        try {
            JSONObject event = new JSONObject(eventJson);
            String type = event.optString("type");
            if (!("Task".equals(type) || "Tool".equals(type) || "LLM".equals(type))) {
                return null;
            }
            return type + " " + event.optString("label") + ": " + event.optString("status");
        } catch (RuntimeException e) {
            return null;
        }
    }
}
```

The page then needs two endpoints in place of `/api/audit`. Add them to `NamingAuditorPage`; they use the method `write` of that class:

```java
    /** GET .../api/start?appId=...&amp;lang=en|id starts the audit on a background thread and returns its run id. */
    @Path("/api/start")
    public void apiStart(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String appId = request.getParameter("appId");
        if (appId == null || appId.trim().isEmpty()) {
            write(response, 400, new JSONObject().put("error", "appId is required"));
            return;
        }
        String runId = AuditRuns.start(appId.trim(), request.getParameter("lang"));
        write(response, 200, new JSONObject().put("runId", runId));
    }

    /** GET .../api/progress?runId=...&amp;from=N returns the progress lines from index N on, and the result when done. */
    @Path("/api/progress")
    public void apiProgress(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AuditRuns.Run run = AuditRuns.get(request.getParameter("runId"));
        if (run == null) {
            write(response, 404, new JSONObject().put("error", "unknown run"));
            return;
        }
        int from = 0;
        try {
            from = Math.max(0, Integer.parseInt(request.getParameter("from")));
        } catch (NumberFormatException ignored) {
            // start from the first line
        }
        JSONArray lines = new JSONArray();
        int next;
        synchronized (run.lines) {
            for (int i = from; i < run.lines.size(); i++) {
                lines.put(run.lines.get(i));
            }
            next = run.lines.size();
        }
        JSONObject body = new JSONObject().put("lines", lines).put("next", next).put("done", run.done);
        if (run.done) {
            NamingAuditor.Result r = run.result;
            body.put("ok", r != null && r.ok).put("report", r == null ? "" : r.report).put("error", run.error);
        }
        write(response, 200, body);
    }
```

The script of the page calls `/api/start`, then `/api/progress` every second with the `next` value of the last answer as `from`, until `done` is true.

## Second idea: the implementation reviewer (outline)

The same pattern fits an "implementation reviewer" that reviews an app against company standards. Java collects facts, the model judges them against a written standard, and findings are stored by a tool.

1. **Standard as data.** Keep the company standard as rules with ids (for example "every list has a search filter") in a form, a file or a skill. Give the agent only the rules for the artifact type it reviews.
2. **Read tools.** One registered tool per artifact type reads definitions through the Joget DAOs, as `NamingCheckTool` does, and returns compact JSON. Never send whole app definitions to the model.
3. **A `record_finding` tool.** The model reports each problem by calling `record_finding(artifactId, rule, severity, message)`. The tool validates the arguments, returns `{"error": ...}` for bad ones, and stores the finding keyed by `config.getRunId()` (`RecordFindingTool`, registered like any tool). The findings are the result; the model's text is only a summary.
4. **Tasks.** Two parallel tasks review forms and lists, and a dependent summary task runs only when a finding was recorded. `ReviewerRequest` builds it:

```java
package org.joget.samples.systemagent;

import java.util.List;
import java.util.Map;
import org.joget.ai.agent.core.AgentFactory;
import org.joget.ai.agent.model.AgentComponent;
import org.joget.ai.agent.model.AgentLLM;
import org.joget.ai.agent.model.AgentPromptAbstract;
import org.joget.ai.agent.model.AgentToolAbstract;
import org.joget.ai.agent.model.SystemAgentRequest;
import org.joget.plugin.base.PluginManager;

/** Two reviews in parallel, then a summary that runs only when a finding was recorded. */
final class ReviewerRequest {

    private ReviewerRequest() {
    }

    static AgentPromptAbstract user(PluginManager pm, String text) {
        return AgentFactory.create(pm, AgentComponent.TextPromptAgentPrompt, Map.of("msgRole", "USER", "prompt", text));
    }

    static SystemAgentRequest build(PluginManager pm, AgentLLM llm, String runId, String appId) {
        AgentPromptAbstract persona = AgentFactory.create(pm, AgentComponent.PersonaAgentPrompt, Map.of(
                "role", "Joget implementation reviewer",
                "backstory", "You check an app against the company standard. Call record_finding once per problem."));
        List<AgentToolAbstract> tools = List.of(new RecordFindingTool());

        SystemAgentRequest request = SystemAgentRequest.builder()
                .llm(llm)
                .systemPrompts(List.of(persona))
                .addParallelTask("forms", List.of(user(pm, "Review the forms of app " + appId + ".")), tools)
                .addParallelTask("lists", List.of(user(pm, "Review the lists of app " + appId + ".")), tools)
                .endParallelGroup()
                .addDependentTask("summary", null,
                        previous -> List.of(user(pm, "Write the management summary of the "
                                + RecordFindingTool.count(runId) + " findings recorded for app " + appId + ".")),
                        List.<AgentToolAbstract>of(),
                        () -> RecordFindingTool.count(runId) > 0,       // skipped when nothing was recorded
                        "forms", "lists")
                .build();
        request.setRunId(runId);
        return request;
    }
}
```

5. **Success.** The review succeeded when every task returned a non-failed response **and** the tools were called. Do not trust text alone.
6. **Triggers.** A Settings page for ad hoc reviews, a process tool at "ready for release", a timer for a weekly review. Pass a different `triggerSource` for each.
7. **Output.** Store the findings in a form (one row per finding, with the run id) so that implementers can filter and close them.

## Testing and packaging

Most parts can be tested without Joget and without a model: the rule, the suggested ids, the identity of the agent and the tool's error path are plain Java. Test the model call by hand. This is the test class of the sample:

```java
package org.joget.samples.systemagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.joget.ai.agent.model.AgentToolAbstract;
import org.joget.ai.agent.model.ToolExecution;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Plain JUnit, no Joget and no model. */
class AuditorTest {

    @Test
    void goodIdsPassAndBadIdsAreExplained() {
        assertTrue(NamingRules.problems("employee_leave_request").isEmpty());
        assertEquals(1, NamingRules.problems("EmployeeLeave").size());
        assertTrue(NamingRules.problems("a__b").stream().anyMatch(p -> p.contains("double underscore")));
        assertTrue(NamingRules.problems("x".repeat(41)).stream().anyMatch(p -> p.contains("longer than")));
        assertEquals(List.of("id is empty"), NamingRules.problems(""));
    }

    @Test
    void suggestionsFollowTheConvention() {
        assertEquals("employee_leave_request", NamingRules.suggest("employeeLeaveRequest"));
        assertEquals("employee_leave_request", NamingRules.suggest("Employee-Leave Request"));
        assertEquals("f_1st_form", NamingRules.suggest("1st form"));
        assertTrue(NamingRules.problems(NamingRules.suggest("Some Very-Odd__Name!!")).isEmpty());
    }

    @Test
    void agentIdentityIsAClassLiteralAndToolIsAnAgentTool() throws Exception {
        assertEquals("org.joget.samples.systemagent.NamingAuditorAgent", NamingAuditorAgent.AGENT_ID);
        AgentToolAbstract tool = new NamingCheckTool();
        assertEquals(NamingCheckTool.FUNCTION, tool.getFunctions().get(0).getName());
        // a missing argument is an error JSON, not an exception
        JSONObject r = new JSONObject(tool.execute(null, null, new ToolExecution(NamingCheckTool.FUNCTION, new JSONObject())));
        assertTrue(r.has("error"));
    }
}
```

Manual test: run the page for an app you know, then open **Monitor > Agent Execution Audit Trail**. The row **Form Naming Auditor** must show **Agent Type** AI Composer, the **Trigger Source** `NAMING_AUDITOR_PAGE` and non-zero tokens, and the trace must show the call of `check_app_naming`. Test the failures too: with a wrong key in the AI Service, and after an **Abort** of a running audit, the page must show `The AI service did not return a report. Run id: ...` and not the `Done` line.

Packaging: `mvn clean package "-Djoget.version=9.1.2"` gives one small bundle jar, `target/form-naming-auditor-1.0-SNAPSHOT.jar`. Upload it in **Settings > Manage Plugins** after Agent Builder is active; the list then shows **Form Naming Auditor (system agent)**, also when you select **Console Page** or **AI Agent Tool** in **Filter by Type**. Without Agent Builder the bundle still starts, and the page answers `Agent Builder is not installed or cannot be linked`.

## Limitations and things to know

- **A tool that throws fails its task.** Return `{"error": ...}`.
- **`dependsOn` does not check success.** Use a `runCondition` on your own flag.
- **No guards, decisions or memory** in the request API, and **no token streaming**: progress is trace events.
- **Settings are fixed:** timeouts, retries and tool-call depth use the defaults, and the history shows **Agent Type** AI Composer for all system agents.
- **Abort.** An administrator aborts from the history page; the run then returns `null`. Code has no public abort call.
- **Cost.** Every run, and every task of a run, calls the model. Limit the page to administrators and use POST.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| A run ends at once with a link error (`NoClassDefFoundError`) | Agent Builder is missing or not active, or a package is not imported | Install Agent Builder first. Check `DynamicImport-Package: *`. |
| "AgentTool class could not be loaded: ..." | The tool is not registered as a plugin | Register it in the activator with `registerService(Tool.class.getName(), ...)` |
| "No active AI service in AI Central Config" | No service is marked active | Create a service and select it as **Active AI Service** |
| "No plugin found for provider: ..." | The service's provider has no LLM plugin | See [Adding a Custom LLM Provider](26-custom-llm-providers.md) |
| `run` returns `null` | Aborted, or an engine error | Check the history for ABORTED and the Joget log |
| The report is invented | The model did not call the tool | Check a flag that your tool sets (see "What run returns, and how to handle it safely" on [System Agents](27-system-agents.md)), make the persona say "always call ... first", use a model with tool calling |
| The run has no user or app in history | Timer or pool thread | Pass `username`, `fullName` and `appId` in `properties`; set the thread user in a timer |
| No progress lines | Subscribed after `run`, or the consumer threw | Subscribe before `run`; catch exceptions in the consumer |
| A dependent task ran after a failed task | `dependsOn` only needs the task to return | Use `runCondition` |
| Tokens are zero | The server does not return `usage` | See [Adding a Custom LLM Provider](26-custom-llm-providers.md) |

## Where to go next

- [System Agents](27-system-agents.md)
- [Writing Custom Tools](24-custom-tools.md)
- [Adding a Custom LLM Provider](26-custom-llm-providers.md)
- [Skills, Central Instructions and Tool Templates](22-skills-and-tool-templates.md)
- [The Examples Repository](29-examples-repository.md)
