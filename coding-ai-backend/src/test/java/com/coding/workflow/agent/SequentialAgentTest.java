package com.coding.workflow.agent;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.agent.flow.LlmRoutingAgent;
import com.coding.graph.core.agent.flow.SequentialAgent;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@SpringBootTest
public class SequentialAgentTest {
    @Resource
    private ChatModel chatModel;

    @Test
    public void testLlmRoutingAgent() throws GraphStateException {
        KeyStrategyFactory stateFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("input", new ReplaceStrategy());
            keyStrategyHashMap.put("reviewed_article", new ReplaceStrategy());
            keyStrategyHashMap.put("article", new ReplaceStrategy());
            return keyStrategyHashMap;
        };

        boolean isStream = true;

        ReactAgent proseWriterAgent = ReactAgent.build(chatModel, ReactAgent.builder()
                .name("writer_agent")
                .modelName("qwen-plus")
                .description("可以写文章。")
                .instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
                .stream(isStream)
                .inputKey("messages")
                .outputKey("article")
                .build());

        ReactAgent poemWriterAgent = ReactAgent.build(chatModel, ReactAgent.builder()
                .name("reviewer_agent")
                .modelName("qwen-plus")
                .description("可以对文章进行评论和修改。")
                .instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。")
                .stream(isStream)
                .inputKey("messages")
                .outputKey("reviewed_article")
                .build());

        SequentialAgent blogAgent = SequentialAgent.builder()
                .name("blog_agent")
                .state(stateFactory)
                .description("可以根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论，必要时做出修改。")
                .inputKey("input")
                .outputKey("reviewed_article")
                .subAgents(List.of(proseWriterAgent, poemWriterAgent))
                .build();

        try {
//            Optional<OverAllState> result = blogAgent.invoke(Map.of("input", "帮我写一首诗"));
//            result.ifPresent(System.out::println);
//            Thread.sleep(10000L);
            AsyncGenerator<NodeOutput> stream = blogAgent.stream(Map.of("input", "帮我写一个100字左右的散文"));
            Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
            processStream(stream, sink).get();
            Thread.sleep(10000L);
        }
        catch (Exception e) {
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
                }
                else {
                    JSONObject nodeOutput = new JSONObject();
                    nodeOutput.put("data", output.getState().data());
                    nodeOutput.put("node", nodeName);
                    content = JSONUtil.toJsonStr(nodeOutput);
                }
                System.out.println("Received output: " + content);
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
}
