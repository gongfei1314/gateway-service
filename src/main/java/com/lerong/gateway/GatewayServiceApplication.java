package com.lerong.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

/**
 * API 网关服务启动类
 *
 * @author Claude Code
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient  // 启用服务发现
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("API Gateway Service Started Successfully!");
        System.out.println("Gateway Port: 8080");
        System.out.println("========================================");
    }

    /**
     * 配置额外的路由规则（可选）
     * 大部分路由已在 application.yml 中配置
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 健康检查路由 - 不需要认证
                .route("health", r -> r
                        .path("/health", "/actuator/health")
                        .and()
                        .order(0)
                        .uri("lb://gateway-service"))
                .build();
    }
}
