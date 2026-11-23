package org.joget.samples.systemagent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.ConsolePagePluginAbstract;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Settings > Form Naming Auditor: the entry point that triggers the system agent.
 *
 * How Joget calls a console page: requests to /web/console/plugin/NAME first go through isAuthorized() (401 when
 * false). With no further path, render() supplies the page body. A longer path is matched against the @Path
 * patterns of the methods declared on this class and the method is invoked with the live request and response.
 * A system agent has no UI and no scheduler of its own: this page, a process tool, a servlet or a timer is what
 * decides when it runs.
 */
public class NamingAuditorPage extends ConsolePagePluginAbstract {

    public static final String NAME = "FormNamingAuditor";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Audits the form and list ids of an app with an Agent Builder system agent";
    }

    @Override
    public String getLabel() {
        return "Form Naming Auditor";
    }

    /** The Settings menu reads this; a console page is not PropertyEditable, so the default would show the name. */
    @Override
    public String getI18nLabel() {
        return getLabel();
    }

    @Override
    public String getClassName() {
        return NamingAuditorPage.class.getName();
    }

    @Override
    public String getPluginIcon() {
        return "<i class=\"fas fa-spell-check\"></i>";
    }

    @Override
    public int getOrder() {
        return 370;
    }

    @Override
    public Location getLocation() {
        return Location.SETTINGS;
    }

    @Override
    public boolean isAuthorized() {
        return WorkflowUtil.isCurrentUserInRole(WorkflowUtil.ROLE_ADMIN);
    }

    @Override
    public String render(HttpServletRequest request, HttpServletResponse response) {
        String base = request.getContextPath() + "/web/console/plugin/" + NAME;
        return resource("/templates/page.html").replace("__BASE__", base);
    }

    /** GET .../api/apps lists the apps the dropdown offers. */
    @Path("/api/apps")
    public void apiApps(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONArray apps = new JSONArray();
        AppDefinitionDao dao = (AppDefinitionDao) AppUtil.getApplicationContext().getBean("appDefinitionDao");
        Collection<AppDefinition> published = dao.findPublishedApps("name", false, null, null);
        if (published != null) {
            for (AppDefinition a : published) {
                apps.put(new JSONObject().put("id", a.getAppId()).put("version", a.getVersion()).put("name", a.getName()));
            }
        }
        write(response, 200, new JSONObject().put("apps", apps));
    }

    /**
     * GET .../api/audit?appId=...&amp;lang=en|id runs the agent and waits for the report. It is a GET to keep the
     * sample small; a real page that triggers a billable action should POST with the CSRF token.
     */
    @Path("/api/audit")
    public void apiAudit(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String appId = request.getParameter("appId");
        if (appId == null || appId.trim().isEmpty()) {
            write(response, 400, new JSONObject().put("error", "appId is required"));
            return;
        }
        try {
            NamingAuditor.Result r = NamingAuditor.audit(appId.trim(), request.getParameter("lang"));
            write(response, 200, new JSONObject().put("ok", r.ok).put("runId", r.runId).put("report", r.report));
        } catch (LinkageError e) {
            write(response, 409, new JSONObject().put("error", "Agent Builder is not installed or cannot be linked: " + e));
        } catch (RuntimeException e) {
            LogUtil.warn(NamingAuditorPage.class.getName(), "Audit failed: " + e);
            write(response, 500, new JSONObject().put("error", String.valueOf(e.getMessage())));
        }
    }

    private static void write(HttpServletResponse response, int status, JSONObject body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(body.toString());
    }

    private static String resource(String path) {
        try (InputStream in = NamingAuditorPage.class.getResourceAsStream(path)) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
