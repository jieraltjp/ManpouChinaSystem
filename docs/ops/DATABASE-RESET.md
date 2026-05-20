# 数据库重置操作文档

> 日期：2026-05-20
> 适用环境：开发/测试环境，生产禁止执行
> 前提：allinone (18090) 和 user-service (18081) 已启动

---

## 重置范围

| 表名 | 说明 | 依赖顺序 |
|------|------|---------|
| `audit_log` | 操作审计日志 | 1 |
| `qc_record` | 验货记录 | 2 |
| `qc_image` | 验货图片 | 3（依赖 qc_record） |
| `domestic_customs_record` | 国内报关记录 | 4 |
| `japan_customs_record` | 日本报关记录 | 5（依赖 logistics_plan） |
| `logistics_plan` | 物流计划 | 6（依赖 procurement） |
| `procurement_snapshot` | 发注单快照 | 7（依赖 procurement） |
| `demand_procurement_mapping` | 需求-发注关联 | 8（依赖 demand + procurement） |
| `replenishment_demand` | 补货需求单 | 9（依赖 product） |
| `demand` | 需求单（仅未定/発注待状态） | 10 |
| `procurement` | 发注单（仅未定/発注待状态） | 11 |

**保留（不重置）**：user（用户）、role、permission、factory（505）、product（4999）

---

## 操作步骤

### 步骤 1：获取管理员 Token

```bash
TOKEN=$(curl -s "http://localhost:18081/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | node -e "let d=''; process.stdin.on('data',c=>d+=c); process.stdin.on('end',()=>{try{const r=JSON.parse(d); console.log(r.data.accessToken)}catch(e){console.log('')}})")
echo $TOKEN
```

### 步骤 2：逐表清理

```bash
BASE="http://localhost:18090/api/v1"

# 1. audit_log（无依赖，最先清）
for id in $(curl -s "$BASE/audit-logs?page=0&pageSize=500" -H "Authorization: Bearer $TOKEN" \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{JSON.parse(d).data.content.forEach(x=>console.log(x.id))}catch(e){}})" ); do
  curl -s -X DELETE "$BASE/audit-logs/$id" -H "Authorization: Bearer $TOKEN"
  echo " audit_log $id done"
done

# 2. qc_record（无依赖）
for id in $(curl -s "$BASE/qc-records?page=0&pageSize=500" -H "Authorization: Bearer $TOKEN" \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{JSON.parse(d).data.content.forEach(x=>console.log(x.id))}catch(e){}})" ); do
  curl -s -X DELETE "$BASE/qc-records/$id" -H "Authorization: Bearer $TOKEN"
  echo " qc_record $id done"
done

# 3. domestic_customs_record
for id in $(curl -s "$BASE/customs?page=0&pageSize=500" -H "Authorization: Bearer $TOKEN" \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{JSON.parse(d).data.content.forEach(x=>console.log(x.id))}catch(e){}})" ); do
  curl -s -X DELETE "$BASE/customs/$id" -H "Authorization: Bearer $TOKEN"
  echo " domestic_customs $id done"
done

# 4. japan_customs_record
for id in $(curl -s "$BASE/japan-customs?page=0&pageSize=500" -H "Authorization: Bearer $TOKEN" \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{JSON.parse(d).data.content.forEach(x=>console.log(x.id))}catch(e){}})" ); do
  curl -s -X DELETE "$BASE/japan-customs/$id" -H "Authorization: Bearer $TOKEN"
  echo " japan_customs $id done"
done

# 5. demand_procurement_mapping
for id in $(curl -s "$BASE/demand-mappings?page=0&pageSize=500" -H "Authorization: Bearer $TOKEN" \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{JSON.parse(d).data.content.forEach(x=>console.log(x.id))}catch(e){}})" ); do
  curl -s -X DELETE "$BASE/demand-mappings/$id" -H "Authorization: Bearer $TOKEN"
  echo " demand_mapping $id done"
done

# 6. logistics_plan（无 REST API，需确认是否有数据）

# 7. procurement_snapshot（无 REST API）

# 8. replenishment_demand（无依赖）
for id in $(curl -s "$BASE/replenishment/demands?page=0&pageSize=500" -H "Authorization: Bearer $TOKEN" \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{JSON.parse(d).data.content.forEach(x=>console.log(x.id))}catch(e){}})" ); do
  curl -s -X DELETE "$BASE/replenishment/demands/$id" -H "Authorization: Bearer $TOKEN"
  echo " replenishment_demand $id done"
done

# 9. demand（仅删除未定/発注待状态）
for id in $(curl -s "$BASE/demands?page=0&pageSize=500" -H "Authorization: Bearer $TOKEN" \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{JSON.parse(d).data.content.filter(x=>['未定','発注待'].includes(x.status)).forEach(x=>console.log(x.id))}catch(e){}})" ); do
  curl -s -X DELETE "$BASE/demands/$id" -H "Authorization: Bearer $TOKEN"
  echo " demand $id done"
done

# 10. procurement（仅删除未定/発注待状态）
for id in $(curl -s "$BASE/procurements?page=0&pageSize=500" -H "Authorization: Bearer $TOKEN" \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{JSON.parse(d).data.content.filter(x=>['未定','発注待'].includes(x.status)).forEach(x=>console.log(x.id))}catch(e){}})" ); do
  curl -s -X DELETE "$BASE/procurements/$id" -H "Authorization: Bearer $TOKEN"
  echo " procurement $id done"
done
```

### 步骤 3：验证

```bash
echo "=== 验证 ==="
for ep in demands procurements shipments qc-records containers logistics/ships \
         customs/domestic customs japan-customs replenishment/demands audit-logs; do
  count=$(curl -s "$BASE/$ep?page=0&pageSize=1" -H "Authorization: Bearer $TOKEN" \
    | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{console.log(JSON.parse(d).data.totalElements||0)}catch(e){console.log('ERR')}})")
  [ "$count" != "0" ] && echo "⚠ $ep: $count 条" || echo "✅ $ep: 0 条"
done

echo ""
echo "=== 核心数据（应保留） ==="
for ep in factories products; do
  count=$(curl -s "$BASE/$ep?page=0&pageSize=1" -H "Authorization: Bearer $TOKEN" \
    | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{console.log(JSON.parse(d).data.totalElements||0)}catch(e){console.log('ERR')}})")
  echo "✅ $ep: $count 条"
done
```

---

## REST API 端点速查

| 表 | 端点 | 删除方式 |
|----|------|---------|
| audit_log | DELETE `/audit-logs/{id}` | 逐条 |
| qc_record | DELETE `/qc-records/{id}` | 逐条 |
| qc_image | DELETE `/qc/images/{id}` | 逐条 |
| domestic_customs_record | DELETE `/customs/{id}` | 逐条 |
| japan_customs_record | DELETE `/japan-customs/{id}` | 逐条 |
| replenishment_demand | DELETE `/replenishment/demands/{id}` | 逐条 |
| demand | DELETE `/demands/{id}` | 逐条（仅未定/発注待） |
| procurement | DELETE `/procurements/{id}` | 逐条（仅未定/発注待） |
| demand_procurement_mapping | DELETE `/demand-mappings/{id}` | 逐条 |
| logistics_plan | 无 REST API | 需手动 SQL |
| procurement_snapshot | 无 REST API | 需手动 SQL |
| qc_image（无主键） | 由 qc_record 联动删除 | 无需单独处理 |

---

## 无 REST API 表的手动 SQL

`procurement_snapshot`、`logistics_plan`、`qc_image` 无批量删除接口，
通过 Flyway V 基线化处理（生产禁止）或手动 SQL：

```sql
-- ⚠️ 生产环境禁止执行，仅开发/测试使用

-- 清空 qc_image
TRUNCATE TABLE qc_image;

-- 清空 procurement_snapshot
TRUNCATE TABLE procurement_snapshot;

-- 清空 logistics_plan
TRUNCATE TABLE logistics_plan;
```

---

## 快速脚本（一键执行）

将以下内容保存为 `scripts/reset-test-data.sh`：

```bash
#!/bin/bash
set -e

echo "获取 Token..."
TOKEN=$(curl -s "http://localhost:18081/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | node -p "JSON.parse(require('fs').readFileSync('/dev/stdin','utf8')).data.accessToken")

BASE="http://localhost:18090/api/v1"

delete_all() {
  local endpoint=$1
  echo "清理 $endpoint..."
  local ids=$(curl -s "$BASE/$endpoint?page=0&pageSize=1000" -H "Authorization: Bearer $TOKEN" \
    | node -p "JSON.parse(require('fs').readFileSync('/dev/stdin','utf8')).data.content?.map(x=>x.id).join('\n') || ''")
  for id in $ids; do
    curl -s -X DELETE "$BASE/$endpoint/$id" -H "Authorization: Bearer $TOKEN" > /dev/null
    echo "  $id"
  done
  echo "  完成"
}

delete_all "audit-logs"
delete_all "qc-records"
delete_all "qc/images"
delete_all "customs"
delete_all "japan-customs"
delete_all "replenishment/demands"

echo "清理 demands（仅未定/発注待）..."
curl -s "$BASE/demands?page=0&pageSize=1000" -H "Authorization: Bearer $TOKEN" \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{JSON.parse(d).data.content.filter(x=>['未定','発注待'].includes(x.status)).forEach(x=>{require('child_process').execSync('curl -s -X DELETE http://localhost:18090/api/v1/demands/'+x.id+' -H \"Authorization: Bearer $TOKEN\" >/dev/null');console.log('demand '+x.id)})}catch(e){}})" || true

echo "清理 procurements（仅未定/発注待）..."
curl -s "$BASE/procurements?page=0&pageSize=1000" -H "Authorization: Bearer $TOKEN" \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{try{JSON.parse(d).data.content.filter(x=>['未定','発注待'].includes(x.status)).forEach(x=>{require('child_process').execSync('curl -s -X DELETE http://localhost:18090/api/v1/procurements/'+x.id+' -H \"Authorization: Bearer $TOKEN\" >/dev/null');console.log('procurement '+x.id)})}catch(e){}})" || true

echo ""
echo "=== 重置完成 ==="
```
