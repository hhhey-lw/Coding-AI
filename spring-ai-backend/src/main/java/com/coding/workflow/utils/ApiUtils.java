package com.coding.workflow.utils;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

/**
 * API 相关操作的工具类。提供生成 User-Agent 字符串和 HTTP 头的方法。
 *
 * @since 1.0.0.3
 */
public class ApiUtils {

	/** 用于 User-Agent 字符串的 SDK 标识 */
	private static final String SDK_FLAG = "agentscope";

	/**
	 * 生成包含 SDK 版本和系统信息的 User-Agent 字符串。
	 * @return 格式化后的 User-Agent 字符串
	 */
	public static String userAgent() {
		return String.format("%s/%s; java/%s; platform/%s; processor/%s", SDK_FLAG, "1.0.0-M1",
				System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.arch"));
	}

	/**
	 * 创建包含 User-Agent 信息的基础 HTTP 头。
	 * @return 包含基础头信息的 MultiValueMap
	 */
	public static MultiValueMap<String, String> getBaseHeaders() {
		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add(HttpHeaders.USER_AGENT, userAgent());

		return headers;
	}

}