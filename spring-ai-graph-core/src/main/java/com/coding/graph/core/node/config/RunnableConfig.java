package com.coding.graph.core.node.config;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Data
@Builder
public class RunnableConfig {

    private String threadId;

    private String nextNode;

    private Map<String, Object> metadata;

    public Optional<String> threadId() {
        return ofNullable(threadId);
    }

    public Optional<String> nextNode() {
        return ofNullable(nextNode);
    }

    public Optional<Object> metadata(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return ofNullable(metadata).map(m -> m.get(key));
    }
}
