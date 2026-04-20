# Ubuntu 本地开发 / 轻量部署文档

> **Phase 0 目标**：manpou-allinone + web + user-service 三服务跑通
> **原则**：最简路径，不依赖 Docker，一步一步来
> **最后更新**：2026-04-20

---

## 1. 架构概览

```
┌──────────────────────────────────────────────────────┐
│                     Ubuntu Server                      │
│                                                       │
│  ┌──────────────┐  ┌───────────────┐  ┌──────────┐ │
│  │ user-service │  │manpou-allinone│  │   web    │ │
│  │   (18081)    │  │    (18090)     │  │  (13000) │ │
│  │  JWT 认证    │  │  6 领域业务    │  │  Vue 3   │ │
│  └──────────────┘  └───────────────┘  └─────┬────┘ │
│                                              │       │
│                    ┌──────────────────────────┘       │
│                    │                                  │
│               Nginx (80/443)                         │
│               反向代理到各服务                        │
└──────────────────────────────────────────────────────┘
```

**Phase 0 服务清单**：

| 服务 | 端口 | 作用 |
|------|------|------|
| manpou-allinone | 18090 | 核心：product/warehouse/customs/logistics/finance/notification |
| user-service | 18081 | 认证：JWT 签发、用户管理（保留独立） |
| web | 13000 | 前端：Vue 3 + Vite（npm run dev） |

---

## 2. 前置条件

### 2.1 Ubuntu 版本

推荐 **Ubuntu 22.04 LTS (Jammy)** 或更高。

```bash
# 检查版本
cat /etc/os-release
```

### 2.2 安装 Java 21

```bash
# 更新包索引
sudo apt update

# 安装 OpenJDK 21
sudo apt install -y openjdk-21-jdk

# 验证
java -version
# openjdk version "21.x.x" ...

# 设置 JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### 2.3 安装 Maven

```bash
# 安装 Maven
sudo apt install -y maven

# 验证
mvn -version
# Maven 3.x.x ...
```

### 2.4 安装 Node.js 20+

```bash
# 使用 NodeSource 安装 Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# 验证
node -v    # v20.x.x
npm -v     # 10.x.x
```

### 2.5 安装 Git

```bash
sudo apt install -y git
git --version
```

---

## 3. 获取代码

```bash
# 在家目录或 /opt 下克隆
cd ~
git clone https://github.com/<your-org>/ManpouChinaSystem.git
cd ManpouChinaSystem

# 或如果已经克隆过
cd ManpouChinaSystem
git pull origin main
```

---

## 4. 构建后端（manpou-allinone）

### 4.1 编译打包

```bash
cd ~/ManpouChinaSystem/apps/manpou-allinone

# 编译（跳过测试）
mvn clean package -DskipTests

# 产物
# apps/manpou-allinone/target/manpou-allinone-1.0.0-SNAPSHOT.jar
```

### 4.2 构建 user-service（可选，Phase 0 暂时不用）

```bash
cd ~/ManpouChinaSystem/apps/user-service
mvn clean package -DskipTests
```

### 4.3 创建运行目录

```bash
sudo mkdir -p /opt/manpou
sudo chown $USER:$USER /opt/manpou

# 复制 jar
cp ~/ManpouChinaSystem/apps/manpou-allinone/target/manpou-allinone-*.jar \
   /opt/manpou/manpou-allinone.jar

# 复制密钥目录（如有）
cp -r ~/ManpouChinaSystem/apps/manpou-allinone/src/main/resources/keys \
   /opt/manpou/
```

---

## 5. 配置环境变量

```bash
# 创建环境配置
cat > /opt/manpou/allinone.env << 'EOF'
# Spring Boot
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=18090

# 数据库（Phase 0 使用 H2 内存，无需配置）
# SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/manpou
# SPRING_DATASOURCE_USERNAME=root
# SPRING_DATASOURCE_PASSWORD=your_password

# JWT 密钥目录（容器内路径）
JWT_KEY_DIRECTORY=/opt/manpou/keys

# 日志
LOGGING_LEVEL_ROOT=INFO
EOF

cat /opt/manpou/allinone.env
```

---

## 6. 配置 Systemd 服务（推荐）

### 6.1 manpou-allinone 服务

```bash
sudo cat > /etc/systemd/system/manpou-allinone.service << 'EOF'
[Unit]
Description=Manpou All-in-One Service (6 domains)
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/manpou
EnvironmentFile=/opt/manpou/allinone.env
ExecStart=/usr/lib/jvm/java-21-openjdk-amd64/bin/java \
    -Xms512m -Xmx1024m \
    -jar /opt/manpou/manpou-allinone.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 重载 systemd
sudo systemctl daemon-reload

# 启动
sudo systemctl start manpou-allinone

# 查看状态
sudo systemctl status manpou-allinone

# 开机自启
sudo systemctl enable manpou-allinone
```

### 6.2 user-service 服务（可选）

```bash
sudo cat > /etc/systemd/system/user-service.service << 'EOF'
[Unit]
Description=Manpou User Service (Auth)
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/manpou
ExecStart=/usr/lib/jvm/java-21-openjdk-amd64/bin/java \
    -Xms256m -Xmx512m \
    -jar /opt/manpou/user-service.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl start user-service
sudo systemctl enable user-service
```

### 6.3 验证后端启动

```bash
# 检查 manpou-allinone 健康
curl http://localhost:18090/api/v1/auth/public-key

# 预期返回 JSON 含 kid 和公钥 PEM
```

---

## 7. 部署前端（web）

### 7.1 安装依赖

```bash
cd ~/ManpouChinaSystem/apps/web
npm install
```

### 7.2 开发模式运行

```bash
# 开发模式（热重载，端口 13000）
npm run dev

# 前端访问 http://localhost:13000
# API 请求通过 vite proxy 到 localhost:18090
```

### 7.3 生产构建

```bash
# 构建静态文件
npm run build

# 产物目录
# apps/web/dist/
```

### 7.4 用 Nginx 托管静态文件

```bash
# 安装 Nginx
sudo apt install -y nginx

# 配置 Nginx
sudo cat > /etc/nginx/sites-available/manpou-web << 'EOF'
server {
    listen 80;
    server_name _;

    # 前端静态文件
    root /opt/manpou/web-dist;
    index index.html;

    # 前端路由（Vue History 模式）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理到 manpou-allinone
    location /api/ {
        proxy_pass http://127.0.0.1:18090;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

# 启用站点
sudo ln -sf /etc/nginx/sites-available/manpou-web \
         /etc/nginx/sites-enabled/manpou-web

# 测试配置
sudo nginx -t

# 重新加载
sudo systemctl reload nginx

# 复制前端产物
cp -r ~/ManpouChinaSystem/apps/web/dist /opt/manpou/web-dist
```

---

## 8. 快速验证

```bash
# 1. 后端健康检查
curl http://localhost:18090/api/v1/auth/public-key | head -c 200

# 2. 登录获取 Token
curl -X POST http://localhost:18090/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}' | head -c 300

# 3. 带 Token 访问各领域
TOKEN=$(curl -s -X POST http://localhost:18090/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:18090/api/v1/products

# 4. 前端
curl http://localhost | head -5
```

---

## 9. 日常操作命令

```bash
# ===== 后端 =====
sudo systemctl start   manpou-allinone   # 启动
sudo systemctl stop    manpou-allinone   # 停止
sudo systemctl restart  manpou-allinone   # 重启
sudo systemctl status   manpou-allinone   # 状态
journalctl -u manpou-allinone -f          # 实时日志

# ===== 前端（开发模式）=====
cd ~/ManpouChinaSystem/apps/web
npm run dev

# ===== 重新部署 =====
# 1. 拉取最新代码
cd ~/ManpouChinaSystem && git pull

# 2. 重新编译
cd ~/ManpouChinaSystem/apps/manpou-allinone
mvn clean package -DskipTests

# 3. 替换 jar
cp ~/ManpouChinaSystem/apps/manpou-allinone/target/manpou-allinone-*.jar \
   /opt/manpou/manpou-allinone.jar

# 4. 重启服务
sudo systemctl restart manpou-allinone

# 5. 前端重新构建（如有变更）
cd ~/ManpouChinaSystem/apps/web
npm install && npm run build
cp -r dist /opt/manpou/web-dist
```

---

## 10. 防火墙

```bash
# 开放端口（仅在开发环境，生产用 Nginx + HTTPS）
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 18090/tcp # manpou-allinone（仅开发用，生产走 Nginx）
sudo ufw status
```

---

## 11. 目录结构（部署后）

```
/opt/manpou/
├── manpou-allinone.jar    # Spring Boot jar
├── allinone.env           # 环境变量
├── keys/                  # JWT 密钥（RSA PEM，不提交）
└── web-dist/             # Vue 构建产物（Nginx 托管）
    ├── index.html
    ├── assets/
    └── ...

/etc/systemd/system/
├── manpou-allinone.service
└── user-service.service

~/ManpouChinaSystem/       # 源代码（git 管理）
```

---

## 12. Phase 0 → Phase 1 演进

```
Phase 0（当前）
  manpou-allinone:18090  ← auth/login, 各领域 CRUD
  user-service:18081      ← JWT 签发（保留）
  web:13000 (dev) / Nginx (prod)
  数据库: H2 内存

Phase 1
  + api-gateway:18080     ← 统一入口，路由到 manpou-allinone
  + MySQL (Docker)        ← H2 → MySQL
  web vite proxy: 18080

Phase 2+
  Kafka, Redis, Nacos, CI/CD...
```

---

## 13. 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| `curl` 返回 403 | Spring Security 拦截 | 需带 JWT Token |
| 端口被占用 | 进程未关闭 | `sudo lsof -i :18090` → `kill -9 <PID>` |
| H2 数据库重启丢失数据 | H2 内存模式默认行为 | Phase 1 接入 MySQL |
| 前端 404 | Nginx try_files 配置错误 | 检查 `location /` 块 |
| `java: command not found` | JAVA_HOME 未设置 | 检查 systemd Environment 或 .bashrc |
