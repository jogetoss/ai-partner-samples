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
