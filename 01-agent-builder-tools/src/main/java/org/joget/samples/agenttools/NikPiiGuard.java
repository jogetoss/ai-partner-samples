package org.joget.samples.agenttools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import org.joget.ai.agent.model.AgentException;
import org.joget.ai.agent.model.AgentGuardAbstract;
import org.joget.ai.agent.model.GuardResult;
import org.joget.ai.agent.model.LLMConfig;
import org.joget.ai.agent.model.Messages;
import org.joget.ai.agent.model.Response;
import org.json.JSONObject;

/**
 * Guard: keeps full NIK numbers (Indonesian national ID, personal data) out of the conversation.
 *
 * Contract: extend AgentGuardAbstract (the runtime fails the task with "Guard class could not be loaded" for a class
 * that only implements AgentGuard). filterInput runs before every model call, filterOutput on the final answer.
 * resolveViolation() turns the builder's "On violation" choice (block / sanitize / redirect) into a GuardResult.
 * SANITIZE does nothing by itself: the runtime only reads the reason, so this guard rewrites the message / response
 * in place before returning.
 *
 * Property "direction": input, output or both. Only 16 digit numbers that pass the province and birth date rules
 * count as a NIK, so order numbers and phone numbers are left alone.
 */
public class NikPiiGuard extends AgentGuardAbstract {

    @Override
    public GuardResult filterInput(Messages messages, LLMConfig config) throws AgentException {
        if (messages == null || !watches("input")) {
            return GuardResult.pass();
        }
        for (int i = 0; i < messages.length(); i++) {
            JSONObject m = messages.optJSONObject(i);
            // Only what the user typed: the system prompt and tool results are not the guard's business.
            if (m == null || !"user".equals(m.optString("role")) || !(m.opt("content") instanceof String)) {
                continue;
            }
            String text = m.getString("content");
            List<String> hits = findNiks(text);
            if (!hits.isEmpty()) {
                String clean = mask(text);
                m.put("content", clean);
                return resolveViolation(clean, hits.size() + " NIK number(s) in the request");
            }
        }
        return GuardResult.pass();
    }

    @Override
    public GuardResult filterOutput(Response response, LLMConfig config) throws AgentException {
        String text = response == null ? null : response.getContent();
        if (text == null || !watches("output")) {
            return GuardResult.pass();
        }
        List<String> hits = findNiks(text);
        if (hits.isEmpty()) {
            return GuardResult.pass();
        }
        String clean = mask(text);
        GuardResult result = resolveViolation(clean, hits.size() + " NIK number(s) in the answer");
        if (result.getAction() == GuardResult.Action.SANITIZE) {
            response.setContent(clean);
        }
        return result;
    }

    private boolean watches(String side) {
        String d = getPropertyString("direction");
        return d == null || d.isEmpty() || "both".equals(d) || side.equals(d);
    }

    static List<String> findNiks(String text) {
        List<String> hits = new ArrayList<>();
        Matcher m = IndonesianIds.NIK_CANDIDATE.matcher(text);
        while (m.find()) {
            if (IndonesianIds.looksLikeNik(m.group(), null)) {
                hits.add(m.group());
            }
        }
        return hits;
    }

    static String mask(String text) {
        Matcher m = IndonesianIds.NIK_CANDIDATE.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String nik = m.group();
            m.appendReplacement(sb, Matcher.quoteReplacement(IndonesianIds.looksLikeNik(nik, null) ? IndonesianIds.maskNik(nik) : nik));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @Override
    public String getName() {
        return "NikPiiGuard";
    }

    @Override
    public String getVersion() {
        return Ext.VERSION;
    }

    @Override
    public String getLabel() {
        return "NIK PII Guard";
    }

    @Override
    public String getDescription() {
        return "Blocks, masks or redirects when a full NIK appears in the request or the answer";
    }

    @Override
    public String getClassName() {
        return NikPiiGuard.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        return Ext.propertyOptions(getClassName(), "NikPiiGuard");
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-user-shield\"></i>";
    }
}
