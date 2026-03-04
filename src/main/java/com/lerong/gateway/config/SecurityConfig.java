package com.lerong.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * API 网关安全配置
 * 配置 JWT 验证和公开端点
 *
 * @author Claude Code
 * @version 1.0
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            // 禁用 CSRF
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            // 禁用 CORS
            .cors(ServerHttpSecurity.CorsSpec::disable)
            // 配置授权规则
            .authorizeExchange(exchanges -> exchanges
                // 公开端点 - 不需要认证
                .pathMatchers(
                    "/health",
                    "/actuator/**",
                    "/api/auth/**",           // 认证服务所有端点公开
                    "/oauth2/**",             // OAuth2 授权端点
                    "/.well-known/**",        // JWK 公钥端点
                    "/public/**",             // 公开API
                    "/login",                 // 登录页面
                    "/login.html"             // 登录页面
                ).permitAll()
                // 其他所有请求都需要认证（需要携带有效的 JWT）
                .anyExchange().authenticated()
            )
            // 对于需要认证的请求，不启用OAuth2 Resource Server
            // 因为我们使用的是mock token，不是真正的JWT
            // 真正的认证会在各个微服务中处理
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((exchange, denied) -> {
                    // 返回401未授权
                    exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                })
            );

        return http.build();
    }
}
