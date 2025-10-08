package com.coding.workflow.service.impl.processor;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import com.coding.workflow.enums.ErrorCodeEnum;
import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.manager.VideoGenerationManager;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.model.request.VideoGenerationRequest;
import com.coding.workflow.model.response.VideoGenerationResponse;
import com.coding.workflow.service.AbstractExecuteProcessor;
import com.coding.workflow.utils.AssertUtil;
import com.coding.workflow.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class VideoGenExecuteProcessor extends AbstractExecuteProcessor {
    // 输出配置
    public static final String VIDEO = "video";
    public static final String MESSAGE = "message";

    @Resource
    private VideoGenerationManager videoGenerationManager;

    @Override
    public String getNodeType() {
        return NodeTypeEnum.VIDEO_GEN.getCode();
    }

    @Override
    public String getNodeDescription() {
        return NodeTypeEnum.VIDEO_GEN.getDesc();
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
            AssertUtil.isTrue(StringUtils.isNotBlank(param.getModelId()), "模型ID不能为空");
            AssertUtil.isTrue(StringUtils.isNotBlank(param.getInputPrompt()), "输入提示词不能为空");
            AssertUtil.isTrue(StringUtils.isNotBlank(param.getFirstFrameImage()), "首帧图像不能为空");

            // 1.3 替换模板变量
            String finalPrompt = replaceTemplateContent(param.getInputPrompt(), context);
            String finalFirstFrameImage = replaceTemplateContent(param.getFirstFrameImage(), context);
            String finalTailFrameImage = replaceTemplateContent(param.getTailFrameImage(), context);
            param.setInputPrompt(finalPrompt);
            param.setFirstFrameImage(extractFirstImageUrl(finalFirstFrameImage));
            param.setTailFrameImage(extractFirstImageUrl(finalTailFrameImage));

            // 2. 构建视频生成请求
            VideoGenerationRequest request = VideoGenerationRequest.builder()
                    .provider(param.getProvider())
                    .model(param.getModelId())
                    .prompt(finalPrompt)
                    .videoSetting(VideoGenerationRequest.VideoSetting.builder()
                            .resolution(param.getResolution())
                            .duration(param.getDuration())
                            .firstFrameImage(param.getFirstFrameImage())
                            .lastFrameImage(param.getTailFrameImage())
                            .watermark(false)
                            .promptExtend(true)
                            .build())
                    .build();

            log.info("开始生成视频, provider: {}, model: {}, prompt: {}",
                param.getProvider(), param.getModelId(), finalPrompt);

            // 3. 调用视频生成服务
            VideoGenerationResponse response = videoGenerationManager.generateVideo(request);

            // 4. 处理返回结果
            if (response.isSuccess()) {
                // 构建输入数据
                nodeResult.setInput(JSONUtil.toJsonStr(param));

                // 构建输出结果
                HashMap<String, Object> output = Maps.newHashMap();
                output.put(VIDEO, response.getVideoUrl());
                output.put(MESSAGE, "视频生成成功");
                output.put("result", response);
                nodeResult.setOutput(JSONUtil.toJsonStr(output));

                log.info("视频生成成功, 视频URL: {}", response.getVideoUrl());
            } else {
                nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
                String failReason = response.getErrorMessage();
                nodeResult.setErrorInfo(failReason);
                nodeResult.setError(ErrorCodeEnum.WORKFLOW_EXECUTE_ERROR.toError(failReason));

                // 构建失败输出结果
                HashMap<String, Object> output = Maps.newHashMap();
                output.put(VIDEO, "");
                output.put(MESSAGE, "视频生成失败");
                output.put("result", response);
                nodeResult.setOutput(JSONUtil.toJsonStr(output));

                log.error("视频生成失败: {}", failReason);
            }

        } catch (Exception e) {
            log.error("视频生成执行异常", e);
            nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
            String errorMsg = "视频生成执行异常: " + e.getMessage();
            nodeResult.setErrorInfo(errorMsg);
            nodeResult.setError(ErrorCodeEnum.WORKFLOW_EXECUTE_ERROR.toError(errorMsg));
        }

        return nodeResult;
    }

    /**
     * 从输入字符串中提取第一个图片 URL，支持以下两种输入：
     * 1. JSON 数组字符串，如 "[\"https://example.com/xxx.jpg\", ...]" → 返回第一个元素 "https://example.com/xxx.jpg"
     * 2. 普通字符串，如 "https://example.com/xxx.jpg" → 原样返回
     *
     * @param input 输入的字符串，可能是 JSON 数组 or 单个 URL
     * @return 第一个图片 URL（如果是数组），或原字符串（如果是普通字符串）。如果输入无效，返回空字符串 ""
     */
    public static String extractFirstImageUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return ""; // 空安全
        }

        String trimmedInput = input.trim();

        // ===== 情况 1：如果输入是 JSON 数组格式（以 [ 开头，以 ] 结尾）=====
        if (trimmedInput.startsWith("[") && trimmedInput.endsWith("]")) {
            try {
                // 直接解析为 JSONArray（org.json 会帮我们处理转义、格式等一切问题）
                JSONArray jsonArray = new JSONArray(trimmedInput);

                // 如果数组里有至少一个元素
                if (jsonArray.size() > 0) {
                    // 直接取出第一个元素，并原样返回（已经是正常字符串，比如 "https://..."）
                    return jsonArray.get(0).toString();
                }
            } catch (JSONException e) {
                // 如果不是合法的 JSON 数组，那么降级处理，当作普通字符串返回
            }
        }

        // ===== 情况 2：如果不是 JSON 数组，直接原样返回输入字符串 =====
        return trimmedInput;
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
         * 模型ID
         */
        @JsonProperty("model_id")
        private String modelId;

        /**
         * 输入提示词
         */
        @JsonProperty("input_prompt")
        private String inputPrompt;

        /**
         * 分辨率
         */
        private String resolution;

        /**
         * 时长（秒）
         */
        private Integer duration;

        /**
         * 首帧图像URL
         */
        @JsonProperty("first_frame_image")
        private String firstFrameImage;

        /**
         * 尾帧图像URL
         */
        @JsonProperty("tail_frame_image")
        private String tailFrameImage;

    }
}
