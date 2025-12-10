package com.coding.agentflow.service.node;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.coding.agentflow.model.model.Node;
import com.coding.graph.core.state.OverAllState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 节点抽象基类
 * 提供通用的执行框架和工具方法
 */
@Slf4j
public abstract class AbstractNode implements NodeExecutor {

    // 静态常量，避免重复编译
    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile("\\{\\{\\s*([a-zA-Z_][\\w\\.-]*)\\s*\\}\\}");

    @Override
    public Map<String, Object> execute(Node node, OverAllState state) throws Exception {
        long startTime = System.currentTimeMillis();
        
        // 前置验证
        if (!validate(node)) {
            throw new IllegalArgumentException("节点配置验证失败: " + node.getId());
        }

        log.info("开始执行节点: {} (类型: {})", node.getId(), node.getType());

        // 执行前置处理
        preExecute(node, state);

        // 执行核心逻辑
        Map<String, Object> result = doExecute(node, state);

        // 执行后置处理
        postExecute(node, state, result);

        long executionTime = System.currentTimeMillis() - startTime;
        log.info("节点执行完成: {} (耗时: {}ms)", node.getId(), executionTime);

        return result;
    }

    /**
     * 核心执行逻辑（由子类实现）
     *
     * @param node 节点配置
     * @param state 执行状态
     * @return 执行结果数据，如果包含 AsyncGenerator 框架会自动识别流式输出
     * @throws Exception 执行失败时抛出异常
     */
    protected abstract Map<String, Object> doExecute(Node node, OverAllState state) throws Exception;

    /**
     * 执行前置处理（可选，子类可覆盖）
     *
     * @param node 节点配置
     * @param state 执行状态
     */
    protected void preExecute(Node node, OverAllState state) {
        // 默认空实现，子类可选择性覆盖
    }

    /**
     * 执行后置处理（可选，子类可覆盖）
     *
     * @param node 节点配置
     * @param state 执行状态
     * @param result 执行结果数据
     */
    protected void postExecute(Node node, OverAllState state, Map<String, Object> result) {
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

    protected String getConfigParamAsString(Node node, String key) {
        return getConfigParamAsString(node, key, null);
    }

    /**
     * 从节点配置中获取整数参数
     *
     * @param node 节点配置
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    protected Integer getConfigParamAsInteger(Node node, String key, Integer defaultValue) {
        Object value = getConfigParam(node, key);
        return value != null ? Integer.valueOf(value.toString()) : defaultValue;
    }
    protected Integer getConfigParamAsInteger(Node node, String key) {
        return getConfigParamAsInteger(node, key, null);
    }

    /**
     * 从节点配置中获取Double参数
     *
     * @param node 节点配置
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    protected Double getConfigParamAsDouble(Node node, String key, Double defaultValue) {
        Object value = getConfigParam(node, key);
        return value != null ? Double.valueOf(value.toString()) : defaultValue;
    }

    protected Double getConfigParamAsDouble(Node node, String key) {
        return getConfigParamAsDouble(node, key, null);
    }

    /**
     * 从节点配置中获取Boolean参数
     *
     * @param node 节点配置
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    protected Boolean getConfigParamAsBoolean(Node node, String key, Boolean defaultValue) {
        Object value = getConfigParam(node, key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(value.toString());
    }

    protected Boolean getConfigParamAsBoolean(Node node, String key) {
        return getConfigParamAsBoolean(node, key, null);
    }

    /**
     * 从节点配置中获取列表参数
     *
     * @param node 节点配置
     * @param key 参数键
     * @return 参数值
     */
    protected List<String> getConfigParamAsList(Node node, String key) {
        Object value = getConfigParam(node, key);

        // 1. null 直接返回空列表
        if (value == null) {
            return Collections.emptyList();
        }

        // 2. 如果是 List，遍历并转为 String
        if (value instanceof List<?>) {
            return ((List<?>) value).stream()
                    .map(this::safeToString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 3. 如果是数组（Object[]），也支持
        if (value instanceof Object[]) {
            return Arrays.stream((Object[]) value)
                    .map(this::safeToString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 4. 尝试作为 JSON 字符串解析
        String strValue = safeToString(value);
        if (strValue != null) {
            try {
                JSONArray jsonArray = JSONUtil.parseArray(strValue);
                if (jsonArray != null) {
                    return jsonArray.toList(String.class); // Hutool 转换
                }
            } catch (Exception e) {
                // JSON 解析失败，可能是普通字符串
                log.debug("JSON 解析失败，按单值处理: key={}, value={}", key, strValue, e);
            }
        }

        // 5. 兜底：当作单值处理（包装成单元素列表）
        String singleValue = safeToString(value);
        return singleValue != null ? Collections.singletonList(singleValue) : Collections.emptyList();
    }

    /**
     * 安全转为字符串，避免 null 和异常
     */
    private String safeToString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        // 其他类型（如 Date、自定义对象）也可调用 toString()
        return obj.toString();
    }

    /**
     * 从节点配置中获取Map参数
     *
     * @param node 节点配置
     * @param key 参数键
     * @return Map参数值，如果不是Map类型则返回空Map
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> getConfigParamAsMap(Node node, String key) {
        Object value = getConfigParam(node, key);
        return value instanceof Map ? (Map<String, Object>) value : new HashMap<>();
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

    /**
     * 替换提示词中的变量 - 注意这里只能平级替换
     * 支持 {{variableName}} 格式的变量替换
     *
     * @param prompt 提示词模板
     * @param state 执行状态
     * @return 替换后的提示词
     */
    protected String replaceTemplateWithVariable(String prompt, OverAllState state) {
        if (StringUtils.isBlank(prompt) || state == null) {
            return prompt;
        }

        // 预编译正则表达式（静态常量，避免重复编译）
        Pattern pattern = VARIABLE_PATTERN;
        Matcher matcher = pattern.matcher(prompt);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = resolveVariableValue(variableName, state);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 解析变量值
     */
    private String resolveVariableValue(String variableName, OverAllState state) {
        if (!state.data().containsKey(variableName)) {
            log.warn("未找到变量值: {}", variableName);
            return "{{" + variableName + "}}";
        }

        Object value = state.data().get(variableName);
        if (value == null) {
            log.debug("变量值为null: {}", variableName);
            return "";
        }

        return value.toString();
    }

}
