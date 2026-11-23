package org.joget.samples.llm;

import java.util.List;
import org.joget.ai.config.llm.LLMModelCapability;
import org.joget.ai.config.llm.LLMModelCapabilityProvider;
import org.joget.ai.config.llm.LLMParameter;
import org.joget.plugin.base.ExtDefaultPlugin;

/** States which parameters the models accept, and declares one parameter of our own. */
public class GatewayCapabilityProvider extends ExtDefaultPlugin implements LLMModelCapabilityProvider {

    private static final LLMParameter REPETITION_PENALTY =
            LLMParameter.builder(GatewayNames.REPETITION_PENALTY)  // property name and capability id
                    .jsonKey("repetition_penalty")                 // key written into the request body
                    .valueType(LLMParameter.ValueType.DECIMAL)
                    .label("Repetition Penalty")
                    .description("Penalises tokens that already appeared. 1.0 turns it off.")
                    .placeholder("e.g. 1.1")
                    .build();

    @Override
    public String getProviderName() {
        return GatewayNames.PROVIDER;
    }

    @Override
    public List<LLMParameter> getParameters() {
        return List.of(REPETITION_PENALTY);
    }

    /** modelName is lower-case, without vendor prefix and date. "" asks for the default profile. */
    @Override
    public LLMModelCapability.Capabilities getCapabilities(String modelName) {
        if (modelName.contains("lite") || modelName.contains("8b")) {
            return LLMModelCapability.Capabilities.builder()
                    .parameters(LLMModelCapability.TEMPERATURE, LLMModelCapability.MAX_TOKENS)
                    .thinking(LLMModelCapability.Thinking.NONE)
                    .build();
        }
        return LLMModelCapability.Capabilities.builder()
                .parameters(LLMModelCapability.TEMPERATURE, LLMModelCapability.TOP_P,
                        LLMModelCapability.MAX_TOKENS, REPETITION_PENALTY.getId())
                .thinking(LLMModelCapability.Thinking.NONE)
                .build();
    }

    @Override
    public String getName() {
        return "Private Gateway Capabilities";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Per-model parameter support for the private gateway";
    }
}
