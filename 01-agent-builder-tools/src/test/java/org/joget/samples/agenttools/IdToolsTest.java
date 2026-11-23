package org.joget.samples.agenttools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.joget.ai.agent.model.AgentEnhancerAbstract;
import org.joget.ai.agent.model.AgentGuardAbstract;
import org.joget.ai.agent.model.AgentToolAbstract;
import org.joget.ai.agent.model.GuardResult;
import org.joget.ai.agent.model.LLMConfig;
import org.joget.ai.agent.model.Messages;
import org.joget.ai.agent.model.Response;
import org.joget.ai.agent.model.ToolExecution;
import org.joget.plugin.base.ExtDefaultPlugin;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Plain JUnit: no Joget server, no Spring, no model. Properties are set one key at a time because
 * ExtDefaultPlugin.setProperties needs the Spring context (the runtime itself calls it with the palette values).
 */
class IdToolsTest {

    // Jakarta (31), regency 74, district 01, born 15 Jan 1990, serial 0001. Women add 40 to the day: 55.
    static final String NIK_MALE = "3174011501900001";
    static final String NIK_FEMALE = "3174015501900001";

    private static <T extends ExtDefaultPlugin> T with(T plugin, Object... kv) {
        for (int i = 0; i < kv.length; i += 2) {
            plugin.setProperty((String) kv[i], kv[i + 1]);
        }
        return plugin;
    }

    private static JSONObject run(IndonesianIdValidatorTool tool, String type, String number) {
        JSONObject args = new JSONObject().put("idType", type).put("number", number);
        return new JSONObject(tool.execute(null, new LLMConfig(), new ToolExecution(IndonesianIdValidatorTool.FUNCTION, args)));
    }

    // ---- tool ----------------------------------------------------------------------------------------------

    @Test
    void registersAsAnAgentTool() {
        IndonesianIdValidatorTool tool = new IndonesianIdValidatorTool();
        assertTrue(tool instanceof AgentToolAbstract);
        assertEquals(IndonesianIdValidatorTool.FUNCTION, tool.getFunctions().get(0).getName());
    }

    @Test
    void decodesAMaleNikAndMasksByDefault() {
        JSONObject r = run(with(new IndonesianIdValidatorTool(), "mode", "strict"), "NIK", "3174.0115.0190.0001");
        assertTrue(r.getBoolean("valid"));
        assertEquals("DKI Jakarta", r.getString("province"));
        assertEquals("1990-01-15", r.getString("birthDate"));
        assertEquals("male", r.getString("gender"));
        assertEquals("317401******0001", r.getString("number"));
    }

    @Test
    void decodesAFemaleNikAndReturnsFullNumberWhenAllowed() {
        JSONObject r = run(with(new IndonesianIdValidatorTool(), "returnFullNumber", "true"), "nik", NIK_FEMALE);
        assertEquals("female", r.getString("gender"));
        assertEquals("1990-01-15", r.getString("birthDate"));
        assertEquals(NIK_FEMALE, r.getString("number"));
    }

    @Test
    void strictRejectsUnknownProvinceAndImpossibleDateButFormatModeAccepts() {
        IndonesianIdValidatorTool strict = with(new IndonesianIdValidatorTool(), "mode", "strict");
        assertFalse(run(strict, "NIK", "9974011501900001").getBoolean("valid"));
        assertTrue(run(strict, "NIK", "3174013113900001").getString("reason").contains("birth date"));
        assertTrue(run(with(new IndonesianIdValidatorTool(), "mode", "format"), "NIK", "9974011501900001").getBoolean("valid"));
    }

    @Test
    void extraProvinceGridExtendsTheBuiltInTable() {
        Map<String, String> row = new HashMap<>();
        row.put("code", "99");
        row.put("name", "Provinsi Contoh");
        IndonesianIdValidatorTool tool = with(new IndonesianIdValidatorTool(), "mode", "strict", "extraProvinces", new Object[] {row});
        assertEquals("Provinsi Contoh", run(tool, "NIK", "9974011501900001").getString("province"));
    }

    @Test
    void validatesNpwpInBothLengths() {
        IndonesianIdValidatorTool tool = with(new IndonesianIdValidatorTool(), "mode", "strict", "returnFullNumber", "true");
        JSONObject r = run(tool, "NPWP", "09.254.294.4-407.000");
        assertTrue(r.getBoolean("valid"));
        assertEquals("09.254.294.4-407.000", r.getString("number"));
        assertEquals("407", r.getString("taxOfficeCode"));
        assertFalse(run(tool, "NPWP", "09.254.294.4-000.000").getBoolean("valid"));
        assertTrue(run(tool, "NPWP", NIK_MALE).getBoolean("valid"));
        assertFalse(run(tool, "NPWP", "12345").getBoolean("valid"));
    }

    @Test
    void sixteenDigitNpwpStartingWithZeroIsCheckedAsTheOldFifteenDigits() {
        // Companies and non-residents: 0 in front of the old 15 digit NPWP. It is not a NIK.
        IndonesianIdValidatorTool strict = with(new IndonesianIdValidatorTool(), "mode", "strict", "returnFullNumber", "true");
        JSONObject r = run(strict, "NPWP", "0092542944407000");
        assertTrue(r.getBoolean("valid"), r.toString());
        assertEquals("0092542944407000", r.getString("number"));
        assertEquals("407", r.getString("taxOfficeCode"));
        assertEquals("000", r.getString("branchCode"));
        assertTrue(r.getString("reason").contains("starting with 0"));
        // typed with separators, masked by default
        JSONObject masked = run(with(new IndonesianIdValidatorTool(), "mode", "strict"), "NPWP", "0092.5429.4440.7000");
        assertTrue(masked.getBoolean("valid"));
        assertEquals("009254*******000", masked.getString("number"));
        // the strict tax office rule applies to the 15 digits after the 0; format mode accepts
        assertFalse(run(strict, "NPWP", "0092542944000000").getBoolean("valid"));
        assertTrue(run(with(new IndonesianIdValidatorTool(), "mode", "format"), "NPWP", "0092542944000000").getBoolean("valid"));
    }

    @Test
    void sixteenDigitNpwpNotStartingWithZeroIsCheckedAsANik() {
        IndonesianIdValidatorTool tool = with(new IndonesianIdValidatorTool(), "mode", "strict");
        JSONObject ok = run(tool, "NPWP", NIK_FEMALE);
        assertTrue(ok.getBoolean("valid"));
        assertEquals("317401******0001", ok.getString("number"));
        assertFalse(ok.has("taxOfficeCode"));
        assertFalse(run(tool, "NPWP", "9974011501900001").getBoolean("valid"), "unknown province: not a NIK");
        assertFalse(run(tool, "NPWP", "3174013113900001").getBoolean("valid"), "impossible birth date: not a NIK");
        assertFalse(run(tool, "NPWP", "00925429444070001").getBoolean("valid"), "17 digits");
    }

    @Test
    void guardAndEnhancerLeaveASixteenDigitCompanyNpwpAlone() {
        Response r = new Response("NPWP 0092542944407000 valid.");
        assertEquals(GuardResult.Action.PASS, with(new NikPiiGuard(), "onViolation", "block").filterOutput(r, new LLMConfig()).getAction());
        with(new NpwpFormatEnhancer(), "notice", "").enhance(null, new LLMConfig(), r);
        assertEquals("NPWP 0092542944407000 valid.", r.getContent());
    }

    @Test
    void policyAndBadInputReturnErrorJsonInsteadOfThrowing() {
        IndonesianIdValidatorTool nikOnly = with(new IndonesianIdValidatorTool(), "idTypes", "NIK");
        assertTrue(run(nikOnly, "NPWP", "092542944407000").getString("error").contains("only allowed"));
        assertTrue(run(nikOnly, "passport", "X").has("error"));
        assertTrue(new JSONObject(nikOnly.execute(null, null, null)).has("error"));
    }

    // ---- guard ---------------------------------------------------------------------------------------------

    @Test
    void guardBlocksSanitizesAndIgnoresLookalikes() {
        assertTrue(new NikPiiGuard() instanceof AgentGuardAbstract);
        Response leaky = new Response("Nomor KTP Anda " + NIK_MALE + " valid.");

        GuardResult blocked = with(new NikPiiGuard(), "onViolation", "block").filterOutput(leaky, new LLMConfig());
        assertEquals(GuardResult.Action.BLOCK, blocked.getAction());

        GuardResult masked = with(new NikPiiGuard(), "onViolation", "sanitize").filterOutput(leaky, new LLMConfig());
        assertEquals(GuardResult.Action.SANITIZE, masked.getAction());
        assertEquals("Nomor KTP Anda 317401******0001 valid.", leaky.getContent(), "sanitize must mutate the response");

        // 16 digits that are not a NIK (province 99) and a 15 digit number are not touched
        Response ok = new Response("Order 9974011501900001 and 092542944407000");
        assertEquals(GuardResult.Action.PASS, with(new NikPiiGuard(), "onViolation", "block").filterOutput(ok, new LLMConfig()).getAction());
    }

    @Test
    void guardInputSideAndDirectionSwitch() {
        Messages in = new Messages();
        in.addMessage("SYSTEM", "You are an assistant. NIK " + NIK_MALE);
        in.addMessage("USER", "Cek KTP saya " + NIK_MALE);
        NikPiiGuard inputGuard = with(new NikPiiGuard(), "onViolation", "block", "direction", "input");
        assertEquals(GuardResult.Action.BLOCK, inputGuard.filterInput(in, new LLMConfig()).getAction());
        // direction=output: the same request passes
        assertEquals(GuardResult.Action.PASS, with(new NikPiiGuard(), "onViolation", "block", "direction", "output")
                .filterInput(in, new LLMConfig()).getAction());
    }

    // ---- enhancer ------------------------------------------------------------------------------------------

    @Test
    void enhancerFormatsNpwpAndAppendsTheNoticeOnce() {
        assertTrue(new NpwpFormatEnhancer() instanceof AgentEnhancerAbstract);
        NpwpFormatEnhancer enhancer = with(new NpwpFormatEnhancer(), "notice", NpwpFormatEnhancer.DEFAULT_NOTICE);
        Response r = new Response("NPWP 092542944407000 sudah valid.");
        enhancer.enhance(null, new LLMConfig(), r);
        assertTrue(r.getContent().startsWith("NPWP 09.254.294.4-407.000 sudah valid."), "setContent is what the runtime reads");
        assertTrue(r.getContent().endsWith(NpwpFormatEnhancer.DEFAULT_NOTICE));
        enhancer.enhance(null, new LLMConfig(), r);
        assertEquals(1, r.getContent().split("Catatan:", -1).length - 1, "notice is not appended twice");
    }

    @Test
    void enhancerLeavesUnrelatedAnswersAndEmptyNoticeAlone() {
        Response plain = new Response("Halo, ada yang bisa dibantu?");
        with(new NpwpFormatEnhancer(), "notice", NpwpFormatEnhancer.DEFAULT_NOTICE).enhance(null, new LLMConfig(), plain);
        assertEquals("Halo, ada yang bisa dibantu?", plain.getContent());
        Response quiet = new Response("NIK valid");
        with(new NpwpFormatEnhancer(), "notice", "").enhance(null, new LLMConfig(), quiet);
        assertEquals("NIK valid", quiet.getContent());
    }
}
