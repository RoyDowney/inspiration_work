package com.inspiration_work.agent.tools;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class QwenServiceFactory {

    @Resource
    private ChatModel qwenChatModel;

    @Resource
    private StreamingChatModel qwenStreamingChatModel;

    /**
     * 无 ChatMemory：页联调 / 代码生成等单次请求体积极大，累积历史会把上下文撑满并导致模型输出在尾部截断（tables、listFields 等缺失）。
     * 需要多轮对话时请另建带 memory 的 AiService。
     */
    @Bean
    public QwenStreamChatService qwenStreamChatService() {
        return AiServices.builder(QwenStreamChatService.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .build();
    }
}
