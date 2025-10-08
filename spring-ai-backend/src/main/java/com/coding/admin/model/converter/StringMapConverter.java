package com.coding.admin.model.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 字符串和Map转换工具类
 * @author coding
 * @date 2025-09-21
 */
@Component
public class StringMapConverter {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public Map<String, Object> stringToMap(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }
    
    public String mapToString(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }
}