package com.inspiration_work.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - Agent MiniMax 对话响应")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatRespVO {

    @Schema(description = "模型回复正文")
    private String reply;
}
