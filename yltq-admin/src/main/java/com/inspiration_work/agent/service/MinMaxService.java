package com.inspiration_work.agent.service;

import java.util.function.Consumer;

/**
 * MiniMax 对话：默认走官方 chatcompletion_v2；可选 OpenAI 兼容（LangChain4j）。
 */
public interface MinMaxService {

    boolean available();

    /**
     * 非流式完整回复。
     */
    String chat(String systemPrompt, String userMessage) throws Exception;

    /**
     * 流式：按服务端返回的增量多次回调 {@code onDelta}（UTF-8 文本片段）。
     * <p>
     * 当前实现仅在 {@code yudao.minimax.api-mode=official} 下可用。
     */
    void chatStream(String systemPrompt, String userMessage, Consumer<String> onDelta) throws Exception;
}
