package com.coding.workflow.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 枚举，表示聊天消息中不同类型的内容。目前支持文本和多模态内容类型。
 *
 * @since 1.0.0.3
 */
@Getter
@AllArgsConstructor
public enum ContentTypeEnum {

	/** 表示纯文本内容 */
	@JsonProperty("text")
	TEXT("text"),

	/** 表示多模态内容（如带图片的文本） */
	@JsonProperty("multimodal")
	MULTIMODAL("multimodal"),;

	/** 内容类型的字符串值 */
	private final String value;

}