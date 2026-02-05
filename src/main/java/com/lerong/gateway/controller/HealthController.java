package com.lerong.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 *
 * @author Claude Code
 * @version 1.0
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    /**
     * 健康检查端点
     */
    @RequestMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "gateway-service");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("description", "API Gateway with JWT Authentication");
        return Mono.just(health);
    }

    /**
     * 网关信息端点
     */
    @GetMapping("/info")
    public Mono<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "gateway-service");
        info.put("description", "API Gateway Service with JWT Authentication");
        info.put("version", "1.0.0");
        info.put("author", "Claude Code");
        info.put("features", new String[]{
                "Spring Cloud Gateway",
                "OAuth2 JWT Validation",
                "Global Exception Handling",
                "CORS Support",
                "Load Balancing",
                "Request Routing"
        });
        return Mono.just(info);
    }
}
