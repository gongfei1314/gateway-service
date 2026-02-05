# Gateway Service - API 网关服务

## 项目简介

这是基于 Spring Cloud Gateway 和 Spring Security OAuth2 Resource Server 构建的 API 网关服务，作为企业级微服务架构的统一入口。

### 核心功能

- ✅ **统一路由**: 将所有微服务请求统一通过网关转发
- ✅ **JWT 认证**: 验证从 auth-service 签发的 JWT 令牌
- ✅ **OAUTH2 集成**: 通过 JWK 端点验证令牌签名
- ✅ **全局异常处理**: 统一的错误响应格式
- ✅ **CORS 支持**: 跨域资源共享配置
- ✅ **请求过滤**: 提取用户信息并传递给下游服务
- ✅ **健康检查**: Actuator 监控端点

## 技术栈

- Java 17
- Spring Boot 3.5.9
- Spring Cloud Gateway 2024.0.0
- Spring Security OAuth2 Resource Server
- Lombok

## 项目结构

```
gateway-service/
├── src/main/java/com/lerong/gateway/
│   ├── GatewayServiceApplication.java      # 启动类
│   ├── config/
│   │   └── SecurityConfig.java             # 安全配置（JWT验证）
│   ├── controller/
│   │   └── HealthController.java           # 健康检查控制器
│   ├── exception/
│   │   └── GlobalExceptionHandler.java     # 全局异常处理器
│   └── filter/
│       └── JwtAuthenticationFilter.java    # JWT认证过滤器
├── src/main/resources/
│   └── application.yml                     # 配置文件
└── pom.xml                                 # Maven配置
```

## 配置说明

### 路由配置

网关配置了以下路由规则（在 `application.yml` 中）：

| 路由规则 | 目标服务 | 是否需要认证 |
|---------|---------|------------|
| `/api/auth/**` | auth-service:8081 | ❌ 否 |
| `/api/users/**` | user-service:8082 | ✅ 是 |
| `/api/customers/**` | customer-service:8083 | ✅ 是 |
| `/api/projects/**` | project-service:8084 | ✅ 是 |
| `/api/tasks/**` | task-service:8085 | ✅ 是 |
| `/api/finance/**` | finance-service:8086 | ✅ 是 |

### JWT 验证配置

网关通过以下配置验证 JWT：

```yaml
spring.security.oauth2.resourceserver.jwt:
  jwk-set-uri: http://localhost:8081/.well-known/jwks.json
  issuer-uri: http://localhost:8081
```

### 公开端点

以下端点不需要 JWT 认证：

- `/health` - 健康检查
- `/actuator/**` - 监控端点
- `/api/auth/**` - 认证服务（登录、注册等）
- `/oauth2/**` - OAuth2 授权端点
- `/.well-known/**` - JWK 公钥端点
- `/public/**` - 公开 API
- `/login`, `/login.html` - 登录页面

## 使用说明

### 1. 启动网关服务

```bash
# 确保先启动 auth-service（端口 8081）
cd auth-service
mvn spring-boot:run

# 在另一个终端启动 gateway-service
cd gateway-service
mvn spring-boot:run
```

### 2. 测试公开端点

```bash
# 健康检查（不需要 JWT）
curl http://localhost:8080/health

# 返回示例
{
  "status": "UP",
  "service": "gateway-service",
  "timestamp": "2026-02-04T22:00:00",
  "description": "API Gateway with JWT Authentication"
}
```

### 3. 测试 JWT 认证流程

#### 步骤 1: 登录获取 JWT

```bash
# 通过网关访问 auth-service 登录
curl -X POST http://localhost:8080/api/auth/perform_login \
  -d "username=admin&password=admin123" \
  -c cookies.txt
```

#### 步骤 2: 使用 JWT 访问受保护的端点

```bash
# 访问需要认证的端点（需要携带 JWT）
curl http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4. 测试受保护的端点

```bash
# 不带 JWT 访问受保护端点 -> 401 Unauthorized
curl http://localhost:8080/api/users/profile

# 返回示例
{
  "timestamp": "2026-02-04T22:00:00",
  "status": 401,
  "error": "Unauthorized",
  "errorType": "AuthenticationException",
  "message": "认证失败，请重新登录",
  "path": "/api/users/profile"
}
```

## 错误响应格式

所有错误都返回统一的 JSON 格式：

```json
{
  "timestamp": "2026-02-04T22:00:00",
  "status": 401,
  "error": "Unauthorized",
  "errorType": "AuthenticationException",
  "message": "详细错误信息",
  "path": "/api/users/profile"
}
```

## HTTP 状态码

| 状态码 | 说明 |
|-------|------|
| 200 | 请求成功 |
| 401 | 未认证（JWT 无效或缺失） |
| 403 | 权限不足 |
| 404 | 路由不存在 |
| 500 | 网关内部错误 |
| 502 | 后端服务不可用 |
| 503 | 服务过载 |

## 请求头传递

网关会从 JWT 中提取用户信息，并通过以下请求头传递给下游服务：

- `X-User-Id`: 用户ID
- `X-User-Name`: 用户名
- `X-User-Email`: 用户邮箱
- `X-User-Roles`: 用户角色列表

下游服务可以直接从这些请求头中获取用户信息，无需再次解析 JWT。

## 与 Auth-Service 集成

网关依赖 `auth-service` 提供以下功能：

1. **JWK 端点**: `http://localhost:8081/.well-known/jwks.json` - 获取公钥用于验证 JWT 签名
2. **OAuth2 端点**: `/oauth2/authorize`, `/oauth2/token` - 签发 JWT 令牌
3. **登录端点**: `/perform_login` - 用户登录

## 日志配置

默认开启 DEBUG 日志级别：

```yaml
logging:
  level:
    com.lerong.gateway: DEBUG
    org.springframework.cloud.gateway: DEBUG
    org.springframework.security: DEBUG
```

## 监控端点

- `GET /actuator/health` - 健康检查
- `GET /actuator/info` - 应用信息
- `GET /actuator/gateway/routes` - 查看所有路由
- `GET /actuator/gateway/globalfilters` - 查看所有过滤器

## 常见问题

### 1. 启动失败：无法连接到 auth-service

**原因**: auth-service 未启动或端口不是 8081

**解决**: 先启动 auth-service，确保其在 8081 端口运行

### 2. JWT 验证失败

**原因**:
- JWT 已过期
- JWT 签名无效
- auth-service 的 JWK 端点不可访问

**解决**: 检查 auth-service 日志，确认 JWK 端点可访问

### 3. 路由 404

**原因**: 后端服务未启动

**解决**: 启动对应的微服务（user-service、customer-service 等）

## 后续优化建议

1. **限流熔断**: 集成 Sentinel 或 Resilience4j
2. **服务发现**: 集成 Nacos 实现动态路由
3. **链路追踪**: 集成 Sleuth + Zipkin
4. **日志收集**: 集成 ELK 收集网关日志
5. **灰度发布**: 实现基于权重的路由转发
6. **API 文档**: 集成 Swagger 展示所有路由
7. **监控告警**: 集成 Prometheus + Grafana

## 作者

Claude Code

## 版本

v1.0.0 - 2026-02-04
