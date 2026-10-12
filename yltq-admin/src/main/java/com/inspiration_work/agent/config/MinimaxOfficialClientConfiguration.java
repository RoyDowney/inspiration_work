package com.inspiration_work.agent.config;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspiration_work.agent.minimax.MinimaxOfficialApiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 官方 chatcompletion_v2 HTTP 客户端（默认 api-mode）。
 */
@Configuration
@ConditionalOnProperty(prefix = "yudao.minimax", name = "enabled", havingValue = "true")
public class MinimaxOfficialClientConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "yudao.minimax", name = "api-mode", havingValue = "official", matchIfMissing = true)
    public MinimaxOfficialApiClient minimaxOfficialApiClient(MinMaxProperties properties, ObjectMapper objectMapper) {
        if (StrUtil.isBlank(properties.getApiKey())) {
            throw new IllegalStateException("已启用 yudao.minimax.enabled=true，但未配置 yudao.minimax.api-key");
        }
        return new MinimaxOfficialApiClient(properties, objectMapper);
    }
}
