package com.coding.agentflow.service.node;

import com.coding.agentflow.model.model.Node;
import com.coding.graph.core.state.OverAllState;

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
     * @param state 执行状态（包含流程级别的共享数据）
     * @return 节点执行结果数据，如果包含 AsyncGenerator 框架会自动识别流式输出
     * @throws Exception 执行失败时抛出异常
     */
    Map<String, Object> execute(Node node, OverAllState state) throws Exception;

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
