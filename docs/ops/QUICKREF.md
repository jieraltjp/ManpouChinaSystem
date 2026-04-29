# 速查卡

## 拉取 + 启动（一行搞定）

```bash
git pull origin main && ./scripts/start-all.sh
```

---

## 启停

| 操作 | 命令 |
|---|---|
| 启动全部 | `./scripts/start-all.sh` |
| 停止全部 | `./scripts/stop-all.sh` |
| 重启全部 | `./scripts/restart-all.sh` |
| 查看状态 | `./scripts/start-all.sh status` |
| 只启后端 | `./scripts/start-all.sh manpou` |
| 只启前端 | `./scripts/start-all.sh web` |

---

## 端口

| 服务 | 端口 |
|---|---|
| manpou-allinone | 18090 |
| user-service | 18081 |
| web 前端 | 13000 |
| Nacos | 8848 |

---

## 日志

```bash
# 后端日志
tail -f logs/manpou-allinone.log

# 前端日志
tail -f logs/web.log
```

---

## Docker

```bash
# 查看运行中的容器
docker ps

# 重启基础设施容器（Nacos / MySQL / Redis / Kafka，不删数据）
docker compose -f docker/compose.yaml restart

# 完全重建（不删除命名卷，需清数据时加 -v）
docker compose -f docker/compose.yaml up -d --build
```

---

## 编译

```bash
./scripts/build-all.sh
```

---

## 首次配置（新机器）

### Windows

```bash
./scripts/init-config.sh dev
```

### Ubuntu（已通过 SSH 安装依赖后）

```bash
cd /opt/ManpouChinaSystem
./scripts/init-config.sh dev
./scripts/start-all.sh
```
