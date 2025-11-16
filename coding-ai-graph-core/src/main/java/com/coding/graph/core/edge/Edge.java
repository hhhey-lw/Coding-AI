package com.coding.graph.core.edge;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 边的定义
 * @param sourceId 起点
 * @param targets 终点列表
 */
public record Edge(String sourceId, List<EdgeValue> targets) {
    // 构造函数
    public Edge(String id) {
        this(id, List.of());
    }
    public Edge(String sourceId, EdgeValue target) {
        this(sourceId, List.of(target));
    }
    // 判断是否为并行边
    public boolean isParallel() {
        return targets.size() > 1;
    }

    // 获取第一个目标边
    public EdgeValue target() {
        if (isParallel()) {
            throw new IllegalStateException(String.format("Edge '%s' is parallel", sourceId));
        }
        return targets.get(0);
    }
    // 根据目标ID检查是否有匹配的目标
    public boolean anyMatchByTargetId(String targetId) {
        return targets().stream()
                .anyMatch(v -> (v.id() != null) ? Objects.equals(v.id(), targetId)
                        : v.value().mappings().containsValue(targetId)
                );
    }

    // 根据起点Id判断是否相等：用于聚合起点一致的边为一条边
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Edge node = (Edge) o;
        return Objects.equals(sourceId, node.sourceId);
    }
    // 创建边：Func1:节点的起点Id更新函数，Func2:节点的终点Id更新函数
    public Edge withSourceAndTargetIdsUpdated(Function<String, String> newSourceId, Function<String, EdgeValue> newTarget) {
        var newTargets = targets.stream().map(t -> t.withTargetIdsUpdated(newTarget)).toList();
        return new Edge(newSourceId.apply(sourceId), newTargets);
    }
}
