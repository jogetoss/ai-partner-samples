package org.joget.samples.checklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joget.ai.agent.model.AgentAbortedException;
import org.joget.ai.agent.model.AgentToolAbstract;
import org.joget.ai.agent.model.ToolExecution;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Plain JUnit: no Joget server, no Spring, no model. The composer class itself needs a server and is not tested here. */
class ChecklistToolTest {

    private static JSONObject call(ChecklistComposerAgentTool tool, String function, JSONObject args) {
        return new JSONObject(tool.execute(null, null, new ToolExecution(function, args)));
    }

    @AfterEach
    void clear() { // the state is in ThreadLocals: leave nothing for the next test
        ChecklistComposerAgentTool.CURRENT.remove();
        ChecklistComposerAgentTool.RESULT_HOLDER.remove();
        ChecklistComposerAgentTool.RUN_ID.remove();
    }

    @Test
    void isAHiddenSequentialAgentToolWithTwoFunctions() {
        ChecklistComposerAgentTool tool = new ChecklistComposerAgentTool();
        assertTrue(tool instanceof AgentToolAbstract);
        assertTrue(tool.isHidden());
        assertEquals(false, tool.isParallelSafe());
        assertEquals("set_checklist", tool.getFunctions().get(0).getName());
        assertEquals("add_item", tool.getFunctions().get(1).getName());
    }

    @Test
    void buildsANewChecklistStepByStep() {
        ChecklistComposerAgentTool tool = new ChecklistComposerAgentTool();
        ChecklistComposerAgentTool.CURRENT.set(new JSONObject().put("id", "").put("name", ""));
        assertEquals("success", call(tool, "set_checklist", new JSONObject().put("id", "onboarding").put("name", "Onboarding")).getString("status"));
        assertEquals(1, call(tool, "add_item", new JSONObject().put("id", "laptop").put("label", "Hand over the laptop")).getInt("items"));
        assertEquals(2, call(tool, "add_item", new JSONObject().put("id", "badge").put("label", "Print the badge")).getInt("items"));

        JSONObject result = ChecklistComposerAgentTool.RESULT_HOLDER.get();
        assertEquals("onboarding", result.getString("id"));
        assertEquals("Onboarding", result.getString("name"));
        assertEquals("badge", result.getJSONArray("items").getJSONObject(1).getString("id"));
    }

    @Test
    void startsFromTheCurrentArtifactAndKeepsItsIdWhenNoneIsGiven() {
        ChecklistComposerAgentTool tool = new ChecklistComposerAgentTool();
        ChecklistComposerAgentTool.CURRENT.set(new JSONObject("{\"id\":\"leave\",\"name\":\"Leave\",\"items\":[{\"id\":\"form\",\"label\":\"Fill in the form\"}]}"));
        call(tool, "set_checklist", new JSONObject().put("name", "Leave request"));
        JSONObject result = ChecklistComposerAgentTool.RESULT_HOLDER.get();
        assertEquals("leave", result.getString("id"));
        assertEquals("Leave request", result.getString("name"));
        assertEquals(1, result.getJSONArray("items").length());
    }

    @Test
    void errorsComeBackAsTextAndDoNotChangeTheResult() {
        ChecklistComposerAgentTool tool = new ChecklistComposerAgentTool();
        ChecklistComposerAgentTool.CURRENT.set(new JSONObject("{\"id\":\"leave\",\"name\":\"Leave\",\"items\":[{\"id\":\"form\",\"label\":\"Fill in the form\"}]}"));
        JSONObject duplicate = call(tool, "add_item", new JSONObject().put("id", "form").put("label", "Again"));
        assertEquals("error", duplicate.getString("status"));
        assertTrue(duplicate.getString("message").contains("already exists"));
        assertEquals("error", call(tool, "remove_everything", new JSONObject()).getString("status"));
        assertNull(ChecklistComposerAgentTool.RESULT_HOLDER.get());
    }

    @Test
    void aStoppedRunThrowsBeforeTheFunctionRuns() {
        ChecklistComposerAgentTool tool = new ChecklistComposerAgentTool();
        ChecklistComposerAgentTool.begin("RUN-1");
        try {
            ChecklistComposerAgentTool.RUN_ID.set("RUN-1");
            ChecklistComposerAgentTool.stop("RUN-1");
            assertThrows(AgentAbortedException.class,
                    () -> tool.execute(null, null, new ToolExecution("add_item", new JSONObject().put("id", "a").put("label", "A"))));
            assertNull(ChecklistComposerAgentTool.RESULT_HOLDER.get());
        } finally {
            ChecklistComposerAgentTool.end("RUN-1");
        }
    }
}
