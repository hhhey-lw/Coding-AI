package com.coding.graph.core.state.strategy;

public class ReplaceStrategy implements KeyStrategy {
    @Override
    public Object apply(Object oldValue, Object newValue) {
        return newValue;
    }
}
