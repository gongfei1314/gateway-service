#!/bin/bash

echo "=========================================="
echo "API 网关 JWT 验证测试"
echo "=========================================="
echo ""

# 测试 1: 公开端点
echo "【测试 1】访问公开端点 /health（不需要 JWT）"
curl -s http://localhost:8080/health
echo -e "\n"

# 测试 2: 通过网关访问 auth-service（不需要 JWT）
echo "【测试 2】通过网关访问 auth-service 健康检查（不需要 JWT）"
curl -s http://localhost:8080/api/auth/health
echo -e "\n"

# 测试 3: 访问受保护的端点（不带 JWT，应该返回 401）
echo "【测试 3】访问受保护的端点 /api/users/profile（不带 JWT，应该返回 401 或 403）"
curl -s http://localhost:8080/api/users/profile
echo -e "\n"

# 测试 4: 测试 JWK 端点（公开，用于获取公钥）
echo "【测试 4】访问 JWK 公钥端点（通过网关转发到 auth-service）"
curl -s http://localhost:8080/api/auth/.well-known/jwks.json
echo -e "\n"

echo "=========================================="
echo "测试完成"
echo "=========================================="
