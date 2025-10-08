package com.coding.workflow.service;

import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.WorkflowContext;
import lombok.Data;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public interface ExecuteProcessor {
    /**
     * 返回节点类型
     */
    String getNodeType();

    /**
     * 返回节点描述
     */
    String getNodeDescription();

    /**
     * 执行工作流节点
     *
     * @param graph   有向无环图
     * @param node    节点
     * @param context 工作流上下文
     */
    void execute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context);

    /**
     * 验证节点参数
     *
     * @param graph 有向无环图
     * @param node  节点
     * @return 参数验证结果
     */
    CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node);

    /**
     * 节点参数验证结果
     */
    @Data
    class CheckNodeParamResult implements Serializable {
        /** 验证是否成功 */
        private boolean success;
        /** 错误信息列表 */
        private List<String> errorInfos = new ArrayList<>();
        /** 被验证节点的ID */
        private String nodeId;
        /** 被验证节点的名称 */
        private String nodeName;
        /** 被验证节点的类型 */
        private String nodeType;

        public static CheckNodeParamResult success() {
            CheckNodeParamResult result = new CheckNodeParamResult();
            result.setSuccess(true);
            return result;
        }
    }
}
