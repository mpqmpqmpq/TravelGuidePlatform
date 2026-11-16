package com.mpq.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 排除登录接口、文档接口和其他不需要认证的接口
        registry.addInterceptor(jwtInterceptor)
                .excludePathPatterns(
                        "/auth/login",       // 登录接口
                        "/auth/email/login",
                        "/auth/**",
                        "/upload/image",
                        "/mcp/chat",
                        "/ai/**",
                        "/chatImage/chat",
                        "/registers/**",
                        "/ws/**",
                        "/actuator/**" //性能监控
                )
                .addPathPatterns("/**"); // 保护其他所有接口
    }
}