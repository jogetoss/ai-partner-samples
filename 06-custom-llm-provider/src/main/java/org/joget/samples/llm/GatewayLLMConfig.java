package org.joget.samples.llm;

import org.joget.ai.config.config.AbstractLLMConfig;
import org.joget.ai.config.model.LLMCredentialData;
import org.json.JSONObject;

/** The Central Config view of one saved service. super(data) delivers the saved fields to the LLM plugin. */
public class GatewayLLMConfig extends AbstractLLMConfig {

    public GatewayLLMConfig(LLMCredentialData data) {
        super(data);
        this.providerName = GatewayNames.PROVIDER;
        JSONObject saved = data.getAdditionalConfig() == null ? new JSONObject() : data.getAdditionalConfig();
        // Composers and system agents pass getApiKey() and getApiEndpoint() to the LLM plugin as apiKey and apiUrl.
        this.apiKey = first(data.getApiKey(), saved.optString(GatewayNames.KEY_FIELD, ""));
        this.customEndpointUrl = first(data.getCustomLlmUrl(), saved.optString(GatewayNames.URL_FIELD, ""));
        this.modelName = first(data.getModelName(), GatewayNames.DEFAULT_MODEL);
    }

    private static String first(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback.trim();
    }

    @Override
    protected String getDefaultApiEndpoint() {
        return GatewayNames.DEFAULT_BASE_URL;
    }

    @Override
    public String getDefaultModelName() {
        return GatewayNames.DEFAULT_MODEL;
    }

    @Override
    public float getDefaultTemperature() {
        return 0.2f;
    }

    @Override
    public int getDefaultMaxLength() {
        return 4096;
    }

    @Override
    public String getBasePrompt() {
        return basePrompt == null ? "You are a helpful assistant." : basePrompt;
    }

    @Override
    public void setBasePrompt(String prompt) {
        this.basePrompt = prompt;
    }

    /** The default demands an API key. A gateway inside the company network may not use one. */
    @Override
    public void validate() throws Exception {
        if (modelName == null || modelName.isEmpty()) {
            throw new Exception("Model name is required for " + providerName);
        }
    }
}
