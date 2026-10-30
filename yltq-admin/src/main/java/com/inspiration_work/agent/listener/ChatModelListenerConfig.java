package com.inspiration_work.agent.listener;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ChatModelListenerConfig {
    
    @Bean
    ChatModelListener chatModelListener() {
        return new ChatModelListener() {
            @Override
            public void onRequest(ChatModelRequestContext requestContext) {
                log.debug("模型请求，消息数：{}", requestContext.chatRequest().messages().size());
            }

            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                log.debug("模型响应完成");
            }

            @Override
            public void onError(ChatModelErrorContext errorContext) {
                log.warn("模型调用失败：{}", errorContext.error().getClass().getSimpleName());
            }
        };
    }
}