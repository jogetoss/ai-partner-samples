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
