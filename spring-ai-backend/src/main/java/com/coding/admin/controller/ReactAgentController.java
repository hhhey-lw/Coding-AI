package com.coding.admin.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coding.admin.common.MessageVO;
import com.coding.admin.manager.AgentManager;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.StreamingOutput;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/ai/agent")
public class ReactAgentController {

    @Resource
    private AgentManager agentManager;

    private ReactAgent reactAgent;

    @PostConstruct
    private void init() throws GraphStateException {
        this.reactAgent = agentManager.buildAgent();
    }

    /**
     * SSE 流式接口：接收 prompt，返回 Server-Sent Events 流
     */
    @GetMapping(value = "/react", produces = "text/event-stream;charset=UTF-8")
    public Flux<ServerSentEvent<String>> streamAgent(@RequestParam("prompt") String prompt) throws GraphStateException, GraphRunnerException {
        // 1. 调用 Agent，获取异步流式结果
        AsyncGenerator<NodeOutput> result = this.reactAgent.stream(Map.of(
                "messages", List.of(new UserMessage(prompt))
        ));

        // 2. 创建 SSE Sink
        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 3. 处理流，将内容推送到 Sink
        processStream(result, sink);

        // 4. 返回 SSE Flux 流
        return sink.asFlux();
    }

    /**
     * 处理流式输出，将每个 NodeOutput 转为 SSE 事件并发送
     */
    private CompletableFuture<Void> processStream(AsyncGenerator<NodeOutput> generator,
                                                  Sinks.Many<ServerSentEvent<String>> sink) {
        return generator.forEachAsync(output -> {
            try {
                if (output instanceof StreamingOutput streamingOutput) {
                    // 构造成 JSON（可选，前端可按需解析）
                    AssistantMessage assistantMessage = streamingOutput.getChatResponse().getResult().getOutput();
                    MessageVO messageVO = MessageVO.builder()
                            .role(assistantMessage.getMessageType().getValue())
                            .content(assistantMessage.getText())
                            .toolCalls(assistantMessage.getToolCalls().toString())
                            .build();

                    String content = JSONUtil.toJsonStr(messageVO); // {"node":"root","text":"xxx"}

                    // 构造 SSE 事件，格式必须为：data: xxx\n\n
                    ServerSentEvent<String> sse = ServerSentEvent.<String>builder()
                            .data(content)  // 这里的 content 会自动被包装为 "data: xxx\n\n"
                            .id(String.valueOf(System.currentTimeMillis())) // 可选，事件ID
                            .event("message") // 可选，自定义事件类型
                            .build();

                    System.out.println("✅ SSE 已推送: " + content);
                    // 发送到 Sink
                    sink.tryEmitNext(sse);
                }

            } catch (Exception e) {
                sink.tryEmitError(e); // 出错时终止流
            }
        }).thenAccept(v -> {
            // 正常完成时关闭流
            sink.tryEmitComplete();
        }).exceptionally(e -> {
            // 异常时通知客户端
            sink.tryEmitError(e);
            return null;
        });
    }

}
