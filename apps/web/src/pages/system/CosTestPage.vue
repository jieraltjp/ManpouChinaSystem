<template>
  <div class="page">
    <el-page-header @back="() => $router.back()" content="COS 腾讯云测试" class="page-header" />

    <!-- 配置状态 -->
    <el-card shadow="never" class="status-card">
      <template #header>
        <div class="card-header">
          <span>配置状态</span>
          <el-button size="small" @click="loadStatus" :loading="statusLoading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>
      <el-descriptions :column="2" border v-if="status">
        <el-descriptions-item label="是否启用">
          <el-tag :type="status.enabled ? 'success' : 'danger'" size="small">
            {{ status.enabled ? '已启用' : '未启用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="SecretId 已配置">
          <el-tag :type="status.secretIdSet ? 'success' : 'warning'" size="small">
            {{ status.secretIdSet ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="SecretKey 已配置">
          <el-tag :type="status.secretKeySet ? 'success' : 'warning'" size="small">
            {{ status.secretKeySet ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Bucket">{{ status.bucket }}</el-descriptions-item>
        <el-descriptions-item label="Region">{{ status.region }}</el-descriptions-item>
        <el-descriptions-item label="Domain" :span="2">{{ status.domain }}</el-descriptions-item>
        <el-descriptions-item label="存储路径前缀">{{ status.prefix }}</el-descriptions-item>
        <el-descriptions-item label="最大文件大小">{{ (status.maxFileSize / 1024 / 1024).toFixed(1) }} MB</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else-if="!statusLoading" description="点击「刷新」加载配置状态" />
    </el-card>

    <!-- 上传测试 -->
    <el-card shadow="never" class="upload-card">
      <template #header>
        <div class="card-header">
          <span>上传测试</span>
          <el-tag type="info" size="small">支持 JPG / PNG / WEBP，单文件最大 5MB</el-tag>
        </div>
      </template>
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :file-list="fileList"
        accept="image/jpeg,image/png,image/webp"
        class="upload-dragger"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽图片到此处，或 <em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">仅供开发调试使用</div>
        </template>
      </el-upload>
      <div class="upload-actions">
        <el-button type="primary" :loading="uploadLoading" :disabled="!selectedFile" @click="handleUpload">
          上传到 COS
        </el-button>
        <el-button :disabled="!selectedFile" @click="selectedFile = null; fileList = []">清除</el-button>
      </div>

      <!-- 上传结果 -->
      <div v-if="uploadResult" class="upload-result">
        <el-divider content-position="left">上传成功</el-divider>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="文件名称">{{ uploadResult.filename }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ (uploadResult.size / 1024).toFixed(1) }} KB</el-descriptions-item>
          <el-descriptions-item label="COS URL" :span="2">
            <el-link :href="uploadResult.url" target="_blank" type="primary">{{ uploadResult.url }}</el-link>
          </el-descriptions-item>
        </el-descriptions>
        <div class="preview-img" v-if="isImage(uploadResult.contentType)">
          <el-image :src="uploadResult.url" fit="contain" class="cos-preview" :preview-src-list="[uploadResult.url]" />
        </div>
        <div class="result-actions">
          <el-button size="small" type="danger" @click="handleDelete(uploadResult.url)">
            <el-icon><Delete /></el-icon> 从 COS 删除
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Refresh, Delete } from '@element-plus/icons-vue'
import { getCosStatus, uploadCosFile, deleteCosFile, type CosStatusInfo, type CosUploadResult } from '@/api/cos'

const status = ref<CosStatusInfo | null>(null)
const statusLoading = ref(false)
const selectedFile = ref<File | null>(null)
const fileList = ref<{ name: string; uid: number }[]>([])
const uploadLoading = ref(false)
const uploadResult = ref<CosUploadResult | null>(null)

async function loadStatus() {
  statusLoading.value = true
  try {
    const res = await getCosStatus()
    status.value = res.data
  } catch {
    // error already handled by interceptor
  } finally {
    statusLoading.value = false
  }
}

function handleFileChange(file: { raw: File }) {
  selectedFile.value = file.raw
  fileList.value = [{ name: file.raw.name, uid: file.raw.lastModified }]
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploadLoading.value = true
  uploadResult.value = null
  try {
    const res = await uploadCosFile(selectedFile.value)
    uploadResult.value = res.data
    ElMessage.success('上传成功')
  } catch {
    // error already handled by interceptor
  } finally {
    uploadLoading.value = false
  }
}

async function handleDelete(url: string) {
  try {
    await deleteCosFile(url)
    uploadResult.value = null
    ElMessage.success('删除成功')
  } catch {
    // error already handled by interceptor
  }
}

function isImage(contentType: string) {
  return contentType?.startsWith('image/')
}

onMounted(() => {
  loadStatus()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}

.status-card,
.upload-card {
  margin-bottom: 16px;
  max-width: 800px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.upload-dragger {
  width: 100%;
}

.upload-actions {
  margin-top: 16px;
}

.upload-result {
  margin-top: 16px;
}

.cos-preview {
  margin-top: 12px;
  max-width: 400px;
  max-height: 300px;
  border: 1px solid #eee;
  border-radius: 4px;
}

.result-actions {
  margin-top: 12px;
}
</style>
