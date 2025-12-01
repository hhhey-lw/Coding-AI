package com.coding.agentflow.service;


import com.coding.agentflow.model.model.AgentFlowConfig;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.CompiledGraph;

public interface AgentFlowService {

    /**
     * 转换WorkflowConfig为CompiledGraph
     *
     * @param agentFlowConfig 工作流配置
     * @return CompiledGraph 编译图
     */
    CompiledGraph convertToCompiledGraph(AgentFlowConfig agentFlowConfig) throws GraphStateException;

}
