# SPEC-B01 — 补货需求业务规格（步骤1）

> **版本**: 1.1.0
> **更新**: 2026-04-22（v1.1.0：新增商品自动补全 API 与 DemandPage 前端改造）
> **创建**: 2026-04-22
> **状态**: ✅ 已实现
> **业务步号**: 01（补货需求/新品采购）
> **对应 UI 文档**: `docs/ui/pages/01-demand.md`
> **对应数据库文档**: `docs/database/DB-01-procurement-demand.md`

---

## 1. 业务背景

补货需求是整个采购链路的入口。非新品走补货流程，新品走新品采购流程。录入后转为发注单（Procurement），进入步骤2。

**两种意图必须分离**：
- `REPLENISHMENT`（补货）：基于历史销售数据补货，通常有子货号
- `NEW_PURCHASE`（新品采购）：全新商品立项，无子货号

---

## 2. 聚合根

### ReplenishmentDemand（补货需求单）

```
ReplenishmentDemand（聚合根）
├── id: Long
├── demandCode: String           # D-YYYYMMDD-NNN
├── demandType: DemandType        # REPLENISHMENT(补货) / NEW_PURCHASE(新品采购)
├── productCode: String           # 主货号
├── subProductCodes: List<String> # 子货号列表（JSON 存储；新品可为空）
├── quantity: Integer           # 需求量
├── destination: String         # 目的地
├── japanLead: String           # 日本担当
├── status: DemandStatus        # PENDING → CONVERTED → CANCELLED
├── linkedProcurementId: Long    # 关联的采购单ID（CONVERTED时赋值）
└── 领域方法
    ├── convertToProcurement()   # 转为发注单，生成 Procurement
    └── cancel()                 # 取消需求
```

---

## 3. 枚举

```java
public enum DemandType {
    REPLENISHMENT,   // 非新品 — 补货
    NEW_PURCHASE      // 新品 — 采购
}

public enum DemandStatus {
    PENDING,     // 待确认（录入后默认）
    CONVERTED,  // 已转采购（生成 Procurement 后推进至此）
    CANCELLED   // 已取消
}
```

---

## 4. 状态流转

```
  PENDING ──[转采购]──▶ CONVERTED
     │
     └──[取消]──▶ CANCELLED
```

---

## 5. API 设计

### 5.1 补货需求 API

```
GET    /api/v1/demands?page=&pageSize=&demandType=&productCode=&status=
GET    /api/v1/demands/{id}
POST   /api/v1/demands
PATCH  /api/v1/demands/{id}
POST   /api/v1/demands/{id}/convert    # 转采购
POST   /api/v1/demands/{id}/revert     # 撤销转换
DELETE /api/v1/demands/{id}
```

### 5.2 商品自动补全 API（补货页主/子货号选择）

```
GET    /api/v1/products/suggest/master-codes?keyword={keyword}   # 主货号下拉补全
GET    /api/v1/products/suggest/sub-codes?masterCode={code}    # 子货号候选项
```

> 对应设计文档：`docs/design/FEATURE-货号自动补全与多子货号选择.md`

---

## 6. 代码实现状态

- [x] ✅ `ReplenishmentDemand` 聚合根实体
- [x] ✅ `DemandType` 枚举
- [x] ✅ `DemandStatus` 枚举
- [x] ✅ `ReplenishmentDemandRepository` 领域接口
- [x] ✅ `ReplenishmentDemandUseCase` 用例服务（含 convert + revertConversion）
- [x] ✅ `ReplenishmentDemandController` REST 控制器
- [x] ✅ `@/api/demand.ts` 前端 API 客户端
- [x] ✅ `DemandPage.vue` 页面骨架
- [x] ✅ 主货号自动补全（el-select remote + 300ms 防抖）
- [x] ✅ 子货号多选（el-select multiple + filterable + allow-create）
- [x] ✅ `ProductController` suggest 端点（/suggest/master-codes + /suggest/sub-codes）
