package org.joget.samples.composertool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joget.ai.agent.model.Function;
import org.joget.ai.agent.model.ToolExecution;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Plain JUnit: no Joget server, no Spring, no LLM. Properties are set one key at a time with setProperty, because
 * ExtDefaultPlugin.setProperties(Map) needs the Spring application context.
 */
class AcmeStandardsToolTest {

    private static String run(AcmeStandardsTool tool, String topic) {
        JSONObject args = new JSONObject();
        if (topic != null) {
            args.put("topic", topic);
        }
        return tool.execute(null, null, new ToolExecution(AcmeStandardsTool.DEFAULT_FUNCTION, args));
    }

    @Test
    void publishesOneFunctionWithTheTopicsAsAnEnum() {
        Function f = new AcmeStandardsTool().getFunctions().get(0);
        assertEquals("lookup_acme_standard", f.getName());
        String schema = f.toString();
        assertTrue(schema.contains("cost_centres"));
        assertTrue(schema.contains("leave_types"));
        assertTrue(schema.contains("approval_matrix"));
    }

    @Test
    void functionNameAndDescriptionComeFromTheTemplate() {
        AcmeStandardsTool tool = new AcmeStandardsTool();
        tool.setProperty("name", "acme_lookup");
        tool.setProperty("description", "Custom description");
        Function f = tool.getFunctions().get(0);
        assertEquals("acme_lookup", f.getName());
        assertEquals("Custom description", f.getDescription());
    }

    @Test
    void optionListsKeepEnglishValuesAndBahasaLabels() {
        JSONObject out = new JSONObject(run(new AcmeStandardsTool(), "leave_types"));
        assertEquals("leave_types", out.getString("topic"));
        JSONArray options = out.getJSONArray("options");
        assertEquals("annual", options.getJSONObject(0).getString("value"));
        assertEquals("Cuti Tahunan", options.getJSONObject(0).getString("label"));
    }

    @Test
    void costCentresReturnCodeAsValue() {
        JSONArray options = new JSONObject(run(new AcmeStandardsTool(), "cost_centres")).getJSONArray("options");
        assertEquals("CC-1000", options.getJSONObject(0).getString("value"));
        assertTrue(options.getJSONObject(0).getString("label").contains("Jakarta"));
    }

    @Test
    void approvalMatrixHasThreeRules() {
        JSONObject out = new JSONObject(run(new AcmeStandardsTool(), "approval_matrix"));
        assertEquals(3, out.getJSONArray("rules").length());
    }

    @Test
    void unknownTopicIsAnErrorStringListingTheTopics() {
        JSONObject out = new JSONObject(run(new AcmeStandardsTool(), "salaries"));
        assertTrue(out.getString("error").contains("Unknown topic"));
        assertTrue(out.getString("error").contains("departments"));
    }

    @Test
    void missingArgumentsNeverThrow() {
        assertTrue(new JSONObject(new AcmeStandardsTool().execute(null, null, null)).has("error"));
        assertTrue(new JSONObject(run(new AcmeStandardsTool(), null)).has("error"));
    }

    @Test
    void overrideReplacesATopicAndKeepsTheOthers() {
        AcmeStandardsTool tool = new AcmeStandardsTool();
        tool.setProperty("overrideJson",
                "{\"departments\":{\"options\":[{\"value\":\"legal\",\"label\":\"Hukum\"}]}}");
        JSONArray departments = new JSONObject(run(tool, "departments")).getJSONArray("options");
        assertEquals(1, departments.length());
        assertEquals("legal", departments.getJSONObject(0).getString("value"));
        assertEquals("annual",
                new JSONObject(run(tool, "leave_types")).getJSONArray("options").getJSONObject(0).getString("value"));
    }

    @Test
    void overrideCanAddANewTopic() {
        AcmeStandardsTool tool = new AcmeStandardsTool();
        tool.setProperty("overrideJson", "{\"regions\":{\"options\":[{\"value\":\"java\",\"label\":\"Jawa\"}]}}");
        assertFalse(new JSONObject(run(tool, "regions")).has("error"));
    }

    @Test
    void invalidOverrideJsonIsReportedNotThrown() {
        AcmeStandardsTool tool = new AcmeStandardsTool();
        tool.setProperty("overrideJson", "{not json");
        assertTrue(new JSONObject(run(tool, "departments")).getString("error").contains("not valid"));
    }

    @Test
    void identityMethodsAreConsistent() {
        AcmeStandardsTool tool = new AcmeStandardsTool();
        assertEquals(AcmeStandardsTool.class.getName(), tool.getClassName());
        assertTrue(tool.getInfoTemplate().contains("lookup_acme_standard"));
    }
}
