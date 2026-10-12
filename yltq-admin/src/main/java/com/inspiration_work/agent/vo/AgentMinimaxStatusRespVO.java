package com.inspiration_work.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "MiniMax 集成状态")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMinimaxStatusRespVO {

    @Schema(description = "yudao.minimax.enabled 且当前 api-mode 下客户端已就绪")
    private Boolean available;

    @Schema(description = "yudao.minimax.api-mode：official | openai-compatible")
    private String apiMode;

    @Schema(description = "模型名")
    private String modelName;

    @Schema(description = "official 模式下完整 HTTP 端点；openai-compatible 时为 openai-base-url")
    private String endpointDisplay;
}
