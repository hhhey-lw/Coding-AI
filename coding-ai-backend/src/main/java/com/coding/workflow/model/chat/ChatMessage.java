package com.coding.workflow.model.chat;

import com.coding.workflow.enums.ContentTypeEnum;
import com.coding.workflow.enums.MessageRoleEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * 表示聊天会话中的一条消息。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {

	/** 消息发送者的角色 */
	@JsonProperty("role")
	private MessageRoleEnum role;

	/** 消息内容的类型 */
	@JsonProperty("content_type")
	@Builder.Default
	private ContentTypeEnum contentType = ContentTypeEnum.TEXT;

	/** 消息的实际内容 */
	@JsonProperty("content")
	@JsonDeserialize(using = ChatMessageContentDeserializer.class)
	private Object content;

	/** 消息发送者的名称 */
	@JsonProperty("name")
	private String name;

	/** 与消息关联的工具调用列表 */
	@JsonProperty("tool_calls")
	private List<ModelToolCall> modelToolCalls;

	/** 消息的推理内容 */
	@JsonProperty("reasoning_content")
	private String reasoningContent;


	/**
	 * 聊天消息内容的自定义反序列化器。用于处理不同类型的内容：数组、文本和其他 JSON 节点。
	 */
	public static class ChatMessageContentDeserializer extends JsonDeserializer<Object> {

		/**
		 * 将 JSON 内容反序列化为合适的对象类型。
		 * @param p JSON 解析器
		 * @param ctxt 反序列化上下文
		 * @return 反序列化后的对象（List<MultimodalContent>、String 或 JsonNode）
		 * @throws IOException 如果反序列化失败
		 */
		@Override
		public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			JsonNode node = p.getCodec().readTree(p);
			// TODO 补充多模态的处理
			if (node.isTextual()) {
				return node.asText();
			}
			else {
				return node;
			}
		}

	}

}