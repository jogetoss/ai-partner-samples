package org.joget.samples.systemagent;

import java.util.Collection;
import org.joget.ai.agent.model.AgentLLM;
import org.joget.ai.agent.model.AgentToolAbstract;
import org.joget.ai.agent.model.Function;
import org.joget.ai.agent.model.Functions;
import org.joget.ai.agent.model.LLMConfig;
import org.joget.ai.agent.model.ToolExecution;
import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.dao.DatalistDefinitionDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.DatalistDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The agent's one tool: reads the forms and lists of a Joget app and returns every id that breaks the naming rule,
 * with a suggested replacement. Deterministic facts in, so the model only has to write them up.
 *
 * It must be a registered plugin (see Activator): Agent Builder rebuilds each tool of a run by class name.
 * Errors are returned as {"error": ...}; a thrown exception would end the whole task.
 */
public class NamingCheckTool extends AgentToolAbstract {

    public static final String FUNCTION = "check_app_naming";

    @Override
    public Functions getFunctions() {
        Function f = new Function(FUNCTION, "Check the naming of all forms and lists of a Joget app against the naming standard. "
                + "Returns {appId, appVersion, formsChecked, listsChecked, violations[{type, id, name, problems[], suggestion}]}.");
        f.addParameter("appId", Function.ParameterType.STRING, "Joget app id", true);
        f.addParameter("appVersion", Function.ParameterType.STRING, "App version number; empty for the published (or latest) version", false);
        Functions fs = new Functions();
        fs.add(f);
        return fs;
    }

    @Override
    public String execute(AgentLLM llm, LLMConfig config, ToolExecution call) {
        JSONObject args = call == null || call.getArguments() == null ? new JSONObject() : call.getArguments();
        String appId = args.optString("appId", "").trim();
        if (appId.isEmpty()) {
            return error("appId is required");
        }
        try {
            AppDefinitionDao appDao = (AppDefinitionDao) AppUtil.getApplicationContext().getBean("appDefinitionDao");
            Long version = parseVersion(args.optString("appVersion", ""));
            if (version == null) {
                version = appDao.getPublishedVersion(appId);
            }
            if (version == null) {
                version = appDao.getLatestVersion(appId);
            }
            AppDefinition app = version == null ? null : appDao.loadVersion(appId, version);
            if (app == null) {
                return error("App " + appId + " was not found");
            }
            JSONArray violations = new JSONArray();
            int forms = 0;
            int lists = 0;
            FormDefinitionDao formDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
            Collection<FormDefinition> formDefs = formDao.getFormDefinitionList(null, app, null, null, null, null);
            for (FormDefinition f : formDefs == null ? new java.util.ArrayList<FormDefinition>() : formDefs) {
                forms++;
                check(violations, "form", f.getId(), f.getName());
            }
            DatalistDefinitionDao listDao = (DatalistDefinitionDao) AppUtil.getApplicationContext().getBean("datalistDefinitionDao");
            Collection<DatalistDefinition> listDefs = listDao.getDatalistDefinitionList(null, app, null, null, null, null);
            for (DatalistDefinition l : listDefs == null ? new java.util.ArrayList<DatalistDefinition>() : listDefs) {
                lists++;
                check(violations, "list", l.getId(), l.getName());
            }
            return new JSONObject().put("appId", appId).put("appVersion", version).put("formsChecked", forms)
                    .put("listsChecked", lists).put("violations", violations).toString();
        } catch (RuntimeException e) {
            LogUtil.warn(NamingCheckTool.class.getName(), FUNCTION + " failed: " + e);
            return error("Check failed: " + e.getMessage());
        }
    }

    private static void check(JSONArray violations, String type, String id, String name) {
        java.util.List<String> problems = NamingRules.problems(id);
        if (!problems.isEmpty()) {
            violations.put(new JSONObject().put("type", type).put("id", id).put("name", name == null ? "" : name)
                    .put("problems", new JSONArray(problems)).put("suggestion", NamingRules.suggest(id)));
        }
    }

    private static Long parseVersion(String s) {
        try {
            return s == null || s.trim().isEmpty() ? null : Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String error(String message) {
        return new JSONObject().put("error", message).toString();
    }

    @Override
    public String getName() {
        return "NamingCheckTool";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getLabel() {
        return "App Naming Check";
    }

    @Override
    public String getDescription() {
        return "Lists the forms and lists of an app whose id breaks the naming standard";
    }

    @Override
    public String getClassName() {
        return NamingCheckTool.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        return "[]";
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-check-double\"></i>";
    }

    @Override
    public String getInfoTemplate() {
        return "<div><strong>check_app_naming</strong>(appId, appVersion)</div>";
    }
}
