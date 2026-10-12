package com.inspiration_work.agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 始终加载 {@link MinMaxProperties}，与 {@link MinMaxModelConfig} 是否启用无关。
 */
@Configuration
@EnableConfigurationProperties(MinMaxProperties.class)
public class MinMaxPropertiesConfiguration {
}
