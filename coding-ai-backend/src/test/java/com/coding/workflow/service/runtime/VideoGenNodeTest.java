package com.coding.workflow.service.runtime;

import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.impl.processor.VideoGenExecuteProcessor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class VideoGenNodeTest {

    @Resource
    private VideoGenExecuteProcessor videoGenExecuteProcessor;

    @Test
    void testVideoGen() throws InterruptedException {
        Node node = new Node();
        node.setId("video_gen");
        node.setName("视频生成");
        node.setType(NodeTypeEnum.VIDEO_GEN.getCode());

        node.setConfig(Node.NodeCustomConfig.of(List.of(), List.of(), Map.of(
                "provider", "BaiLian",
                "model_id", "wan2.2-kf2v-flash",
                "resolution", "480P",
                "input_prompt", "基于提供的人像照片，生成一段3秒的iPhone苹果风格Live图，视角为手持自拍（手机贴近面部，手臂自然前伸），画面内容为人物自然状态下的微动态：包含轻微的头部转动（左右不超过15度）、自然眨眼（1-2次）、嘴角微微上扬的微笑，整体动作流畅不突兀。背景保持原照片环境（如室内/室外），光线和色彩与原图一致，手机边缘虚化自然（模拟手持时的景深效果）",
                "duration", "5",
                "first_frame_image", "https://youke1.picui.cn/s1/2025/09/25/68d543618acc4.jpeg"
        )));

        WorkflowContext context = new WorkflowContext();
        context.setConfigId(123L);
        context.setInstanceId(321L);
        NodeResult nodeResult = videoGenExecuteProcessor.innerExecute(null, node, context);
        System.out.println(nodeResult);
    }

}
