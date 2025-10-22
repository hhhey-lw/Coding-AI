package com.coding.workflow.agent;

import cn.hutool.json.JSONObject;
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
import org.springframework.ai.chat.prompt.ChatOptions;
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
                .systemPrompt("""
                        你是一个乐于助人、聪明且细心的 AI 助手，你的目标是尽可能准确、高效地帮助用户解决问题。
                        你具备调用特定工具的能力，可以帮助你更好地完成用户请求。当你认为调用某个工具能够推进任务时，你应该明确说明你的思考过程，并按照规定的格式调用工具。
                        请遵循以下规则：
                        🧠 规则 1：先思考，再行动：
                        - 在回答用户问题之前，先分析问题，明确你接下来要做什么。
                        - 如果任务需要获取额外信息、生成媒体内容、执行计算等，考虑是否需要调用工具。
                        - 你的每一步都应该包含清晰的思考（Thought），说明你为什么这么做。
                        🛠️ 规则 2：如果你决定调用工具，请明确工具名称和参数：
                        📝 规则 3：如果不需要调用工具，直接给出问题的答案：
                        - 如果你认为不需要调用任何工具就能直接回答用户问题，请直接输出答案。
                        🔄 规则 4：如果工具返回了结果（Observation），请基于结果进一步思考并给出最终回答
                        - 当你获得工具的执行结果后，结合上下文，给出清晰、完整的最终回复。
                        （如果工具被调用，后续你可能还会收到工具的返回结果，然后继续输出下一轮的 Thought 和 Action）
                        """)
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
//                         你好呀！！
//                        """))
//        ));
//        System.out.println(JSONUtil.toJsonStr(result.get().data()));



        AsyncGenerator<NodeOutput> result = reactAgent.stream(Map.of(
                "messages", List.of(new UserMessage("""
                        Hi, 请你生成一张海边落日的图片，参考图是 https://example.com/reference.jpg。还要生成一段音乐，歌词和音乐风格你可以自己发挥。
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
//                System.out.println("Received output: " + output);
                String nodeName = output.getNode();
                String content;
                if (output instanceof StreamingOutput streamingOutput) {
                    content = JSONUtil.toJsonStr(Map.of(nodeName, streamingOutput.getChatResponse().getResult().getOutput().getText()));
                }
                else {
                    JSONObject nodeOutput = new JSONObject();
                    nodeOutput.put("data", output.getState().data());
                    nodeOutput.put("node", nodeName);
                    content = JSONUtil.toJsonStr(nodeOutput);
                }
                System.out.println(">> " + content);
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
