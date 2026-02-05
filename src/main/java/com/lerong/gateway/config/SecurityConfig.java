package com.lerong.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.LogoutWebFilter;

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
            // 配置授权规则
            .authorizeExchange(exchanges -> exchanges
                // 公开端点 - 不需要认证
                .pathMatchers(
                    "/health",
                    "/actuator/**",
                    "/api/auth/**",           // 认证服务端点（登录、注册等）
                    "/oauth2/**",             // OAuth2 授权端点
                    "/.well-known/**",        // JWK 公钥端点
                    "/public/**",             // 公开API
                    "/login",                 // 登录页面
                    "/login.html"             // 登录页面
                ).permitAll()
                // 其他所有请求都需要认证（需要携带有效的 JWT）
                .anyExchange().authenticated()
            )
            // 启用 OAuth2 Resource Server 的 JWT 支持
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(jwtDecoder())
                )
            )
            // 禁用 CSRF（网关服务通常不需要）
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            // 禁用 CORS（在 Gateway 全局配置中已处理）
            .cors(ServerHttpSecurity.CorsSpec::disable);

        return http.build();
    }

    /**
     * 配置 JWT 解码器
     * 从授权服务器的 JWK 端点获取公钥用于验证 JWT 签名
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // JWK 端点 URI - 从 auth-service 获取公钥
        String jwkSetUri = "http://localhost:8081/.well-known/jwks.json";

        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri)
                .build();

        // 可选：自定义 JWT 验证器
        // jwtDecoder.setJwtValidator(JwtValidators.createDefault());

        return jwtDecoder;
    }
}
