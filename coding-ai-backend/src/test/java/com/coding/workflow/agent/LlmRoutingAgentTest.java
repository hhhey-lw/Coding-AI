package com.coding.workflow.agent;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.agent.flow.LlmRoutingAgent;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.StreamingOutput;
import com.coding.graph.core.state.OverAllState;
import com.coding.graph.core.state.strategy.KeyStrategy;
import com.coding.graph.core.state.strategy.KeyStrategyFactory;
import com.coding.graph.core.state.strategy.ReplaceStrategy;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@SpringBootTest
public class LlmRoutingAgentTest {
    @Resource
    private ChatModel chatModel;

    @Test
    public void testLlmRoutingAgent() throws GraphStateException {
        KeyStrategyFactory stateFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("input", new ReplaceStrategy());
            keyStrategyHashMap.put("topic", new ReplaceStrategy());
            keyStrategyHashMap.put("article", new ReplaceStrategy());
            return keyStrategyHashMap;
        };

        boolean isStream = true;

        ReactAgent proseWriterAgent = ReactAgent.builder()
                .name("prose_writer_agent")
                .modelName("qwen-plus")
                .description("可以写散文文章。")
                .instruction("你是一个知名的作家，擅长写散文。请根据用户的提问进行回答。")
                .chatModel(chatModel)
                .stream(isStream)
                .inputKey("messages")
                .outputKey("prose_article")
                .build();

        ReactAgent poemWriterAgent = ReactAgent.builder()
                .name("poem_writer_agent")
                .modelName("qwen-plus")
                .description("可以写现代诗。")
                .instruction("你是一个知名的诗人，擅长写现代诗。请根据用户的提问进行回答。")
                .chatModel(chatModel)
                .stream(isStream)
                .inputKey("messages")
                .outputKey("poem_article")
                .build();

        LlmRoutingAgent blogAgent = LlmRoutingAgent.builder()
                .name("blog_agent")
                .model(chatModel)
                .state(stateFactory)
                .description("可以根据用户给定的主题写文章或作诗。")
                .inputKey("input")
                .outputKey("topic")
                .subAgents(List.of(proseWriterAgent, poemWriterAgent))
                .build();

        try {
//            Optional<OverAllState> result = blogAgent.invoke(Map.of("input", "帮我写一首诗"));
//            result.ifPresent(System.out::println);
//            Thread.sleep(10000L);
            AsyncGenerator<NodeOutput> stream = blogAgent.stream(Map.of("input", "帮我写一首诗"));
            Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
            processStream(stream, sink).get();
            Thread.sleep(10000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    CompletableFuture<Void> processStream(AsyncGenerator<NodeOutput> generator,
                                          Sinks.Many<ServerSentEvent<String>> sink) {
        return generator.forEachAsync(output -> {
            try {
                String nodeName = output.getNode();
                String content;
                if (output instanceof StreamingOutput streamingOutput) {
                    content = JSONUtil.toJsonStr(Map.of(nodeName, streamingOutput.getChatResponse().getResult().getOutput().getText()));
                } else {
                    JSONObject nodeOutput = new JSONObject();
                    nodeOutput.put("data", output.getState().data());
                    nodeOutput.put("node", nodeName);
                    content = JSONUtil.toJsonStr(nodeOutput);
                }
                System.out.println("Received output: " + content);
                sink.tryEmitNext(ServerSentEvent.builder(content).build());
            } catch (Exception e) {
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
}
