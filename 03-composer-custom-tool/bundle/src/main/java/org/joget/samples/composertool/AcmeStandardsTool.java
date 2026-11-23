package org.joget.samples.composertool;

import java.util.List;
import org.joget.ai.agent.model.AgentLLM;
import org.joget.ai.agent.model.AgentToolAbstract;
import org.joget.ai.agent.model.Function;
import org.joget.ai.agent.model.Functions;
import org.joget.ai.agent.model.LLMConfig;
import org.joget.ai.agent.model.ToolExecution;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tool the composers can call while they generate an artifact: it returns the example option lists and standards of the fictional company Acme, so
 * the generated form carries the company's cost centres, departments and leave types instead of invented ones.
 *
 * <p>How it is used. The tool is not on any composer by default. A Tool Template (Central Config) wraps this class, and a
 * skill lists that template in its {@code tools:} frontmatter. When the composer calls {@code invoke_skill} for that skill,
 * Agent Builder instantiates the template and adds {@code lookup_acme_standard} to the live function list for the rest
 * of that run.
 *
 * <p>Contract used: {@link #getFunctions()} publishes one function; {@link #execute} receives the model's arguments and
 * returns a String that becomes the tool message. Failures are returned as {"error": ...} and never thrown, because a
 * throwing tool aborts the whole composer run.
 */
public class AcmeStandardsTool extends AgentToolAbstract {

    public static final String DEFAULT_FUNCTION = "lookup_acme_standard";

    @Override
    public Functions getFunctions() {
        // The template may rename the function or reword its description; both default to the values below.
        String name = getPropertyString("name");
        String description = getPropertyString("description");
        if (name == null || name.trim().isEmpty()) {
            name = DEFAULT_FUNCTION;
        }
        if (description == null || description.trim().isEmpty()) {
            description = "Look up an Acme company standard by topic and get it back as JSON. Use it BEFORE you add a "
                    + "dropdown, radio or checkbox for cost centres, departments, leave types or request status, and when you "
                    + "need the approval matrix or the date, currency and request number formats. Option lists come back as "
                    + "{value,label} pairs: put value in the stored value and label in the option label exactly as returned.";
        }
        Function f = new Function(name.trim(), description);
        String[] topics = StandardsCatalogue.load(null).topics().toArray(new String[0]);
        f.addParameter("topic", Function.ParameterType.STRING, "The standard to look up", topics, true);
        Functions fs = new Functions();
        fs.add(f);
        return fs;
    }

    @Override
    public String execute(AgentLLM llm, LLMConfig config, ToolExecution call) {
        JSONObject args = call == null || call.getArguments() == null ? new JSONObject() : call.getArguments();
        try {
            StandardsCatalogue catalogue = StandardsCatalogue.load(getPropertyString("overrideJson"));
            String topic = args.optString("topic", "").trim();
            JSONObject found = topic.isEmpty() ? null : catalogue.topic(topic);
            if (found == null) {
                List<String> available = catalogue.topics();
                return error("Unknown topic '" + topic + "'. Available topics: " + String.join(", ", available));
            }
            found.put("topic", topic);
            return found.toString();
        } catch (JSONException e) {
            // Most likely a typo in the template's Override box.
            LogUtil.warn(AcmeStandardsTool.class.getName(), DEFAULT_FUNCTION + ": invalid override JSON: " + e.getMessage());
            return error("The override JSON in the tool template is not valid: " + e.getMessage());
        } catch (RuntimeException e) {
            LogUtil.warn(AcmeStandardsTool.class.getName(), DEFAULT_FUNCTION + " failed: " + e);
            return error("Lookup failed: " + e.getMessage());
        }
    }

    private static String error(String message) {
        return new JSONObject().put("error", message).toString();
    }

    // ---- ExtDefaultPlugin / PropertyEditable / AgentBuilderPaletteElement ----

    @Override
    public String getName() {
        return "AcmeStandardsTool";
    }

    @Override
    public String getVersion() {
        return Ext.VERSION;
    }

    @Override
    public String getLabel() {
        return "Acme Standards Lookup";
    }

    @Override
    public String getDescription() {
        return "Returns Acme option lists and standards as JSON for the composers";
    }

    @Override
    public String getClassName() {
        return AcmeStandardsTool.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        return Ext.propertyOptions(getClassName(), "AcmeStandardsTool");
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-book\"></i>";
    }

    /** Card shown on the Agent Builder canvas and in Central Config. */
    @Override
    public String getInfoTemplate() {
        return "<div><strong>lookup_acme_standard(topic)</strong><br/>Returns Acme option lists and standards</div>";
    }
}
