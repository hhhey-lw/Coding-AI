package com.coding.workflow.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.util.Map;

/**
 * JSON 工具类，提供对象与 JSON、Map 之间的转换方法。
 */
public class JsonUtils {

    /** 全局唯一的 ObjectMapper 实例，用于 JSON 处理 */
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将 Map 转换为指定类型的对象。
     * @param map 需要转换的 Map
     * @param clazz 目标对象的类类型
     * @param <T> 目标对象类型
     * @return 转换后的对象
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        return objectMapper.convertValue(map, clazz);
    }

    /**
     * 将对象序列化为 JSON 字符串。
     * @param obj 需要序列化的对象
     * @return 序列化后的 JSON 字符串
     * @throws RuntimeException 序列化失败时抛出运行时异常
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <K, V> Map<K, V> fromJsonToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}