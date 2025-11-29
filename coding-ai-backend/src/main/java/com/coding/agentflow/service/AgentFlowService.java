package com.coding.agentflow.service;


import com.coding.agentflow.model.workflow.WorkflowConfig;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.StateGraph;

public interface AgentFlowService {

    /**
     * 转换WorkflowConfig为StateGraph
     *
     * @param workflowConfig 工作流配置
     * @return stateGraph 状态图
     */
    StateGraph convertToStateGraph(WorkflowConfig workflowConfig) throws GraphStateException;

}
