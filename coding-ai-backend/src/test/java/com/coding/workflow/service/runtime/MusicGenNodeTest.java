package com.coding.workflow.service.runtime;

import cn.hutool.json.JSONUtil;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.impl.processor.MusicGenExecuteProcessor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
public class MusicGenNodeTest {

    @Resource
    private MusicGenExecuteProcessor musicGenExecuteProcessor;

    @Test
    void testMusicGen() throws InterruptedException {
        Node node = new Node();
        node.setId("music_gen");
        node.setType("MusicGen");
        node.setName("音乐生成");
        node.setConfig(Node.NodeCustomConfig.of(null, null, Map.of(
                "provider", "minimax",
                "model", "music-1.5", // 模型名称
                "input", "充满活力的 R&B 与 Rap 融合曲风，律动感强，散发夜晚都市的魅力与自信，适合 late night 场景，节奏鲜明，情绪高涨。",
                "lyrics", "[Intro]\n" +
                    "\n" +
                    "\u200B\u200B\"Tick-tock... 三万光年的时差，\u200B\u200B\n" +
                    "\n" +
                    "\u200B\u200B你名字在黑洞边缘循环播放...\"\u200B\u200B\n" +
                    "\n" +
                    "\u200B\u200B[Verse 1]\n" +
                    "\n" +
                    "凌晨三点半的星图在视网膜灼烧，\n" +
                    "\n" +
                    "你留下的咖啡渍蒸发成星云暗号。\n" +
                    "\n" +
                    "对话框定格在「晚安」的像素残骸，\n" +
                    "\n" +
                    "我像颗故障卫星，绕着回忆公转逃不开。\n" +
                    "\n" +
                    "秒针卡在相遇的经纬度坐标，\n" +
                    "\n" +
                    "你眼里的银河系正批量生产寂寥。\n" +
                    "\n" +
                    "\"要不要逃？\" 舌尖抵住这句叛逃宣言，\n" +
                    "\n" +
                    "却听见钟摆溶解前最后的冷笑——\n" +
                    "\n" +
                    "\u200B\u200B\"时间偷走了我们，连收据都不给。\"\u200B\u200B\n" +
                    "\n" +
                    "\u200B\u200B[Pre-Chorus]\n" +
                    "\n" +
                    "A star-crossed night，霓虹在血管里暴动，\n" +
                    "\n" +
                    "我们是被引力操控的笨重钟摆。\n" +
                    "\n" +
                    "每一次触碰都像偷窃未来的赃物，\n" +
                    "\n" +
                    "而时间这个骗子...它早把答案藏进倒带中。\n" +
                    "\n" +
                    "\u200B\u200B[Chorus]\n" +
                    "\n" +
                    "\u200B\u200BStar-drifting through the cracks of yesterday，\u200B\u200B\n" +
                    "\n" +
                    "\u200B\u200B你是我瞳孔里永不熄灭的耀斑。\u200B\u200B\n" +
                    "\n" +
                    "\u200B\u200B时针在撒谎，说我们从未走散，\u200B\u200B\n" +
                    "\n" +
                    "\u200B\u200B可城堡废墟上，只剩贪吃蛇啃噬着遗憾。\u200B\u200B\n" +
                    "\n" +
                    "\n" +
                    "\u200B\u200B[Verse 2]\n" +
                    "\n" +
                    "Yo！我扛着韵脚在平行宇宙搬救兵，\n" +
                    "\n" +
                    "你的影子却比黑洞更擅长吞噬光影。\n" +
                    "\n" +
                    "那些未发送的讯息堆积成陨石带，\n" +
                    "\n" +
                    "在记忆体深处闪烁像故障的LED。\n" +
                    "\n" +
                    "我试图像DJ般scratch命运的唱盘，\n" +
                    "\n" +
                    "但每个loop都循环到你离场的片段。\n" +
                    "\n" +
                    "当你说\"未来太吵\"转身走向光年外，\n" +
                    "\n" +
                    "我成了被遗弃在时空夹缝的...旧唱片。\n" +
                    "\n" +
                    "\u200B\u200B[Bridge]\n" +
                    "\n" +
                    "\u200B\u200B\"Tell me why——\"\u200B\u200B\n" +
                    "\u200B\u200B\"Tell me why——\"\u200B\n" +
                    "\n" +
                    "\u200B\u200B\"4 letters... T-I-M-E...\"\u200B\u200B\n" +
                    "\n" +
                    "\n" +
                    "\u200B\u200B[Outro]\n" +
                    "\n" +
                    "后来所有流星都长得像你的侧脸，\n" +
                    "\n" +
                    "而我困在重播键按烂的午夜。\n" +
                    "\n" +
                    "如果宇宙允许最后一次任性穿越，\n" +
                    "\n" +
                    "能否停在——你转身前的，那帧画面？"
        )));
        WorkflowContext context = new WorkflowContext();
        context.setConfigId(-1L);
        context.setInstanceId(-1L);
        NodeResult nodeResult = musicGenExecuteProcessor.innerExecute(null, node, context);
        System.out.println(JSONUtil.toJsonStr(nodeResult));

    }

}
