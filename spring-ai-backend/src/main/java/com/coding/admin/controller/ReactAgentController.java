package com.coding.admin.controller;

import cn.hutool.json.JSONUtil;
import com.coding.admin.model.vo.MessageVO;
import com.coding.admin.manager.AgentManager;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.StreamingOutput;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
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
     * SSE 流式接口：使用 SseEmitter 实现真正的流式传输
     * 
     * ⚠️ 不使用 forEachAsync 的原因：
     * forEachAsync 的 for 循环会阻塞等待所有流式数据产生完毕才返回，
     * 导致无法实现真正的实时流式传输。
     */
    @GetMapping(value = "/react")
    public SseEmitter streamAgent(@RequestParam("prompt") String prompt) throws GraphStateException, GraphRunnerException {
        
        log.info("🚀 收到 SSE 请求，prompt: {}", prompt);
        
        // 创建 SseEmitter，超时时间 5 分钟
        SseEmitter emitter = new SseEmitter(300_000L);
        
        // 调用 Agent，获取流式结果
        AsyncGenerator<NodeOutput> generator = this.reactAgent.stream(Map.of(
                "messages", List.of(new UserMessage(prompt))
        ));
        
        // 🔥 使用独立线程 + 迭代器实现真正的流式处理
        // 避免使用 forEachAsync，因为它会阻塞等待所有数据
        CompletableFuture.runAsync(() -> {
            try {
                log.info("🚀 开始流式处理");
                
                // 使用迭代器逐个处理数据，每产生一个就立即发送一个
                for (NodeOutput output : generator) {
                    if (output instanceof StreamingOutput streamingOutput) {
                        try {
                            // 获取消息内容
                            AssistantMessage assistantMessage = streamingOutput.getChatResponse().getResult().getOutput();

                            // 构造 MessageVO
                            MessageVO messageVO = MessageVO.builder()
                                    .role(assistantMessage.getMessageType().getValue())
                                    .content(assistantMessage.getText())
                                    .toolCalls(assistantMessage.getToolCalls().toString())
                                    .build();

                            String jsonContent = JSONUtil.toJsonStr(messageVO);

                            log.info("✅ 推送消息: {}", jsonContent);

                            // 立即发送数据
                            emitter.send(SseEmitter.event()
                                    .data(jsonContent)
                                    .name("message"));

                        } catch (IOException e) {
                            log.error("❌ 发送消息失败", e);
                            emitter.completeWithError(e);
                            return; // 终止处理
                        }
                    }
                }
                
                // 所有消息处理完毕，发送结束信号
                log.info("✅ Generator 处理完毕");

                try {
                    // 发送结束信号
                    MessageVO endMessage = MessageVO.builder()
                            .role("system")
                            .content("[STREAM_END]")
                            .toolCalls("[]")
                            .build();

                    emitter.send(SseEmitter.event()
                            .data(JSONUtil.toJsonStr(endMessage))
                            .name("message"));

                    log.info("✅ 已发送结束信号");

                    // 完成流
                    emitter.complete();
                    log.info("✅ SSE 流已完成");

                } catch (IOException e) {
                    log.error("❌ 发送结束信号失败", e);
                    emitter.completeWithError(e);
                }
                
            } catch (Exception e) {
                // 异常处理
                log.error("❌ 流处理异常", e);
                emitter.completeWithError(e);
            }
        });
        
        // 监听超时和完成事件
        emitter.onTimeout(() -> {
            log.warn("⏰ SSE 连接超时");
            emitter.complete();
        });
        
        emitter.onCompletion(() -> {
            log.info("🔚 SSE 连接已关闭");
        });
        
        emitter.onError((e) -> {
            log.error("❌ SSE 连接错误", e);
        });
        
        return emitter;
    }
}
