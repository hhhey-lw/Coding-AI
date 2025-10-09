package com.coding.graph.core.state;

import com.coding.graph.core.state.strategy.KeyStrategy;
import lombok.Builder;

import java.io.Serializable;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;


@Builder
public final class OverAllState implements Serializable {
    /**
     * 存储状态数据
     */
    private final Map<String, Object> data;

    /**
     * 存储各个key的更新策略：覆盖or追加
     */
    private final Map<String, KeyStrategy> keyStrategies;

    /**
     * 输入节点的默认key
     */
    public static final String DEFAULT_INPUT_KEY = "input";

    // 构造函数
    public OverAllState() {
        this.data = new HashMap<>();
        this.keyStrategies = new HashMap<>();
    }

    public OverAllState(Map<String, Object> data) {
        this.data = data;
        this.keyStrategies = new HashMap<>();
    }

    public OverAllState(Map<String, Object> data, Map<String, KeyStrategy> keyStrategies) {
        this.data = data != null ? data : new HashMap<>();
        this.keyStrategies = keyStrategies != null ? keyStrategies : new HashMap<>();
    }

    /**
     * 清空状态数据
     */
    public void clear() {
        this.data.clear();
    }

    /**
     * 状态数据Map
     *
     * @return 状态数据
     */
    public final Map<String, Object> data() {
        return data != null ? unmodifiableMap(data) : unmodifiableMap(new HashMap<>());
    }

    public Map<String, KeyStrategy> keyStrategies() {
        return keyStrategies;
    }

    /**
     * 获取状态数据
     *
     * @return 状态数据
     */
    public final <T> Optional<T> value(String key) {
        return ofNullable((T) data().get(key));
    }

    /**
     * 获取状态数据并转换为指定类型
     *
     * @param key  状态数据key
     * @param type 目标类型
     * @param <T>  目标类型泛型
     * @return 转换后的状态数据
     */
    public final <T> Optional<T> value(String key, Class<T> type) {
        if (type == null) {
            return value(key);
        }

        Object value = data().get(key);
        if (value == null) {
            return Optional.empty();
        }

        try {
            // Direct type conversion
            if (type.isInstance(value)) {
                return ofNullable(type.cast(value));
            }

            // Special handling for List type
            if (List.class.isAssignableFrom(type) && value instanceof List) {
                @SuppressWarnings("unchecked")
                T castedList = (T) value;
                return ofNullable(castedList);
            }

            // If no match, try direct conversion (maintain original behavior)
            return ofNullable(type.cast(value));
        }
        catch (ClassCastException e) {
            // Return empty Optional when conversion fails
            return Optional.empty();
        }
    }

    /**
     * 获取状态数据，若不存在则返回默认值
     * @param key 状态数据key
     * @param defaultValue 默认值
     */
    public final <T> T value(String key, T defaultValue) {
        return (T) value(key).orElse(defaultValue);
    }

    /**
     * 更新状态数据：根据Key Strategy决定是覆盖还是追加
     */
    public Map<String, Object> updateState(Map<String, Object> partialState) {
        if (partialState == null) {
            return data();
        }

        Map<String, KeyStrategy> keyStrategies = keyStrategies();
        partialState.keySet().forEach(key -> {
            KeyStrategy strategy = keyStrategies.getOrDefault(key, KeyStrategy.REPLACE);
            this.data.put(key, strategy.apply(value(key, null), partialState.get(key)));
        });
        return data();
    }

    /**
     * 更新状态数据：根据Key Strategy决定是覆盖还是追加
     *
     * @param state 当前状态数据
     * @param partialState  新状态数据
     * @return 更新后的状态数据
     */
    public static Map<String, Object> updateState(Map<String, Object> state, Map<String, Object> partialState, Map<String, KeyStrategy> keyStrategies) {
        Objects.requireNonNull(state, "state 不能为 null");
        if (partialState == null || partialState.isEmpty()) {
            return state;
        }

        // 合并两个状态
        Map<String, Object> updatedPartialState = updatePartialStateFromSchema(state, partialState, keyStrategies);


        return Stream.concat(state.entrySet().stream(), updatedPartialState.entrySet().stream())
                .collect(toMapRemovingNulls(Map.Entry::getKey, Map.Entry::getValue, (currentValue, newValue) -> newValue));
    }

    /**
     * 根据Key Strategy合并两个状态
     */
    private static Map<String, Object> updatePartialStateFromSchema(Map<String, Object> state,
                                                                    Map<String, Object> partialState,
                                                                    Map<String, KeyStrategy> keyStrategies) {
        if (partialState == null || partialState.isEmpty()) {
            return partialState;
        }

        return partialState.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            KeyStrategy strategy = keyStrategies.getOrDefault(entry.getKey(), KeyStrategy.REPLACE);
                            return strategy.apply(state.get(entry.getKey()), entry.getValue());
                        }
                ));
    }

    // 去除null值的toMap收集器
    private static <T, K, U> Collector<T, ?, Map<K, U>> toMapRemovingNulls(Function<? super T, ? extends K> keyMapper,
                                                                           Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        return Collector.of(HashMap::new, (map, element) -> {
            K key = keyMapper.apply(element);
            U value = valueMapper.apply(element);
            if (value == null) {
                map.remove(key);
            }
            else {
                map.merge(key, value, mergeFunction);
            }
        }, (map1, map2) -> {
            map2.forEach((key, value) -> {
                if (value != null) {
                    map1.merge(key, value, mergeFunction);
                }
            });
            return map1;
        }, Collector.Characteristics.UNORDERED);
    }

}
