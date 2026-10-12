package com.inspiration_work.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - Agent MiniMax 对话请求")
@Data
public class AgentChatReqVO {

    @Schema(description = "系统提示词，可选")
    private String systemPrompt;

    @Schema(description = "用户消息")
    @NotBlank(message = "用户消息不能为空")
    private String message;
}
