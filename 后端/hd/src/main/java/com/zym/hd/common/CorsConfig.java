package com.zym.hd.common;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 全局跨域配置
 * 允许前端开发服务器（Vite 默认 5173 端口）访问后端接口
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的前端来源（根据实际情况修改，生产环境替换为真实域名）
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",   // Vite 开发服务器
                "http://localhost:5174",   // Vite 备用端口
                "http://localhost:3000",   // 其他本地前端
                "http://127.0.0.1:5173"
        ));

        // 允许所有 HTTP 方法
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 允许所有请求头（包括 Authorization、Content-Type 等）
        config.setAllowedHeaders(List.of("*"));

        // 允许携带认证信息（Cookie、Authorization Header）
        config.setAllowCredentials(true);

        // 预检请求（OPTIONS）缓存时间，单位：秒（1小时）
        config.setMaxAge(3600L);

        // 暴露给前端可读取的响应头
        config.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Disposition"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口路径生效
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
