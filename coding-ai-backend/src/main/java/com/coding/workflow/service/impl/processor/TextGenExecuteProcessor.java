package com.coding.workflow.service.impl.processor;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.coding.workflow.enums.MessageRoleEnum;
import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.manager.TextCompletionManager;
import com.coding.workflow.model.chat.*;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.AbstractExecuteProcessor;
import com.coding.workflow.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TextGenExecuteProcessor extends AbstractExecuteProcessor {

    @Resource
    private TextCompletionManager textCompletionManager;

    @Override
    public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
        NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

        NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
        ModelConfig modelConfig = nodeParam.getModelConfig();

        // 构建请求消息
        List<Message> requestMessages = Lists.newArrayList();
        String sysPrompt = nodeParam.getSysPromptContent();
        if (StrUtil.isNotBlank(sysPrompt)) {
            requestMessages.add(new SystemMessage(replaceTemplateContent(sysPrompt, context)));
        }

        // TODO 添加短期记忆的上下文

        String userPrompt = nodeParam.getPromptContent();
        if (StrUtil.isNotBlank(userPrompt)) {
            requestMessages.add(new UserMessage(constructUserMessage(userPrompt, context)));
        }

        // 构建模型参数
        HashMap<String, Object> paramMap = Maps.newHashMap();
        List<ModelConfig.ModelParam> params = modelConfig.getParams();
        if (CollectionUtil.isNotEmpty(params)) {
            params.forEach(modelParam -> {
                if (BooleanUtil.isTrue(modelParam.getEnable())) {
                    paramMap.put(modelParam.getKey(), modelParam.getValue());
                }
            });
        }

        ModelTmpResponseContent tmpResponseContent = new ModelTmpResponseContent();
        Mono<AgentResponse> chatResponse = textCompletionManager.chat(modelConfig.getProvider(), "TextGen", modelConfig.getModelId(), paramMap, requestMessages);
        AgentResponse agentResponse = chatResponse.doOnNext((response) -> {
            handleResponse(response, requestMessages, paramMap, modelConfig, node, context,
                    tmpResponseContent);
        }).block();

        if (agentResponse.isSuccess()) {
            String responseText = tmpResponseContent.getTemporaryContent().toString();
            String reasoningContent = tmpResponseContent.getTemporaryReasoningContent().toString();
            if (StrUtil.isBlank(responseText)) {
                nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
                nodeResult.setErrorInfo("模型调用没有返回任何内容");
            }
            else {
                Map<String, Object> inputObj = Maps.newHashMap();
                inputObj.put("provider", modelConfig.getProvider());
                inputObj.put("modelId", modelConfig.getModelId());
                inputObj.put("messages", convertToChatMessage(requestMessages));
                inputObj.put("params", paramMap);
                nodeResult.setInput(JsonUtils.toJson(decorateInput(inputObj)));
                Map<String, Object> outputMap = Maps.newHashMap();
                outputMap.put(OUTPUT_DECORATE_PARAM_KEY, responseText);
                if (StrUtil.isNotBlank(reasoningContent)) {
                    outputMap.put("reasoning_content", reasoningContent);
                }
                nodeResult.setOutput(JsonUtils.toJson(outputMap));
                // nodeResult.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());
                nodeResult.setUsages(Lists.newArrayList(agentResponse.getModelUsage()));
            }
        }
        else {
            nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
            nodeResult.setErrorInfo(agentResponse.getError().getMessage());
        }
        return nodeResult;

    }

    private String constructUserMessage(String userPrompt, WorkflowContext context) {
        userPrompt = replaceTemplateContent(userPrompt, context);

        // TODO 构造视觉理解

        return userPrompt;
    }

    private void handleResponse(AgentResponse response, List<Message> requestMessages, Map<String, Object> paramMap,
                                ModelConfig modelConfig, Node node, WorkflowContext context, ModelTmpResponseContent tmpResponseContent) {
        if (!response.isSuccess()) {
            Error error = response.getError();
            // Node execution error, save execution result and record error information
            context.getNodeResultMap()
                    .put(node.getId(), NodeResult.error(node, "工作流执行失败: " + error.getMessage()));
        }
        else {
            NodeResult nodeResult = new NodeResult();
            nodeResult.setNodeId(node.getId());
            nodeResult.setNodeName(node.getName());
            nodeResult.setNodeType(node.getType());
            nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
            Map<String, Object> inputObj = Maps.newHashMap();
            inputObj.put("provider", modelConfig.getProvider());
            inputObj.put("modelId", modelConfig.getModelId());
            inputObj.put("messages", convertToChatMessage(requestMessages));
            inputObj.put("params", paramMap);
            nodeResult.setInput(JsonUtils.toJson(decorateInput(inputObj)));
            String content = fetchContent(response);
            String reasoningContent = fetchReasoningContent(response);
            tmpResponseContent.getTemporaryContent().append(content == null ? "" : content);
            tmpResponseContent.getTemporaryReasoningContent().append(reasoningContent == null ? "" : reasoningContent);
            Map<String, Object> tmpMap = decorateOutput(tmpResponseContent.getTemporaryContent().toString());
            String tmpReasoningContent = tmpResponseContent.getTemporaryReasoningContent().toString();
            if (StrUtil.isNotBlank(tmpReasoningContent)) {
                tmpMap.put("reasoning_content", tmpReasoningContent);
            }
            nodeResult.setOutput(JsonUtils.toJson(tmpMap));
            nodeResult.setUsages(Lists.newArrayList(response.getModelUsage()));
            context.getNodeResultMap().put(node.getId(), nodeResult);
        }
    }

    protected List<ChatMessage> convertToChatMessage(List<Message> messages) {
        return messages.stream().map(message -> {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setRole(MessageRoleEnum.of(message.getMessageType().getValue()));
            chatMessage.setContent(message.getText());
            return chatMessage;
        }).collect(Collectors.toList());
    }

    protected Map<String, Object> decorateInput(Object input) {
        Map<String, Object> outputObj = new HashMap<>();
        outputObj.put(INPUT_DECORATE_PARAM_KEY, input);
        return outputObj;
    }

    private static String fetchContent(AgentResponse gptResponse) {
        Object content = gptResponse.getMessage().getContent();
        if (content != null) {
            return String.valueOf(content);
        }
        else {
            return "";
        }
    }


    private static String fetchReasoningContent(AgentResponse gptResponse) {
        String reasoningContent = gptResponse.getMessage().getReasoningContent();
        if (StrUtil.isNotBlank(reasoningContent)) {
            return reasoningContent;
        }
        else {
            return "";
        }
    }

    protected Map<String, Object> decorateOutput(Object output) {
        Map<String, Object> outputObj = new HashMap<>();
        outputObj.put(OUTPUT_DECORATE_PARAM_KEY, output);
        return outputObj;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.TEXT_GEN.getCode();
    }

    @Override
    public String getNodeDescription() {
        return NodeTypeEnum.TEXT_GEN.getDesc();
    }


    @Data
    public static class NodeParam {

        @JsonProperty("sys_prompt_content")
        private String sysPromptContent;

        @JsonProperty("prompt_content")
        private String promptContent;

        @JsonProperty("model_config")
        private ModelConfig modelConfig;

//        @JsonProperty("short_memory")
//        private ShortTermMemory shortMemory;

    }

    @Data
    private static class ModelTmpResponseContent {

        StringBuilder temporaryContent = new StringBuilder();

        StringBuilder temporaryReasoningContent = new StringBuilder();

    }

}
