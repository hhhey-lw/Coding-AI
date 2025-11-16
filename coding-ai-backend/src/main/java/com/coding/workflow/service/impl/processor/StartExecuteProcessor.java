package com.coding.workflow.service.impl.processor;

import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.AbstractExecuteProcessor;
import com.coding.workflow.utils.JsonUtils;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class StartExecuteProcessor extends AbstractExecuteProcessor {

    @Override
    public String getNodeType() {
        return NodeTypeEnum.START.getCode();
    }

    @Override
    public String getNodeDescription() {
        return NodeTypeEnum.START.getDesc();
    }

    @Override
    public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
        long start = System.currentTimeMillis();

        NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

        Map<String, Object> userMap = context.getUserMap();
        if (userMap != null) {
            userMap.keySet().forEach(key -> {
                // 新增
                if (!context.getVariablesMap().containsKey(node.getId())) {
                    Map<String, Object> userObj = Maps.newHashMap();
                    userObj.put(key, userMap.get(key));
                    context.getVariablesMap().put(node.getId(), userObj);
                }
                // 合并
                else {
                    Map<String, Object> userObj = (Map<String, Object>) context.getVariablesMap().get(node.getId());
                    userObj.put(key, userMap.get(key));
                    context.getVariablesMap().put(node.getId(), userObj);
                }
            });
        }

        nodeResult.setInput(JsonUtils.toJson(userMap));
        nodeResult.setOutput(JsonUtils.toJson(userMap));
        nodeResult.setNodeExecuteTime((System.currentTimeMillis() - start) + "ms");
        return nodeResult;
    }

    @Override
    protected void handleVariables(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context,
                                NodeResult nodeResult) {
    }

}
