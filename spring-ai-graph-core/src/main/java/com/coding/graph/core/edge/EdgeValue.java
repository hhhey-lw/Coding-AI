package com.coding.graph.core.edge;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 边包装，包含边ID和边条件
 *
 * @param id    节点Id
 * @param value 边条件
 */
public record EdgeValue(String id, EdgeCondition value) {

    public EdgeValue(String id) {
        this(id, null);
    }

    public EdgeValue(EdgeCondition value) {
        this(null, value);
    }

    // 更新EdgeCondition中的目标ID，Function：给id追加子图的id前缀
    public EdgeValue withTargetIdsUpdated(Function<String, EdgeValue> target) {
        if (id != null) {
            return target.apply(id);
        }

        Map<String, String> newMappings = value.mappings().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
            var v = target.apply(e.getValue());
            return (v.id() != null) ? v.id() : e.getValue();
        }));

        return new EdgeValue(null, new EdgeCondition(value.action(), newMappings));
    }
}
