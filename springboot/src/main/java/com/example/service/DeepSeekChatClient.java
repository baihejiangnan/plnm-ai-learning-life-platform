package com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class DeepSeekChatClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    public DeepSeekChatClient(
            @Value("${app.ai.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:deepseek-v4-pro}") String model,
            @Value("${app.ai.timeout-seconds:60}") int timeoutSeconds
    ) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = StringUtils.hasText(model) ? model.trim() : "deepseek-v4-pro";
        this.timeoutSeconds = Math.max(timeoutSeconds, 10);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(this.timeoutSeconds, 30)))
                .build();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    public String chat(String prompt, Double temperature, Double topP) {
        if (!isConfigured() || !StringUtils.hasText(prompt)) {
            return null;
        }
        try {
            Map<String, Object> body = buildRequestBody(prompt, false, temperature, topP);
            HttpRequest request = baseRequest()
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            return extractMessageContent(response.body());
        } catch (Exception ignored) {
            return null;
        }
    }

    public Flux<String> stream(String prompt, Double temperature, Double topP) {
        if (!isConfigured() || !StringUtils.hasText(prompt)) {
            return Flux.empty();
        }
        return Flux.<String>create(sink -> {
            try {
                Map<String, Object> body = buildRequestBody(prompt, true, temperature, topP);
                HttpRequest request = baseRequest()
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                        .build();
                HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    sink.complete();
                    return;
                }
                try (Stream<String> lines = response.body()) {
                    lines.forEach(line -> handleSseLine(line, sink));
                }
                sink.complete();
            } catch (Exception e) {
                sink.complete();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private HttpRequest.Builder baseRequest() {
        return HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey);
    }

    private Map<String, Object> buildRequestBody(String prompt, boolean stream, Double temperature, Double topP) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", stream);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        if (temperature != null) {
            body.put("temperature", clamp(temperature, 0.0, 2.0));
        }
        if (topP != null) {
            body.put("top_p", clamp(topP, 0.0, 1.0));
        }
        return body;
    }

    private void handleSseLine(String line, reactor.core.publisher.FluxSink<String> sink) {
        if (!StringUtils.hasText(line) || !line.startsWith("data:")) {
            return;
        }
        String data = line.substring(5).trim();
        if ("[DONE]".equals(data)) {
            sink.complete();
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return;
            }
            JsonNode delta = choices.get(0).path("delta");
            String content = delta.path("content").asText("");
            if (StringUtils.hasText(content)) {
                sink.next(content);
            }
        } catch (IOException ignored) {
        }
    }

    private String extractMessageContent(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).path("message").path("content").asText(null);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String stripTrailingSlash(String value) {
        String text = StringUtils.hasText(value) ? value.trim() : "https://api.deepseek.com";
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
