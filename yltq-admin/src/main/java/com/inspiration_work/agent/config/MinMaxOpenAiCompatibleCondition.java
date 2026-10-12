package com.inspiration_work.agent.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@code yudao.minimax.enabled=true} 且 {@code yudao.minimax.api-mode=openai-compatible}。
 */
public class MinMaxOpenAiCompatibleCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        if (!"true".equalsIgnoreCase(env.getProperty("yudao.minimax.enabled", "false"))) {
            return false;
        }
        String mode = env.getProperty("yudao.minimax.api-mode", MinMaxProperties.API_MODE_OFFICIAL);
        return MinMaxProperties.API_MODE_OPENAI_COMPATIBLE.equalsIgnoreCase(
                mode == null ? "" : mode.trim());
    }
}
