package com.coding.graph.core.node.command;

import java.util.Map;
import java.util.Objects;

/**
 * 目标节点 + 状态
 * @param gotoNode
 * @param update
 */
public record Command(String gotoNode, Map<String, Object> update) {

    public Command {
        Objects.requireNonNull(gotoNode, "gotoNode 不能为 null");
        Objects.requireNonNull(update, "update 不能为 null");
    }

    public Command(String gotoNode) {
        this(gotoNode, Map.of());
    }

}
