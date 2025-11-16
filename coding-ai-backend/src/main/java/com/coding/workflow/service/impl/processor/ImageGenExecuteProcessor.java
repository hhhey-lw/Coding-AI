package com.coding.workflow.service.impl.processor;

import cn.hutool.json.JSONUtil;
import com.coding.workflow.enums.ErrorCodeEnum;
import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.manager.ImageGenerationManager;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.model.request.ImageGenerationRequest;
import com.coding.workflow.model.response.ImageGenerationResponse;
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

import java.util.*;

@Slf4j
@Service
public class ImageGenExecuteProcessor extends AbstractExecuteProcessor {

    /**
     * 默认单次最大生成图片数量：3张
     * */
    private static final Integer DEFAULT_MAX_IMAGES = 3;

    @Resource
    private ImageGenerationManager imageGenerationManager;

    @Override
    public String getNodeType() {
        return NodeTypeEnum.IMG_GEN.getCode();
    }

    @Override
    public String getNodeDescription() {
        return NodeTypeEnum.IMG_GEN.getDesc();
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
            AssertUtil.isTrue(StringUtils.isNotBlank(param.getModel()), "模型ID不能为空");
            AssertUtil.isTrue(StringUtils.isNotBlank(param.getPrompt()), "输入提示词不能为空");
            AssertUtil.isTrue(StringUtils.isNotBlank(param.getImgSize()), "图像尺寸不能为空");
            AssertUtil.isTrue(param.getMaxImages() <= DEFAULT_MAX_IMAGES, "单次生成图片数量不能超过 " + DEFAULT_MAX_IMAGES);

            // 1.3 替换模板变量
            String finalPrompt = replaceTemplateContent(param.getPrompt(), context);

            // 2. 构建图像生成请求
            ImageGenerationRequest request = ImageGenerationRequest.builder()
                    .provider(param.getProvider())
                    .modelId(param.getModel())
                    .prompt(finalPrompt)
                    .size(param.getImgSize())
                    .imageUrls(param.getImageUrls().stream()
                            .map(urlVar -> getValueFromPayload(urlVar, context.getVariablesMap()).toString())
                            .toList())
                    .maxImages(param.getMaxImages())
                    .watermark(param.getWatermark())
                    .build();

            log.info("开始生成图像, provider: {}, modelId: {}, prompt: {}", param.getProvider(), param.getModel(), finalPrompt);

            // 3. 调用图像生成服务
            ImageGenerationResponse response = imageGenerationManager.generateImages(request);

            // 4. 处理返回结果
            if (response.isSuccess()) {
                List<String> generatedUrls = response.getImageUrls();
                // 构建输入数据
                nodeResult.setInput(JSONUtil.toJsonStr(param));
                // 构建输出结果
                Map<String, Object> resultMap = Maps.newHashMap();
                resultMap.put(OUTPUT_DECORATE_PARAM_KEY, generatedUrls.toArray(new String[0]));
                nodeResult.setOutput(JSONUtil.toJsonStr(resultMap));

                log.info("图像生成成功, 生成了 {} 张图片", generatedUrls.size());
            } else {
                nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
                String failReason = response.getErrorMessage();
                nodeResult.setErrorInfo(failReason);

                log.error("图像生成失败: {}", failReason);
            }

        } catch (Exception e) {
            log.error("图像生成执行异常", e);
            nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
            String errorMsg = "图像生成执行异常: " + e.getMessage();
            nodeResult.setErrorInfo(errorMsg);
        }

        return nodeResult;
    }

    @Data
    public static class NodeParam {
        /** 模型提供商 */
        private String provider;
        /** 模型ID */
        private String model;
        /** 输入提示词 */
        private String prompt;
        /** 图像尺寸 */
        private String imgSize;
        /** 一次生成图片数量 */
        private Integer maxImages;
        /** 参考图 */
        private List<String> imageUrls;
        /** 是否启用水印 */
        private Boolean watermark;
    }

}
