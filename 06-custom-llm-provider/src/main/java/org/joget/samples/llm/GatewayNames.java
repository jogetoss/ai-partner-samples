package org.joget.samples.llm;

/**
 * The names that must be identical in every extension point. A mismatch is silent, so keep them in one place.
 */
public final class GatewayNames {

    /** Shown in the Model Class list. Matched case-insensitively. */
    public static final String PROVIDER = "PrivateGateway";

    public static final String DEFAULT_BASE_URL = "http://localhost:8000/v1";
    public static final String DEFAULT_MODEL = "private-chat";

    /**
     * Names of the two credential fields of the AI Service form. Do not call a field "apiKey": the form has a
     * field of its own with that name, and the two would both be shown.
     */
    public static final String URL_FIELD = "gatewayUrl";
    public static final String KEY_FIELD = "gatewayKey";

    /** Id of the one extra parameter this provider declares. */
    public static final String REPETITION_PENALTY = "repetitionPenalty";

    private GatewayNames() {
    }
}
