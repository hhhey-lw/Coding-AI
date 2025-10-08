package com.coding.graph.core.state.strategy;

import java.util.Map;

@FunctionalInterface
public interface KeyStrategyFactory {

    Map<String, KeyStrategy> apply();

}
