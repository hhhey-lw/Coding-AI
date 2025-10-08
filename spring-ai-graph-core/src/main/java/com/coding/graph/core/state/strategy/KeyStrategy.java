package com.coding.graph.core.state.strategy;

import java.util.function.BiFunction;

public interface KeyStrategy extends BiFunction<Object, Object, Object> {

    KeyStrategy REPLACE = new ReplaceStrategy();
    KeyStrategy APPEND = new AppendStrategy();

}
