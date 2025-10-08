package com.coding.workflow.service.runtime;

import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.impl.processor.ImageGenExecuteProcessor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class ImageGenNodeTest {

    @Resource
    private ImageGenExecuteProcessor imageGenExecuteProcessor;

    @Test
    void testImageGen() throws InterruptedException {
        Node node = new Node();
        node.setId("img_gen");
        node.setName("图片生成");
        node.setType(NodeTypeEnum.IMG_GEN.getCode());

        node.setConfig(Node.NodeCustomConfig.of(List.of(), List.of(), Map.of(
                "provider", "volcengine",
                "modelId", "doubao-seedream-4-0-250828",
                "input", "${Start.output}左侧人物站得略靠前，参考我图1的主体形象特征造型保持不变，需要保持人脸相似度；右侧人物参考图2的主体形象特征保持造型不变，需要保持人脸相似度，略微内扣身体，拍摄角度为高角度俯拍，使头部比例被夸张放大，符合典型的日韩视觉自拍风格。背景为纯白色，简洁干净，进一步凸显人物主体。画面风格偏向日系视觉系，整体画面清晰度高，用iphone前置自拍，最终呈现出精致、时尚、略带的合影效果。要求人物实现无缝融进画面，视觉过渡自然，整体画面光线明亮且均匀。",
                "imgSize", "4k",
                "maxImages", 2,
                "imageUrls", List.of("https://ark-common-storage-prod-cn-beijing.tos-cn-beijing.volces.com/experience_video/2101586132/0/20250924/0d7b4c81-3350-4e8e-be1f-20ba26747a93.png?X-Tos-Algorithm=TOS4-HMAC-SHA256&X-Tos-Content-Sha256=UNSIGNED-PAYLOAD&X-Tos-Credential=AKLTMjgxMzUwNzliYzdlNDE4MTllYjJjZGVlOWQ3N2M1ZDY%2F20250924%2Ftos-cn-beijing.volces.com%2Ftos%2Frequest&X-Tos-Date=20250924T153719Z&X-Tos-Expires=604800&X-Tos-SignedHeaders=host&X-Tos-Signature=654cc56ac5a50c007ed10f666b01985ecdee2a4112fbefb583056b43d5877bf8",
                        "https://ark-common-storage-prod-cn-beijing.tos-cn-beijing.volces.com/experience_video/2101586132/0/20250924/ed5fa251-931d-40fa-b7c3-1d6aef95f523.png?X-Tos-Algorithm=TOS4-HMAC-SHA256&X-Tos-Content-Sha256=UNSIGNED-PAYLOAD&X-Tos-Credential=AKLTMjgxMzUwNzliYzdlNDE4MTllYjJjZGVlOWQ3N2M1ZDY%2F20250924%2Ftos-cn-beijing.volces.com%2Ftos%2Frequest&X-Tos-Date=20250924T153719Z&X-Tos-Expires=604800&X-Tos-SignedHeaders=host&X-Tos-Signature=264a745e700d624c7369f203bdd5b21297f605b1e0f50729b2cbedcd7704a630")
        )));

        WorkflowContext context = new WorkflowContext();
        context.setConfigId(-1L);
        context.setInstanceId(-1L);
        context.getVariablesMap().put("Start", Map.of("output", "请将图1和图2融合成一张双人俯拍自拍照，画面构图紧凑，两位主体靠得很近，头部略微上仰，眼神直视镜头，营造出强烈的视觉冲击力。"));
        NodeResult nodeResult = imageGenExecuteProcessor.innerExecute(null, node, context);
        System.out.println(nodeResult);
    }

}
