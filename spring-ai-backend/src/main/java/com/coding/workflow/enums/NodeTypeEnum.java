package com.coding.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NodeTypeEnum {
    START("Start", "开始节点"),
    TEXT_GEN("TextGen", "大语言模型节点"),
    MCP("MCP", "MCP节点"),
    IMG_GEN("ImgGen", "图像生成节点"),
    VIDEO_GEN("VideoGen", "视频生成节点"),
    MUSIC_GEN("MusicGen", "视频生成节点"),
    SCRIPT("Script", "脚本执行节点"),
    EMAIL("Email", "发送邮件执行节点"),
    JUDGE("Judge", "判断节点"),
    END("End", "结束节点"),

    WORKFLOW_FINISH("WorkflowFinish", "工作流结束标记")
    ;

    private final String code;

    private final String desc;
}
