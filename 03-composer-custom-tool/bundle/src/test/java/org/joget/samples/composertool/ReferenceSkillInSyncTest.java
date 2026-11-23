package org.joget.samples.composertool;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * 04-custom-skills/acme-standards-reference is a static copy of the bundled standards for clients that cannot call the
 * tool (Claude Code over Composer MCP). This test keeps the two from drifting. It is skipped when the skill folder is not
 * next to this project (for example when the bundle is built on its own).
 */
class ReferenceSkillInSyncTest {

    private static final Path SKILL = Paths.get("..", "..", "04-custom-skills", "acme-standards-reference", "SKILL.md");

    @Test
    void everyOptionOfTheBundledStandardsIsInTheReferenceSkill() throws IOException {
        assumeTrue(Files.exists(SKILL), "reference skill not found at " + SKILL.toAbsolutePath());
        String skill = new String(Files.readAllBytes(SKILL), StandardCharsets.UTF_8);
        StandardsCatalogue catalogue = StandardsCatalogue.load(null);
        for (String topic : new String[] {"cost_centres", "departments", "leave_types", "request_status"}) {
            JSONArray options = catalogue.topic(topic).getJSONArray("options");
            for (int i = 0; i < options.length(); i++) {
                JSONObject o = options.getJSONObject(i);
                String row = "| " + o.getString("value") + " | " + o.getString("label") + " |";
                assertTrue(skill.contains(row), "reference skill misses the row " + row + " of topic " + topic);
            }
        }
    }

    @Test
    void approvalMatrixThresholdsAreInTheReferenceSkill() throws IOException {
        assumeTrue(Files.exists(SKILL), "reference skill not found at " + SKILL.toAbsolutePath());
        String skill = new String(Files.readAllBytes(SKILL), StandardCharsets.UTF_8);
        assertTrue(skill.contains("| 5.000.000 | line_manager |"));
        assertTrue(skill.contains("| 50.000.000 | line_manager, department_head |"));
        assertTrue(skill.contains("finance_director"));
    }
}
