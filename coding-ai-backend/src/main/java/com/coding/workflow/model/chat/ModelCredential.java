package com.coding.workflow.model.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModelCredential implements Serializable {

	/** 模型服务的 API 端点 URL */
	private String endpoint;

	/** 认证用的 API 密钥 */
	@JsonProperty("api_key")
	private String apiKey;

}