package com.coding.workflow.model.chat;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author weilong
 * @date 2025/9/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TextContent extends Content {

    /** 文本内容 */
    private String text;
}
