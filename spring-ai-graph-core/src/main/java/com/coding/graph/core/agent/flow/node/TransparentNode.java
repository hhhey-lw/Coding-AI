package com.coding.graph.core.agent.flow.node;

import com.coding.graph.core.node.action.NodeAction;
import com.coding.graph.core.state.OverAllState;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 透明节点（TransparentNode）：实现NodeAction接口，核心作用是在图执行流程中传递数据，
 * 将指定输入键（inputKey）对应的状态数据，直接映射到指定输出键（outputKey），不修改数据本身，仅做键的映射转换。
 */
public class TransparentNode implements NodeAction {

    private final String inputKey;  // 输入键：用于从全局状态中获取数据的键名
    private final String outputKey; // 输出键：用于将获取到的数据存入全局状态的键名

    /**
     * 构造透明节点实例
     * @param outputKey 输出键，用于存储数据的键名
     * @param inputKey 输入键，用于获取数据的键名
     */
    public TransparentNode(String outputKey, String inputKey) {
        if (!StringUtils.hasLength(inputKey) || !StringUtils.hasLength(outputKey)) {
            throw new IllegalArgumentException("inputKey（输入键）和outputKey（输出键）不能为空或空白字符串。");
        }
        this.inputKey = inputKey;
        this.outputKey = outputKey;
    }

    /**
     * 执行节点逻辑：从全局状态中获取输入键对应的数据，映射到输出键并返回
     * @param state 全局状态对象，存储整个图执行过程中的数据
     * @return 包含输出键与对应数据的映射集合
     * @throws Exception 当从全局状态中获取不到输入键对应的数据时，抛出非法参数异常
     */
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, Object> updatedState = new HashMap<>();
        // 从全局状态中获取输入键对应的数据，若不存在则抛出异常
        Object value = state.value(inputKey)
                .orElseThrow(
                        () -> new IllegalArgumentException("在全局状态（state）中未找到输入键 '" + inputKey + "'，当前状态：" + state));
        // 将获取到的数据存入输出键对应的映射中
        updatedState.put(this.outputKey, value);
        return updatedState;
    }

    /**
     * 创建透明节点的构建器（Builder）实例，用于通过链式调用构建TransparentNode
     * @return 透明节点的构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 透明节点（TransparentNode）的构建器类，通过链式调用设置输入键和输出键，简化对象创建过程
     */
    public static class Builder {

        private String outputKey; // 待设置的输出键
        private String inputKey;  // 待设置的输入键

        /**
         * 构建透明节点实例
         * @return 配置好输入键和输出键的TransparentNode实例
         */
        public TransparentNode build() {
            return new TransparentNode(outputKey, inputKey);
        }

        /**
         * 设置输出键（链式调用）
         * @param outputKey 要设置的输出键
         * @return 构建器本身（Builder），用于继续链式调用
         */
        public Builder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }

        /**
         * 设置输入键（链式调用）
         * @param inputKey 要设置的输入键
         * @return 构建器本身（Builder），用于继续链式调用
         */
        public Builder inputKey(String inputKey) {
            this.inputKey = inputKey;
            return this;
        }

    }

}