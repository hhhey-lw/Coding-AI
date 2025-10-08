package com.coding.workflow.model.chat;

import com.coding.workflow.enums.AgentStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.coding.workflow.exception.error.Error;

import java.io.Serializable;

/**
 * 代理完成请求的响应模型。
 *
 * @since 1.0.0.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse implements Serializable {

	/** 请求的唯一标识符 */
	@JsonProperty("request_id")
	private String requestId;

	/** 会话的唯一标识符 */
	@JsonProperty("conversation_id")
	private String conversationId;

	/** 代理的当前状态 */
	private AgentStatusEnum status;

	/** 会话中响应的索引 */
	private String index;

	/** 聊天消息内容 */
	private ChatMessage message;

	/** 响应创建的时间戳 */
	private Long created;

	/** 用于生成响应的模型标识符 */
	private String model;

	/** 请求的使用统计信息 */
	private ModelUsage modelUsage;

	/** 请求失败时的错误信息 */
	private Error error;

	/** 检查响应是否成功 */
	@JsonIgnore
	public boolean isSuccess() {
		return error == null;
	}

}