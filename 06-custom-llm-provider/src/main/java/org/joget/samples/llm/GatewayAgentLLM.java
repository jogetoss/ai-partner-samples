package org.joget.samples.llm;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.joget.ai.agent.model.AgentException;
import org.joget.ai.agent.model.AgentLLM;
import org.joget.ai.agent.model.AgentLLMAbstract;
import org.joget.ai.agent.model.Functions;
import org.joget.ai.agent.model.LLMConfig;
import org.joget.ai.agent.model.Messages;
import org.joget.ai.agent.model.Response;
import org.joget.ai.agent.model.ToolExecution;
import org.joget.ai.config.llm.LLMModelCapability;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Wire format and transport for an OpenAI-compatible chat/completions gateway.
 *
 * The class sends the request itself with the HTTP client of the JDK. It does not override prepareRequestHeader or
 * parseResponse of AgentLLMAbstract: their signatures name Apache HttpClient classes, and Agent Builder does not
 * export that library, so a class in another bundle that names them cannot be loaded.
 */
public class GatewayAgentLLM extends AgentLLMAbstract {

    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?</think>\\s*");
    private static final String JSON_INSTRUCTION_KEY = "_jsonInstruction";

    // ---- identity ------------------------------------------------------------------------------

    @Override
    public String getName() {
        return "PrivateGatewayAgentLLM";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getLabel() {
        return "Private Gateway LLM";
    }

    @Override
    public String getDescription() {
        return "Chat model behind a private OpenAI-compatible gateway";
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-server\"></i>";
    }

    /** AgentLLM.Type is a fixed list. EXTERNAL is the value for everything that is not built in. */
    @Override
    public AgentLLM.Type getLLMType() {
        return AgentLLM.Type.EXTERNAL;
    }

    /** Written to the Model Class column of the usage rows. */
    @Override
    public String getLLMTypeName() {
        return GatewayNames.PROVIDER;
    }

    /** The name under which GatewayCapabilityProvider is registered. */
    @Override
    protected String getCapabilityProviderName() {
        return GatewayNames.PROVIDER;
    }

    /** The properties dialog of this element on the agent canvas. */
    @Override
    public String getPropertyOptions() {
        return "[{\"title\":\"Private Gateway LLM\",\"properties\":["
                + "{\"name\":\"apiKey\",\"label\":\"API Key\",\"type\":\"password\"},"
                + "{\"name\":\"tenantId\",\"label\":\"Tenant ID (X-Tenant-Id header)\",\"type\":\"textfield\"},"
                + "{\"name\":\"model\",\"label\":\"Model\",\"type\":\"textfield\",\"required\":\"True\","
                + "\"value\":\"" + GatewayNames.DEFAULT_MODEL + "\"},"
                + "{\"name\":\"apiUrl\",\"label\":\"API URL\",\"type\":\"textfield\","
                + "\"value\":\"" + GatewayNames.DEFAULT_BASE_URL + "\"},"
                + "{\"name\":\"maxTokens\",\"label\":\"Max Tokens\",\"type\":\"number\",\"value\":\"4096\"},"
                + "{\"name\":\"temperature\",\"label\":\"Temperature\",\"type\":\"number\"},"
                + "{\"name\":\"topP\",\"label\":\"Top P\",\"type\":\"number\"},"
                + "{\"name\":\"" + GatewayNames.REPETITION_PENALTY + "\",\"label\":\"Repetition Penalty\","
                + "\"type\":\"number\"},"
                + "{\"name\":\"socketTimeoutSeconds\",\"label\":\"Read Timeout (seconds)\",\"type\":\"number\"},"
                + "{\"name\":\"nativeJson\",\"label\":\"Gateway supports response_format\",\"type\":\"checkbox\","
                + "\"options\":[{\"value\":\"true\",\"label\":\"\"}]}"
                + "]}]";
    }

    // ---- request -------------------------------------------------------------------------------

    /** A property of this element or service, trimmed; empty when not set. */
    private String valueOf(String property) {
        String v = getPropertyString(property);
        return v == null ? "" : v.trim();
    }

    /** The base URL gets /chat/completions appended unless it already ends with /completions. */
    protected String getUrl() {
        String url = valueOf("apiUrl");
        if (url.isEmpty()) {
            url = GatewayNames.DEFAULT_BASE_URL;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url.endsWith("/completions") ? url : url + "/chat/completions";
    }

    /** Content type, the key if there is one (a gateway inside the company network may need none), the tenant. */
    private void addHeaders(HttpRequest.Builder request) {
        request.header("Content-Type", "application/json");
        String key = valueOf("apiKey");
        if (!key.isEmpty()) {
            request.header("Authorization", "Bearer " + key);
        }
        String tenant = valueOf("tenantId");
        if (!tenant.isEmpty()) {
            request.header("X-Tenant-Id", tenant);
        }
    }

    /** Fills the request body: task override (_task_*), then plugin property. Parameters the model rejects are removed. */
    protected void presetConfig(LLMConfig config, Functions functions) {
        String taskModel = config.optString("_task_model", "");
        String model = taskModel.isEmpty() ? getPropertyString("model") : taskModel;
        if (model == null || model.isEmpty()) {
            model = GatewayNames.DEFAULT_MODEL;
        }
        config.put("model", model);
        LLMModelCapability.Capabilities caps = LLMModelCapability.resolve(getCapabilityProviderName(), model);

        config.remove("max_tokens");
        putOptional(config, "maxTokens", "max_tokens", Integer.class);
        if (config.has("_task_maxTokens")) {
            config.put("max_tokens", config.getInt("_task_maxTokens"));
        }
        int ceiling = positiveInt(getPropertyString("maxOutputTokens"));   // composers ask for 16000
        if (ceiling > 0 && config.optInt("max_tokens", 0) > ceiling) {
            config.put("max_tokens", ceiling);
        }

        if (caps.supports(LLMModelCapability.TEMPERATURE)) {
            putOptional(config, "temperature", "temperature", Double.class);
            if (config.has("_task_temperature")) {
                config.put("temperature", config.getDouble("_task_temperature"));
            }
        } else {
            config.remove("temperature");
        }
        if (caps.supports(LLMModelCapability.TOP_P)) {
            putOptional(config, "topP", "top_p", Double.class);
            if (config.has("_task_topP")) {
                config.put("top_p", config.getDouble("_task_topP"));
            }
        } else {
            config.remove("top_p");
        }

        // Our own declared parameter(s): typed, written under their JSON key, or removed.
        applyDeclaredParameters(config, getCapabilityProviderName(), model,
                LLMModelCapability.getBuiltInParameterIds());

        if (functions != null && !functions.isEmpty()) {     // some servers reject this without tools
            config.put("parallel_tool_calls", config.isParallelToolCalls());
        } else {
            config.remove("parallel_tool_calls");
        }
    }

    /** The base class removes only _task_ keys. Remove every other key that starts with an underscore too. */
    @Override
    protected String preparePayload(LLMConfig config, Messages messages, Functions functions) {
        JSONObject payload = new JSONObject(super.preparePayload(config, messages, functions));
        String instruction = payload.optString(JSON_INSTRUCTION_KEY, "");
        payload.keySet().removeIf(k -> k.startsWith("_"));
        if (!instruction.isEmpty() && payload.has("messages")) {
            JSONArray withInstruction = new JSONArray();
            withInstruction.put(new JSONObject().put("role", "system").put("content", instruction));
            JSONArray original = payload.getJSONArray("messages");
            for (int i = 0; i < original.length(); i++) {
                withInstruction.put(original.get(i));
            }
            payload.put("messages", withInstruction);
        }
        return payload.toString();
    }

    /** JSON mode: response_format if supported, and always the instruction as a leading system message. */
    @Override
    public void injectStructuredOutput(LLMConfig config, String mode, String jsonSchema) {
        boolean hasSchema = jsonSchema != null && !jsonSchema.trim().isEmpty();
        if ("true".equals(getPropertyString("nativeJson"))) {
            config.put("response_format", new JSONObject().put("type", "json_object"));
        }
        config.put(JSON_INSTRUCTION_KEY, hasSchema
                ? "Respond with one valid JSON object that matches this schema exactly. "
                  + "No prose, no markdown fences.\nSchema:\n" + jsonSchema.trim()
                : "Respond with one valid JSON object only. No prose, no markdown fences.");
    }

    // ---- response ------------------------------------------------------------------------------

    /**
     * Reads one reply into the result. Unlike the default parser it accepts content that is null, tool arguments
     * as an object, and reasoning text. An AgentException is not retried.
     */
    void readReply(Response result, String body) {
        result.setFullJsonResponse(body);      // the token counts are read from the "usage" object of this text
        if (body == null || body.isEmpty()) {
            return;
        }
        JSONObject json;
        try {
            json = new JSONObject(body);
        } catch (Exception notJson) {
            throw new AgentException("API Error: the gateway returned a reply that is not JSON: "
                    + abbreviate(body, 300));
        }
        if (json.has("error")) {
            Object err = json.get("error");
            String msg = err instanceof JSONObject
                    ? ((JSONObject) err).optString("message", err.toString()) : String.valueOf(err);
            throw new AgentException("API Error: " + msg);
        }
        JSONArray choices = json.optJSONArray("choices");
        JSONObject message = choices == null || choices.isEmpty() ? null : choices.getJSONObject(0).optJSONObject("message");
        if (message == null) {
            return;
        }

        String content = message.isNull("content") ? null : message.optString("content", null);
        if (content != null) {
            content = THINK_BLOCK.matcher(content).replaceAll("").trim();   // keep the visible answer only
            message.put("content", content);
        }
        message.remove("reasoning_content");
        result.setMessage(message);
        if (content != null && !content.isEmpty()) {
            result.setContent(content);
        }

        JSONArray toolCalls = message.optJSONArray("tool_calls");
        for (int i = 0; toolCalls != null && i < toolCalls.length(); i++) {
            JSONObject call = toolCalls.getJSONObject(i);
            JSONObject fn = call.optJSONObject("function");
            if (fn == null) {
                continue;
            }
            String name = fn.optString("name");
            Object raw = fn.opt("arguments");   // a string on OpenAI, an object on some gateways
            JSONObject args;
            try {
                args = raw instanceof JSONObject ? (JSONObject) raw
                        : (raw == null || String.valueOf(raw).trim().isEmpty()) ? new JSONObject()
                        : new JSONObject(String.valueOf(raw));
            } catch (Exception badJson) {
                throw new AgentException("API Error: the model sent arguments for tool '" + name
                        + "' that are not valid JSON: " + abbreviate(String.valueOf(raw), 300));
            }
            fn.put("arguments", args.toString());   // the history must carry a string
            ToolExecution execution = new ToolExecution(name, args);
            execution.setId(call.optString("id"));
            result.addToolExecution(execution);
        }
    }

    private static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static int positiveInt(String s) {
        try {
            return s == null || s.trim().isEmpty() ? 0 : Math.max(0, Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ---- transport and entry point ---------------------------------------------------------------

    /**
     * One blocking call. getStreamingResponse of the base class is not overridden, so a streaming run also comes
     * here and the answer arrives in one piece.
     */
    @Override
    public Response getResponse(LLMConfig config, Messages messages, Functions functions) throws AgentException {
        presetConfig(config, functions);
        int readSeconds = positiveInt(getPropertyString("socketTimeoutSeconds"));   // slow models need more than 120 s
        if (readSeconds == 0) {
            readSeconds = config.getLlmSocketTimeoutSeconds();
        }
        String payload = preparePayload(config, messages, functions);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, config.getLlmConnectTimeoutSeconds())))
                .proxy(ProxySelector.getDefault())
                .build();
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(getUrl()))
                .timeout(Duration.ofSeconds(Math.max(1, readSeconds)))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        addHeaders(request);

        int attempts = Math.max(1, config.getLlmRetryMaxAttempts());
        String lastProblem = "";
        for (int attempt = 1; attempt <= attempts; attempt++) {
            Response result = new Response();
            result.setRequestPayload(payload);          // shown in the "LLM" node of the trace
            try {
                HttpResponse<String> reply = client.send(request.build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = reply.statusCode();
                if ((status == 429 || status >= 500) && attempt < attempts) {
                    lastProblem = "HTTP " + status + " from the gateway";
                } else {
                    readReply(result, reply.body());
                    persistLLMCall(result, config);     // the usage row of the AI Usage Dashboard
                    return result;
                }
            } catch (AgentException known) {            // a clear answer from the gateway: do not retry
                if (known.getResponse() == null) {
                    known.setResponse(result);          // keeps the request and the raw reply for the trace
                }
                throw known;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AgentException("The call to the gateway was interrupted");
            } catch (Exception networkOrTimeout) {
                lastProblem = String.valueOf(networkOrTimeout);
            }
            if (attempt < attempts) {
                pause(config, attempt);
            }
        }
        throw new AgentException("API Error: the gateway call failed after " + attempts + " attempt(s): " + lastProblem);
    }

    /** Waits initialDelay * 2^(attempt-1), at most maxDelay, plus up to one second at random. */
    private static void pause(LLMConfig config, int attempt) {
        long delay = Math.min((long) Math.max(0, config.getLlmRetryInitialDelayMs()) << (attempt - 1),
                Math.max(0, config.getLlmRetryMaxDelayMs())) + ThreadLocalRandom.current().nextInt(1000);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AgentException("The retry was interrupted");
        }
    }
}
