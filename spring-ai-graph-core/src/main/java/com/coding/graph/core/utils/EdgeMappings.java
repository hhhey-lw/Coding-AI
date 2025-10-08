package com.coding.graph.core.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 边映射工具类，用于构建边的映射关系
 */
public class EdgeMappings {

    public static class Builder {
        private final Map<String, String> mappings = new LinkedHashMap<>();

        public Builder to(String destination, String label) {
            mappings.put(label, destination);
            return this;
        }

        public Map<String, String> build() {
            return Collections.unmodifiableMap(mappings);
        }

    }

    public static Builder builder() {
        return new Builder();
    }

}
