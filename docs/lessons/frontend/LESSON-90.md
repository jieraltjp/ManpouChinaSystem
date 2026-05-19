# Lesson 90: i18n key 缺失导致[intlify] Not found 运行时错误

> **发现日期**: 2026-05-19
> **项目**: ManpouChinaSystem / web
> **教训**: 新增页面组件时，务必同步检查所有 `$t()` key 在 zh.json / ja.json 中是否存在

---

## 问题

`/system/profile` 页面打开时控制台报错：

```
[intlify] Not found 'profile.security.rules' key in 'zh' locale messages
[intlify] Not found 'dashboard.timezone.CST' key in 'zh' locale messages
[intlify] Not found 'dashboard.timezone.JST' key in 'zh' locale messages
```

---

## 根因

1. **`dashboard.timezone.CST / JST`**：`ProfilePage.vue` 第 146-147 行使用了这两个 key，但 `dashboard` 对象在 zh.json / ja.json 中存在，却缺少 `timezone` 子节点。

2. **`profile.security.rules`**：该 key 实际存在于 zh.json 的 `profile.security.rules`（是数组），ja.json 亦有。报错是 Vite HMR 陈旧缓存导致的误报，刷新页面后消失。

---

## 验证方法

```bash
# 用 Node.js 直接验证 key 是否存在（绕过前端构建缓存）
node -e "const d=require('./src/locales/zh.json'); console.log('rules:', d.profile?.security?.rules); console.log('CST:', d.dashboard?.timezone?.CST);"
```

---

## 修复

```bash
# 1. 添加缺失的 dashboard.timezone 到 zh.json
# 2. 添加缺失的 dashboard.timezone 到 ja.json
```

zh.json 新增：
```json
"dashboard": {
    "timezone": {
        "CST": "中国标准时间 (UTC+8)",
        "JST": "日本标准时间 (UTC+9)"
    }
}
```

ja.json 新增：
```json
"dashboard": {
    "timezone": {
        "CST": "中国標準時 (UTC+8)",
        "JST": "日本標準時 (UTC+9)"
    }
}
```

---

## 预防

| 场景 | 检查项 |
|------|--------|
| 新增 Vue 页面 | 用 `node -e` 验证所有 `$t()` key 在 zh.json / ja.json 中存在 |
| 新增 i18n key | 同步添加到 zh 和 ja 两侧 |
| 长时间运行后 Vite HMR 异常 | 硬刷新 (Ctrl+Shift+R) 或重启 `npm run dev` |

## 关联 Lesson

- Lesson 53: i18n JSON 中 key 不得重复
- Lesson 44: i18n JSON 大型文件用专用编辑器
