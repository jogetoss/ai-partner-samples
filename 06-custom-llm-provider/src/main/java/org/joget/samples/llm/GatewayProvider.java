package org.joget.samples.llm;

import java.util.Arrays;
import java.util.List;
import org.joget.ai.config.config.LLMConfig;
import org.joget.ai.config.dao.LLMCallsDao;
import org.joget.ai.config.llm.LLMProviderAbstract;
import org.joget.ai.config.llm.LLMService;
import org.joget.ai.config.model.LLMCredentialData;

/** Adds "PrivateGateway" to the Model Class list of AI Central Config > AI Services. */
public class GatewayProvider extends LLMProviderAbstract {

    @Override
    public String getProviderName() {
        return GatewayNames.PROVIDER;
    }

    /** Suggestions for Model Name. The box also accepts any id the user types. */
    @Override
    public List<String> getModelNames() {
        return Arrays.asList("private-chat", "private-chat-large", "private-lite-8b");
    }

    @Override
    public LLMConfig createConfig(LLMCredentialData credentialData) {
        return new GatewayLLMConfig(credentialData);
    }

    /** Only code that uses LLMServiceFactory calls this. Agents, composers and system agents never do. */
    @Override
    public LLMService createService(LLMConfig config, LLMCallsDao llmCallsDao) {
        throw new UnsupportedOperationException(GatewayNames.PROVIDER + " does not provide an LLMService.");
    }

    /** The credential fields of the AI Service form. Each is saved with the service and passed to the LLM plugin. */
    @Override
    public String getPropertyOptions() {
        return "["
                + "{\"name\":\"" + GatewayNames.URL_FIELD + "\",\"label\":\"Gateway Base URL\",\"type\":\"textfield\","
                + "\"placeholder\":\"" + GatewayNames.DEFAULT_BASE_URL + "\"},"
                + "{\"name\":\"" + GatewayNames.KEY_FIELD + "\",\"label\":\"Gateway API Key\",\"type\":\"password\"},"
                + "{\"name\":\"tenantId\",\"label\":\"Tenant ID\",\"type\":\"textfield\"},"
                + "{\"name\":\"maxOutputTokens\",\"label\":\"Max Output Tokens Allowed\",\"type\":\"textfield\"},"
                + "{\"name\":\"socketTimeoutSeconds\",\"label\":\"Read Timeout (seconds)\",\"type\":\"textfield\"}"
                + "]";
    }

    @Override
    public String getName() {
        return "Private Gateway Provider";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "AI Service provider for a private OpenAI-compatible gateway";
    }

    @Override
    public String getLabel() {
        return "Private Gateway";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }
}
