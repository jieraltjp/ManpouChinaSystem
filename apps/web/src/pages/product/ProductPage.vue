<template>
  <div class="page">
    <!-- 统计行 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Goods /></el-icon></div>
            <div>
              <div class="stat-value">{{ pagination.total }}</div>
              <div class="stat-label">{{ $t('product.stat.total') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-row :gutter="16">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('product.filter.masterCode')">
          <el-input v-model="filterForm.masterCode" :placeholder="$t('product.filter.masterCodePlaceholder')" clearable style="width:160px" />
        </el-form-item>
        <el-form-item :label="$t('product.filter.keyword')">
          <el-input v-model="filterForm.keyword" :placeholder="$t('product.filter.keywordPlaceholder')" clearable style="width:160px" />
        </el-form-item>
        <el-form-item :label="$t('product.filter.hsCode')">
          <el-input v-model="filterForm.hsCode" :placeholder="$t('product.filter.hsCode')" clearable style="width:120px" />
        </el-form-item>
        <el-form-item :label="$t('product.filter.hsCodeJp')">
          <el-input v-model="filterForm.hsCodeJp" :placeholder="$t('product.filter.hsCodeJp')" clearable style="width:120px" />
        </el-form-item>
        <el-form-item :label="$t('product.filter.factoryName')">
          <el-input v-model="filterForm.factoryName" :placeholder="$t('product.filter.factoryNamePlaceholder')" clearable style="width:120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ $t('product.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('product.filter.reset') }}</el-button>
          <el-button type="primary" @click="onNew" v-if="hasPermission('product:create')">
            <el-icon><Plus /></el-icon>{{ $t('product.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
      </el-row>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="masterCode" :label="$t('product.column.masterCode')" min-width="110" />
        <el-table-column prop="subCode" :label="$t('product.column.subCode')" min-width="120" />
        <el-table-column :label="$t('product.column.image')" min-width="70" align="center">
          <template #default="{ row }">
            <a v-if="row.imageUrl" :href="row.imageUrl" target="_blank" title="查看大图">
              <img :src="row.imageUrl" style="width:36px;height:36px;object-fit:cover;border-radius:4px;border:1px solid #eee;" loading="lazy" />
            </a>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="janCode" :label="$t('product.column.janCode')" min-width="100" show-overflow-tooltip />
        <el-table-column prop="nameZh" :label="$t('product.column.nameZh')" min-width="150" show-overflow-tooltip />
        <el-table-column prop="nameJa" :label="$t('product.column.nameJa')" min-width="110" show-overflow-tooltip />
        <el-table-column prop="nameEn" :label="$t('product.column.nameEn')" min-width="130" show-overflow-tooltip />
        <el-table-column prop="origin" :label="$t('product.column.origin')" min-width="80" />
        <el-table-column prop="category" :label="$t('product.column.category')" min-width="90">
          <template #default="{ row }">
            <span v-if="row.category">{{ $t('product.category.' + row.category) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('product.column.linkedFactories')" min-width="130" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.factoryNames" :content="row.factoryNames" placement="top" :disabled="!row.factoryNames">
              <span class="factory-names-text">{{ row.factoryNames }}</span>
            </el-tooltip>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="unitPriceRmb" :label="$t('product.column.unitPriceRmb')" min-width="100" align="right">
          <template #default="{ row }">{{ row.unitPriceRmb != null ? $t('common.currency.cny') + Number(row.unitPriceRmb).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="taxPoint" :label="$t('product.column.taxPoint')" min-width="70" align="center">
          <template #default="{ row }">{{ row.taxPoint != null ? row.taxPoint : '-' }}</template>
        </el-table-column>
        <el-table-column prop="material" :label="$t('product.column.material')" min-width="90" show-overflow-tooltip />
        <el-table-column prop="requiresQc" :label="$t('product.column.requiresQc')" min-width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.requiresQc" type="warning" size="small">{{ $t('product.column.requiresQc') }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('product.column.action')" min-width="150" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('product.action.detail') }}</el-button>
            <el-button link type="warning" size="small" @click.stop="onEdit(row)" v-if="hasPermission('product:update')">{{ $t('product.action.edit') }}</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)" v-if="hasPermission('product:delete')">{{ $t('product.action.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          background
          :current-page="pagination.page"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="$t('product.drawer.title')" size="680px" direction="rtl" bodyStyle="overflow-y: auto">
      <div v-if="currentRow" class="drawer-content">

        <!-- 基本信息 -->
        <div class="drawer-section-title">{{ $t('product.drawer.section.basicInfo') }}</div>
        <div class="detail-grid">
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.masterCode') }}</span><span class="detail-value">{{ currentRow.masterCode }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.subCode') }}</span><span class="detail-value">{{ currentRow.subCode || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.janCode') }}</span><span class="detail-value">{{ currentRow.janCode || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.category') }}</span>
            <span class="detail-value">
              <el-tag v-if="currentRow.category" size="small">{{ $t('product.category.' + currentRow.category) }}</el-tag>
              <span v-else>-</span>
            </span>
          </div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.nameZh') }}</span><span class="detail-value">{{ currentRow.nameZh || '-' }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.nameEn') }}</span><span class="detail-value">{{ currentRow.nameEn || '-' }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.nameJa') }}</span><span class="detail-value">{{ currentRow.nameJa || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.material') }}</span><span class="detail-value">{{ currentRow.material || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.materialJa') }}</span><span class="detail-value">{{ currentRow.materialJa || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.colorName') }}</span><span class="detail-value">{{ currentRow.colorName || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.origin') }}</span><span class="detail-value">{{ currentRow.origin || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.status') }}</span><span class="detail-value">{{ currentRow.status || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.unit') }}</span><span class="detail-value">{{ currentRow.unit || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.quantities') }}</span><span class="detail-value">{{ currentRow.quantities ?? '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.cartonQty') }}</span><span class="detail-value">{{ currentRow.cartonQty ?? '-' }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.imageUrl') }}</span>
            <span class="detail-value">
              <a v-if="currentRow.imageUrl" :href="currentRow.imageUrl" target="_blank" style="color:#409EFF">{{ currentRow.imageUrl }}</a>
              <span v-else>-</span>
            </span>
          </div>
        </div>

        <!-- 规格信息 -->
        <div class="drawer-section-title">{{ $t('product.drawer.section.specInfo') }}</div>
        <div class="detail-grid">
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.unitPriceRmb') }}</span><span class="detail-value">{{ currentRow.unitPriceRmb != null ? `¥${Number(currentRow.unitPriceRmb).toFixed(2)}` : '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.taxPoint') }}</span><span class="detail-value">{{ currentRow.taxPoint ?? '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.taxRate') }}</span><span class="detail-value">{{ currentRow.taxRate != null ? `${(Number(currentRow.taxRate) * 100).toFixed(1)}%` : '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.amountRmb') }}</span><span class="detail-value">{{ currentRow.amountRmb != null ? `¥${Number(currentRow.amountRmb).toFixed(2)}` : '-' }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.hsCode') }} / {{ $t('product.drawer.hsCodeJp') }}</span><span class="detail-value">{{ currentRow.hsCode || '-' }} / {{ currentRow.hsCodeJp || '-' }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.declarationElements') }}</span><span class="detail-value">{{ currentRow.declarationElements || '-' }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.lengthCm') }} × {{ $t('product.drawer.widthCm') }} × {{ $t('product.drawer.heightCm') }}</span><span class="detail-value">{{ dims(currentRow) }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.volumeCbm') }}</span><span class="detail-value">{{ currentRow.volumeCbm != null ? Number(currentRow.volumeCbm).toFixed(6) : '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.netWeight') }}</span><span class="detail-value">{{ currentRow.netWeightKg != null ? `${currentRow.netWeightKg} ${$t('common.units.kg')}` : '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.grossWeight') }}</span><span class="detail-value">{{ currentRow.grossWeightKg != null ? `${currentRow.grossWeightKg} ${$t('common.units.kg')}` : '-' }}</span></div>
        </div>

        <!-- 外箱信息 -->
        <div class="drawer-section-title">{{ $t('product.drawer.section.packageInfo') }}</div>
        <div class="detail-grid">
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.unitsPerPackage') }}</span><span class="detail-value">{{ currentRow.unitsPerPackage ?? '-' }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.packageLengthCm') }} × {{ $t('product.drawer.packageWidthCm') }} × {{ $t('product.drawer.packageHeightCm') }}</span><span class="detail-value">{{ pkgDims(currentRow) }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.packageVolumeCbm') }}</span><span class="detail-value">{{ currentRow.packageVolumeCbm != null ? Number(currentRow.packageVolumeCbm).toFixed(6) : '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.packageWeightKg') }}</span><span class="detail-value">{{ currentRow.packageWeightKg != null ? `${currentRow.packageWeightKg} ${$t('common.units.kg')}` : '-' }}</span></div>
        </div>

        <!-- 仓储信息 -->
        <div class="drawer-section-title">{{ $t('product.drawer.section.warehouseInfo') }}</div>
        <div class="detail-grid">
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.warehouse') }}</span><span class="detail-value">{{ currentRow.warehouse || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.requiresQc') }}</span><span class="detail-value">{{ currentRow.requiresQc ? $t('common.format.yes') : '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.lastUsedDate') }}</span><span class="detail-value">{{ currentRow.lastUsedDate || '-' }}</span></div>
        </div>

        <!-- 关联工厂 -->
        <div class="drawer-section-title">
          {{ $t('product.drawer.section.factories') }}
          <span class="factory-count-badge" v-if="productFactories.length > 0">{{ productFactories.length }}</span>
        </div>
        <div v-if="factoriesLoading" class="factories-loading">
          <el-icon class="is-loading"><Loading /></el-icon> {{ $t('common.loading') }}
        </div>
        <div v-else-if="productFactories.length === 0" class="factories-empty">
          {{ $t('product.drawer.noFactories') }}
        </div>
        <div v-else class="factories-list">
          <div v-for="factory in productFactories" :key="factory.factoryId" class="factory-card">
            <div class="factory-header">
              <span class="factory-name">{{ factory.factoryName || '-' }}</span>
              <el-tag v-if="factory.isPreferred" type="warning" size="small">{{ $t('product.drawer.preferred') }}</el-tag>
              <el-tag v-if="factory.cooperationStatus" size="small">{{ factory.cooperationStatus }}</el-tag>
            </div>
            <div class="factory-detail">
              <span>{{ $t('product.drawer.factoryCode') }}: {{ factory.factoryCode || '-' }}</span>
              <span>{{ $t('product.drawer.factoryLocation') }}: {{ [factory.province, factory.city].filter(Boolean).join('') || '-' }}</span>
              <span>{{ $t('product.drawer.contact') }}: {{ factory.contactName || '-' }}{{ factory.contactPhone ? ` (${factory.contactPhone})` : '' }}</span>
              <span>{{ $t('product.drawer.moq') }}: {{ factory.moq ?? '-' }}</span>
              <span>{{ $t('product.drawer.leadTimeDays') }}: {{ factory.leadTimeDays != null ? `${factory.leadTimeDays}${ $t('product.drawer.days') }` : '-' }}</span>
              <span>{{ $t('product.drawer.unitPrice') }}: {{ factory.unitPriceRmb != null ? `¥${Number(factory.unitPriceRmb).toFixed(2)}` : '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 备注 -->
        <div class="drawer-section-title">{{ $t('product.drawer.remarks') }}</div>
        <div class="detail-remarks">{{ currentRow.remarks || '-' }}</div>

        <!-- 审计信息 -->
        <div class="drawer-section-title" style="margin-top:8px">{{ $t('product.drawer.createTime') }} / {{ $t('product.drawer.updateTime') }}</div>
        <div class="detail-grid" style="margin-bottom:0">
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.createTime') }}</span><span class="detail-value">{{ formatTime(currentRow.createTime) }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.updateTime') }}</span><span class="detail-value">{{ formatTime(currentRow.updateTime) }}</span></div>
        </div>
        <div class="drawer-footer">
          <el-button @click="detailVisible = false">{{ $t('product.drawer.close') }}</el-button>
          <el-button type="primary" @click="onEditFromDrawer" v-if="hasPermission('product:update')">{{ $t('product.drawer.edit') }}</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 新规/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="840px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <!-- 基本信息 -->
        <div class="dialog-section-title">{{ $t('product.drawer.section.basicInfo') }}</div>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.masterCode')" prop="masterCode">
              <el-input v-model="form.masterCode" :placeholder="$t('product.dialog.masterCodePlaceholder')" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.subCode')">
              <el-input v-model="form.subCode" :placeholder="$t('product.dialog.subCodePlaceholder')" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.janCode')">
              <el-input v-model="form.janCode" :placeholder="$t('product.dialog.janCodePlaceholder')" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.nameZh')" prop="nameZh">
              <el-input v-model="form.nameZh" :placeholder="$t('product.dialog.nameZhPlaceholder')" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.nameEn')">
              <el-input v-model="form.nameEn" :placeholder="$t('product.dialog.nameEnPlaceholder')" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.nameJa')">
              <el-input v-model="form.nameJa" :placeholder="$t('product.dialog.nameJaPlaceholder')" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="$t('product.dialog.category')">
              <el-select v-model="form.category" clearable style="width:100%">
                <el-option value="OEM" :label="$t('product.category.OEM')" />
                <el-option value="ORDINARY" :label="$t('product.category.ORDINARY')" />
                <el-option value="FACTORY_DIRECT" :label="$t('product.category.FACTORY_DIRECT')" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="$t('product.dialog.status')">
              <el-input v-model="form.status" maxlength="32" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.material')">
              <el-input v-model="form.material" :placeholder="$t('product.dialog.materialPlaceholder')" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.materialJa')">
              <el-input v-model="form.materialJa" :placeholder="$t('product.dialog.materialJaPlaceholder')" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.origin')">
              <el-input v-model="form.origin" :placeholder="$t('product.dialog.originPlaceholder')" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.colorName')">
              <el-input v-model="form.colorName" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.unit')">
              <el-input v-model="form.unit" :placeholder="$t('product.dialog.unitPlaceholder')" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.quantities')">
              <el-input-number v-model="form.quantities" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.cartonQty')">
              <el-input-number v-model="form.cartonQty" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 规格信息 -->
        <div class="dialog-section-title">{{ $t('product.drawer.section.specInfo') }}</div>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.unitPriceRmb')">
              <el-input-number v-model="form.unitPriceRmb" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.taxPoint')">
              <el-input-number v-model="form.taxPoint" :min="0" :precision="4" style="width:100%" :placeholder="$t('product.dialog.taxPointPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.taxRate')">
              <el-input-number v-model="form.taxRate" :min="0" :max="1" :precision="4" style="width:100%" :placeholder="$t('product.dialog.taxRatePlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.hsCode')">
              <el-input v-model="form.hsCode" :placeholder="$t('product.dialog.hsCodePlaceholder')" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.hsCodeJp')">
              <el-input v-model="form.hsCodeJp" :placeholder="$t('product.dialog.hsCodeJpPlaceholder')" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.lengthCm')">
              <el-input-number v-model="form.lengthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.widthCm')">
              <el-input-number v-model="form.widthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.heightCm')">
              <el-input-number v-model="form.heightCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.netWeight')">
              <el-input-number v-model="form.netWeightKg" :min="0" :precision="4" style="width:100%" :placeholder="$t('product.dialog.netWeightPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.grossWeight')">
              <el-input-number v-model="form.grossWeightKg" :min="0" :precision="4" style="width:100%" :placeholder="$t('product.dialog.grossWeightPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="$t('product.dialog.declarationElements')">
          <el-input v-model="form.declarationElements" type="textarea" :rows="2" :placeholder="$t('product.dialog.declarationElementsPlaceholder')" />
        </el-form-item>

        <el-form-item :label="$t('product.drawer.imageUrl')">
          <el-input v-model="form.imageUrl" :placeholder="$t('product.drawer.imageUrl')" maxlength="512" />
        </el-form-item>

        <!-- 仓储信息 -->
        <div class="dialog-section-title">{{ $t('product.drawer.section.warehouseInfo') }}</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.warehouse')">
              <el-input v-model="form.warehouse" :placeholder="$t('product.dialog.warehousePlaceholder')" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.requiresQc')">
              <el-switch v-model="form.requiresQc" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="$t('product.dialog.remarks')">
          <el-input v-model="form.remarks" type="textarea" :rows="2" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('product.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('product.dialog.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Goods, Loading } from '@element-plus/icons-vue'
import { productApi } from '@/api/product'
import type { ProductPageVO, CreateProductRequest, UpdateProductRequest, ProductFactoryVO } from '@/api/product'
import { usePermission } from '@/composables/usePermission'

const { t, locale: localeRef } = useI18n()
const { hasPermission } = usePermission()

function formatTime(ts: string | undefined | null): string {
  if (!ts) return '-'
  return new Date(ts).toLocaleString(localeRef.value === 'ja' ? 'ja-JP' : 'zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

const loading = ref(false)
const submitting = ref(false)
const factoriesLoading = ref(false)
const tableData = ref<ProductPageVO[]>([])
const productFactories = ref<ProductFactoryVO[]>([])
const detailVisible = ref(false)
const formVisible = ref(false)
const currentRow = ref<ProductPageVO | null>(null)
const isEdit = ref(false)
const formRef = ref()

const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const filterForm = reactive({ masterCode: '', keyword: '', hsCode: '', hsCodeJp: '', factoryName: '' })

const defaultForm = (): CreateProductRequest => ({
  masterCode: '',
  subCode: '',
  janCode: '',
  nameZh: '',
  nameEn: '',
  nameJa: '',
  material: '',
  materialJa: '',
  origin: '',
  colorName: '',
  imageUrl: '',
  category: undefined,
  status: '',
  unit: '',
  quantities: undefined,
  cartonQty: undefined,
  lengthCm: undefined,
  widthCm: undefined,
  heightCm: undefined,
  netWeightKg: undefined,
  grossWeightKg: undefined,
  unitPriceRmb: undefined,
  taxPoint: 1.1,
  taxRate: 0.1,
  hsCode: '',
  hsCodeJp: '',
  declarationElements: '',
  warehouse: '',
  requiresQc: false,
  remarks: '',
})

const form = reactive<CreateProductRequest>(defaultForm())

const formRules = {
  masterCode: [{ required: true, message: () => t('product.validation.masterCodeRequired'), trigger: 'blur' }],
  nameZh: [{ required: true, message: () => t('product.validation.nameZhRequired'), trigger: 'blur' }],
}

const formTitle = computed(() => isEdit.value ? t('product.dialog.editTitle') : t('product.dialog.newTitle'))

async function loadData() {
  loading.value = true
  try {
    const res = await productApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      masterCode: filterForm.masterCode || undefined,
      keyword: filterForm.keyword || undefined,
      hsCode: filterForm.hsCode || undefined,
      hsCodeJp: filterForm.hsCodeJp || undefined,
      factoryName: filterForm.factoryName || undefined,
    })
    const data = res.data
    tableData.value = data?.content ?? []
    pagination.total = data?.totalElements ?? 0
  } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.masterCode = ''
  filterForm.keyword = ''
  filterForm.hsCode = ''
  filterForm.hsCodeJp = ''
  filterForm.factoryName = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  formVisible.value = true
}

async function onView(row: ProductPageVO) {
  currentRow.value = row
  detailVisible.value = true
  productFactories.value = []
  factoriesLoading.value = true
  try {
    const res = await productApi.getProductFactories(row.id)
    productFactories.value = res.data || []
  } catch {
    productFactories.value = []
  } finally {
    factoriesLoading.value = false
  }
}

function onEdit(row: ProductPageVO) {
  isEdit.value = true
  currentRow.value = row
  detailVisible.value = false
  Object.assign(form, {
    masterCode: row.masterCode,
    subCode: row.subCode || '',
    janCode: row.janCode || '',
    nameZh: row.nameZh || '',
    nameEn: row.nameEn || '',
    nameJa: row.nameJa || '',
    material: row.material || '',
    materialJa: row.materialJa || '',
    origin: row.origin || '',
    colorName: row.colorName || '',
    imageUrl: row.imageUrl || '',
    category: row.category,
    status: row.status || '',
    unit: row.unit || '',
    quantities: row.quantities,
    cartonQty: row.cartonQty,
    lengthCm: row.lengthCm,
    widthCm: row.widthCm,
    heightCm: row.heightCm,
    netWeightKg: row.netWeightKg,
    grossWeightKg: row.grossWeightKg,
    unitPriceRmb: row.unitPriceRmb,
    taxPoint: row.taxPoint ?? 1.1,
    taxRate: row.taxRate ?? 0.1,
    hsCode: row.hsCode || '',
    hsCodeJp: row.hsCodeJp || '',
    declarationElements: row.declarationElements || '',
    warehouse: row.warehouse || '',
    requiresQc: row.requiresQc ?? false,
    remarks: row.remarks || '',
  })
  formVisible.value = true
}

function onEditFromDrawer() {
  if (currentRow.value) onEdit(currentRow.value)
}

async function onDelete(row: ProductPageVO) {
  await ElMessageBox.confirm(
    t('product.message.deleteConfirm', { code: row.masterCode }),
    t('product.message.deleteConfirmTitle'),
  )
  await productApi.delete(row.id)
  ElMessage.success(t('product.message.deleteSuccess'))
  loadData()
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentRow.value) {
      await productApi.update(currentRow.value.id, form as UpdateProductRequest)
      ElMessage.success(t('product.message.updateSuccess'))
    } else {
      await productApi.create(form)
      ElMessage.success(t('product.message.createSuccess'))
    }
    formVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function onPageChange(page: number) {
  pagination.page = page
  loadData()
}

function dims(row: ProductPageVO) {
  const l = row.lengthCm, w = row.widthCm, h = row.heightCm
  if (l || w || h) return `${l ?? '-'} × ${w ?? '-'} × ${h ?? '-'} cm`
  return '-'
}

function pkgDims(row: ProductPageVO) {
  const l = row.packageLengthCm, w = row.packageWidthCm, h = row.packageHeightCm
  if (l || w || h) return `${l ?? '-'} × ${w ?? '-'} × ${h ?? '-'} cm`
  return '-'
}

loadData()
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.table-card :deep(.el-card__body) { padding: 16px; }
.stats-row { margin-bottom: 4px; }
.drawer-content {
  padding: 0 16px;
}
.drawer-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #1A1A2E;
  margin: 16px 0 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #E8ECF1;
  display: flex;
  align-items: center;
  gap: 8px;
}
.dialog-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #409EFF;
  margin: 12px 0 8px;
  padding-bottom: 4px;
  border-bottom: 1px dashed #d0d7de;
}
.factory-count-badge {
  background: var(--color-primary);
  color: #fff;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
}
.factory-names-text {
  display: inline-block;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: #409EFF;
  cursor: pointer;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 20px;
  padding: 0;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-item.full-width {
  grid-column: 1 / -1;
}
.detail-label {
  font-size: 12px;
  color: #909399;
}
.detail-value {
  font-size: 14px;
  color: #303133;
  word-break: break-all;
}
.factories-loading,
.factories-empty {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 13px;
}
.factories-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.factory-card {
  border: 1px solid #E8ECF1;
  border-radius: 8px;
  padding: 12px;
  background: #FAFBFC;
}
.factory-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.factory-name {
  font-weight: 600;
  font-size: 14px;
  color: #1A1A2E;
  flex: 1;
}
.factory-detail {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 16px;
  font-size: 12px;
  color: #606266;
}
.detail-remarks {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  padding: 4px 0;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.drawer-footer {
  padding: 16px 0 0;
  border-top: 1px solid var(--border-color);
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
.btn-blue { color: #409EFF !important; }
:deep(.el-drawer__body) { overflow-y: auto !important; overflow-x: hidden; }
</style>
