package com.coding.graph.core.state.strategy;

import java.util.HashMap;
import java.util.Map;

public class KeyStrategyFactoryBuilder {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, KeyStrategy> strategies = new HashMap<>();

        private KeyStrategy defaultStrategy = new ReplaceStrategy();

        public Builder addStrategy(String key, KeyStrategy strategy) {
            strategies.put(key, strategy);
            return this;
        }

        public Builder addStrategy(String key) {
            strategies.put(key, defaultStrategy);
            return this;
        }

        public Builder addStrategies(Map<String, KeyStrategy> strategiesMap) {
            strategies.putAll(strategiesMap);
            return this;
        }

        public Builder defaultStrategy(KeyStrategy defaultStrategy) {
            this.defaultStrategy = defaultStrategy;
            return this;
        }

        public Builder removeStrategy(String key) {
            strategies.remove(key);
            return this;
        }

        public KeyStrategyFactory build() {
            final Map<String, KeyStrategy> immutableStrategies = Map.copyOf(strategies);
            return () -> immutableStrategies;
        }
    }

}
