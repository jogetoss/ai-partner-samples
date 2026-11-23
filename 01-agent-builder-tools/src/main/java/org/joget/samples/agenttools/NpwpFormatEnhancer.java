package org.joget.samples.agenttools;

import java.util.regex.Matcher;
import org.joget.ai.agent.model.AgentEnhancerAbstract;
import org.joget.ai.agent.model.AgentException;
import org.joget.ai.agent.model.AgentLLM;
import org.joget.ai.agent.model.LLMConfig;
import org.joget.ai.agent.model.Response;

/**
 * Enhancer: post-processes the final answer. It rewrites every bare 15 digit NPWP to the official
 * 99.999.999.9-999.999 layout and, when the answer talks about NIK or NPWP, appends a short notice (Bahasa
 * Indonesia by default, editable in the palette) that the check is structural only.
 *
 * Contract: enhance() must write its result back with response.setContent(...). The runtime ignores the String
 * that enhance() returns, so an enhancer that only returns the new text changes nothing.
 */
public class NpwpFormatEnhancer extends AgentEnhancerAbstract {

    static final String DEFAULT_NOTICE = "Catatan: pengecekan hanya memeriksa format, bukan data resmi DJP atau Dukcapil.";

    @Override
    public String enhance(AgentLLM llm, LLMConfig config, Response response) throws AgentException {
        String text = response == null ? null : response.getContent();
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher m = IndonesianIds.NPWP_RAW.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(IndonesianIds.formatNpwp(m.group())));
        }
        m.appendTail(sb);
        String result = sb.toString();

        String notice = getPropertyString("notice");
        if (notice == null) {  // the palette stores its default; an unconfigured instance adds nothing
            notice = "";
        }
        if (!notice.trim().isEmpty() && result.matches("(?is).*\\b(NIK|NPWP)\\b.*") && !result.contains(notice)) {
            result = result + "\n\n" + notice.trim();
        }
        response.setContent(result);
        return result;
    }

    @Override
    public String getName() {
        return "NpwpFormatEnhancer";
    }

    @Override
    public String getVersion() {
        return Ext.VERSION;
    }

    @Override
    public String getLabel() {
        return "NPWP Format and Notice";
    }

    @Override
    public String getDescription() {
        return "Formats NPWP numbers in the answer and appends a Bahasa Indonesia notice";
    }

    @Override
    public String getClassName() {
        return NpwpFormatEnhancer.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        return Ext.propertyOptions(getClassName(), "NpwpFormatEnhancer");
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-file-invoice\"></i>";
    }

    @Override
    public String getInfoTemplate() {
        return "<div><dl><dt>Notice</dt><dd>${properties.notice}</dd></dl></div>";
    }
}
