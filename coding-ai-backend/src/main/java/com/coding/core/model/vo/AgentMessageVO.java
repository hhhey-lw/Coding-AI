package com.coding.core.model.vo;

import com.coding.core.enums.AgentMessageRoleEnum;
import com.coding.core.enums.AgentMessageTypeEnum;
import lombok.*;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessageVO {

    /**
     * 角色 @see com.coding.core.enums.AgentMessageRoleEnum
     */
    private String role;

    /**
     * 事件类型 @see com.coding.core.enums.AgentMessageTypeEnum
     */
    private String type;

    /**
     * 正文内容
     */
    private String content;

    /**
     * 推理内容
     */
    private String reasoningContent;
    
    /**
     * 工具调用列表（TOOL_CALL 时使用）
     */
    private List<AgentToolCallVO> toolCalls;

    /**
     * 工具返回结果（TOOL_RESULT 时使用）
     */
    private List<AgentToolResponseVO> toolResponses;

    /**
     * 构建执行完成消息
     */
    public static AgentMessageVO buildFinishMessage() {
        return AgentMessageVO.builder()
                .role(AgentMessageRoleEnum.ASSISTANT.name())
                .type(AgentMessageTypeEnum.STREAM_END.name())
                .content("执行完成")
                .build();
    }

}

