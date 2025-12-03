package com.coding.agentflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.agentflow.model.model.AgentFlowConfig;
import com.coding.agentflow.model.request.AgentFlowConfigRequest;
import com.coding.agentflow.model.response.AgentFlowConfigResponse;
import com.coding.agentflow.service.AgentFlowConfigService;
import com.coding.agentflow.service.AgentFlowService;
import com.coding.core.common.Result;
import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.StreamingOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/agent-flow")
@AllArgsConstructor
@Tag(name = "Agent工作流配置管理")
public class AgentFlowController {

    private final AgentFlowConfigService agentFlowConfigService;
    private final AgentFlowService agentFlowService;
    private final ObjectMapper objectMapper;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<Page<AgentFlowConfigResponse>> page(@RequestParam(defaultValue = "1") Integer current,
                                                       @RequestParam(defaultValue = "10") Integer size,
                                                       @RequestParam(required = false) String name) {
        return Result.success(agentFlowConfigService.pageAgentFlows(current, size, name));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询")
    public Result<AgentFlowConfigResponse> getById(@PathVariable Long id) {
        return Result.success(agentFlowConfigService.getAgentFlowById(id));
    }

    @PostMapping("/save")
    @Operation(summary = "保存或更新")
    public Result<Boolean> save(@RequestBody AgentFlowConfigRequest request) {
        return Result.success(agentFlowConfigService.saveOrUpdateAgentFlow(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(agentFlowConfigService.removeAgentFlow(id));
    }

    @PostMapping("/execute/stream")
    @Operation(summary = "流式执行工作流")
    public SseEmitter executeStream(@RequestParam Long flowId, @RequestBody Map<String, Object> input) throws GraphStateException, GraphRunnerException {
        log.info("开始流式执行工作流, flowId: {}, input: {}", flowId, input);

        // 1. 获取工作流配置
        AgentFlowConfigResponse flowResponse = agentFlowConfigService.getAgentFlowById(flowId);
        if (flowResponse == null) {
            throw new IllegalArgumentException("工作流不存在: " + flowId);
        }

        // 2. 转换为 AgentFlowConfig
        AgentFlowConfig agentFlowConfig = new AgentFlowConfig();
        agentFlowConfig.setNodes(flowResponse.getNodes());
        agentFlowConfig.setEdges(flowResponse.getEdges());

        // 3. 编译工作流
        CompiledGraph compiledGraph = agentFlowService.convertToCompiledGraph(agentFlowConfig);

        // 4. 创建 SseEmitter
        SseEmitter emitter = new SseEmitter(300_000L);

        // 5. 执行工作流并获取流式输出
        AsyncGenerator<NodeOutput> generator = compiledGraph.stream(input);

        // 6. 处理流式输出
        generator.streamForEach(output -> {
            try {
                if (output == null || StringUtils.isBlank(output.getNode())) {
                    return;
                }

                // 处理流式输出
                if (output instanceof StreamingOutput streamingOutput) {
                    String content = streamingOutput.getChatResponse().getResult().getOutput().getText();
                    if (content != null && !content.isEmpty()) {
                        Map<String, Object> message = new HashMap<>();
                        message.put("type", "chunk");
                        message.put("nodeId", output.getNode());
                        message.put("content", content);

                        emitter.send(SseEmitter.event()
                                .data(objectMapper.writeValueAsString(message)));
                    }
                } else {
                    // 处理节点完成输出
                    Map<String, Object> message = new HashMap<>();
                    message.put("type", "node_complete");
                    message.put("nodeId", output.getNode());
                    message.put("state", output.getState() != null ? output.getState().data() : null);

                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(message)));
                }
            } catch (JsonProcessingException e) {
                log.error("JSON序列化失败", e);
                emitter.completeWithError(e);
            } catch (IOException e) {
                log.error("发送SSE事件失败", e);
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("处理流式输出异常", e);
                emitter.completeWithError(e);
            }
        }).exceptionally(error -> {
            log.error("工作流执行异常", error);
            emitter.completeWithError(error);
            return null;
        }).thenRun(() -> {
            try {
                // 发送完成信号
                Map<String, Object> finishMessage = new HashMap<>();
                finishMessage.put("type", "finish");
                emitter.send(SseEmitter.event()
                        .data(objectMapper.writeValueAsString(finishMessage)));
                emitter.complete();
                log.info("工作流执行完成, flowId: {}", flowId);
            } catch (Exception e) {
                log.error("发送完成信号失败", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
