package com.lerong.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * JWT 认证过滤器
 * 从请求中提取 JWT 并验证，将用户信息传递给下游服务
 *
 * @author Claude Code
 * @version 1.0
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final ReactiveJwtDecoder jwtDecoder;

    public JwtAuthenticationFilter(ReactiveJwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 跳过不需要认证的路径
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 从请求头中提取 Authorization
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 没有 JWT，继续执行链（会被 SecurityConfig 拦截）
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);  // 去掉 "Bearer " 前缀

        log.debug("Processing JWT token for path: {}", path);

        // 解码并验证 JWT
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    // JWT 验证成功，提取用户信息
                    String username = jwt.getClaimAsString("sub");
                    if (username == null) {
                        username = jwt.getClaimAsString("preferred_username");
                    }

                    log.debug("JWT validated for user: {}", username);

                    // 准备用户信息
                    final String userId = extractClaim(jwt, "user_id");
                    final String userName = username != null ? username : "";
                    final String userEmail = extractClaim(jwt, "email");
                    final String userRoles = extractClaim(jwt, "roles");

                    // 将用户信息添加到请求头传递给下游服务
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(r -> {
                                r.header("X-User-Id", userId);
                                r.header("X-User-Name", userName);
                                r.header("X-User-Email", userEmail);
                                r.header("X-User-Roles", userRoles);
                            })
                            .build();

                    return chain.filter(mutatedExchange);
                })
                .onErrorResume(ex -> {
                    log.error("JWT validation failed: {}", ex.getMessage());
                    // 继续执行链，让 SecurityConfig 处理认证失败
                    return chain.filter(exchange);
                });
    }

    /**
     * 判断是否为公开路径
     */
    private boolean isPublicPath(String path) {
        return path.equals("/health") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/api/auth/") ||
                path.startsWith("/oauth2/") ||
                path.startsWith("/.well-known/") ||
                path.startsWith("/public/") ||
                path.equals("/login") ||
                path.equals("/login.html");
    }

    /**
     * 从 JWT 中提取声明
     */
    private String extractClaim(Jwt jwt, String claimName) {
        Object claim = jwt.getClaims().get(claimName);
        return claim != null ? claim.toString() : "";
    }
}
