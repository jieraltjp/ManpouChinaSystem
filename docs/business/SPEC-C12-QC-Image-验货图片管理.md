# SPEC-C12：验货图片管理

> 状态：**设计** | 版本：v1.0.0 | 更新：2026-05-01

---

## 1. 业务背景

验货记录需要附带图片证据（验货现场照片），图片存储在腾讯云 COS，上传后需支持预览和删除。

---

## 2. 现有代码审计

### 2.1 已实现（后端）

| 文件 | 状态 | 说明 |
|------|------|------|
| `qc/domain/model/QcImage.java` | ✅ 完成 | Entity，含软删除字段 |
| `qc/domain/repository/QcImageRepository.java` | ✅ 完成 | 软删除查询方法 |
| `qc/interfaces/controller/QcImageController.java` | ✅ 完成 | 4个 REST 接口 |
| `CosService.java` | ✅ 已修复 | `Content-Disposition: inline`，预览可用 |

**API 设计：**

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/qc/images/upload` | 单张上传，`?qcRecordId=` 可选 |
| `POST` | `/qc/images/upload-multiple` | 批量上传（最多9张） |
| `DELETE` | `/qc/images?id=` | 软删 + COS 删除 |
| `GET` | `/qc/images?qcRecordId=` | 查询某记录的所有图片 |

### 2.2 已实现（前端 API）

| 方法 | 状态 | 说明 |
|------|------|------|
| `inspectionApi.uploadImage(file, qcRecordId?)` | ✅ 完成 | 单张 |
| `inspectionApi.uploadImages(files, qcRecordId?)` | ✅ 完成 | 批量 |
| `inspectionApi.deleteImage(id)` | ✅ 完成 | 删除 |
| `inspectionApi.listImages(qcRecordId)` | ✅ 完成 | 查询 |

### 2.3 已实现（前端页面）

| 功能 | 状态 | 说明 |
|------|------|------|
| 新建弹窗 `el-upload` | ✅ 完成 | 本地预览 + COS 上传 |
| 详情抽屉图片展示 | ⚠️ 部分 | 读 `QcRecord.images`（URL 字符串），未读 `qc_image` 表 |
| 编辑时加载已有图片 | ❌ 未实现 | `onEdit()` 未加载图片到 `uploadFileList` |
| 图片预览（`response-content-disposition=inline`） | ✅ 已修复 | CosService 已处理 |

---

## 3. 问题分析

### 问题 1：详情抽屉数据源不一致

- **现状**：详情抽屉 `drawerImageList` 从 `QcRecord.images`（字符串字段，按 `\n` 分割 URL）读取
- **问题**：上传后的图片存在 `qc_image` 表，不在 `QcRecord.images` 字段
- **表现**：上传图片成功，但详情抽屉看不到

### 问题 2：编辑时无法管理已有图片

- **现状**：`onEdit()` 只填充表单字段，不加载已有图片到 `uploadFileList`
- **问题**：编辑时看不到已上传的图片，无法删除旧图片

### 问题 3：双重存储

- `QcRecord.images` 字段（历史遗留，`\n` 分隔 URL 字符串）
- `qc_image` 表（新设计，正确关联到 qcRecordId）

---

## 4. 设计决策

### 决策：统一使用 `qc_image` 表作为图片数据源

- 删除 `QcRecord.images` 字段的使用（历史遗留）
- 详情抽屉改为调用 `GET /qc/images?qcRecordId=`
- 编辑时调用 `GET /qc/images?qcRecordId=` 加载图片到 `uploadFileList`
- 图片上传成功后立即持久化到 `qc_image` 表（有 qcRecordId）

### 优点

1. 图片与记录精确关联（qcRecordId 字段）
2. 支持软删除（图片可恢复）
3. 详情/编辑/删除均读同一数据源，不一致
4. `CosService.delete()` 同步删除 COS 对象

### 存储限制

| 限制 | 值 |
|------|-----|
| 单文件大小 | ≤ 5 MB |
| 文件格式 | JPG / PNG / WEBP |
| 单记录图片数 | ≤ 9 张 |
| 存储路径 | `qc-images/YYYY/MM/DD/{uuid}.{ext}` |

---

## 5. 数据库设计

表 `qc_image`（已存在，Flyway 无需变更）：

```sql
CREATE TABLE qc_image (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    qc_record_id    BIGINT       COMMENT '关联验货记录ID，可为空（上传时未指定）',
    filename        VARCHAR(255) NOT NULL COMMENT 'COS对象key（uuid.ext）',
    original_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    url             VARCHAR(512) NOT NULL COMMENT '完整访问URL（含inline参数）',
    size            BIGINT      NOT NULL COMMENT '文件大小（字节）',
    mime_type       VARCHAR(64) NOT NULL COMMENT 'MIME类型',
    uploaded_by     BIGINT       COMMENT '上传人用户ID',
    create_time     DATETIME    NOT NULL,
    is_deleted     TINYINT(1)  NOT NULL DEFAULT 0,
    deleted_at     DATETIME,
    deleted_by     BIGINT,
    INDEX idx_qc_record_id (qc_record_id),
    INDEX idx_filename (filename),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验货记录图片';
```

---

## 6. API 详细设计

### 6.1 上传单张图片

```
POST /api/v1/qc/images/upload
Content-Type: multipart/form-data

file: <binary>
qcRecordId?: <number>   // 可选：验货记录ID，暂不支持时传空

Response 200:
{
  "code": "ok",
  "data": {
    "url": "https://.../uuid.jpg?response-content-disposition=inline",
    "filename": "uuid.jpg",
    "size": 123456
  }
}
```

### 6.2 批量上传

```
POST /api/v1/qc/images/upload-multiple
Content-Type: multipart/form-data

files: <binary> (最多9个)
qcRecordId?: <number>

Response 200:
{
  "code": "ok",
  "data": [
    { "url": "...", "filename": "...", "size": 123 },
    ...
  ]
}
```

### 6.3 查询图片列表

```
GET /api/v1/qc/images?qcRecordId=123

Response 200:
{
  "code": "ok",
  "data": [
    {
      "id": 1,
      "qcRecordId": 123,
      "filename": "uuid.jpg",
      "originalName": "现场照片1.jpg",
      "url": "https://.../uuid.jpg?response-content-disposition=inline",
      "size": 123456,
      "mimeType": "image/jpeg",
      "uploadedBy": null,
      "createTime": "2026-05-01T10:00:00"
    }
  ]
}
```

### 6.4 删除图片

```
DELETE /api/v1/qc/images?id=123

Response 200:
{
  "code": "ok",
  "data": null
}
```

> 软删除记录，同时调用 `CosService.delete(url)` 删除 COS 对象。

---

## 7. 前端改造计划

### 7.1 详情抽屉（onView）

**现状**：读 `QcRecord.images` 字符串
**改造**：调用 `inspectionApi.listImages(qcRecordId)` 加载图片列表

```typescript
// onView(row)
currentRow.value = row
drawerImages.value = []  // 重置
if (row.id) {
  const res = await inspectionApi.listImages(row.id)
  drawerImages.value = res.data ?? []
}
drawerVisible.value = true
```

### 7.2 编辑弹窗（onEdit）

**现状**：不加载图片
**改造**：调用 `inspectionApi.listImages(qcRecordId)`，将已有图片加入 `uploadFileList`（带 `id` 标识用于区分新增/已有）

```typescript
// uploadFileList 项结构扩展
interface UploadFileItem {
  id?: number       // 数据库ID，有则代表已有图片
  name: string
  url: string        // 已上传URL
  raw?: File         // 本地文件
  status: 'ready' | 'uploaded'
}
```

### 7.3 提交逻辑（onSubmit）

**现状**：提交时上传图片，合并到 `form.images` 字符串
**改造**：
1. 已有的图片（`uploadFileList` 中 `id` 有值的项）**跳过上传**
2. 新增的图片（`raw` 有值的项）调用 `uploadImages()`
3. 删除的图片（不在 `uploadFileList` 中的已有图片）调用 `deleteImage(id)`

```typescript
async function onSubmit() {
  // 1. 处理删除：找出被移除的已有图片
  const toDelete = existingImageIds.value.filter(id => !uploadFileList.value.some(f => f.id === id))
  await Promise.all(toDelete.map(id => inspectionApi.deleteImage(id)))

  // 2. 上传新增的本地文件
  const newFiles = uploadFileList.value.filter(f => f.raw && !f.id)
  if (newFiles.length) {
    const res = await inspectionApi.uploadImages(newFiles.map(f => f.raw!), qcRecordId)
    // 新图片 URL 已通过 res 返回
  }

  // 3. 提交表单（图片已在上两步处理完毕，不再传 form.images）
  ...
}
```

### 7.4 详情抽屉图片展示

```vue
<el-image
  v-for="img in drawerImages"
  :key="img.id"
  :src="img.url"
  :preview-src-list="drawerImages.map(i => i.url)"
  fit="cover"
  class="drawer-image-thumb"
/>
```

---

## 8. 自检清单

### 接口一致性
- [ ] `GET /qc/images?qcRecordId=` 后端已实现 ✅
- [ ] `DELETE /qc/images?id=` 软删 + COS 删除 ✅
- [ ] `POST /upload` / `/upload-multiple` 后端已实现 ✅

### 前端改造
- [ ] `onView()` 加载图片到 `drawerImages`
- [ ] `onEdit()` 加载图片到 `uploadFileList`（区分已有/新增）
- [ ] `onSubmit()` 跳过已有图片上传，删除被移除的已有图片
- [ ] `drawerImages` 展示改用 `qc_image` 数据源
- [ ] `response-content-disposition=inline` 已修复（CosService） ✅

### 数据库
- [ ] `qc_image` 表已存在，无需 Flyway 迁移 ✅

### 限制
- [ ] 单文件 5MB ✅（后端已校验）
- [ ] 最多 9 张/记录 ✅（后端已校验）
- [ ] 仅 JPG/PNG/WEBP ✅（后端已校验）

---

## 9. 文件清单

### 后端（无需修改）
- `qc/domain/model/QcImage.java` — Entity ✅
- `qc/domain/repository/QcImageRepository.java` — Repository ✅
- `qc/interfaces/controller/QcImageController.java` — Controller ✅
- `common/service/CosService.java` — `inline` + `?response-content-disposition=inline` ✅

### 前端（需修改）
- `api/inspection.ts` — 类型补充 `UploadFileItem` 接口
- `pages/procurement/QcRecordPage.vue` — 改造 onView / onEdit / onSubmit / 详情抽屉

### 前端（无需修改）
- `api/inspection.ts` — API 方法已存在 ✅
- `CosTestPage.vue` — 图片预览已通过 inline 修复 ✅
