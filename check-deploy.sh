#!/bin/bash

# do-write 部署前检查脚本
# 用于验证配置是否正确，避免部署时出现问题

set -e

echo "=========================================="
echo "   do-write 部署前检查"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查计数
ERRORS=0
WARNINGS=0

# 检查函数
check_pass() {
    echo -e "${GREEN}✓${NC} $1"
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
    WARNINGS=$((WARNINGS + 1))
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
    ERRORS=$((ERRORS + 1))
}

# 1. 检查 .env 文件
echo "1. 检查环境变量文件..."
if [ -f ".env" ]; then
    check_pass ".env 文件存在"

    # 检查关键配置
    source .env

    # 检查数据库密码
    if [ "$MYSQL_PASSWORD" == "please-change-me" ]; then
        check_fail "MySQL 密码未修改（请在 .env 中修改 MYSQL_PASSWORD）"
    else
        check_pass "MySQL 密码已自定义"
    fi

    # 检查 Redis 密码
    if [ "$REDIS_PASSWORD" == "please-change-me" ]; then
        check_fail "Redis 密码未修改（请在 .env 中修改 REDIS_PASSWORD）"
    else
        check_pass "Redis 密码已自定义"
    fi

    # 检查 APP_BASE_URL
    if [[ "$APP_BASE_URL" == *"localhost"* ]]; then
        check_warn "APP_BASE_URL 仍为 localhost（生产环境请修改为 https://yourdomain.com）"
    elif [[ "$APP_BASE_URL" == http://* ]]; then
        check_warn "APP_BASE_URL 使用 HTTP（建议使用 HTTPS）"
    else
        check_pass "APP_BASE_URL 已配置为 HTTPS"
    fi

    # 检查 AI 加密密钥
    if [ "$AI_ENCRYPTION_KEY" == "D0-Writ3-32b!t" ]; then
        check_warn "AI 加密密钥使用默认值（建议修改为随机32位字符）"
    else
        check_pass "AI 加密密钥已自定义"
    fi

else
    check_fail ".env 文件不存在（请复制 .env.example 为 .env）"
fi

echo ""

# 2. 检查 Docker 环境
echo "2. 检查 Docker 环境..."
if command -v docker &> /dev/null; then
    check_pass "Docker 已安装 ($(docker --version))"
else
    check_fail "Docker 未安装"
fi

if command -v docker-compose &> /dev/null || docker compose version &> /dev/null; then
    check_pass "Docker Compose 已安装"
else
    check_fail "Docker Compose 未安装"
fi

echo ""

# 3. 检查 SSL 证书（如果配置了 HTTPS）
echo "3. 检查 SSL 证书..."
if [ -d "docker/nginx/ssl" ]; then
    CRT_COUNT=$(find docker/nginx/ssl -name "*.crt" -o -name "*.pem" | wc -l)
    KEY_COUNT=$(find docker/nginx/ssl -name "*.key" | wc -l)

    if [ $CRT_COUNT -gt 0 ] && [ $KEY_COUNT -gt 0 ]; then
        check_pass "SSL 证书文件已上传 ($CRT_COUNT 个证书，$KEY_COUNT 个私钥)"

        # 检查 nginx 配置是否为 HTTPS
        if grep -q "listen 443" docker/nginx/default.conf; then
            check_pass "Nginx 已配置 HTTPS (443端口)"
        else
            check_warn "发现 SSL 证书，但 Nginx 未配置 HTTPS（请参考 default-https.conf.example）"
        fi
    else
        if [[ "$APP_BASE_URL" == https://* ]]; then
            check_fail "APP_BASE_URL 使用 HTTPS，但未找到 SSL 证书文件"
        else
            check_warn "未找到 SSL 证书（如需 HTTPS，请上传证书到 docker/nginx/ssl/）"
        fi
    fi
else
    check_warn "docker/nginx/ssl 目录不存在"
fi

echo ""

# 4. 检查端口占用
echo "4. 检查端口占用..."
check_port() {
    PORT=$1
    if lsof -i:$PORT &> /dev/null || netstat -tuln 2>/dev/null | grep -q ":$PORT "; then
        check_warn "端口 $PORT 已被占用（可能导致容器启动失败）"
    else
        check_pass "端口 $PORT 可用"
    fi
}

check_port 80
check_port 443
check_port 3306
check_port 6379
check_port 8099

echo ""

# 5. 检查必要的文件
echo "5. 检查必要的文件..."
check_file() {
    if [ -f "$1" ]; then
        check_pass "$1 存在"
    else
        check_fail "$1 不存在"
    fi
}

check_file "docker-compose.yml"
check_file "docker/nginx/default.conf"
check_file "docker/write-server/Dockerfile"
check_file "docker/frontend/Dockerfile"
check_file "docs/sql/init.sql"

echo ""

# 6. 检查磁盘空间
echo "6. 检查磁盘空间..."
AVAILABLE=$(df -BG . | tail -1 | awk '{print $4}' | sed 's/G//')
if [ "$AVAILABLE" -gt 20 ]; then
    check_pass "磁盘空间充足 (${AVAILABLE}GB 可用)"
elif [ "$AVAILABLE" -gt 10 ]; then
    check_warn "磁盘空间偏少 (${AVAILABLE}GB 可用，建议至少20GB)"
else
    check_fail "磁盘空间不足 (${AVAILABLE}GB 可用，建议至少20GB)"
fi

echo ""

# 总结
echo "=========================================="
echo "   检查完成"
echo "=========================================="
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ 所有检查通过！可以开始部署。${NC}"
    echo ""
    echo "部署命令："
    echo "  docker compose up -d"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ 发现 $WARNINGS 个警告，建议修复后再部署。${NC}"
    echo ""
    echo "如果确定要继续，执行："
    echo "  docker compose up -d"
    exit 0
else
    echo -e "${RED}✗ 发现 $ERRORS 个错误，$WARNINGS 个警告。${NC}"
    echo -e "${RED}请修复错误后再尝试部署。${NC}"
    exit 1
fi