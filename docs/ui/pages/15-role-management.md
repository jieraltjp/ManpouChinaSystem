# 页面规格 — 角色管理

> **版本**: 1.1.0
> **创建**: 2026-04-30
> **更新**: 2026-05-12（v1.1.0：权限配置由 el-tree 改为扁平化权限表格（方案B））
> **路由**: `/system/role`
> **组件**: `apps/web/src/pages/system/RolePage.vue`
> **对应后端**: `GET/POST/PUT/DELETE /api/v1/roles` · `GET /api/v1/permissions/tree`
> **对应 DB**: SPEC-B11 V8（role / permission 表）
> **依赖文档**: `SPEC-B11-用户中心与权限体系.md` · `SPEC-B11-IMPLEMENT.md`
> **状态**: ✅ 前端页面完成（api/role.ts + RolePage.vue + Vite proxy + 路由；后端权限控制 Phase 3 待 allinone 实现）

---

## 1. 设计概述

左侧角色列表 + 右侧扁平化权限表格（方案B），左右分栏布局。权限不展示 code。

```
┌─────────────────────────────────────────────────────────────────────────┐
│  页面标题：角色管理                              [新增角色]              │
├──────────────────┬────────────────────────────────────────────────────┤
│  角色列表          │  系统管理员  权限配置                 [保存权限]     │
│  ┌────────────┐  │                                                    │
│  │ 系统管理员  │  │  补货需求   ☑查看  ☑创建  ☑编辑  ☑删除           │
│  │   ADMIN   │  │  发注单     ☑查看  ☑创建  ☑编辑  ☑删除           │
│  ├────────────┤  │  出货管理   ☑查看  ☑创建  ☑编辑  ☑删除           │
│  │ 运营主管    │  │  验货       ☑查看  ☑创建  ☑编辑  ☑删除           │
│  │   MANAGER  │  │  物流调配   ☑查看  ☑创建  ☑编辑  ☑删除           │
│  ├────────────┤  │  集拼       ☑查看  ☑创建  ☑编辑  ☑删除           │
│  │ 普通运营    │  │  货柜       ☑查看  ☑创建  ☑编辑  ☑删除           │
│  │   OPERATOR │  │  国内报关   ☑查看  ☑创建  ☑编辑  ☑删除  ☑审批    │
│  ├────────────┤  │  日本清关   ☑查看  ☑创建  ☐启动  ☐完成  ☑删除  │
│  │ 查看者      │  │  退税       ☑查看  ☑创建  ☑编辑  ☐完成  ☑删除  │
│  │   VIEWER   │  │  销售       ☑查看  ☑创建  ☑编辑  ☑删除           │
│  └────────────┘  │  工厂       ☑查看  ☑创建  ☑编辑  ☑删除           │
│                   │  商品       ☑查看  ☑创建  ☑编辑  ☑删除           │
│  [+ 新增角色]      │  order     ☑查看                                    │
│                   │  用户管理   ☐查看  ☐创建  ☐编辑  ☐删除           │
│                   │            ☐审核注册用户  ☐重置密码              │
│                   │  角色管理   ☐查看  ☐创建  ☐编辑  ☐分配权限       │
│                   │  permission ☐查看                                    │
│                   │  操作日志   ☐查看  ☐导出                             │
│                   │                                                     │
└───────────────────┴────────────────────────────────────────────────────┘

+──────────────────────────────┐
│  新增/编辑 角色               │
│  ────────────────────────── │
│  角色编码: [ADMIN_______]    │
│  （系统内置不可修改）          │
│  ────────────────────────── │
│  角色名称:                    │
│  中文: [系统管理员_______]   │
│  日文: [システム管理者___]   │
│  ────────────────────────── │
│  角色类型: [SYSTEM ▼]        │
│  （SYSTEM=系统内置不可删除）   │
│  ────────────────────────── │
│  描述: [_______________]     │
│  ────────────────────────── │
│              [取消] [保存]   │
└──────────────────────────────┘
```

**方案B 扁平化表格设计说明**：
- 14 个模块 = 14 行（行高 40px，总高 ~560px，固定不撑高页面）
- 每行权限横向排布，超长行用 flex-wrap 换行
- 每行顶部显示模块名（横跨所有列），权限以 el-tag 或 el-checkbox-inline 紧凑排列
- 无层级折叠，无 code 展示，权限名作为标签主文字

---

## 2. 功能说明

| 功能 | 描述 |
|------|------|
| 角色列表 | 左侧边栏，显示角色名称+编码，预置角色可编辑（isEditable=1） |
| 权限表格 | 右侧，扁平化权限表格（方案B），14模块×横向权限标签 |
| 权限保存 | 选中角色后修改权限，点保存 |
| 新增角色 | 填写角色编码（中英文名）+ 描述 |
| 编辑角色 | 修改角色名称和描述（所有角色均可编辑） |
| 删除角色 | 所有角色均可删除（无 isEditable 限制） |
| 查看成员 | 点击角色后显示该角色下的用户数 |

---

## 3. i18n key（新增）

| Key | 中文 | 日语 |
|-----|------|------|
| `role.title` | 角色管理 | 役割管理 |
| `role.newButton` | 新增角色 | 役割を追加 |
| `role.column.roleCode` | 角色编码 | 役割コード |
| `role.column.roleNameCn` | 角色名称 | 役割名 |
| `role.column.roleNameJp` | 役割名 | 役割名（日） |
| `role.column.roleType` | 角色类型 | 役割タイプ |
| `role.column.description` | 描述 | 説明 |
| `role.column.userCount` | 成员数 | メンバー数 |
| `role.column.createTime` | 创建时间 | 作成日時 |
| `role.type.SYSTEM` | 系统内置 | システム組み込み |
| `role.type.BUSINESS` | 业务角色 | ビジネス役割 |
| `role.action.edit` | 编辑 | 編集 |
| `role.action.delete` | 删除 | 削除 |
| `role.action.config` | 配置权限 | 権限を設定 |
| `role.dialog.createTitle` | 新增角色 | 役割を追加 |
| `role.dialog.editTitle` | 编辑角色 | 役割を編集 |
| `role.dialog.roleCode` | 角色编码 | 役割コード |
| `role.dialog.roleNameCn` | 角色名称（中文） | 役割名（中国語） |
| `role.dialog.roleNameJp` | 角色名称（日文） | 役割名（日本語） |
| `role.dialog.roleType` | 角色类型 | 役割タイプ |
| `role.dialog.description` | 描述 | 説明 |
| `role.module.procurement` | 发注管理 | 発注管理 |
| `role.module.logistics` | 物流管理 | 物流管理 |
| `role.module.customs` | 报关管理 | 通関管理 |
| `role.module.finance` | 财务管理 | 财务管理 |
| `role.module.sales` | 销售管理 | 販売管理 |
| `role.module.product` | 商品管理 | 商品管理 |
| `role.module.system` | 系统管理 | システム管理 |
| `role.message.createSuccess` | 角色创建成功 | 役割が作成されました |
| `role.message.updateSuccess` | 角色更新成功 | 役割が更新されました |
| `role.message.deleteSuccess` | 角色删除成功 | 役割が削除されました |
| `role.message.deleteConfirm` | 确认删除角色「{name}」？ | 役割「{name}」を削除しますか？ |
| `role.message.deleteSystemRole` | 系统内置角色不可删除 | システム組み込みの役割は削除できません |
| `role.message.permissionSaved` | 权限配置已保存 | 権限設定が保存されました |

---

*上一页：[14-user-management](./14-user-management.md) | 下一页：[16-audit-log](./16-audit-log.md)*
