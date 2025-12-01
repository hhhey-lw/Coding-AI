package com.coding.agentflow.service.node;

import com.coding.agentflow.model.model.Node;

import java.util.Map;

/**
 * 节点执行器接口
 * 定义所有节点的执行行为规范
 */
public interface NodeExecutor {

    /**
     * 执行节点逻辑
     *
     * @param node 节点配置信息
     * @param context 执行上下文（包含流程级别的共享数据）
     * @return 节点执行结果
     */
    NodeExecutionResult execute(Node node, Map<String, Object> context);

    /**
     * 验证节点配置是否有效
     *
     * @param node 节点配置信息
     * @return 验证是否通过
     */
    boolean validate(Node node);

    /**
     * 获取节点支持的类型
     *
     * @return 节点类型
     */
    String getNodeType();
}
