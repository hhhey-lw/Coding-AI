package com.coding.workflow.service.impl.processor;

import cn.hutool.json.JSONUtil;
import com.coding.admin.common.Result;
import com.coding.workflow.service.ai.McpServerService;
import com.coding.workflow.enums.ErrorCodeEnum;
import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.enums.ValueFromEnum;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.model.request.McpServerCallToolRequest;
import com.coding.workflow.model.response.McpServerCallToolResponse;
import com.coding.workflow.service.AbstractExecuteProcessor;
import com.coding.workflow.utils.AssertUtil;
import com.coding.workflow.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 *  负责MCP节点的执行逻辑
 *
 */
@Slf4j
@Service
public class MCPExecuteProcessor extends AbstractExecuteProcessor {
    // 输出配置
    private static final String RESULT = "result";

    @Resource
    private McpServerService mcpServerService;

    @Override
    public String getNodeType() {
        return NodeTypeEnum.MCP.getCode();
    }

    @Override
    public String getNodeDescription() {
        return NodeTypeEnum.MCP.getDesc();
    }

    @Override
    public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) throws InterruptedException {
        NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

        // 1. 参数检查: 参数放置在node中
        AssertUtil.isNotNull(node.getConfig(), String.format("节点：%s > 配置不能为空", node.getId()));
        AssertUtil.isNotNull(node.getConfig().getNodeParam(), String.format("节点：%s > 节点参数不能为空", node.getId()));
        AssertUtil.isNotNull(node.getConfig().getInputParams(), String.format("节点：%s > 节点入参不能为空", node.getId()));

        // 1.2 构建MCP请求
        McpServerCallToolRequest request = constructMcpServerCallToolRequest(node, context);

        // 2. 调用MCP工具
        Result<McpServerCallToolResponse> responseResult = mcpServerService.callTool(request);

        // 3. 封装响应结果
        if (responseResult.isSuccess()) {
            McpServerCallToolResponse toolResponse = responseResult.getData();
            if (toolResponse != null && Boolean.FALSE.equals(toolResponse.getIsError())
                    && toolResponse.getContent() != null && !toolResponse.getContent().isEmpty()) {
                // 3.1 正常响应
                Map<String, Object> outputMap = Maps.newHashMap();
                outputMap.put(RESULT, toolResponse.getContent());
                nodeResult.setOutput(JSONUtil.toJsonStr(outputMap));
                nodeResult.setInput(JSONUtil.toJsonStr(Map.of(
                        "Input", JSONUtil.toJsonStr(request)
                )));
            } else {
                // 3.2 响应异常
                String errorMsg = "MCP工具调用失败，" + responseResult.getMessage();
                if (responseResult.getData() != null) {
                    errorMsg += ", " + responseResult.getData().getContent();
                }
                log.error("节点：{} > {}", node.getId(), errorMsg);
                nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
                nodeResult.setErrorCode(responseResult.getCode().toString());
                nodeResult.setErrorInfo(errorMsg);
                nodeResult.setError(ErrorCodeEnum.WORKFLOW_EXECUTE_ERROR.toError(errorMsg));
            }
        }
        
        return nodeResult;
    }

    /**
     * 构建MCP服务调用请求
     *
     * @param node    当前节点
     * @param context 工作流上下文
     * @return 构建的MCP服务调用请求
     */
    private McpServerCallToolRequest constructMcpServerCallToolRequest(Node node, WorkflowContext context) {
        McpServerCallToolRequest request = new McpServerCallToolRequest();

        NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
        request.setServerCode(nodeParam.getServerCode());
        request.setToolName(nodeParam.getToolName());
        request.setToolParams(convertToolParams(node.getConfig().getInputParams(), context));
        request.setRequestId(UUID.randomUUID().toString());

        return request;
    }

    /**
     * 转换工具参数
     *
     * @param inputParams 输入参数列表
     * @param context     工作流上下文
     * @return 转换后的参数映射
     */
    private Map<String, Object> convertToolParams(List<Node.InputParam> inputParams, WorkflowContext context) {
        HashMap<String, Object> inputObjMap = Maps.newHashMap();
        if (inputParams != null) {
            for (Node.InputParam inputParam : inputParams) {
                if (StringUtils.isNotBlank(inputParam.getKey()) &&
                        Objects.nonNull(inputParam.getValue()) &&
                        StringUtils.isNotBlank(inputParam.getValueFrom())) {
                    // 1. infer类型的变量
                    if (ValueFromEnum.refer.name().equals(inputParam.getValueFrom())) {
                        String valueFromContext = getValueFromContext(inputParam, context);
                        inputObjMap.put(inputParam.getKey(), valueFromContext);
                    }
                    // 2. 基本类型
                    else {
                        inputObjMap.put(inputParam.getKey(), inputParam.getValue());
                    }
                }
            }
        }
        return inputObjMap;
    }

    @Data
    public static class NodeParam {

        /**
         * MCP服务编码
         */
        @JsonProperty("server_code")
        private String serverCode;

        /**
         * MCP服务名称
         */
        @JsonProperty("server_name")
        private String serverName;

        /**
         * 工具名称
         */
        @JsonProperty("tool_name")
        private String toolName;

    }



}
