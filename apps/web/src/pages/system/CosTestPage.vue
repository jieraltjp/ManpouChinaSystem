<template>
  <div v-if="hasRole('ADMIN')" class="page">
    <el-page-header @back="() => $router.back()" :content="$t('cosTest.title')" class="page-header" />

    <!-- 配置状态 -->
    <el-card shadow="never" class="status-card">
      <template #header>
        <div class="card-header">
          <span>{{ $t('cosTest.statusTitle') }}</span>
          <el-button size="small" @click="loadStatus" :loading="statusLoading">
            <el-icon><Refresh /></el-icon> {{ $t('cosTest.refresh') }}
          </el-button>
        </div>
      </template>
      <el-descriptions :column="2" border v-if="status">
        <el-descriptions-item :label="$t('cosTest.enabled')">
          <el-tag :type="status.enabled ? 'success' : 'danger'" size="small">
            {{ status.enabled ? $t('cosTest.enabled') : $t('cosTest.disabled') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('cosTest.secretIdConfigured')">
          <el-tag :type="status.secretIdSet ? 'success' : 'warning'" size="small">
            {{ status.secretIdSet ? $t('cosTest.yes') : $t('cosTest.no') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('cosTest.secretKeyConfigured')">
          <el-tag :type="status.secretKeySet ? 'success' : 'warning'" size="small">
            {{ status.secretKeySet ? $t('cosTest.yes') : $t('cosTest.no') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('cosTest.bucket')">{{ status.bucket }}</el-descriptions-item>
        <el-descriptions-item :label="$t('cosTest.region')">{{ status.region }}</el-descriptions-item>
        <el-descriptions-item :label="$t('cosTest.domain')" :span="2">{{ status.domain }}</el-descriptions-item>
        <el-descriptions-item :label="$t('cosTest.prefix')">{{ status.prefix }}</el-descriptions-item>
        <el-descriptions-item :label="$t('cosTest.maxFileSize')">{{ (status.maxFileSize / 1024 / 1024).toFixed(1) }} MB</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else-if="!statusLoading" :description="$t('cosTest.clickRefresh')" />
    </el-card>

    <!-- 上传测试 -->
    <el-card shadow="never" class="upload-card">
      <template #header>
        <div class="card-header">
          <span>{{ $t('cosTest.uploadTitle') }}</span>
          <el-tag type="info" size="small">{{ $t('cosTest.uploadHint') }}</el-tag>
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
        <div class="el-upload__text" v-html="$t('cosTest.dragHint')" />
        <template #tip>
          <div class="el-upload__tip">{{ $t('cosTest.devOnly') }}</div>
        </template>
      </el-upload>
      <div class="upload-actions">
        <el-button v-if="hasRole('ADMIN')" type="primary" :loading="uploadLoading" :disabled="!selectedFile" @click="handleUpload">
          {{ $t('cosTest.uploadToCos') }}
        </el-button>
        <el-button :disabled="!selectedFile" @click="selectedFile = null; fileList = []">{{ $t('cosTest.clear') }}</el-button>
      </div>

      <!-- 上传结果 -->
      <div v-if="uploadResult" class="upload-result">
        <el-divider content-position="left">{{ $t('cosTest.uploadSuccess') }}</el-divider>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="$t('cosTest.filename')">{{ uploadResult.filename }}</el-descriptions-item>
          <el-descriptions-item :label="$t('cosTest.filesize')">{{ (uploadResult.size / 1024).toFixed(1) }} KB</el-descriptions-item>
          <el-descriptions-item :label="$t('cosTest.cosUrl')" :span="2">
            <el-link :href="uploadResult.url" target="_blank" type="primary">{{ uploadResult.url }}</el-link>
          </el-descriptions-item>
        </el-descriptions>
        <div class="preview-img" v-if="isImage(uploadResult.contentType)">
          <el-image :src="uploadResult.url" fit="contain" class="cos-preview" :preview-src-list="[uploadResult.url]" />
        </div>
        <div class="result-actions">
          <el-button v-if="hasRole('ADMIN')" size="small" type="danger" @click="handleDelete(uploadResult.url)">
            <el-icon><Delete /></el-icon> {{ $t('cosTest.deleteFromCos') }}
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
import { usePermission } from '@/composables/usePermission'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const { hasRole } = usePermission()

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
    ElMessage.success(t('cosTest.uploadSuccess'))
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
    ElMessage.success(t('cosTest.deleteSuccess'))
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
