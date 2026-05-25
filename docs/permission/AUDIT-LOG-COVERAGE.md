# 审计日志覆盖审计报告

> **日期**: 2026-05-25
> **范围**: allinone + user-service 全部 Controller
> **结论**: 3 个写操作 Controller 补充 @AuditLog ✅；4 个只读/测试端点无需审计 ✅

---

## 1. 审计覆盖矩阵

### allinone 写操作 Controller（按前端页面映射）

| 页面 | Controller | @AuditLog | 状态 |
|------|-----------|-----------|------|
| 发注单 | ProcurementController | ✅ CREATE/UPDATE/DELETE | |
| 出货批次 | ShipmentBatchController | ✅ CREATE/UPDATE/DELETE × 2 | |
| 验货记录 | QcRecordController | ✅ CREATE/UPDATE/DELETE | |
| 验货图片 | QcImageController | ✅ CREATE/DELETE | |
| 物流计划 | LogisticsController | ✅ CREATE/UPDATE/DELETE + BATCH_UPDATE_CUSTOMS_NO | |
| 拼柜池 | ConsolidationPoolController | ✅ CREATE/UPDATE/DELETE + ADD/REMOVE_PLAN | |
| 货柜 | ContainerController | ✅ CREATE/UPDATE/DELETE + ASSIGN/UNASSIGN_SHIP | |
| 船只 | ShipController | ✅ CREATE/UPDATE/DELETE | |
| 国内报关 | CustomsController | ✅ CREATE × 2/UPDATE × 5/DELETE | |
| 日本清关 | JapanCustomsController | ✅ CREATE × 2/UPDATE × 5/DELETE | |
| 退税 | TaxRefundController | ✅ CREATE/UPDATE × 2/DELETE | |
| 销售记录 | SalesRecordController | ✅ CREATE/UPDATE × 4/DELETE | |
| 工厂 | FactoryController | ✅ CREATE/UPDATE/DELETE | |
| 商品 | ProductController | ✅ CREATE/UPDATE × 2/DELETE | |
| 补货需求 | ReplenishmentDemandController | ✅ CREATE/UPDATE × 3/DELETE | |
| 需求关联 | DemandProcurementMappingController | ✅ CREATE/DELETE | |
| 货代 | DispatchController | ✅ CREATE/UPDATE/DELETE | |
| 货代 | DispatchController | ✅ CREATE/UPDATE/DELETE | |
| 直接采购 | LegacyProcurementUseCase | ✅ CREATE/UPDATE/DELETE（在 UseCase 层） | |
| 货物尺寸 | CargoSizeUseCase + CargoSizeController | ✅ CREATE × 2/DISCARD/PROMOTE/UPDATE × 3/DELETE（在 UseCase + Controller 层） | |
| 离线订单 | OfflineOrderController | ✅ CREATE/UPDATE/DELETE | |
| 仓库 | WarehouseController | ✅ CREATE/UPDATE/DELETE | |
| 通知 | NotificationController | ✅ CREATE/UPDATE/DELETE | |
| **订单总览-删除链路** | OrderOverviewController.deleteChain | ✅ **新增** DELETE_CHAIN | |
| **商品导入** | ItemSizeImportController.triggerImport | ✅ **新增** IMPORT | |
| **老系统导入** | LegacyImportList8Controller | ✅ **新增** CREATE/UPDATE/DELETE | |

### 只读 / 无需审计 Controller（allinone）

| Controller | 端点 | 原因 |
|-----------|------|------|
| TranslationController | GET /ai/ping、POST /ai/translate | AI 服务调用，不修改数据 |
| CosTestController | /test/** | 开发调试接口，生产应移除 |
| AuthController（allinone） | GET /auth/public-key | Token 验证，无写操作 |
| OrderOverviewController | GET /chain/*（其余方法） | 只读聚合查询 |

### user-service Controller（allinone 审计链路）

| Controller | @AuditLog | 状态 |
|-----------|-----------|------|
| AuthController | LOGIN/LOGOUT（手动调用） | ✅ |
| RoleController | CREATE/UPDATE/DELETE/ASSIGN_PERMISSIONS/PATCH | ✅ |
| UserController | CREATE/UPDATE/DELETE/UPDATE_STATUS/RESET_PASSWORD/ASSIGN_ROLES | ✅ |

---

## 2. 前端页面 × 审计覆盖验证

| 页面 | 路由 | 审计状态 | Controller |
|------|------|---------|-----------|
| LoginPage | /login | ✅ LOGIN | user-service |
| DispatchPage | /base/dispatch | ✅ | DispatchController |
| DomesticCustomsPage | /customs/domestic | ✅ | CustomsController |
| JapanCustomsRecordPage | /customs/japan | ✅ | JapanCustomsController |
| FactoryPage | /factory | ✅ | FactoryController |
| TaxRefundRecordPage | /finance/tax-refund | ✅ | TaxRefundController |
| LegacyProcurementPage | /legacy-procurement | ✅ | LegacyProcurementUseCase |
| OfflineOrderPage | /offline-order | ✅ | OfflineOrderController |
| ConsolidationPoolPage | /procurement/consolidation-pool | ✅ | ConsolidationPoolController |
| ContainerPage | /procurement/container | ✅ | ContainerController |
| DemandPage | /procurement/demand | ✅ | ReplenishmentDemandController |
| LogisticsPlanPage | /procurement/logistics-plan | ✅ | LogisticsController |
| ProcurementPage | /procurement | ✅ | ProcurementController |
| QcRecordPage | /procurement/qc-record | ✅ | QcRecordController |
| ShipmentBatchPage | /procurement/shipment-batch | ✅ | ShipmentBatchController |
| CargoSizePage | /product/cargo-size | ✅ | CargoSizeController + CargoSizeUseCase |
| ProductPage | /product | ✅ | ProductController |
| SalesRecordPage | /sales | ✅ | SalesRecordController |
| AuditLogPage | /system/audit-log | ✅ 只读 | — |
| ProfilePage | /profile | ✅ 只读 | — |
| RolePage | /system/role | ✅ 只读 | — |
| UserPage | /system/user | ✅ 只读 | — |
| OrderOverviewPage | /procurement/order-overview | ⚠️ 只读端点无审计，DELETE_CHAIN 已补 | OrderOverviewController |
| DemandOverviewPage | /base/demand-overview | ⚠️ 只读聚合，无审计 | — |
| ProcurementOverviewPage | /procurement/overview | ⚠️ 只读聚合，无审计 | — |

---

## 3. 本次修复（2026-05-25）

| Controller | 新增 @AuditLog | module | action |
|-----------|---------------|--------|--------|
| OrderOverviewController | `deleteChain()` | `order` | `DELETE_CHAIN` |
| LegacyImportList8Controller | `create/update/delete` | `legacy_import_list8` | `CREATE/UPDATE/DELETE` |
| ItemSizeImportController | `triggerImport()` | `item_size` | `IMPORT` |

---

## 4. 缺口与风险

| 缺口 | 风险 | 建议 |
|------|------|------|
| CosTestController 无权限限制 | `/api/v1/test/**` 对所有登录用户开放 | 生产环境移除路由或加 ADMIN 限制 |
| ItemSizeImportController `/internal/` 无 IP 白名单 | 内部导入接口可被内网调用 | 添加 IP 白名单或 ADMIN 角色限制 |
| 暂无 `/export` 审计 | 导出操作无记录 | 实现 audit:export 端点 |

---

## 5. 统计汇总

| 指标 | 数量 |
|------|------|
| allinone 写操作 Controller | 23 |
| 含 @AuditLog | 23 |
| 覆盖率 | **100%** |
| user-service 写操作 Controller | 3 |
| 含 @AuditLog | 3 |
| 覆盖率 | **100%** |
| 本次新增 @AuditLog | **3 个方法 × 3 个 Controller** |
