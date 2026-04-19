# UI 截图存放目录

> **说明**：手动截图补充，本目录用于存放各页面的实际运行截图。
> 截图命名规范：`{页面编号}-{页面名称}-{日期}.png`

---

## 截图清单

| 文件 | 对应页面 | 状态 |
|------|---------|------|
| `01-login.png` | 登录页 `/login` | 待补充 |
| `02-dashboard.png` | 仪表盘 `/dashboard` | 待补充 |
| `03-examples.png` | 示例管理 `/examples` | 待补充 |
| `04-procurement.png` | 采购单管理 `/test` | 待补充 |

---

## 截图要求

- **分辨率**：1920×1080 或更高
- **格式**：PNG，不压缩
- **命名**：`{编号}-{名称}.png`
- **内容**：各页面全屏截图，包含实际数据填充

## 补充方式

1. 启动前端：`cd apps/web && npm run dev`
2. 启动后端：`mvn spring-boot:run`（user-service + procurement-service）
3. 启动网关：`mvn spring-boot:run`（api-gateway，端口 18080）
4. 访问 http://localhost:13000，逐页面截图
5. 将截图放入本目录，命名为上表中的文件名
