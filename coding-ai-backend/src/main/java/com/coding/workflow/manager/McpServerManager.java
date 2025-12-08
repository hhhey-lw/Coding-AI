package com.coding.workflow.manager;

import com.coding.core.config.AiServiceConfigProperties;
import com.coding.core.service.AiMcpConfigService;
import com.coding.core.service.AiProviderConfigService;
import com.coding.workflow.model.chat.Content;
import com.coding.workflow.model.request.McpServerCallToolRequest;
import com.coding.workflow.model.response.McpServerCallToolResponse;
import com.coding.workflow.model.chat.TextContent;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * @author weilong
 * @date 2025/9/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerManager {

    private final AiMcpConfigService aiMcpConfigService;
    private final AiProviderConfigService aiProviderConfigService;

    public McpServerCallToolResponse callTool(McpServerCallToolRequest request) {
        // 1. 从YAML配置获取MCP服务器配置
        AiServiceConfigProperties.McpServerConfig mcpServer = aiMcpConfigService.getByServerCode(request.getServerCode());
        if (mcpServer == null) {
            throw new RuntimeException("MCP服务器配置不存在: " + request.getServerCode());
        }

        // 2. 从YAML配置获取提供商配置
        AiServiceConfigProperties.ProviderConfig providerConfig = aiProviderConfigService.getByProviderCodeAndServiceType(
            mcpServer.getProviderCode(), "MCP");
        if (providerConfig == null) {
            throw new RuntimeException("提供商配置不存在: " + mcpServer.getProviderCode());
        }

        // 3. 创建Client客户端
        McpSyncClient client = buildMcpSyncClient(providerConfig, mcpServer);

        // 4. 构建MCP请求参数
        McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest(
                request.getToolName(), request.getToolParams());

        McpServerCallToolResponse response = new McpServerCallToolResponse();
        try {
            // 5. 执行MCP请求
            client.initialize();

            McpSchema.CallToolResult callToolResult = client.callTool(callToolRequest);

            // 6. 解析MCP执行结果
            response.setIsError(callToolResult.isError());
            List<McpSchema.Content> contentList = callToolResult.content();
            List<Content> content = new ArrayList<>();
            contentList.forEach(item -> {
                if (item.type().equals("text")) {
                    TextContent contentItem = new TextContent();
                    contentItem.setType(item.type());
                    McpSchema.TextContent textContent = (McpSchema.TextContent) item;
                    contentItem.setText(textContent.text());
                    content.add(contentItem);
                }
            });
            response.setContent(content);
        } catch (Exception e) {
            log.error("调用MCP工具失败", e);
            throw e;
        } finally {
            if (client != null) {
                client.close();
            }
        }

        return response;
    }

    /**
     * 构建MCP同步客户端
     *
     * @param providerConfig 提供商配置
     * @param mcpServer MCP服务器配置
     * @return MCP同步客户端
     */
    private McpSyncClient buildMcpSyncClient(AiServiceConfigProperties.ProviderConfig providerConfig, AiServiceConfigProperties.McpServerConfig mcpServer) {
        // 创建HTTP客户端
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10));

        // 创建传输层
        HttpClientSseClientTransport transport = HttpClientSseClientTransport
                .builder(providerConfig.getBaseUrl())
                .sseEndpoint(mcpServer.getEndpoint())
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .clientBuilder(httpClientBuilder)
                .httpRequestCustomizer((builder, method, endpoint, body, context) -> {
                    if (providerConfig.getAuthorization() != null) {
                        if (providerConfig.getAuthorization().startsWith("Bearer ")) {
                            builder.header("Authorization", providerConfig.getAuthorization());
                        } else {
                            builder.header("Authorization", "Bearer " + providerConfig.getAuthorization());
                        }
                    }
                })
                .build();

        // 创建并配置同步客户端
        return McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .initializationTimeout(Duration.ofSeconds(10))
                .capabilities(McpSchema.ClientCapabilities.builder()
                        .roots(true)
                        .sampling()
                        .build())
                .build();
    }
}
