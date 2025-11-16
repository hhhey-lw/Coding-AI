package com.coding.graph.core.node;

import com.coding.graph.core.node.factory.ActionFactory;
import lombok.Getter;

import java.util.function.Function;

@Getter
public class Node {
    private final String id;

    private final ActionFactory actionFactory;

    public Node(String id) {
        this(id, null);
    }

    public Node(String id, ActionFactory actionFactory) {
        this.id = id;
        this.actionFactory = actionFactory;
    }

    public boolean isParallel() {
        return false;
    }

    public Node withIdUpdated(Function<String, String> newId) {
        return new Node(newId.apply(id), actionFactory);
    }
}
