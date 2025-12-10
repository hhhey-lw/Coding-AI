package com.coding.agentflow.service.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.function.Function;

/**
 * 本地工具抽象基类
 *
 * @param <I> 输入参数类型
 * @param <O> 输出结果类型
 */
@Slf4j
public abstract class AbstractAgentTool<I, O> implements AgentTool, Function<I, O> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ToolCallback getToolCallback() {
        return FunctionToolCallback.builder(getName(), this)
                .description(getDescription())
                .inputType(getInputType())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> args) {
        try {
            // 将 Map 转换为具体的输入对象
            I input = objectMapper.convertValue(args, getInputType());
            return apply(input);
        } catch (Exception e) {
            log.error("工具执行失败: {}", getName(), e);
            throw new RuntimeException("工具执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取输入参数类型 Class
     */
    protected abstract Class<I> getInputType();
}
