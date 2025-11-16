package com.coding.workflow.model.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 表示包含音频数据和元数据的音频输出响应。
 *
 * @since 1.0.0.3
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AudioOutput implements Serializable {

	/** 音频输出的唯一标识符 */
	@JsonProperty("id")
	private String id;

	/** Base64 编码的音频数据 */
	@JsonProperty("data")
	private String data;

	/** 音频数据的过期时间戳 */
	@JsonProperty("expires_at")
	private Long expiresAt;

	/** 音频内容的文本转录 */
	@JsonProperty("transcript")
	private String transcript;

}