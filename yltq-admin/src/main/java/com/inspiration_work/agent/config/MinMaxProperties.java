package com.inspiration_work.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

/**
 * MiniMax 接入配置。
 * <p>
 * 默认使用官方 HTTP API（{@code api-mode=official}），与文档一致：
 * {@code POST https://api.minimaxi.com/v1/text/chatcompletion_v2}。
 */
@ConfigurationProperties(prefix = "yudao.minimax")
@Data
public class MinMaxProperties {

    public static final String API_MODE_OFFICIAL = "official";
    public static final String API_MODE_OPENAI_COMPATIBLE = "openai-compatible";

    /**
     * 是否启用 Agent / MiniMax 相关 Bean 与接口。
     */
    private boolean enabled = false;

    /**
     * {@code official}：官方 chatcompletion_v2（默认）；
     * {@code openai-compatible}：LangChain4j OpenAI 兼容（需配置 openai-base-url）。
     */
    private String apiMode = API_MODE_OFFICIAL;

    /**
     * API Key（Bearer），建议环境变量注入。
     */
    private String apiKey = "";

    /**
     * 官方 API 根地址（不含路径），默认与 OpenAPI servers 一致。
     *
     * @see <a href="https://platform.minimaxi.com/docs/api-reference/text-chat">文本对话</a>
     */
    private String officialBaseUrl = "https://api.minimaxi.com";

    /**
     * 官方文本对话路径（相对 official-base-url）。
     */
    private String officialChatPath = "/v1/text/chatcompletion_v2";

    /**
     * OpenAI 兼容模式下的 baseUrl（须含版本前缀，如 {@code https://api.minimax.io/v1}），仅 api-mode=openai-compatible 时生效。
     */
    private String openaiBaseUrl = "https://api.minimax.io/v1";

    /**
     * 模型 ID：官方文档示例为 {@code MiniMax-M2.7}；以控制台为准。
     */
    private String modelName = "MiniMax-M2.7";

    private Double temperature = 0.7;

    /**
     * 对应官方字段 {@code max_completion_tokens}（上限以模型文档为准）。
     */
    private Integer maxTokens = 2048;

    private boolean logRequests = false;

    private boolean logResponses = false;

    /**
     * 是否注册 LangChain4j {@code OpenAiStreamingChatModel}（仅 openai-compatible 模式）。
     */
    private boolean streamingEnabled = true;

    /**
     * 可选 Header {@code Minimax-Group-Id}
     */
    private String groupId = "";

    /** HTTP 调用超时（秒） */
    private int timeoutSeconds = 120;

    public boolean isOfficialApiMode() {
        return apiMode == null
                || apiMode.isBlank()
                || API_MODE_OFFICIAL.equalsIgnoreCase(apiMode.trim());
    }

    public boolean isOpenaiCompatibleApiMode() {
        return API_MODE_OPENAI_COMPATIBLE.equalsIgnoreCase(
                apiMode == null ? "" : apiMode.trim().toLowerCase(Locale.ROOT));
    }

    /** 状态接口展示用完整官方 URL */
    public String resolveOfficialEndpointDisplay() {
        String base = officialBaseUrl == null ? "" : officialBaseUrl.trim();
        String path = officialChatPath == null ? "" : officialChatPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }
}
