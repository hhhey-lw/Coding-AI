package com.coding.admin.manager;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coding.admin.manager.tool.ImageGenerateService;
import com.coding.admin.manager.tool.MusicGenerateService;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.StreamingOutput;
import com.coding.graph.core.node.impl.LlmNode;
import com.coding.graph.core.node.impl.ToolNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class AgentManager {

    private final ChatClient chatClient;

    private final ToolCallbackResolver resolver;

    public AgentManager(ChatModel chatModel, ToolCallbackResolver resolver) {
        this.resolver = resolver;
        this.chatClient = ChatClient.builder(chatModel)
                // 实现 Chat Memory 的 Advisor
                // 在使用 Chat Memory 时，需要指定对话 ID，以便 Spring AI 处理上下文。  好坑啊：!!! 这个功能会多生成会话消息
//                .defaultAdvisors(
//                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build()
//                )
                // 实现 Logger 的 Advisor
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .internalToolExecutionEnabled(false)
                                .parallelToolCalls(false)
                                .model("qwen-max")
                                .build()
                )
                .build();
    }

    public ReactAgent buildAgent() throws GraphStateException {
        List<ToolCallback> toolCallbacks = List.of(FunctionToolCallback.builder("generateMusic", new MusicGenerateService())
                .description("根据提示词和歌词，生成一段音乐，并返回音乐的URL地址")
                .inputType(MusicGenerateService.Request.class)
                .build(), FunctionToolCallback.builder("generateImage", new ImageGenerateService())
                .description("根据提示词和参考图生成图片，并返回图片的URL地址")
                .inputType(ImageGenerateService.Request.class)
                .build());

        ReactAgent.Builder builder = ReactAgent.Builder.builder()
                .name("react-agent")
                .inputKey("messages")
                .resolver(resolver)
                .maxIterations(10)
                .build();

        LlmNode llmNode = LlmNode.builder()
                .systemPrompt("你是一个乐于助人的助手，能够根据用户的需求，调用相关的工具生成音乐和图片。")
                .chatClient(chatClient)
                .toolCallbacks(toolCallbacks)
                .model("qwen-max")
                .messagesKey("messages")
                .stream(true)
                .build();

        ToolNode toolNode = ToolNode.builder()
                .llmResponseKey(LlmNode.LLM_RESPONSE_KEY)
                .toolCallbackResolver(resolver)
                .toolCallbacks(toolCallbacks)
                .build();

        return new ReactAgent(llmNode, toolNode, builder);
    }

}
