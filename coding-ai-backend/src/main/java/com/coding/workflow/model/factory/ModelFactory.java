package com.coding.workflow.model.factory;

import cn.hutool.core.util.StrUtil;
import com.coding.core.config.AiServiceConfigProperties;
import com.coding.core.service.AiProviderConfigService;
import com.coding.workflow.exception.BizException;
import com.coding.workflow.utils.ApiUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelFactory {

    private final AiProviderConfigService aiProviderConfigService;

    public ChatModel getChatModel(String provider, String serviceType) {
        // 从YAML配置获取提供商配置
        AiServiceConfigProperties.ProviderConfig providerConfig = aiProviderConfigService.getByProviderCodeAndServiceType(provider, serviceType);
        if (providerConfig == null) {
            throw new BizException("不支持的模型提供商：" + provider + "，服务类型：" + serviceType);
        }

        OpenAiApi openAiApi = buildOpenAiApi(providerConfig);
        return OpenAiChatModel.builder().openAiApi(openAiApi).build();
    }

    /**
     * 构建OpenAI API客户端
     */
    private OpenAiApi buildOpenAiApi(AiServiceConfigProperties.ProviderConfig providerConfig) {
        OpenAiApi.Builder openAiApiBuilder = OpenAiApi.builder()
                .apiKey(providerConfig.getAuthorization())
                .responseErrorHandler(new ResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) throws IOException {
                        return response.getStatusCode().isError();
                    }

                    @Override
                    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
                        if (response.getStatusCode().isError()) {
                            String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                            log.error("调用模型失败, code: {}, body: {}", response.getStatusCode(), body);
                            throw new BizException("模型调用失败！");
                        }
                    }
                })
                .headers(ApiUtils.getBaseHeaders());
        if (StrUtil.isNotBlank(providerConfig.getBaseUrl())) {
            String endpoint = providerConfig.getBaseUrl();

            // 移除末尾的 /v1 或 /v1/，因为 OpenAiApi.Builder 会自动添加 /v1
            if (endpoint.endsWith("/v1") || endpoint.endsWith("/v1/")) {
                endpoint = endpoint.replaceAll("/v1/?$", "");
            }

            openAiApiBuilder.baseUrl(endpoint);
        }

        return openAiApiBuilder.build();
    }

}
