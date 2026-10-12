package com.inspiration_work.agent.service;

import cn.hutool.core.util.StrUtil;
import com.inspiration_work.agent.config.MinMaxProperties;
import com.inspiration_work.agent.minimax.MinimaxOfficialApiClient;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


@Service
@Slf4j
public class MinMaxServiceImpl implements MinMaxService {

    @Resource
    private MinMaxProperties minimaxProperties;
    @Resource
    private ObjectProvider<MinimaxOfficialApiClient> minimaxOfficialApiClientProvider;
    @Resource
    private ObjectProvider<OpenAiChatModel> minimaxChatModelProvider;

    private static  String AGENT_MINIMAX_NOT_ENABLED = "MiniMax 未启用：请在配置中设置 yudao.minimax.enabled=true 并配置 api-key";
    private static  String AGENT_MINIMAX_KEY_MISSING ="MiniMax 模型未初始化：请检查 yudao.minimax.api-key 是否已配置";
    private static  String AGENT_MINIMAX_CHAT_FAIL = "MiniMax 对话失败：";
    private static  String AGENT_MINIMAX_HTML_INVALID = "";

    @Override
    public boolean available() {
        if (!minimaxProperties.isEnabled()) {
            return false;
        }
        if (minimaxProperties.isOfficialApiMode()) {
            return minimaxOfficialApiClientProvider.getIfAvailable() != null;
        }
        return minimaxChatModelProvider.getIfAvailable() != null;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) throws Exception {
        if (!minimaxProperties.isEnabled()) {
            throw new Exception(AGENT_MINIMAX_NOT_ENABLED);
        }
        if (StrUtil.isBlank(userMessage)) {
            throw new Exception(AGENT_MINIMAX_CHAT_FAIL);
        }
        if (minimaxProperties.isOfficialApiMode()) {
            MinimaxOfficialApiClient client = minimaxOfficialApiClientProvider.getIfAvailable();
            if (client == null) {
                throw new Exception(AGENT_MINIMAX_KEY_MISSING);
            }
            try {
                return StrUtil.nullToEmpty(client.chatCompletionSync(systemPrompt, userMessage.trim()));
            } catch (Exception e) {
                log.warn("[MinMaxServiceImpl][chat][official] fail", e);
                throw new Exception(AGENT_MINIMAX_CHAT_FAIL);
            }
        }
        OpenAiChatModel model = minimaxChatModelProvider.getIfAvailable();
        if (model == null) {
            throw new Exception(AGENT_MINIMAX_KEY_MISSING);
        }
        List<ChatMessage> messages = new ArrayList<>();
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.add(UserMessage.from(userMessage.trim()));
        ChatRequest request = ChatRequest.builder().messages(messages).build();
        try {
            String text = model.chat(request).aiMessage().text();
            return StrUtil.nullToEmpty(text);
        } catch (Exception e) {
            log.warn("[MinMaxServiceImpl][chat][openai] fail", e);
            throw new Exception(AGENT_MINIMAX_HTML_INVALID);
        }
    }

    @Override
    public void chatStream(String systemPrompt, String userMessage, Consumer<String> onDelta) throws Exception {
        if (!minimaxProperties.isEnabled()) {
            throw new Exception(AGENT_MINIMAX_NOT_ENABLED);
        }
        if (StrUtil.isBlank(userMessage)) {
            throw new Exception(AGENT_MINIMAX_CHAT_FAIL);
        }
        if (!minimaxProperties.isOfficialApiMode()) {
            throw new Exception(AGENT_MINIMAX_CHAT_FAIL);
        }
        MinimaxOfficialApiClient client = minimaxOfficialApiClientProvider.getIfAvailable();
        if (client == null) {
            throw new Exception(AGENT_MINIMAX_KEY_MISSING);
        }
        try {
            client.chatCompletionStream(systemPrompt, userMessage.trim(), onDelta);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception(AGENT_MINIMAX_CHAT_FAIL);
        } catch (IOException e) {
            throw new Exception(AGENT_MINIMAX_CHAT_FAIL);
        }
    }

    private static String describeOfficialHttpFailure(Throwable e) {
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        if (msg.contains("header parser received no bytes")) {
            return msg + "。常见原因：请求 JSON 过大被对端直接断开、official-base-url 不可达、或网络/代理中断；"
                    + "若调用 chat-with-html 或 /infra/codegen/* /v2（AI 解析 HTML），请缩小 HTML、减少多文件同传或改用 v1 本地解析；"
                    + "并检查网关/代理对 POST body 的大小限制。";
        }
        return msg;
    }
}
