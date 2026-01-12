# 1Panel 面板 Docker 部署指南

本文档详细说明如何在使用1Panel面板的服务器上部署 do-write 项目，并配置nginx + 腾讯云SSL证书。

## 目录
- [前置准备](#前置准备)
- [部署步骤](#部署步骤)
- [SSL证书配置](#ssl证书配置)
- [OAuth配置](#oauth配置)
- [常见问题](#常见问题)

---

## 前置准备

### 1. 服务器要求

- **最低配置**: 2C/4G/40GB SSD
- **推荐配置**: 4C/8G/80GB SSD（同机部署所有服务）
- **操作系统**: Ubuntu 20.04+ / CentOS 7+ / Debian 10+
- **已安装**: 1Panel面板（Docker和Docker Compose已自动安装）

### 2. 域名准备

- 准备一个已备案的域名（如 `yourdomain.com`）
- 将域名解析到服务器公网IP（A记录）
- 等待DNS解析生效（通常5-10分钟）

### 3. 腾讯云SSL证书下载

1. 登录腾讯云控制台 → SSL证书管理
2. 找到你的域名证书，点击"下载"
3. 选择 **Nginx** 类型下载（会得到一个zip文件）
4. 解压后得到两个文件：
   - `yourdomain.com_bundle.crt`（证书文件）
   - `yourdomain.com.key`（私钥文件）

---

## 部署步骤

### 步骤1: 上传项目代码

**方案A: 使用1Panel文件管理器（推荐新手）**

1. 打开1Panel面板 → 文件管理
2. 进入 `/opt` 目录（或你喜欢的目录）
3. 点击"上传" → 上传项目的zip压缩包
4. 解压到 `/opt/do-write`

**方案B: 使用Git（推荐）**

1. 打开1Panel面板 → 终端
2. 执行命令：
```bash
cd /opt
git clone <your-repo-url> do-write
cd do-write
```

### 步骤2: 上传SSL证书

1. 在1Panel面板 → 文件管理，进入 `/opt/do-write/docker/nginx`
2. 创建 `ssl` 目录：
```bash
mkdir -p /opt/do-write/docker/nginx/ssl
```
3. 上传腾讯云证书文件到 `/opt/do-write/docker/nginx/ssl/` 目录：
   - `yourdomain.com_bundle.crt`
   - `yourdomain.com.key`

### 步骤3: 修改Nginx配置（支持HTTPS）

1. 编辑 `/opt/do-write/docker/nginx/default.conf`，替换为以下内容：

```nginx
# HTTP 自动跳转到 HTTPS
server {
  listen 80;
  server_name yourdomain.com;  # 修改为你的域名

  # 强制跳转到 HTTPS
  return 301 https://$server_name$request_uri;
}

# HTTPS 主配置
server {
  listen 443 ssl http2;
  server_name yourdomain.com;  # 修改为你的域名

  # SSL 证书配置（腾讯云证书）
  ssl_certificate /etc/nginx/ssl/yourdomain.com_bundle.crt;  # 修改为你的证书文件名
  ssl_certificate_key /etc/nginx/ssl/yourdomain.com.key;     # 修改为你的私钥文件名

  # SSL 优化配置
  ssl_protocols TLSv1.2 TLSv1.3;
  ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:HIGH:!aNULL:!MD5:!RC4:!DHE;
  ssl_prefer_server_ciphers on;
  ssl_session_cache shared:SSL:10m;
  ssl_session_timeout 10m;

  client_max_body_size 20m;

  # 后端 API（包含 SSE：需要关闭缓冲，放宽超时）
  location /api/ {
    proxy_pass http://backend:8099;

    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Connection "";

    # SSE 支持
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 600s;
    proxy_send_timeout 600s;
  }

  # 前端（Next.js SSR）
  location / {
    proxy_pass http://frontend:3000;

    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Connection "";

    proxy_read_timeout 120s;
    proxy_send_timeout 120s;
  }
}
```

**重要**: 替换以下内容：
- `yourdomain.com` → 你的实际域名
- `yourdomain.com_bundle.crt` → 你的证书文件名
- `yourdomain.com.key` → 你的私钥文件名

### 步骤4: 修改 docker-compose.yml（挂载SSL证书）

编辑 `/opt/do-write/docker-compose.yml`，在 `nginx` 服务的 `volumes` 部分添加SSL证书挂载：

```yaml
  nginx:
    image: nginx:1.25-alpine
    container_name: do-write-nginx
    volumes:
      - ./docker/nginx/default.conf:/etc/nginx/conf.d/default.conf:ro
      - ./docker/nginx/ssl:/etc/nginx/ssl:ro  # 添加这一行，挂载SSL证书目录
    depends_on:
      - frontend
      - backend
    ports:
      - "80:80"
      - "443:443"  # 添加这一行，暴露443端口
    restart: unless-stopped
```

### 步骤5: 配置环境变量

1. 复制环境变量模板：
```bash
cd /opt/do-write
cp .env.example .env
```

2. 编辑 `.env` 文件，修改以下关键配置：

```bash
# ========== 通用 ==========
TZ=Asia/Shanghai

# 站点对外访问地址（HTTPS域名，用于 OAuth 回调）
APP_BASE_URL=https://yourdomain.com  # 修改为你的域名

# ========== 安全配置（必改！） ==========
MYSQL_PASSWORD=your_strong_mysql_password_here  # 修改为强密码
REDIS_PASSWORD=your_strong_redis_password_here  # 修改为强密码
AI_ENCRYPTION_KEY=your_32_character_encryption_key_here  # 修改为32位随机字符串

# ========== OAuth 配置（后续配置） ==========
# Linux.do OAuth
LINUXDO_CLIENT_ID=your-linuxdo-client-id
LINUXDO_CLIENT_SECRET=your-linuxdo-client-secret

# ========== AI 配置（可选，可后台配置） ==========
OPENAI_API_KEY=sk-placeholder-key
OPENAI_BASE_URL=https://api.openai.com
```

**安全提示**:
- 生成强密码示例（Linux/Mac）: `openssl rand -base64 32`
- 生成加密密钥: `openssl rand -hex 16`（正好32字符）

### 步骤6: 在1Panel中部署

**方案A: 使用1Panel应用商店（如果有Docker Compose模板）**

1. 1Panel → 应用商店 → 自建应用
2. 选择"从Git仓库安装"或"本地路径"
3. 填写项目路径: `/opt/do-write`
4. 选择 `docker-compose.yml` 文件
5. 点击"一键部署"

**方案B: 使用1Panel终端手动启动（推荐完全控制）**

1. 打开1Panel → 终端
2. 执行部署命令：

```bash
cd /opt/do-write

# 构建并启动所有服务
docker compose up -d

# 查看启动日志
docker compose logs -f

# 检查服务状态
docker compose ps
```

预期输出（所有服务都应该是 `Up` 状态）:
```
NAME                   STATUS
do-write-mysql         Up (healthy)
do-write-redis         Up
do-write-chroma        Up
do-write-backend       Up
do-write-frontend      Up
do-write-nginx         Up
```

### 步骤7: 防火墙配置

在1Panel面板中配置防火墙规则：

1. 1Panel → 安全 → 防火墙
2. 添加规则：
   - 端口 `80` → 允许所有IP访问（HTTP，会自动跳转HTTPS）
   - 端口 `443` → 允许所有IP访问（HTTPS）
3. 保存规则

如果使用云服务器，还需要在云厂商控制台配置安全组：
- 阿里云/腾讯云: 安全组规则 → 添加 80、443 端口入站规则

### 步骤8: 验证部署

1. 打开浏览器，访问 `https://yourdomain.com`
2. 检查SSL证书是否正常（浏览器地址栏显示🔒图标）

---

## OAuth配置

部署成功后，需要配置第三方OAuth登录。

### Linux.do OAuth 配置

1. 访问 [Linux.do](https://linux.do) → 个人设置 → 应用
2. 创建新应用：
   - 应用名称: `do-write`
   - 回调地址: `https://yourdomain.com/api/auth/oauth/linuxdo/callback`
3. 获取 `Client ID` 和 `Client Secret`
4. 修改 `.env` 文件中的 OAuth 配置
5. 重启服务: `docker compose restart backend`

### FishPi OAuth 配置（可选）类openid

---

## 常见问题

### Q1: 启动失败，提示端口被占用？

**解决方案**:
```bash
# 检查端口占用
netstat -tunlp | grep -E '80|443|3000|8099|3306|6379'

# 如果1Panel自带nginx占用80/443，需要停止或修改端口
systemctl stop nginx
# 或修改1Panel nginx配置，避免冲突
```

### Q2: SSL证书配置后仍显示"不安全"？

**排查步骤**:
1. 检查证书文件是否正确挂载:
```bash
docker exec -it do-write-nginx ls -l /etc/nginx/ssl/
```
2. 检查nginx配置:
```bash
docker exec -it do-write-nginx nginx -t
```
3. 查看nginx日志:
```bash
docker logs do-write-nginx
```

### Q3: 后端无法连接数据库？

**排查步骤**:
```bash
# 检查MySQL容器是否健康
docker compose ps mysql

# 查看MySQL日志
docker logs do-write-mysql

# 进入backend容器测试连接
docker exec -it do-write-backend sh
ping mysql
```

### Q4: OAuth回调地址错误？

确保 `.env` 中的 `APP_BASE_URL` 配置为完整的HTTPS域名：
```bash
APP_BASE_URL=https://yourdomain.com  # 注意是 https，不是 http
```

### Q5: 如何查看后端日志？

```bash
# 实时查看所有服务日志
docker compose logs -f

# 只查看后端日志
docker compose logs -f backend

# 查看最近100行日志
docker compose logs --tail=100 backend
```

### Q6: 如何更新项目代码？

```bash
cd /opt/do-write

# 拉取最新代码
git pull

# 重新构建并启动
docker compose down
docker compose up -d --build

# 查看启动日志
docker compose logs -f
```

### Q7: 如何备份数据？

**备份数据库**:
```bash
# 导出数据库
docker exec do-write-mysql mysqldump -uroot -p'your_password' do_write > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker exec -i do-write-mysql mysql -uroot -p'your_password' do_write < backup_20240101.sql
```

**备份Docker volumes**:
```bash
# 停止服务
docker compose down

# 备份数据目录（1Panel会自动管理volumes）
tar -czf backup_volumes.tar.gz /var/lib/docker/volumes/do-write_*

# 恢复并重启
docker compose up -d
```

---

## 性能优化建议

### 1. 启用Redis持久化

编辑 `.env`，Redis命令已包含AOF持久化：
```yaml
command: ["redis-server", "--appendonly", "yes", "--requirepass", "your_password"]
```

### 2. 调整后端JVM内存（如果服务器内存充足）

编辑 `docker/write-server/Dockerfile`，调整 `JAVA_TOOL_OPTIONS`:
```dockerfile
ENV JAVA_TOOL_OPTIONS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8"
```

### 3. 启用Nginx gzip压缩

编辑 `docker/nginx/default.conf`，在 `server` 块中添加：
```nginx
  # gzip 压缩
  gzip on;
  gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
  gzip_min_length 1000;
```

---

## 附录: 完整部署检查清单

- [ ] 服务器配置满足最低要求（2C/4G）
- [ ] 域名已解析到服务器IP
- [ ] 腾讯云SSL证书已下载并上传
- [ ] `.env` 文件已配置（特别是密码和域名）
- [ ] `docker-compose.yml` 已添加443端口映射
- [ ] `nginx/default.conf` 已修改为HTTPS配置
- [ ] 防火墙已开放 80、443 端口
- [ ] Docker服务已启动: `docker compose ps` 显示所有服务 Up
- [ ] 网站可通过HTTPS访问，SSL证书有效
- [ ] OAuth应用已创建，回调地址正确

---

## 联系支持

如果遇到问题，可以：
2. 查看Docker日志排查问题
3. 提交Issue到GitHub仓库