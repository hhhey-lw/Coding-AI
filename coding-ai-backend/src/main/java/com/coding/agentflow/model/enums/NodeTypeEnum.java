package com.coding.agentflow.model.enums;

public enum NodeTypeEnum {
    CONDITION_AGENT, CONDITION,
    AGENT, LLM,
    START, END,
    TOOL, RETRIEVER,
    HUMAN_INPUT,
    HTTP
}