package com.inspiration_work.agent.tools;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


@Service
@ConditionalOnProperty(prefix = "agent.qwen", name = "enabled", havingValue = "true")
@Slf4j
public class QwenChatService {

    @Resource
    private ChatModel qwenChatModel;

    @Resource
    private QwenStreamChatService qwenStreamChatService;

    private static final String SYSTEM_MESSAGE = """

            """;

    public String chat(String message) {
        SystemMessage systemMessage = SystemMessage.from(SYSTEM_MESSAGE);
        UserMessage userMessage = UserMessage.from(message);
        ChatResponse chatResponse = qwenChatModel.chat(systemMessage, userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.debug("AI 响应完成");
        return aiMessage.text();
    }

    public String chat(UserMessage userMessage) {
        ChatResponse chatResponse = qwenChatModel.chat(userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.debug("AI 响应完成");
        return aiMessage.text();
    }

    public String chat(SystemMessage systemMessage, UserMessage userMessage) {
        ChatResponse chatResponse = qwenChatModel.chat(systemMessage, userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.debug("AI 响应完成");
        return aiMessage.text();
    }


}
