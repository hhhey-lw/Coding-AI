package com.coding.workflow.agent;

import cn.hutool.json.JSONUtil;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.StreamingOutput;
import com.coding.graph.core.node.impl.LlmNode;
import com.coding.graph.core.node.impl.ToolNode;
import com.coding.graph.core.state.OverAllState;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@SpringBootTest
public class ReactAgentTest {

    @Resource
    private ChatModel chatModel;

    @Resource
    private ToolCallbackResolver resolver;

    @Test
    public void test() throws GraphStateException, GraphRunnerException, ExecutionException, InterruptedException {

        ChatClient chatClient = ChatClient.builder(chatModel)
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
                .systemPrompt("你是一个乐于助人的助手")
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

        ReactAgent reactAgent = new ReactAgent(llmNode, toolNode, builder);
//        Optional<OverAllState> result = reactAgent.invoke(Map.of(
//                "messages", List.of(new UserMessage("""
//                        Hi, 请你生成一张海边落日的图片，参考图是 https://example.com/reference.jpg
//                        还要生成一段音乐，歌词和音乐风格你可以自己发挥。
//                        """))
//        ));
//        System.out.println(JSONUtil.toJsonStr(result.get()));

        AsyncGenerator<NodeOutput> result = reactAgent.stream(Map.of(
                "messages", List.of(new UserMessage("""
                        Hi, 请你生成一张海边落日的图片，参考图是 https://example.com/reference.jpg
                        还要生成一段音乐，歌词和音乐风格你可以自己发挥。
                        """))
        ));

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
        processStream(result, sink).get();
        Thread.sleep(10000L);
    }

    CompletableFuture<Void> processStream(AsyncGenerator<NodeOutput> generator,
                                          Sinks.Many<ServerSentEvent<String>> sink) {
        return generator.forEachAsync(output -> {
            try {
                System.out.println(output);
                String nodeName = output.getNode();
                String content;
                if (output instanceof StreamingOutput streamingOutput) {
                    content = JSONUtil.toJsonStr(Map.of(nodeName, streamingOutput.getChunk()));
                }
                else {
                    Map<String, Object> data = Map.of(
                            "data", output.getState().data(),
                            "node", nodeName
                    );
                    content = JSONUtil.toJsonStr(data);
                }
                sink.tryEmitNext(ServerSentEvent.builder(content).build());
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        }).thenAccept(v -> {
            // 正常完成
            sink.tryEmitComplete();
        }).exceptionally(e -> {
            sink.tryEmitError(e);
            return null;
        });
    }

    static class CustomerTool {

        @Tool(description = "根据提示词和歌词，生成一段音乐，并返回音乐的URL地址")
        String generateMusic(@ToolParam(description = "描述音乐的风格") String prompt,
                             @ToolParam(description = "歌词内容") String lyrics) {
            return "https://example.com/music.mp3";
        }

        @Tool(description = "根据提示词和参考图生成图片，并返回图片的URL地址")
        String generateImage(@ToolParam(description = "生成图片的提示词") String prompt,
                             @ToolParam(description = "参考图URL地址") String referenceImage) {
            return "https://example.com/image.png";
        }
    }

}
