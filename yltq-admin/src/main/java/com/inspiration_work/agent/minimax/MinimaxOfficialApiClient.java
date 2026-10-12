package com.inspiration_work.agent.minimax;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inspiration_work.agent.config.MinMaxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * MiniMax 官方 <a href="https://platform.minimaxi.com/docs/api-reference/text-chat">chatcompletion_v2</a> HTTP 客户端。
 */
@Slf4j
@RequiredArgsConstructor
public class MinimaxOfficialApiClient {

    /**
     * 整段 JSON POST 字节上限（UTF-8）。HTML 经 JSON 转义后体积会远大于原文；超过时远端常见表现为直接断连，
     * JDK HttpClient 报 {@code header parser received no bytes}，故在发送前拦截并给出可读错误。
     */
    private static final int MAX_REQUEST_JSON_BYTES = 900_000;

    private final MinMaxProperties properties;
    private final ObjectMapper objectMapper;

    private volatile HttpClient httpClient;

    private HttpClient httpClient() {
        HttpClient c = httpClient;
        if (c != null) {
            return c;
        }
        synchronized (this) {
            if (httpClient == null) {
                httpClient = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
                        .build();
            }
            return httpClient;
        }
    }

    private static void assertRequestBodyNotTooLarge(String json) throws IOException {
        int n = json.getBytes(StandardCharsets.UTF_8).length;
        if (n > MAX_REQUEST_JSON_BYTES) {
            throw new IOException("MiniMax 请求 JSON 约 " + n + " 字节，超过安全上限 " + MAX_REQUEST_JSON_BYTES
                    + " 字节；远端可能直接断开连接（表现为 header parser received no bytes）。"
                    + "请缩小 HTML 原型、缩短 systemPrompt，或拆分页面后再调用 chat-with-html。");
        }
    }

    private URI endpoint() {
        String base = trimTrailingSlash(properties.getOfficialBaseUrl());
        String path = properties.getOfficialChatPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return URI.create(base + path);
    }

    public String chatCompletionSync(String systemPrompt, String userMessage) throws IOException, InterruptedException {
        ObjectNode body = buildRequestBody(false, systemPrompt, userMessage);
        String json = objectMapper.writeValueAsString(body);
        assertRequestBodyNotTooLarge(json);
        if (properties.isLogRequests()) {
            log.info("[minimax-official][sync] request body={}", json);
        }
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(endpoint())
                .timeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (StrUtil.isNotBlank(properties.getGroupId())) {
            rb.header("Minimax-Group-Id", properties.getGroupId().trim());
        }
        HttpResponse<String> resp = httpClient().send(rb.build(), HttpResponse.BodyHandlers.ofString());
        if (properties.isLogResponses()) {
            log.info("[minimax-official][sync] status={} body={}", resp.statusCode(), StrUtil.subPre(resp.body(), 2000));
        }
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("MiniMax HTTP " + resp.statusCode() + ": " + StrUtil.subPre(resp.body(), 500));
        }
        return parseSyncAssistantText(resp.body());
    }

    /**
     * 流式：读取响应体，按行解析 SSE {@code data: } 或单行 JSON；增量通过 {@code onDelta} 回调 UTF-8 文本片段。
     */
    public void chatCompletionStream(String systemPrompt, String userMessage, Consumer<String> onDelta)
            throws IOException, InterruptedException {
        ObjectNode body = buildRequestBody(true, systemPrompt, userMessage);
        String json = objectMapper.writeValueAsString(body);
        assertRequestBodyNotTooLarge(json);
        if (properties.isLogRequests()) {
            log.info("[minimax-official][stream] request body={}", json);
        }
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(endpoint())
                .timeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .header("Authorization", "Bearer " + properties.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (StrUtil.isNotBlank(properties.getGroupId())) {
            rb.header("Minimax-Group-Id", properties.getGroupId().trim());
        }
        HttpResponse<InputStream> resp = httpClient().send(rb.build(), HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() / 100 != 2) {
            String err = readAll(resp.body());
            throw new IOException("MiniMax HTTP " + resp.statusCode() + ": " + StrUtil.subPre(err, 500));
        }
        try (InputStream in = resp.body();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("data:")) {
                    trimmed = trimmed.substring(5).trim();
                }
                if (trimmed.isEmpty() || "[DONE]".equalsIgnoreCase(trimmed)) {
                    continue;
                }
                String delta = extractStreamDelta(trimmed);
                if (StrUtil.isNotEmpty(delta)) {
                    onDelta.accept(delta);
                }
            }
        }
    }

    private ObjectNode buildRequestBody(boolean stream, String systemPrompt, String userMessage) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModelName());
        root.put("stream", stream);
        if (properties.getTemperature() != null) {
            root.put("temperature", properties.getTemperature());
        }
        if (properties.getMaxTokens() != null) {
            root.put("max_completion_tokens", properties.getMaxTokens());
        }
        ArrayNode messages = root.putArray("messages");
        if (StrUtil.isNotBlank(systemPrompt)) {
            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt.trim());
        }
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userMessage.trim());
        return root;
    }

    private String parseSyncAssistantText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        assertBaseRespOk(root);
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return "";
        }
        JsonNode first = choices.get(0);
        if (first.has("message")) {
            JsonNode content = first.get("message").get("content");
            return content != null && !content.isNull() ? content.asText("") : "";
        }
        return "";
    }

    private void assertBaseRespOk(JsonNode root) throws IOException {
        JsonNode base = root.get("base_resp");
        if (base == null || !base.has("status_code")) {
            return;
        }
        int code = base.get("status_code").asInt();
        if (code != 0) {
            String msg = base.has("status_msg") ? base.get("status_msg").asText() : "";
            throw new IOException("MiniMax base_resp status_code=" + code + " " + msg);
        }
    }

    private String extractStreamDelta(String jsonLine) throws IOException {
        JsonNode root = objectMapper.readTree(jsonLine);
        assertBaseRespOk(root);
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return "";
        }
        JsonNode first = choices.get(0);
        if (first.has("delta")) {
            JsonNode delta = first.get("delta");
            if (delta.has("content")) {
                JsonNode c = delta.get("content");
                return c != null && !c.isNull() ? c.asText("") : "";
            }
        }
        if (first.has("message")) {
            JsonNode content = first.get("message").get("content");
            return content != null && !content.isNull() ? content.asText("") : "";
        }
        return "";
    }

    private static String readAll(InputStream in) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
