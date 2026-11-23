package org.joget.samples.agenttools;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.joget.ai.agent.model.AgentLLM;
import org.joget.ai.agent.model.AgentToolAbstract;
import org.joget.ai.agent.model.Function;
import org.joget.ai.agent.model.Functions;
import org.joget.ai.agent.model.LLMConfig;
import org.joget.ai.agent.model.ToolExecution;
import org.joget.commons.util.LogUtil;
import org.json.JSONObject;

/**
 * Palette tool: checks the structure of an Indonesian NIK or NPWP and decodes what the number itself carries
 * (province, birth date, gender, tax office). It works without any external system.
 *
 * Contract: getFunctions() is the only thing the model sees; execute() gets the model's arguments and returns a
 * String that becomes the tool message. Errors are returned as {"error": ...}, never thrown, because the
 * sequential tool loop rethrows an exception and ends the whole task.
 * Policy lives in the palette properties (set by the builder), so the model cannot argue its way around it:
 *   idTypes             NIK, NPWP or BOTH       which checks the agent may run
 *   mode                format | strict         strict also requires a known province and a real birth date (NIK), or a tax office code (NPWP)
 *   returnFullNumber    checkbox                off = the number comes back masked
 *   extraProvinces      grid code,name          local additions to the built-in province table
 */
public class IndonesianIdValidatorTool extends AgentToolAbstract {

    public static final String FUNCTION = "validate_indonesian_id";

    @Override
    public Functions getFunctions() {
        Function f = new Function(FUNCTION, "Validate an Indonesian identity number and decode it. Use it whenever the user gives "
                + "a NIK (16 digits, national ID) or an NPWP (15 or 16 digits, tax number) and wants to know if it is valid, "
                + "where it was issued or how to write it correctly. Returns JSON with valid, reason, formatted number, "
                + "province, birth date and gender (NIK) or tax office code (NPWP).");
        f.addParameter("idType", Function.ParameterType.STRING, "NIK or NPWP", true);
        f.addParameter("number", Function.ParameterType.STRING, "The number as typed; dots, dashes and spaces are ignored", true);
        Functions fs = new Functions();
        fs.add(f);
        return fs;
    }

    @Override
    public String execute(AgentLLM llm, LLMConfig config, ToolExecution call) {
        JSONObject args = call == null || call.getArguments() == null ? new JSONObject() : call.getArguments();
        try {
            String type = args.optString("idType", "").trim().toUpperCase(Locale.ROOT);
            String digits = IndonesianIds.digitsOnly(args.optString("number", ""));
            String allowed = getPropertyString("idTypes");
            if (!type.equals("NIK") && !type.equals("NPWP")) {
                return error("idType must be NIK or NPWP");
            }
            if (allowed != null && !allowed.isEmpty() && !"BOTH".equals(allowed) && !allowed.equals(type)) {
                return error("This agent is only allowed to validate " + allowed + " numbers");
            }
            boolean strict = "strict".equals(getPropertyString("mode"));
            boolean full = "true".equalsIgnoreCase(getPropertyString("returnFullNumber"));
            JSONObject out = type.equals("NIK") ? nik(digits, strict, full) : npwp(digits, strict, full);
            return out.put("idType", type).toString();
        } catch (RuntimeException e) {
            LogUtil.warn(IndonesianIdValidatorTool.class.getName(), FUNCTION + " failed: " + e);
            return error("Validation failed: " + e.getMessage());
        }
    }

    private JSONObject nik(String d, boolean strict, boolean full) {
        JSONObject out = new JSONObject();
        if (d.length() != 16) {
            return out.put("valid", false).put("reason", "A NIK has 16 digits, got " + d.length());
        }
        String province = IndonesianIds.province(d.substring(0, 2), extraProvinces());
        LocalDate born = IndonesianIds.nikBirthDate(d, LocalDate.now());
        out.put("number", full ? d : IndonesianIds.maskNik(d));
        if (province != null) {
            out.put("province", province);
        }
        if (born != null) {
            out.put("birthDate", born.toString()).put("gender", IndonesianIds.isFemale(d) ? "female" : "male");
        }
        if (strict && province == null) {
            return out.put("valid", false).put("reason", "Unknown province code " + d.substring(0, 2));
        }
        if (strict && born == null) {
            return out.put("valid", false).put("reason", "Digits 7-12 do not form a real birth date");
        }
        return out.put("valid", true).put("reason", strict ? "Structure, province and birth date are plausible" : "Length and digits are correct");
    }

    /**
     * NPWP comes in three shapes:
     *   15 digits                      the old format: 9 taxpayer digits, 3 tax office digits, 3 branch digits
     *   16 digits starting with 0      companies and non-residents: a 0 in front of the old 15 digits
     *   16 digits not starting with 0  individuals: the NPWP is the holder's NIK
     */
    private JSONObject npwp(String d, boolean strict, boolean full) {
        JSONObject out = new JSONObject();
        if (d.length() == 16 && d.charAt(0) != '0') {
            return out.put("valid", IndonesianIds.looksLikeNik(d, extraProvinces())).put("number", full ? d : IndonesianIds.maskNik(d))
                    .put("reason", "16 digits not starting with 0: the NPWP of an individual is the holder's NIK, checked as a NIK");
        }
        if (d.length() != 15 && d.length() != 16) {
            return out.put("valid", false).put("reason", "An NPWP has 15 digits, or 16 digits (the NIK, or 0 followed by the 15 digits), got " + d.length());
        }
        boolean sixteen = d.length() == 16;
        String old = sixteen ? d.substring(1) : d;  // the 15 digits that carry the tax office and branch codes
        if (sixteen) {
            out.put("number", full ? d : IndonesianIds.maskNpwp16(d));
        } else {
            out.put("number", full ? IndonesianIds.formatNpwp(d) : IndonesianIds.maskNpwp(d));
        }
        out.put("taxOfficeCode", old.substring(9, 12)).put("branchCode", old.substring(12, 15));
        if (strict && "000".equals(old.substring(9, 12))) {
            return out.put("valid", false).put("reason", "Tax office code (digits 10-12 of the 15 digit number) cannot be 000");
        }
        return out.put("valid", true).put("reason", sixteen
                ? "16 digits starting with 0: checked as the 15 digit NPWP after the leading 0 (not checked against the tax office)"
                : "Format is correct (not checked against the tax office)");
    }

    /** A grid property arrives as Object[] of Map rows, one per line the builder added. */
    private Map<String, String> extraProvinces() {
        Map<String, String> extra = new HashMap<>();
        Object raw = getProperty("extraProvinces");
        if (raw instanceof Object[]) {
            for (Object row : (Object[]) raw) {
                if (row instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) row;
                    String code = String.valueOf(m.get("code")).trim();
                    if (code.matches("\\d{2}")) {
                        extra.put(code, String.valueOf(m.get("name")).trim());
                    }
                }
            }
        }
        return extra;
    }

    private static String error(String message) {
        return new JSONObject().put("error", message).toString();
    }

    @Override
    public String getName() {
        return "IndonesianIdValidatorTool";
    }

    @Override
    public String getVersion() {
        return Ext.VERSION;
    }

    @Override
    public String getLabel() {
        return "Indonesian ID Validator";
    }

    @Override
    public String getDescription() {
        return "Validates and decodes an Indonesian NIK or NPWP, no external system needed";
    }

    @Override
    public String getClassName() {
        return IndonesianIdValidatorTool.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        return Ext.propertyOptions(getClassName(), "IndonesianIdValidatorTool");
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-id-card\"></i>";
    }

    /** Card on the builder canvas; ${properties.x} is filled in by the builder. */
    @Override
    public String getInfoTemplate() {
        return "<div><dl><dt>Function</dt><dd>validate_indonesian_id</dd><dt>Allowed ID types</dt><dd>${properties.idTypes}</dd><dt>Mode</dt><dd>${properties.mode}</dd></dl></div>";
    }
}
