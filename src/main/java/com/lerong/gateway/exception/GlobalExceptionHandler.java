package com.lerong.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理网关层的各种异常
 *
 * @author Claude Code
 * @version 1.0
 */
@Slf4j
@Order(-1)  // 确保优先级高于默认的异常处理器
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 设置响应头
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status;
        String message;
        String errorType;

        // 根据异常类型确定 HTTP 状态码和错误消息
        if (ex instanceof ResponseStatusException rse) {
            status = (HttpStatus) rse.getStatusCode();
            message = rse.getReason() != null ? rse.getReason() : rse.getMessage();
            errorType = "ResponseStatusException";
        } else if (ex instanceof org.springframework.web.server.ServerWebInputException) {
            status = HttpStatus.BAD_REQUEST;
            message = "请求参数错误: " + ex.getMessage();
            errorType = "BadRequestException";
        } else if (ex instanceof org.springframework.cloud.gateway.support.NotFoundException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "后端服务不可用";
            errorType = "ServiceUnavailableException";
        } else if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
            message = "访问被拒绝，权限不足";
            errorType = "AccessDeniedException";
        } else if (ex instanceof org.springframework.security.core.AuthenticationException) {
            status = HttpStatus.UNAUTHORIZED;
            message = "认证失败，请重新登录";
            errorType = "AuthenticationException";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "网关内部错误: " + ex.getMessage();
            errorType = "InternalServerError";
        }

        response.setStatusCode(status);

        // 记录错误日志
        log.error("Gateway Error - Type: {}, Status: {}, Message: {}, Path: {}",
                errorType, status.value(), message, exchange.getRequest().getPath().value(), ex);

        // 构建错误响应体
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("errorType", errorType);
        errorResponse.put("message", message);
        errorResponse.put("path", exchange.getRequest().getPath().value());

        // 将错误信息转换为 JSON
        String responseBody = toJson(errorResponse);

        DataBuffer buffer = response.bufferFactory()
                .wrap(responseBody.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 简单的 JSON 转换方法
     * 实际项目中建议使用 ObjectMapper
     */
    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
}
