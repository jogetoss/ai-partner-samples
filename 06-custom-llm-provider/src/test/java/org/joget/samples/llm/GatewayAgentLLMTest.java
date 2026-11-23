package org.joget.samples.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.joget.ai.agent.model.Functions;
import org.joget.ai.agent.model.LLMConfig;
import org.joget.ai.agent.model.Message;
import org.joget.ai.agent.model.Messages;
import org.joget.ai.agent.model.Response;
import org.joget.ai.config.llm.LLMModelCapabilityRegistry;
import org.joget.ai.config.model.LLMCredentialData;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** A throw-away HTTP server stands in for the gateway. No Joget runtime and no model. */
class GatewayAgentLLMTest {

    @Test
    void retriesTheRequestAndSendsOnlyWhatTheModelAccepts() throws Exception {
        LLMModelCapabilityRegistry.register(new GatewayCapabilityProvider());   // there is no plugin manager here
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> tenant = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            tenant.set(exchange.getRequestHeaders().getFirst("X-Tenant-Id"));
            boolean first = calls.incrementAndGet() == 1;       // the first call fails with 503
            byte[] reply = (first ? "busy" : "{\"choices\":[{\"message\":{\"role\":\"assistant\","
                    + "\"content\":\"<think>hmm</think>Hello\"}}]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(first ? 503 : 200, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.start();
        try {
            GatewayAgentLLM llm = new GatewayAgentLLM();
            llm.setProperty("apiUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            llm.setProperty("model", "private-lite-8b");     // the lite profile: no top_p
            llm.setProperty("tenantId", "t-42");
            llm.setProperty("topP", "0.9");
            llm.setProperty("maxTokens", "16000");
            llm.setProperty("maxOutputTokens", "4096");
            LLMConfig config = new LLMConfig();
            config.setLlmRetryInitialDelayMs(10);
            Messages messages = new Messages();
            messages.add(new Message(Message.Role.USER, "hi"));

            Response response = llm.getResponse(config, messages, new Functions());

            assertEquals(2, calls.get());                     // the 503 was retried
            assertEquals("Hello", response.getContent());     // <think> removed
            assertEquals("t-42", tenant.get());
            JSONObject sent = new JSONObject(body.get());
            assertFalse(sent.has("top_p"));
            assertEquals(4096, sent.getInt("max_tokens"));    // 16000 clamped to the ceiling
        } finally {
            server.stop(0);
        }
    }

    @Test
    void theServiceFieldsBecomeUrlAndKeyOfTheModelCall() {
        LLMCredentialData saved = new LLMCredentialData();
        saved.setModelName("private-chat-large");
        saved.setApiKey(new JSONObject().put(GatewayNames.URL_FIELD, "https://llm.internal.example/v1")
                .put(GatewayNames.KEY_FIELD, "sk-example-not-a-real-key").put("tenantId", "t-42"));

        GatewayLLMConfig config = new GatewayLLMConfig(saved);

        assertEquals("https://llm.internal.example/v1", config.getApiEndpoint());
        assertEquals("sk-example-not-a-real-key", config.getApiKey());
        assertEquals("private-chat-large", config.getModelName());
        assertEquals("t-42", config.getAdditionalConfig().get("tenantId"));   // reaches the LLM plugin as a property
        assertEquals(GatewayNames.DEFAULT_BASE_URL, new GatewayLLMConfig(new LLMCredentialData()).getApiEndpoint());
    }
}
