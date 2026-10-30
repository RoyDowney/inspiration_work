package com.inspiration_work.agent.model;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * 显式注册流式模型并设置 {@code maxTokens}，避免全页联调等大 JSON 在默认输出上限处被截断（表现为 tables / pageModuleBlueprints 不完整）。
 * <p>
 * 与 starter 自动配置同名 Bean 时依赖 {@code spring.main.allow-bean-definition-overriding=true}，本配置优先（{@link Primary}）。
 */
@Configuration
@ConditionalOnProperty(prefix = "agent.qwen", name = "enabled", havingValue = "true")
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.streaming-chat-model")
@Data
public class QwenStreamingChatModelConfig {

    private String modelName;

    private String apiKey;

    /**
     * 对齐 MiniMax 侧大 JSON 场景；可在 yaml 中 {@code max-tokens} 覆盖。
     */
    private Integer maxTokens = 32_768;

    @Resource
    private ChatModelListener chatModelListener;

    @Bean(name = "qwenStreamingChatModel")
    @Primary
    public StreamingChatModel qwenStreamingChatModel() {
        var b = QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName);
        if (maxTokens != null && maxTokens > 0) {
            b.maxTokens(maxTokens);
        }
        if (chatModelListener != null) {
            b.listeners(List.of(chatModelListener));
        }
        return b.build();
    }
}
