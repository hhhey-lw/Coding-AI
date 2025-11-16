package com.coding.workflow.service.impl.processor;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONUtil;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.enums.ValueFromEnum;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.AbstractExecuteProcessor;
import com.coding.workflow.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;

@Slf4j
@Component
public class EndExecuteProcessor extends AbstractExecuteProcessor {

    @Override
    public String getNodeType() {
        return NodeTypeEnum.END.getCode();
    }

    @Override
    public String getNodeDescription() {
        return NodeTypeEnum.END.getDesc();
    }

    @Override
    public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
        long start = System.currentTimeMillis();
        // 初始化NodeResult
        NodeResult endNodeResult = initNodeResultAndRefreshContext(node, context);

        // 设置End节点出参类型
        Assert.notNull(node.getConfig(), "End节点配置不能为空");

        NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
        String outputType = nodeParam.getOutputType();
        OutputType type = OutputType.fromString(outputType);
        endNodeResult.setOutputType(type.getValue());

        // 根据输出类型，构建结果
        Map<String, Object> result = Maps.newHashMap();
        if (type == OutputType.TEXT) {
            result.put(OUTPUT_DECORATE_PARAM_KEY, replaceTemplateContent(nodeParam.getTextTemplate(), context));
        } else {
            result.put(OUTPUT_DECORATE_PARAM_KEY, JsonUtils.toJson(constructJsonResult(nodeParam.getJsonParams(), context)));
        }

        // 构建End节点结果: Json格式
        endNodeResult.setInput(JSONUtil.toJsonStr(result));
        endNodeResult.setOutput(JSONUtil.toJsonStr(result));
        endNodeResult.setNodeExecuteTime((System.currentTimeMillis() - start) + "ms");
        endNodeResult.setUsages(null);
        return endNodeResult;
    }

    private Map<String, Object> constructJsonResult(List<Node.InputParam> jsonParams, WorkflowContext context) {
        Map<String, Object> result = new HashMap<>();
        jsonParams.forEach(inputParam -> {
            String valueFrom = inputParam.getValueFrom();
            Object value = inputParam.getValue();
            if (ValueFromEnum.refer.name().equals(valueFrom)) {
                if (value == null) {
                    return;
                }
                Object finalValue = getValueFromContext(inputParam, context);
                if (finalValue == null) {
                    return;
                }
                result.put(inputParam.getKey(), finalValue);
            }
            else {
                if (value == null) {
                    return;
                }
                result.put(inputParam.getKey(), value);
            }
        });
        return result;
    }

    @Data
    public static class NodeParam implements Serializable {

        /**
         * 输出结果类型：text, json
         */
        @JsonProperty("output_type")
        private String outputType;

        /**
         * 输出结果的文本模板
         */
        @JsonProperty("text_template")
        private String textTemplate;

        /**
         * 输出结果的JSON字段
         */
        @JsonProperty("json_params")
        private List<Node.InputParam> jsonParams;

        /**
         * 流式开关
         */
        @JsonProperty("stream_switch")
        private Boolean streamSwitch;

    }

    @Getter
    @AllArgsConstructor
    public enum OutputType {

        TEXT("text"), JSON("json");

        private final String value;

        public static OutputType fromString(String value) {
            if (StringUtils.isBlank(value)) {
                return TEXT;
            }
            for (OutputType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return TEXT;
        }
    }

}
