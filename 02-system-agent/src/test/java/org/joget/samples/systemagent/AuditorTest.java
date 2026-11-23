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
