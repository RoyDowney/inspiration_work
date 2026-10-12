package com.inspiration_work.agent.minimax;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.inspiration_work.agent.config.MinMaxProperties;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 估算官方 chatcompletion_v2 请求体 JSON UTF-8 字节数（{@code stream=false}，与 {@code stream=true} 仅差数字位），
 * 与 {@link MinimaxOfficialApiClient} 序列化结构一致，
 * 便于在发送前拦截过大负载（避免远端直接断连：{@code header parser received no bytes}）。
 */
public final class MinimaxChatRequestSizeEstimator {

    private MinimaxChatRequestSizeEstimator() {
    }

    public static int estimateOfficialSyncBodyUtf8Bytes(MinMaxProperties properties, String systemPrompt, String userMessage) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("model", StrUtil.blankToDefault(properties.getModelName(), ""));
        root.put("stream", false);
        if (properties.getTemperature() != null) {
            root.put("temperature", properties.getTemperature());
        }
        if (properties.getMaxTokens() != null) {
            root.put("max_completion_tokens", properties.getMaxTokens());
        }
        List<Map<String, String>> messages = new ArrayList<>();
        if (StrUtil.isNotBlank(systemPrompt)) {
            Map<String, String> sys = new LinkedHashMap<>();
            sys.put("role", "system");
            sys.put("content", systemPrompt.trim());
            messages.add(sys);
        }
        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", StrUtil.nullToDefault(userMessage, "").trim());
        messages.add(user);
        root.put("messages", messages);
        String json = JSONUtil.toJsonStr(root);
        return json.getBytes(StandardCharsets.UTF_8).length;
    }
}
