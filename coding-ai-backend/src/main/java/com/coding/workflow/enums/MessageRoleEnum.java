package com.coding.workflow.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;


@Getter
public enum MessageRoleEnum {

	/** 表示来自用户的消息 */
	@JsonProperty("user")
	USER("user"),

	/** 表示来自 AI 助手的消息 */
	@JsonProperty("assistant")
	ASSISTANT("assistant"),

	/** 表示系统消息 */
	@JsonProperty("system")
	SYSTEM("system"),

	/** 表示来自工具的消息 */
	@JsonProperty("tool")
	TOOL("tool");

	/** 消息角色的字符串值 */
	private final String value;

	MessageRoleEnum(String value) {
		this.value = value;
	}

	/**
	 * 将字符串值转换为对应的 MessageRole 枚举。
	 * @param value 要转换的字符串值
	 * @return 对应的 MessageRole 枚举
	 * @throws IllegalArgumentException 如果值无效
	 */
	public static MessageRoleEnum of(String value) {
		for (MessageRoleEnum messageRoleEnum : MessageRoleEnum.values()) {
			if (messageRoleEnum.getValue().equals(value)) {
				return messageRoleEnum;
			}
		}

		throw new IllegalArgumentException("无效的 MessageType 值: " + value);
	}

}
