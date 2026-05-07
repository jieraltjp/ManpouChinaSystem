# 页面规格 — 用户管理

> **版本**: 1.0.0
> **创建**: 2026-04-30
> **更新**: 2026-05-01（v1.0.0：Phase 3 前端开发）
> **路由**: `/system/user`
> **组件**: `apps/web/src/pages/system/UserPage.vue`
> **对应后端**: `GET/PUT/POST/DELETE /api/v1/users`
> **对应 DB**: SPEC-B11 V7（user 表）
> **依赖文档**: `SPEC-B11-用户中心与权限体系.md` · `SPEC-B11-IMPLEMENT.md`
> **状态**: ✅ Phase 3 完成（UserPage.vue + api/user.ts + Vite proxy + 路由均已实现）

---

## 1. 设计概述

采用 **标准列表+弹窗编辑** 布局，与现有 DemandPage.vue / FactoryPage.vue 保持一致。

```
┌─────────────────────────────────────────────────────────────────────────┐
│  页面标题：用户管理                    [新增用户]  [导出Excel]           │
├─────────────────────────────────────────────────────────────────────────┤
│  统计卡片（4列）                                                      │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                         │
│  │ 总用户数 │ │ 正常   │ │  禁用  │ │ 今日登录 │                         │
│  │   48   │ │  45   │ │   3   │ │   12   │                         │
│  └────────┘ └────────┘ └────────┘ └────────┘                         │
├─────────────────────────────────────────────────────────────────────────┤
│  筛选栏                                                                │
│  [姓名/账号/邮箱 __] [公司 ▼] [部门 ▼] [角色 ▼] [状态 ▼]  [查询][重置]  │
├─────────────────────────────────────────────────────────────────────────┤
│  表格                                                                  │
│  ┌────┬──────┬────┬─────┬────────┬────────┬──────┬──────┬─────────┐  │
│  │编码│ 账号  │姓名│ 邮箱 │ 部门  │ 职务  │角色  │状态  │  操作  │  │
│  ├────┼──────┼────┼─────┼────────┼────────┼──────┼──────┼─────────┤  │
│  │U-1 │admin │张总 │a@t.c │信息中心│主管,报关│ADMIN │ 正常 │详情 编辑│  │
│  │U-2 │li.nam│李明 │l@m.c │采购部 │专员   │OPER..│ 正常 │禁用 编辑│  │
│  │U-3 │zhang.│赵丽 │z@m.c │销售部 │经理   │MGR.. │ 禁用 │启用 编辑│  │
│  └────┴──────┴────┴─────┴────────┴────────┴──────┴──────┴─────────┘  │
│  [分页: << < 1 2 3 ... 10 > >>  共200条]                               │
└─────────────────────────────────────────────────────────────────────────┘

+──────────────────────────────┐
│  用户详情抽屉                 │
│  ┌────────────────────────┐  │
│  │ 头像  张总             │  │
│  │       系统管理员        │  │
│  │       admin@manpou.cn  │  │
│  └────────────────────────┘  │
│  ────────────────────────── │
│  基本信息                    │
│  用户编码: U-0001           │
│  登录账号: admin            │
│  中文姓名: 张总              │
│  日文姓名: —                │
│  邮箱: admin@manpou.cn     │
│  手机号: 13800138000        │
│  头像: [点击上传]           │
│  ────────────────────────── │
│  组织信息                    │
│  所属公司: 漫普中国         │
│  所属部门: 信息中心         │
│  职务: [主管] [报关员]      │
│  ────────────────────────── │
│  权限信息                    │
│  角色: [系统管理员]          │
│  报关备案号: CBP-001        │
│  报关员证: LIC-001         │
│  ────────────────────────── │
│  账户状态                    │
│  状态: [正常]               │
│  最后登录: 2026-04-30 09:00 │
│  最后IP: 192.168.13.201    │
└──────────────────────────────┘

+──────────────────────────────┐
│  新增/编辑 用户              │
│  ────────────────────────── │
│  基本信息                    │
│  用户编码: [U-自动生成___]   │
│  登录账号: [___________]    │
│  中文姓名: [___________]    │
│  日文姓名: [___________]    │
│  邮箱:     [___________]    │
│  手机号:   [___________]    │
│  ────────────────────────── │
│  组织信息                    │
│  所属公司: [漫普中国     ▼]  │
│  所属部门: [信息中心     ▼]  │
│  职务:     ☐ 主管 ☐ 报关员  │
│  ────────────────────────── │
│  海关资质                    │
│  报关备案号: [___________]  │
│  报关员证号: [___________]  │
│  ────────────────────────── │
│  角色: [系统管理员 ▼]       │
│  初始密码: [自动生成    ] 🔄 │
│  ────────────────────────── │
│              [取消] [保存]   │
└──────────────────────────────┘
```

---

## 2. 功能说明

| 功能 | 描述 |
|------|------|
| 统计卡片 | 总用户数 / 正常 / 禁用 / 今日登录（异步加载） |
| 筛选查询 | 姓名/账号/邮箱关键字 + 公司 + 部门 + 角色 + 状态 |
| 新增用户 | 填写基本信息+组织信息+角色分配，支持自动生成初始密码 |
| 详情抽屉 | 左侧头像+基本信息，右侧组织信息+角色+海关资质+账户状态 |
| 编辑用户 | 编辑基本信息/组织信息/职务/海关资质，**不可修改角色** |
| 分配角色 | 行内/详情页弹窗，支持多选角色 |
| 分配职务 | 支持多选职务（el-select multiple） |
| 重置密码 | 生成随机 16 位密码，弹窗显示（仅一次可见） |
| 禁用/启用 | 行内 switch 切换，禁用后不可登录 |
| 批量启用/禁用 | 列表页批量选择后操作 |
| 导出 Excel | 导出当前筛选条件下的用户列表 |

---

## 3. 表格列定义

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 用户编码 | userCode | 100px | U-XXXX 格式 |
| 登录账号 | username | 120px | 唯一 |
| 中文姓名 | nameCn | 100px | — |
| 日文姓名 | nameJp | 100px | 可为空 |
| 邮箱 | email | 180px | 唯一 |
| 手机号 | phone | 120px | 可为空 |
| 所属部门 | departmentName | 120px | 冗余字段 |
| 职务 | positionNames | 150px | 逗号分隔，最多2个+溢出 |
| 角色 | roles | 120px | tag 显示，多个取第一个+溢出 |
| 状态 | status | 80px | 正常=绿色 禁用=灰色 |
| 操作 | — | 160px | 详情/编辑/禁用/重置密码 |

---

## 4. i18n key（新增）

| Key | 中文 | 日语 |
|-----|------|------|
| `user.title` | 用户管理 | ユーザー管理 |
| `user.newButton` | 新增用户 | ユーザーを追加 |
| `user.exportButton` | 导出Excel | Excel出力 |
| `user.stat.total` | 用户总数 | ユーザー総数 |
| `user.stat.normal` | 正常 | 正常 |
| `user.stat.disabled` | 禁用 | 無効 |
| `user.stat.todayLogin` | 今日登录 | 本日ログイン |
| `user.filter.keyword` | 姓名/账号/邮箱 | 名前/アカウント/メール |
| `user.filter.company` | 所属公司 | 所属会社 |
| `user.filter.department` | 所属部门 | 所属部門 |
| `user.filter.role` | 角色 | 役割 |
| `user.filter.status` | 状态 | ステータス |
| `user.column.userCode` | 用户编码 | ユーザーコード |
| `user.column.username` | 登录账号 | ログインアカウント |
| `user.column.nameCn` | 中文姓名 | 中国語名 |
| `user.column.nameJp` | 日文姓名 | 日本語名 |
| `user.column.email` | 邮箱 | メール |
| `user.column.phone` | 手机号 | 電話番号 |
| `user.column.company` | 所属公司 | 所属会社 |
| `user.column.department` | 所属部门 | 所属部門 |
| `user.column.positions` | 职务 | 役職 |
| `user.column.roles` | 角色 | 役割 |
| `user.column.status` | 状态 | ステータス |
| `user.column.lastLoginTime` | 最后登录 | 最終ログイン |
| `user.column.lastLoginIp` | 登录IP | ログインIP |
| `user.column.createTime` | 创建时间 | 作成日時 |
| `user.column.action` | 操作 | 操作 |
| `user.status.normal` | 正常 | 正常 |
| `user.status.disabled` | 禁用 | 無効 |
| `user.action.detail` | 详情 | 詳細 |
| `user.action.edit` | 编辑 | 編集 |
| `user.action.delete` | 删除 | 削除 |
| `user.action.disable` | 禁用 | 無効 |
| `user.action.enable` | 启用 | 有効化 |
| `user.action.resetPassword` | 重置密码 | パスワードリセット |
| `user.action.assignRole` | 分配角色 | 役割を割り当て |
| `user.dialog.createTitle` | 新增用户 | ユーザーの追加 |
| `user.dialog.editTitle` | 编辑用户 | ユーザーの編集 |
| `user.dialog.detailTitle` | 用户详情 | ユーザー詳細 |
| `user.dialog.roleTitle` | 分配角色 | 役割を割り当て |
| `user.dialog.passwordTitle` | 密码重置 | パスワードリセット |
| `user.dialog.userCode` | 用户编码 | ユーザーコード |
| `user.dialog.username` | 登录账号 | ログインアカウント |
| `user.dialog.nameCn` | 中文姓名 | 中国語名 |
| `user.dialog.nameJp` | 日文姓名 | 日本語名 |
| `user.dialog.email` | 邮箱 | メール |
| `user.dialog.phone` | 手机号 | 電話番号 |
| `user.dialog.avatar` | 头像 | アバター |
| `user.dialog.company` | 所属公司 | 所属会社 |
| `user.dialog.department` | 所属部門 | 所属部門 |
| `user.dialog.positions` | 职务 | 役職 |
| `user.dialog.role` | 角色 | 役割 |
| `user.dialog.password` | 初始密码 | 初期パスワード |
| `user.dialog.newPassword` | 新密码 | 新しいパスワード |
| `user.dialog.confirmPassword` | 确认密码 | パスワード確認 |
| `user.dialog.oldPassword` | 旧密码 | 以前のパスワード |
| `user.dialog.cancel` | 取消 | キャンセル |
| `user.dialog.save` | 保存 | 保存 |
| `user.validation.usernameRequired` | 登录账号不能为空 | ログインアカウントは必須です |
| `user.validation.emailRequired` | 邮箱不能为空 | メールは必須です |
| `user.validation.emailFormat` | 邮箱格式不正确 | メールの形式が正しくありません |
| `user.validation.passwordRequired` | 密码不能为空 | パスワードは必須です |
| `user.validation.passwordMismatch` | 两次密码不一致 | パスワードが一致しません |
| `user.validation.companyRequired` | 请选择公司 | 会社を選択してください |
| `user.validation.departmentRequired` | 请选择部门 | 部門を選択してください |
| `user.message.createSuccess` | 用户创建成功 | ユーザーが作成されました |
| `user.message.updateSuccess` | 用户更新成功 | ユーザーが更新されました |
| `user.message.deleteSuccess` | 用户删除成功 | ユーザーが削除されました |
| `user.message.resetPasswordSuccess` | 密码已重置，请妥善保管 | パスワードがリセットされました。大切に保管してください |
| `user.message.deleteConfirm` | 确认删除用户「{username}」？ | ユーザー「{username}」を削除しますか？ |
| `user.message.deleteConfirmTitle` | 删除确认 | 削除の確認 |

---

## 5. 组件结构

```
pages/system/UserPage.vue
├── 统计卡片 el-row
├── 筛选表单 el-form (inline)
├── 表格 el-table
│   ├── 头像列（圆形缩略图）
│   ├── 职务列（多 tag）
│   ├── 角色列（tag）
│   ├── 状态列（switch）
│   └── 操作列（文字按钮）
├── 分页 el-pagination
├── 详情抽屉 UserDetailDrawer.vue
├── 编辑弹窗 UserEditDialog.vue
├── 角色分配弹窗 UserRoleDialog.vue
└── 密码重置弹窗 UserResetPasswordDialog.vue
```

---

## 6. 相关文件

| 文件 | 作用 |
|------|------|
| `apps/web/src/pages/system/UserPage.vue` | 用户管理主页面 |
| `apps/web/src/pages/system/components/UserDetailDrawer.vue` | 详情抽屉 |
| `apps/web/src/pages/system/components/UserEditDialog.vue` | 新增/编辑弹窗 |
| `apps/web/src/pages/system/components/UserRoleDialog.vue` | 角色分配弹窗 |
| `apps/web/src/api/user.ts` | 用户管理 API 客户端（新增） |
| `apps/web/src/types/user.ts` | UserClaims 增强 |
| `apps/web/src/router/index.ts` | 路由定义（新增 `/system/user`） |

---

*上一页：[13-procurement-overview](./13-procurement-overview.md) | 下一页：[15-role-management](./15-role-management.md)*
