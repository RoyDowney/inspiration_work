package com.inspiration_work.agent.config;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * LangChain4j 对接 MiniMax（仅 {@code yudao.minimax.api-mode=openai-compatible} 时启用）。
 */
@Configuration
@Conditional(MinMaxOpenAiCompatibleCondition.class)
@Slf4j
public class MinMaxModelConfig {

    @Bean(name = "minimaxChatModel")
    public OpenAiChatModel minimaxChatModel(MinMaxProperties properties) {
        validateApiKey(properties);
        OpenAiChatModel.OpenAiChatModelBuilder b = OpenAiChatModel.builder()
                .baseUrl(trimTrailingSlash(properties.getOpenaiBaseUrl()))
                .apiKey(properties.getApiKey().trim())
                .modelName(properties.getModelName())
                .temperature(properties.getTemperature() != null ? properties.getTemperature() : 0.7)
                .maxTokens(properties.getMaxTokens() != null ? properties.getMaxTokens() : 8192)
                .timeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses());
        mergeCustomHeaders(b, properties);
        OpenAiChatModel model = b.build();
        log.info("[minimax] OpenAiChatModel ready baseUrl={}, model={}", properties.getOpenaiBaseUrl(), properties.getModelName());
        return model;
    }

    @Bean(name = "minimaxStreamingChatModel")
    @ConditionalOnProperty(prefix = "yudao.minimax", name = "streaming-enabled", havingValue = "true", matchIfMissing = true)
    public OpenAiStreamingChatModel minimaxStreamingChatModel(MinMaxProperties properties) {
        validateApiKey(properties);
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder b = OpenAiStreamingChatModel.builder()
                .baseUrl(trimTrailingSlash(properties.getOpenaiBaseUrl()))
                .apiKey(properties.getApiKey().trim())
                .modelName(properties.getModelName())
                .temperature(properties.getTemperature() != null ? properties.getTemperature() : 0.7)
                .maxTokens(properties.getMaxTokens() != null ? properties.getMaxTokens() : 8192)
                .timeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses());
        mergeStreamingCustomHeaders(b, properties);
        return b.build();
    }

    private static void validateApiKey(MinMaxProperties properties) {
        if (StrUtil.isBlank(properties.getApiKey())) {
            throw new IllegalStateException("已启用 yudao.minimax.enabled=true，但未配置 yudao.minimax.api-key");
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

    private static void mergeCustomHeaders(OpenAiChatModel.OpenAiChatModelBuilder b, MinMaxProperties p) {
        if (StrUtil.isNotBlank(p.getGroupId())) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Minimax-Group-Id", p.getGroupId().trim());
            b.customHeaders(headers);
        }
    }

    private static void mergeStreamingCustomHeaders(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder b,
                                                    MinMaxProperties p) {
        if (StrUtil.isNotBlank(p.getGroupId())) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Minimax-Group-Id", p.getGroupId().trim());
            b.customHeaders(headers);
        }
    }
}
