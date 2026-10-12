package com.inspiration_work.agent.tools;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

//改为手动构建，更灵活
//@AiService
public interface QwenStreamChatService {

    String chat(String userMessage);

    String chat(String systemMessage, String userMessage);


    // 流式对话
    Flux<String> chatStream(@UserMessage String userMessage);

    @SystemMessage("你是一个英语老师，名字叫做roy")
    Flux<String> chatStreamWithSystemMessage(@UserMessage String userMessage);

//    Flux<String> chatStreamWithMemoryId(@MemoryId int memoryId, @UserMessage String userMessage);

    Flux<String> chatStreamWith(@UserMessage String userMessage);

    // 技巧：将动态指令放在 UserMessage 的最前面，作为“上下文注入”
    @UserMessage("当前系统指令：{systemMessage}。\n\n用户问题：{userMessage}")
    String chatWithDynamicInstruction(@V("systemMessage") String systemMessage, @V("userMessage") String userMessage);

    @UserMessage("系统指令：{{systemMessage}}\n\n用户问题：{{userMessage}}")
    Flux<String> chatWithDynamicInstructionStream(@V("systemMessage") String systemMessage, @V("userMessage") String userMessage);
}
