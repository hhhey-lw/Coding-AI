package com.coding.admin.config;

import com.coding.admin.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

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
                        "/ai/agent/**"
                );
    }
}
