package com.coding.workflow.model.chat;

import lombok.Data;

import java.io.Serializable;

@Data
public class Content implements Serializable {

    /** 内容类型 */
    private String type;

}