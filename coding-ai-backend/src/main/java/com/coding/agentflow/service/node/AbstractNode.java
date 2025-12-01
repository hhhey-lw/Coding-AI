package com.coding.agentflow.service.node;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.coding.agentflow.model.model.Node;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 节点抽象基类
 * 提供通用的执行框架和工具方法
 */
@Slf4j
public abstract class AbstractNode implements NodeExecutor {

    @Override
    public NodeExecutionResult execute(Node node, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 前置验证
            if (!validate(node)) {
                return NodeExecutionResult.failure("节点配置验证失败: " + node.getId());
            }

            log.info("开始执行节点: {} (类型: {})", node.getId(), node.getType());

            // 执行前置处理
            preExecute(node, context);

            // 执行核心逻辑
            NodeExecutionResult result = doExecute(node, context);

            // 执行后置处理
            postExecute(node, context, result);

            // 设置执行耗时
            result.setExecutionTime(System.currentTimeMillis() - startTime);

            log.info("节点执行完成: {} (耗时: {}ms, 成功: {})", 
                    node.getId(), result.getExecutionTime(), result.isSuccess());

            return result;

        } catch (Exception e) {
            log.error("节点执行异常: {}", node.getId(), e);
            NodeExecutionResult result = NodeExecutionResult.failure("节点执行异常: " + e.getMessage());
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * 核心执行逻辑（由子类实现）
     *
     * @param node 节点配置
     * @param context 执行上下文
     * @return 执行结果
     */
    protected abstract NodeExecutionResult doExecute(Node node, Map<String, Object> context);

    /**
     * 执行前置处理（可选，子类可覆盖）
     *
     * @param node 节点配置
     * @param context 执行上下文
     */
    protected void preExecute(Node node, Map<String, Object> context) {
        // 默认空实现，子类可选择性覆盖
    }

    /**
     * 执行后置处理（可选，子类可覆盖）
     *
     * @param node 节点配置
     * @param context 执行上下文
     * @param result 执行结果
     */
    protected void postExecute(Node node, Map<String, Object> context, NodeExecutionResult result) {
        // 默认空实现，子类可选择性覆盖
    }

    @Override
    public boolean validate(Node node) {
        if (node == null) {
            log.error("节点配置为空");
            return false;
        }
        if (node.getId() == null || node.getId().isEmpty()) {
            log.error("节点ID为空");
            return false;
        }
        if (node.getType() == null) {
            log.error("节点类型为空");
            return false;
        }
        return doValidate(node);
    }

    /**
     * 自定义验证逻辑（由子类实现）
     *
     * @param node 节点配置
     * @return 验证是否通过
     */
    protected abstract boolean doValidate(Node node);

    /**
     * 从节点配置中获取参数
     *
     * @param node 节点配置
     * @param key 参数键
     * @return 参数值
     */
    protected Object getConfigParam(Node node, String key) {
        if (node.getConfigParams() == null) {
            return null;
        }
        return node.getConfigParams().get(key);
    }

    /**
     * 从节点配置中获取参数
     *
     * @param node 节点配置
     * @param key 参数键
     * @return 参数值
     */
    protected <T> T getConfigParam(Node node, String key, Class<T> clazz) {
        if (node == null || node.getConfigParams() == null) {
            return null;
        }
        Object value = node.getConfigParams().get(key);
        if (clazz.isInstance(value)) {
            return (T) value;
        } else {
            log.warn("配置参数类型不匹配，key: {}, 实际类型: {}, 期望类型: {}",
                    key, value.getClass().getName(), clazz.getName());
            return null;
        }
    }

    /**
     * 从节点配置中获取字符串参数
     *
     * @param node 节点配置
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    protected String getConfigParamAsString(Node node, String key, String defaultValue) {
        Object value = getConfigParam(node, key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 从节点配置中获取字符串参数
     *
     * @param node 节点配置
     * @param key 参数键
     * @return 参数值
     */
    protected List<String> getConfigParamAsList(Node node, String key) {
        Object value = getConfigParam(node, key);

        if (value == null) {
            return Collections.emptyList();
        }

        JSONArray jsonArray = JSONUtil.parseArray((String) value);
        return jsonArray.toList(String.class);
    }

    /**
     * 从上下文中获取数据
     *
     * @param context 上下文
     * @param key 键
     * @return 值
     */
    protected Object getContextData(Map<String, Object> context, String key) {
        return context != null ? context.get(key) : null;
    }

    /**
     * 向上下文中设置数据
     *
     * @param context 上下文
     * @param key 键
     * @param value 值
     */
    protected void setContextData(Map<String, Object> context, String key, Object value) {
        if (context != null) {
            context.put(key, value);
        }
    }
}
