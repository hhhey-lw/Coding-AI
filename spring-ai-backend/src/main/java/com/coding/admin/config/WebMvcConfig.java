package com.coding.admin.config;

import com.coding.admin.interceptor.AuthInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private AuthInterceptor authInterceptor;

    /**
     * 配置全局 Jackson ObjectMapper，让所有 Long 类型字段在序列化为 JSON 时，变成字符串
     * 目的是避免前端 JavaScript 无法安全处理大整数（如 1234567890123456789）导致的精度丢失
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 创建一个 Jackson Module
        SimpleModule module = new SimpleModule();

        // 支持 Java 8 时间类型（必须！）
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 将 Long 类型（包括 Long 和 long）序列化为字符串
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(long.class, ToStringSerializer.instance);

        // 注册该 Module 到 ObjectMapper
        objectMapper.registerModule(module);

        return objectMapper;
    }

    /**
     * 配置 CORS 跨域支持，解决 Preflight OPTIONS 403 问题
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 所有接口路径
                .allowedOrigins("*") // 允许所有来源，生产环境建议指定如 "http://前端域名:端口"
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的 HTTP 方法
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(false) // 是否允许发送 cookie，一般设为 false 除非需要 session/cookie
                .maxAge(3600); // 预检请求缓存时间（单位：秒），减少重复预检
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")
                // 排除登录注册相关接口
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/user/send-code",
                        "/user/refresh-token",
                        "/user/logout",
                        // 排除Swagger相关路径
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/doc.html",
                        // 排除静态资源
                        "/favicon.ico",
                        "/error",
                        // 测试接口
                        "/ai/agent/**",
                        // 知识库
                        "/ai/knowledge/**",
                        "/api/knowledge-base/**",
                        "/api/knowledge-vector/**"
                );
    }
}