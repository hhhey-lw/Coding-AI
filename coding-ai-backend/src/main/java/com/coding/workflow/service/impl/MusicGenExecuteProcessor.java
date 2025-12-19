package com.coding.workflow.service.impl;

import cn.hutool.json.JSONUtil;
import com.coding.workflow.enums.ErrorCodeEnum;
import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.manager.MusicGenerationManager;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.model.request.MusicGenerationRequest;
import com.coding.workflow.model.response.MusicGenerationResponse;
import com.coding.workflow.service.AbstractExecuteProcessor;
import com.coding.workflow.utils.AssertUtil;
import com.coding.workflow.utils.JsonUtils;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class MusicGenExecuteProcessor extends AbstractExecuteProcessor {

    @Resource
    private MusicGenerationManager musicGenerationManager;

    @Override
    public String getNodeType() {
        return NodeTypeEnum.MUSIC_GEN.getCode();
    }

    @Override
    public String getNodeDescription() {
        return NodeTypeEnum.MUSIC_GEN.getDesc();
    }

    @Override
    public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) throws InterruptedException {
        NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

        try {
            // 1. 获取参数
            Map<String, Object> nodeParam = node.getConfig().getNodeParam();
            AssertUtil.isNotNull(nodeParam, "节点参数不能为空");

            // 1.1 获取基本参数
            NodeParam param = JsonUtils.fromMap(nodeParam, NodeParam.class);

            // 1.2 参数验证
            AssertUtil.isTrue(StringUtils.isNotBlank(param.getModel()), "模型不能为空");
            AssertUtil.isTrue(StringUtils.isNotBlank(param.getPrompt()), "输入提示词不能为空");

            // 1.3 替换模板变量
            String finalPrompt = replaceTemplateContent(param.getPrompt(), context);
            String finalLyrics = StringUtils.isNotBlank(param.getLyrics()) ?
                replaceTemplateContent(param.getLyrics(), context) : null;

            // 2. 构建音乐生成请求
            MusicGenerationRequest request = MusicGenerationRequest.builder()
                    .provider(param.getProvider())
                    .model(param.getModel())
                    .prompt(finalPrompt)
                    .lyrics(finalLyrics)
                    .audioSetting(MusicGenerationRequest.AudioSetting.builder()
                            .bitrate(param.getBitrate())
                            .sampleRate(param.getSampleRate())
                            .outputFormat(param.getOutputFormat())
                            .format(param.getFormat())
                            .build())
                    .build();

            log.info("开始生成音乐, provider: {}, model: {}, prompt: {}",
                param.getProvider(), param.getModel(), finalPrompt);

            // 3. 调用音乐生成服务
            MusicGenerationResponse response = musicGenerationManager.generateMusic(request);

            // 4. 处理返回结果
            if (response.isSuccess()) {
                // 构建输入数据
                nodeResult.setInput(JSONUtil.toJsonStr(param));

                // 构建输出结果
                Map<String, Object> resultMap = Maps.newHashMap();
                resultMap.put(OUTPUT_DECORATE_PARAM_KEY, response.getAudioUrl());
                resultMap.put("response", response);
                nodeResult.setOutput(JSONUtil.toJsonStr(resultMap));

                log.info("音乐生成成功, 音频URL: {}", response.getAudioUrl());
            } else {
                nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
                String failReason = response.getErrorMessage();
                nodeResult.setErrorInfo(failReason);

                log.error("音乐生成失败: {}", failReason);
            }

        } catch (Exception e) {
            log.error("音乐生成执行异常", e);
            nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
            String errorMsg = "音乐生成执行异常: " + e.getMessage();
            nodeResult.setErrorInfo(errorMsg);
        }

        return nodeResult;
    }

    /**
     * 节点参数配置
     */
    @Data
    public static class NodeParam {
        /**
         * 供应商名称
         */
        private String provider;

        /**
         * 模型名称
         */
        private String model;

        /**
         * 输入提示词
         */
        private String prompt;

        /**
         * 歌词内容
         */
        private String lyrics;

        // 输出结果参数

        /**
         * 采样率
         */
        private Integer sampleRate;

        /**
         * 比特率
         */
        private Integer bitrate;

        /**
         * 音频格式
         */
        private String format;

        /**
         * 输出格式
         */
        private String outputFormat;
    }
}
