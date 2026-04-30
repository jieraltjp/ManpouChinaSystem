# 页面文档：操作日志

> **页面路径**: `/system/audit-log`
> **组件文件**: `apps/web/src/pages/system/AuditLogPage.vue`
> **路由定义**: `apps/web/src/router/index.ts`
> **权限要求**: `audit:read`
> **依赖**: SPEC-B11-用户中心与权限体系
> **最后更新**: 2026-04-30

---

## 1. 设计概述

标准日志查询页面，支持多条件筛选 + 分页 + 详情查看 + Excel 导出。

```
┌─────────────────────────────────────────────────────────────────────────┐
│  页面标题：操作日志                                    [导出Excel] [刷新] │
├─────────────────────────────────────────────────────────────────────────┤
│  统计卡片（4列）                                                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                   │
│  │ 今日操作  │ │ 用户操作  │ │ 数据变更  │ │ 系统操作  │                   │
│  │   156   │ │    89   │ │    52   │ │    15   │                   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘                   │
├─────────────────────────────────────────────────────────────────────────┤
│  高级筛选                                                             │
│  [操作时间 ▼ 2026-04-01 ~ 2026-04-30]                                 │
│  [操作人 __] [模块 ▼] [动作 ▼] [资源类型 ▼] [资源ID __]               │
│  [IP地阣 __]                                            [查询] [重置]   │
├─────────────────────────────────────────────────────────────────────────┤
│  表格                                                                  │
│  ┌────┬──────┬────┬──────┬────────┬──────┬──────┬─────┬────────────┐ │
│  │时间│ 操作人│账号│ 模块  │ 动作  │资源类型│资源ID│ IP  │  详情     │ │
│  ├────┼──────┼────┼──────┼────────┼──────┼──────┼─────┼────────────┤ │
│  │09:01│ 张总  │admin│ user │ CREATE │  User │ U-002 │192.x│详情 │ │
│  │09:00│ 李明  │li.n│demand│ UPDATE │ Demand│ D-001 │10.x │详情 │ │
│  │08:55│ admin │admin│ auth │ LOGIN  │  —    │  —    │192.x│详情 │ │
│  │08:30│ 赵丽  │zhang│customs│STATUS │Customs│C-001 │10.x │详情 │ │
│  └────┴──────┴────┴──────┴────────┴──────┴──────┴─────┴────────────┘ │
│  [分页: << < 1 2 3 ... 50 > >>  共1000条]                              │
└─────────────────────────────────────────────────────────────────────────┘

+──────────────────────────────┐
│  操作详情                     │
│  ────────────────────────── │
│  基本信息                     │
│  时间: 2026-04-30 09:01:23  │
│  操作人: 张总 (admin)         │
│  公司: 漫普中国               │
│  部门: 信息中心               │
│  IP: 192.168.13.201         │
│  ────────────────────────── │
│  操作信息                     │
│  模块: user                  │
│  动作: CREATE                │
│  HTTP: POST /api/v1/users   │
│  ────────────────────────── │
│  资源信息                     │
│  类型: User                  │
│  ID: 2                       │
│  编码: U-0002                │
│  ────────────────────────── │
│  变更详情                     │
│  {                           │
│    "newData": {              │
│      "userCode": "U-0002",   │
│      "username": "li.nam",   │
│      "nameCn": "李明",       │
│      "email": "li@manpou",  │
│      "status": 1             │
│    }                         │
│  }                           │
│  ────────────────────────── │
│  链路追踪                     │
│  TraceId: abc123...          │
│  RequestId: req-456...       │
└──────────────────────────────┘
```

---

## 2. 功能说明

| 功能 | 描述 |
|------|------|
| 统计卡片 | 今日操作总数 / 用户操作 / 数据变更 / 系统操作（异步加载） |
| 时间范围 | 默认最近7天，支持快捷选项（今日/本周/本月/自定义） |
| 高级筛选 | 操作人 + 模块 + 动作 + 资源类型 + 资源ID + IP地址 |
| 表格展示 | 时间降序，多列显示关键信息 |
| 详情查看 | 抽屉展示完整操作详情，含变更前后值（JSON格式化） |
| 导出 Excel | 导出当前筛选条件下的日志，支持自定义列 |
| 刷新 | 手动刷新当前列表 |

---

## 3. 表格列定义

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 操作时间 | createTime | 160px | yyyy-MM-dd HH:mm:ss |
| 操作人 | userName | 100px | 中文姓名 |
| 账号 | username | 120px | 登录账号 |
| 模块 | module | 100px | 模块标识 |
| 动作 | action | 100px | CREATE/UPDATE/DELETE/LOGIN 等 |
| 资源类型 | resourceType | 120px | User/Demand/Procurement 等 |
| 资源ID | resourceId | 100px | 资源主键或编码 |
| IP | ipAddress | 130px | 客户端 IP |
| 详情 | — | 80px | 文字按钮 |

---

## 4. i18n key（新增）

| Key | 中文 | 日语 |
|-----|------|------|
| `audit.title` | 操作日志 | 操作ログ |
| `audit.refreshButton` | 刷新 | 更新 |
| `audit.exportButton` | 导出Excel | Excel出力 |
| `audit.stat.today` | 今日操作 | 本日の操作 |
| `audit.stat.userActions` | 用户操作 | ユーザー操作 |
| `audit.stat.dataChanges` | 数据变更 | データ変更 |
| `audit.stat.systemActions` | 系统操作 | システム操作 |
| `audit.filter.timeRange` | 操作时间 | 操作時間 |
| `audit.filter.user` | 操作人 | 操作者 |
| `audit.filter.module` | 模块 | モジュール |
| `audit.filter.action` | 动作 | アクション |
| `audit.filter.resourceType` | 资源类型 | リソースタイプ |
| `audit.filter.resourceId` | 资源ID | リソースID |
| `audit.filter.ip` | IP地址 | IPアドレス |
| `audit.column.createTime` | 操作时间 | 操作時間 |
| `audit.column.userName` | 操作人 | 操作者 |
| `audit.column.username` | 账号 | アカウント |
| `audit.column.module` | 模块 | モジュール |
| `audit.column.action` | 动作 | アクション |
| `audit.column.resourceType` | 资源类型 | リソースタイプ |
| `audit.column.resourceId` | 资源ID | リソースID |
| `audit.column.resourceCode` | 资源编码 | リソースコード |
| `audit.column.ipAddress` | IP地址 | IPアドレス |
| `audit.column.httpMethod` | HTTP方法 | HTTPメソッド |
| `audit.column.httpUrl` | 请求路径 | リクエストパス |
| `audit.column.detail` | 详情 | 詳細 |
| `audit.action.detail` | 详情 | 詳細 |
| `audit.action.CREATE` | 创建 | 作成 |
| `audit.action.UPDATE` | 编辑 | 編集 |
| `audit.action.DELETE` | 删除 | 削除 |
| `audit.action.STATUS_CHANGE` | 状态变更 | ステータス変更 |
| `audit.action.LOGIN` | 登录 | ログイン |
| `audit.action.LOGOUT` | 登出 | ログアウト |
| `audit.action.EXPORT` | 导出 | エクスポート |
| `audit.module.user` | 用户管理 | ユーザー管理 |
| `audit.module.role` | 角色管理 | 役割管理 |
| `audit.module.demand` | 补货需求 | 補充需要 |
| `audit.module.procurement` | 发注单 | 発注書 |
| `audit.module.customs` | 国内报关 | 国内通関 |
| `audit.module.japan_customs` | 日本清关 | 日本通関 |
| `audit.module.auth` | 认证 | 認証 |

---

*上一页：[15-role-management](./15-role-management.md) | 下一页：[17-profile](./17-profile.md)*
