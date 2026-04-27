<template>
  <div class="test-page">
    <div class="page-header">
      <h2 class="page-title">{{ $t('order.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon>
          {{ $t('order.newButton') }}
        </el-button>
      </div>
    </div>

    <!-- 统计行 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Document /></el-icon></div>
            <div>
              <div class="stat-value">{{ pagination.total }}</div>
              <div class="stat-label">{{ $t('order.stat.total') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Clock /></el-icon></div>
            <div>
              <div class="stat-value">{{ activeCount }}</div>
              <div class="stat-label">{{ $t('order.stat.active') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div>
              <div class="stat-value">{{ completedCount }}</div>
              <div class="stat-label">{{ $t('order.stat.completed') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#DC2626"><Warning /></el-icon></div>
            <div>
              <div class="stat-value">{{ returnedCount }}</div>
              <div class="stat-label">{{ $t('order.stat.returned') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('order.filter.productCode')">
          <el-input v-model="filterForm.productCode" :placeholder="$t('order.filter.productCodePlaceholder')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="$t('order.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('order.filter.all')" clearable style="width: 140px">
            <el-option v-for="s in statusOptionsWithI18n" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('order.filter.customerCompany')">
          <el-input v-model="filterForm.customerCompany" :placeholder="$t('order.filter.customerCompanyPlaceholder')" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ $t('order.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('order.filter.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="tableRows"
        stripe
        style="width: 100%"
      >
        <el-table-column :label="$t('order.column.productCode')" min-width="160">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
            <span v-if="row.subProductCode" style="color:#999;font-size:11px"> / {{ row.subProductCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="factoryName" :label="$t('order.column.factoryName')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="quantity" :label="$t('order.column.quantity')" min-width="80" align="right" />
        <el-table-column :label="$t('order.column.estimatedPriceJpy')" min-width="150" align="right">
          <template #default="{ row }">
            {{ row.estimatedPriceJpy ? row.estimatedPriceJpy.toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="customerCompany" :label="$t('order.column.customerCompany')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="productLead" :label="$t('order.column.productLead')" min-width="100" />
        <el-table-column prop="plannedShipDate" :label="$t('order.column.plannedShipDate')" min-width="130" />
        <el-table-column prop="leadTimeDays" :label="$t('order.column.leadTimeDays')" min-width="100" align="center">
          <template #default="{ row }">{{ row.leadTimeDays ? `${row.leadTimeDays}天` : '-' }}</template>
        </el-table-column>
        <el-table-column prop="cartonNotes" :label="$t('order.column.cartonNotes')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="status" :label="$t('order.column.status')" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('order.column.createTime')" min-width="160">
          <template #default="{ row }">
            {{ row.createTime ? new Date(row.createTime).toLocaleString(currentLocale === 'ja' ? 'ja-JP' : 'zh-CN', {year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit'}) : '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.column.action')" min-width="220" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">{{ $t('order.message.detail') }}</el-button>
            <el-button link type="primary" size="small" @click.stop="onEdit(row)"
              :disabled="row.status === COMPLETED_STATUS">{{ $t('demand.action.edit') }}</el-button>
            <el-button link type="warning" size="small" @click.stop="onOverview(row)">{{ $t('orderOverview.action.view') }}</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)"
              :disabled="!deletableStatuses.includes(row.status)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('order.drawerTitle')" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('order.drawer.factory')">{{ currentRow.factoryName || (currentRow.factoryId ? `ID:${currentRow.factoryId}` : '-') }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.productCode')">{{ currentRow.productCode }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.subProductCode')">{{ currentRow.subProductCode || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.quantity')">{{ currentRow.quantity }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.material')">{{ currentRow.material || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.requiresQc')">{{ currentRow.requiresQc ? $t('order.drawer.yes') : $t('order.drawer.no') }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.priceRmb')">{{ currentRow.priceRmb }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.exchangeRate')">{{ currentRow.exchangeRate }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.taxPoint')">{{ currentRow.taxPoint }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.estimatedPriceJpy')">{{ currentRow.estimatedPriceJpy?.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.billingType')">{{ billingTypeLabel(currentRow.billingType) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.customsRemarks')" :span="2">{{ currentRow.customsRemarks || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.instructionManual')" :span="2">{{ currentRow.instructionManual || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.status')">
          <el-tag :type="statusType(currentRow.status)" size="small">
            {{ statusLabel(currentRow.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.customerCompany')">{{ currentRow.customerCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.orderDate')">{{ currentRow.orderDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.factoryShipDate')">{{ currentRow.factoryShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.plannedShipDate')">{{ currentRow.plannedShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.actualShipDate')">{{ currentRow.actualShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.leadTimeDays')">{{ currentRow.leadTimeDays ? `${currentRow.leadTimeDays}天` : '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.cartonNotes')">{{ currentRow.cartonNotes || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.productLead')">{{ currentRow.productLead || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.japanLead')">{{ currentRow.japanLead || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.chinaLead')">{{ currentRow.chinaLead || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.destination')" :span="2">{{ currentRow.destination || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.createTime')" :span="2">{{ currentRow.createTime ? new Date(currentRow.createTime).toLocaleString(currentLocale === 'ja' ? 'ja-JP' : 'zh-CN', {year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit'}) : '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.updateTime')" :span="2">{{ currentRow.updateTime ? new Date(currentRow.updateTime).toLocaleString(currentLocale === 'ja' ? 'ja-JP' : 'zh-CN', {year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit'}) : '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="drawer-actions">
        <el-button @click="drawerVisible = false">{{ $t('order.drawer.close') }}</el-button>
        <el-button type="primary" @click="onEdit(currentRow)">{{ $t('order.drawer.edit') }}</el-button>
      </div>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? $t('order.newDialogTitle') : $t('order.editDialogTitle')" width="900px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="88px">
        <!-- 关联需求（创建时可选） -->
        <el-form-item v-if="dialogMode === 'create'" :label="$t('order.dialog.linkedDemand')">
          <el-select v-model="selectedDemandId" :placeholder="$t('order.dialog.linkedDemandPlaceholder')" clearable filterable style="width:100%" @change="onDemandChange">
            <el-option v-for="d in demandOptions" :key="d.id" :label="`${d.demandCode} | ${d.productCode} | ${d.demandType === 'NEW_PURCHASE' ? $t('demand.type.newPurchase') : $t('demand.type.replenishment')} | ${$t('demand.status.' + d.status as any)}`" :value="d.id" />
          </el-select>
        </el-form-item>

        <!-- 选择工厂（必填） -->
        <el-form-item :label="$t('order.dialog.selectFactory')" prop="factoryId">
          <div class="factory-select-row">
            <el-select v-model="formData.factoryId" :placeholder="$t('order.dialog.selectFactoryPlaceholder')" filterable style="flex:1" :disabled="dialogMode === 'update'" @change="onFactorySelected">
              <el-option v-for="f in factoryOptions" :key="f.id" :label="`${f.factoryName}（${f.factoryCode}）`" :value="f.id" />
            </el-select>
            <el-button size="small" :disabled="dialogMode === 'update'" @click="onFactoryNew">
              <el-icon><Plus /></el-icon> {{ $t('order.dialog.factoryNew') }}
            </el-button>
            <el-tooltip :content="$t('order.dialog.factoryEdit')" placement="top">
              <el-button size="small" :disabled="!formData.factoryId || dialogMode === 'update'" @click="onFactoryEditCurrent">
                <el-icon><Edit /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </el-form-item>

        <!-- 商品信息 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.productCode')" prop="productCode">
              <el-input v-model="formData.productCode" :placeholder="$t('order.dialog.productCodePlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.subProductCode')">
              <el-input v-model="formData.subProductCode" :placeholder="$t('order.dialog.subProductCodePlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.quantity')" prop="quantity">
              <el-input-number v-model="formData.quantity" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.material')">
              <el-input v-model="formData.material" :placeholder="$t('order.dialog.materialPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.requiresQc')">
              <el-switch v-model="formData.requiresQc" :active-text="$t('order.common.yes')" :inactive-text="$t('order.common.no')" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 价格 -->
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.priceRmb')" prop="priceRmb">
              <el-input-number v-model="formData.priceRmb" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.exchangeRate')" prop="exchangeRate">
              <el-input-number v-model="formData.exchangeRate" :min="0.0001" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.taxPoint')" prop="taxPoint">
              <el-input-number v-model="formData.taxPoint" :min="0.0001" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('order.dialog.estimatedPricePreview')">
          <div class="price-preview">
            <span class="price-value">{{ previewPriceJpy }}</span>
            <span class="price-unit">{{ $t('common.units.jpy') }}</span>
            <span class="price-formula">= (RMB ÷ {{ formData.taxPoint }} × 1.02 × 1.2) × {{ formData.exchangeRate }} × 1.05</span>
          </div>
        </el-form-item>
        <!-- 报关类型 -->
        <el-form-item :label="$t('order.dialog.billingType')">
          <el-select v-model="formData.billingType" :placeholder="$t('order.dialog.billingTypePlaceholder')" clearable style="width: 100%">
            <el-option v-for="opt in BILLING_TYPE_OPTIONS" :key="opt.value" :value="opt.value" :label="billingTypeLabel(opt.value)" />
          </el-select>
        </el-form-item>
        <!-- 报关备注 + 说明书 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.customsRemarks')">
              <el-input v-model="formData.customsRemarks" :placeholder="$t('order.dialog.customsRemarksPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.instructionManual')">
              <el-input v-model="formData.instructionManual" :placeholder="$t('order.dialog.instructionManualPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 客户公司 + 发送目的地 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.customerCompany')">
              <el-input v-model="formData.customerCompany" :placeholder="$t('order.dialog.customerCompanyPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.destination')">
              <el-input v-model="formData.destination" :placeholder="$t('order.dialog.destinationPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 担当 -->
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.productLead')">
              <el-input v-model="formData.productLead" :placeholder="$t('order.dialog.productLeadPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.japanLead')">
              <el-input v-model="formData.japanLead" :placeholder="$t('order.dialog.japanLeadPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.chinaLead')">
              <el-input v-model="formData.chinaLead" :placeholder="$t('order.dialog.chinaLeadPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 日期 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.orderDate')">
              <el-date-picker v-model="formData.orderDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.factoryShipDate')">
              <el-date-picker v-model="formData.factoryShipDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.plannedShipDate')">
              <el-date-picker v-model="formData.plannedShipDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.actualShipDate')">
              <el-date-picker v-model="formData.actualShipDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.leadTimeDays')">
              <el-select v-model="formData.leadTimeDays" style="width: 100%" clearable>
                <el-option :value="30" label="30天" />
                <el-option :value="45" label="45天" />
                <el-option :value="60" label="60天" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('order.dialog.cartonNotes')">
              <el-input v-model="formData.cartonNotes" :placeholder="$t('order.dialog.cartonNotesPlaceholder')" maxlength="512" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 状态（仅更新模式） -->
        <el-form-item v-if="dialogMode === 'update'" :label="$t('order.dialog.status')" prop="status">
          <el-select v-model="formData.status" style="width: 100%">
            <el-option v-for="s in statusOptionsWithI18n" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('order.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('order.dialog.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 工厂新建/编辑弹窗 -->
    <el-dialog v-model="factoryDialogVisible" :title="factoryDialogMode === 'create' ? $t('order.factoryDialog.newTitle') : $t('order.factoryDialog.editTitle')" width="640px">
      <el-form ref="factoryFormRef" :model="factoryFormData" :rules="factoryFormRules" label-width="110px">
        <el-form-item :label="$t('order.factoryDialog.factoryName')" prop="factoryName">
          <el-input v-model="factoryFormData.factoryName" :placeholder="$t('order.factoryDialog.factoryNamePlaceholder')" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item :label="$t('order.factoryDialog.province')">
              <el-input v-model="factoryFormData.province" :placeholder="$t('order.factoryDialog.provincePlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('order.factoryDialog.city')">
              <el-input v-model="factoryFormData.city" :placeholder="$t('order.factoryDialog.cityPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('order.factoryDialog.county')">
              <el-input v-model="factoryFormData.county" :placeholder="$t('order.factoryDialog.countyPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('order.factoryDialog.roughLocation')">
          <el-input v-model="factoryFormData.roughLocation" :placeholder="$t('order.factoryDialog.roughLocationPlaceholder')" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item :label="$t('order.factoryDialog.contactName')">
              <el-input v-model="factoryFormData.contactName" :placeholder="$t('order.factoryDialog.contactNamePlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('order.factoryDialog.contactPhone')">
              <el-input v-model="factoryFormData.contactPhone" :placeholder="$t('order.factoryDialog.contactPhonePlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item :label="$t('order.factoryDialog.wechat')">
              <el-input v-model="factoryFormData.contactWechat" :placeholder="$t('order.factoryDialog.wechatPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('order.factoryDialog.qq')">
              <el-input v-model="factoryFormData.contactQq" :placeholder="$t('order.factoryDialog.qqPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="factoryDialogMode === 'update'" :label="$t('order.factoryDialog.cooperationStatus')">
          <el-select v-model="factoryFormData.cooperationStatus" style="width:100%" clearable :placeholder="$t('order.filter.all')">
            <el-option value="ACTIVE" :label="$t('order.cooperationStatus.ACTIVE')" />
            <el-option value="SUSPENDED" :label="$t('order.cooperationStatus.SUSPENDED')" />
            <el-option value="ELIMINATED" :label="$t('order.cooperationStatus.ELIMINATED')" />
            <el-option value="POTENTIAL" :label="$t('order.cooperationStatus.POTENTIAL')" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="factoryDialogVisible = false">{{ $t('order.factoryDialog.cancel') }}</el-button>
        <el-button type="primary" :loading="factorySubmitting" @click="onFactorySubmit">{{ $t('order.factoryDialog.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Plus, Clock, CircleCheck, Warning, Document, Edit } from '@element-plus/icons-vue'
import { procurementApi, type ProcurementPageVO, type CreateProcurementRequest, type UpdateProcurementRequest, BILLING_TYPE_OPTIONS } from '@/api/procurement'
import { factoryApi, type FactoryPageVO, type CreateFactoryRequest, type UpdateFactoryRequest } from '@/api/factory'
import { demandApi, type DemandPageVO } from '@/api/demand'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const submitting = ref(false)
const drawerVisible = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const currentRow = ref<ProcurementPageVO | null>(null)
const formRef = ref<FormInstance>()

// 转采购模式：记录当前需求ID，提交后调用 convert API
const convertingDemandId = ref<number | null>(null)
const route = useRoute()
const router = useRouter()
const { t, locale: localeRef } = useI18n()
const currentLocale = computed(() => localeRef.value)

const COMPLETED_STATUS = '完了'
const RETURNED_STATUS = '退货'
const deletableStatuses = ['未定', '発注待']

const ORDER_STATUSES = [
  '未定', '予定', 'OEM', '発注待', '永康', '直送', '倉庫着', '検品', '現地検品',
  'エア便', 'メーカー直送', '輸出', '国内通関', '通関', '日本着', '日本通関完了', '会計', COMPLETED_STATUS, RETURNED_STATUS,
]

const statusOptionsWithI18n = computed(() =>
  ORDER_STATUSES.map(value => ({ value, label: statusLabel(value) })),
)

const filterForm = reactive({
  productCode: '',
  status: '',
  customerCompany: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const tableRows = ref<ProcurementPageVO[]>([])

// 工厂下拉
const factoryOptions = ref<FactoryPageVO[]>([])
async function loadFactories() {
  try {
    const res = await factoryApi.list({ page: 0, pageSize: 200 })
    factoryOptions.value = (res.data.data as { content: FactoryPageVO[] })?.content ?? []
  } catch { /* handled by interceptor */ }
}

// 需求下拉（仅 PENDING）
const demandOptions = ref<DemandPageVO[]>([])
const selectedDemandId = ref<number | null>(null)
async function loadDemands() {
  try {
    const res = await demandApi.list({ page: 0, pageSize: 200, status: 'PENDING' })
    demandOptions.value = (res.data.data as { content: DemandPageVO[] })?.content ?? []
  } catch { /* handled by interceptor */ }
}

/** 选中需求 → 自动带入 productCode / subProductCode / destination / japanLead / quantity */
function onDemandChange(demandId: number | null) {
  if (!demandId) return
  const d = demandOptions.value.find(x => x.id === demandId)
  if (!d) return
  formData.productCode = d.productCode
  formData.subProductCode = d.subProductCode || ''
  formData.destination = d.destination || ''
  formData.japanLead = d.japanLead || ''
  formData.quantity = d.quantity ?? 0
}

/**
 * 转采购入口：从需求单带入数据并打开发注单弹窗。
 * DemandPage 点击"转采购"时通过 defineExpose 调用此方法。
 */
function prefillFromDemand(demand: DemandPageVO) {
  convertingDemandId.value = demand.id
  dialogMode.value = 'create'
  Object.assign(formData, defaultFormData())
  formData.productCode = demand.productCode
  formData.subProductCode = demand.subProductCode || ''
  formData.destination = demand.destination || ''
  formData.japanLead = demand.japanLead || ''
  formData.quantity = demand.quantity ?? 0
  dialogVisible.value = true
}

// ===== 工厂管理 =====
const factoryDialogVisible = ref(false)
const factoryDialogMode = ref<'create' | 'update'>('create')
const factoryCurrentRow = ref<FactoryPageVO | null>(null)
const factoryFormRef = ref<FormInstance>()
const factorySubmitting = ref(false)

const defaultFactoryForm = (): CreateFactoryRequest => ({
  factoryName: '',
  province: '',
  city: '',
  county: '',
  roughLocation: '',
  contactName: '',
  contactPhone: '',
  contactWechat: '',
  contactQq: '',
  cooperationStatus: undefined,
  paymentTerms: undefined,
  notes: '',
})

const factoryFormData = reactive<CreateFactoryRequest>(defaultFactoryForm())
const factoryFormRules = {
  factoryName: [{ required: true, message: () => t('order.validation.factoryNameRequired'), trigger: 'blur' }],
}

function onFactoryNew() {
  factoryDialogMode.value = 'create'
  Object.assign(factoryFormData, defaultFactoryForm())
  factoryDialogVisible.value = true
}

/** 选中工厂变更 — 需求单与工厂完全独立，互不影响 */
function onFactorySelected(_id: number | null) {
  // no-op：工厂选择不干扰需求单选择
}

/** 编辑当前选中的工厂 */
function onFactoryEditCurrent() {
  if (!formData.factoryId) return
  const factory = factoryOptions.value.find(f => f.id === formData.factoryId)
  if (!factory) return
  onFactoryEdit(factory)
}

function onFactoryEdit(row: FactoryPageVO) {
  factoryDialogMode.value = 'update'
  factoryCurrentRow.value = row
  Object.assign(factoryFormData, {
    factoryName: row.factoryName,
    province: row.province || '',
    city: row.city || '',
    county: row.county || '',
    roughLocation: row.roughLocation || '',
    contactName: row.contactName || '',
    contactPhone: row.contactPhone || '',
    contactWechat: row.contactWechat || '',
    contactQq: row.contactQq || '',
    cooperationStatus: row.cooperationStatus,
    paymentTerms: row.paymentTerms,
    notes: row.notes || '',
  })
  factoryDialogVisible.value = true
}

async function onFactorySubmit() {
  if (!factoryFormRef.value) return
  await factoryFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    factorySubmitting.value = true
    try {
      if (factoryDialogMode.value === 'create') {
        await factoryApi.create(factoryFormData as CreateFactoryRequest)
        ElMessage.success(t('order.message.factoryCreateSuccess'))
      } else if (factoryCurrentRow.value) {
        await factoryApi.update(factoryCurrentRow.value.id, factoryFormData as UpdateFactoryRequest)
        ElMessage.success(t('order.message.updateSuccess'))
      }
      factoryDialogVisible.value = false
      await loadFactories()
    } finally {
      factorySubmitting.value = false
    }
  })
}

const activeCount = computed(() =>
  tableRows.value.filter(r => r.status !== COMPLETED_STATUS && r.status !== RETURNED_STATUS).length,
)
const completedCount = computed(() =>
  tableRows.value.filter(r => r.status === COMPLETED_STATUS).length,
)
const returnedCount = computed(() =>
  tableRows.value.filter(r => r.status === RETURNED_STATUS).length,
)

const previewPriceJpy = computed(() => {
  const { priceRmb, taxPoint, exchangeRate } = formData
  if (!priceRmb || !taxPoint || !exchangeRate) return '—'
  const base = (priceRmb / taxPoint * 1.02 * 1.2) * exchangeRate * 1.05
  return Math.round(base * 100) / 100
})

const defaultFormData = (): CreateProcurementRequest & { status?: string } => ({
  factoryId: undefined,
  productCode: '',
  subProductCode: '',
  material: '',
  requiresQc: false,
  quantity: 1,
  priceRmb: 0,
  exchangeRate: 21.5,
  taxPoint: 1.1,
  billingType: undefined,
  customsRemarks: '',
  instructionManual: '',
  actualShipDate: '',
  orderDate: '',
  factoryShipDate: '',
  plannedShipDate: '',
  leadTimeDays: undefined as number | undefined,
  cartonNotes: '',
  customerCompany: '',
  productLead: '',
  japanLead: '',
  chinaLead: '',
  destination: '',
  status: '未定',
})

const formData = reactive<CreateProcurementRequest & { status?: string }>(defaultFormData())

const formRules = {
  factoryId: [{ required: true, message: () => t('order.validation.factoryRequired'), trigger: 'change' }],
  productCode: [
    { required: true, message: () => t('order.validation.productCodeRequired'), trigger: 'blur' },
    { max: 32, message: () => t('order.validation.productCodeMaxLength'), trigger: 'blur' },
  ],
  quantity: [
    { required: true, message: () => t('order.validation.quantityRequired'), trigger: 'blur' },
    { type: 'number', min: 1, message: () => t('order.validation.quantityPositive'), trigger: 'blur' },
  ],
  priceRmb: [
    { required: true, message: () => t('order.validation.priceRmbRequired'), trigger: 'blur' },
    { type: 'number', min: 0, message: () => t('order.validation.priceRmbNonNegative'), trigger: 'blur' },
  ],
  exchangeRate: [
    { required: true, message: () => t('order.validation.exchangeRateRequired'), trigger: 'blur' },
    { type: 'number', min: 0.0001, message: () => t('order.validation.exchangeRatePositive'), trigger: 'blur' },
  ],
  taxPoint: [
    { required: true, message: () => t('order.validation.taxPointRequired'), trigger: 'blur' },
    { type: 'number', min: 0.0001, message: () => t('order.validation.taxPointPositive'), trigger: 'blur' },
  ],
  customerCompany: [{ max: 128, message: () => t('order.validation.customerCompanyMaxLength'), trigger: 'blur' }],
  destination: [{ max: 128, message: () => t('order.validation.destinationMaxLength'), trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await procurementApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      status: filterForm.status || undefined,
      productCode: filterForm.productCode.trim() || undefined,
      customerCompany: filterForm.customerCompany.trim() || undefined,
    })
    const payload = res.data.data as { content: ProcurementPageVO[]; totalElements: number }
    tableRows.value = payload?.content ?? []
    pagination.total = payload?.totalElements ?? 0
    // 删除后若当前页越界，回退到第1页
    if (tableRows.value.length === 0 && pagination.total > 0 && pagination.page > 1) {
      pagination.page = 1
      loadData()
    }
  } catch {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.productCode = ''
  filterForm.status = ''
  filterForm.customerCompany = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  dialogMode.value = 'create'
  selectedDemandId.value = null
  Object.assign(formData, defaultFormData())
  dialogVisible.value = true
}

function onView(row: ProcurementPageVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function onOverview(row: ProcurementPageVO) {
  router.push('/base/overview/' + row.id)
}

function onEdit(row: ProcurementPageVO | null) {
  dialogMode.value = 'update'
  currentRow.value = row
  selectedDemandId.value = null
  Object.assign(formData, {
    factoryId: row?.factoryId ?? undefined,
    productCode: row?.productCode ?? '',
    subProductCode: row?.subProductCode ?? '',
    material: row?.material ?? '',
    requiresQc: row?.requiresQc ?? false,
    quantity: row?.quantity ?? 1,
    priceRmb: row?.priceRmb ?? 0,
    exchangeRate: row?.exchangeRate ?? 21.5,
    taxPoint: row?.taxPoint ?? 1.1,
    billingType: row?.billingType ?? undefined,
    customsRemarks: row?.customsRemarks ?? '',
    instructionManual: row?.instructionManual ?? '',
    actualShipDate: row?.actualShipDate ?? '',
    orderDate: row?.orderDate ?? '',
    factoryShipDate: row?.factoryShipDate ?? '',
    plannedShipDate: row?.plannedShipDate ?? '',
    leadTimeDays: row?.leadTimeDays ?? undefined,
    cartonNotes: row?.cartonNotes ?? '',
    customerCompany: row?.customerCompany ?? '',
    productLead: row?.productLead ?? '',
    japanLead: row?.japanLead ?? '',
    chinaLead: row?.chinaLead ?? '',
    destination: row?.destination ?? '',
    status: row?.status ?? '未定',
  })
  dialogVisible.value = true
}

async function onDelete(row: ProcurementPageVO) {
  try {
    await ElMessageBox.confirm(
      t('order.message.deleteConfirm', { productCode: row.productCode, quantity: row.quantity }),
      t('order.message.deleteConfirmTitle'),
      { confirmButtonText: t('common.delete'), cancelButtonText: t('common.cancel'), type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await procurementApi.delete(row.id)
    ElMessage.success(t('order.message.deleteSuccess'))
    drawerVisible.value = false
    currentRow.value = null
    loadData()
  } catch {
    // error handled by axios interceptor
  }
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      if (dialogMode.value === 'create') {
        const req: CreateProcurementRequest = {
          factoryId: formData.factoryId || undefined,
          productCode: formData.productCode,
          subProductCode: formData.subProductCode || undefined,
          material: formData.material || undefined,
          requiresQc: formData.requiresQc || undefined,
          quantity: formData.quantity,
          priceRmb: formData.priceRmb,
          exchangeRate: formData.exchangeRate,
          taxPoint: formData.taxPoint,
          billingType: formData.billingType || undefined,
          customsRemarks: formData.customsRemarks || undefined,
          instructionManual: formData.instructionManual || undefined,
          actualShipDate: formData.actualShipDate || undefined,
          orderDate: formData.orderDate || undefined,
          factoryShipDate: formData.factoryShipDate || undefined,
          plannedShipDate: formData.plannedShipDate || undefined,
          leadTimeDays: formData.leadTimeDays ?? undefined,
          cartonNotes: formData.cartonNotes || undefined,
          customerCompany: formData.customerCompany || undefined,
          productLead: formData.productLead || undefined,
          japanLead: formData.japanLead || undefined,
          chinaLead: formData.chinaLead || undefined,
          destination: formData.destination || undefined,
          status: formData.status || undefined,
        }
        await procurementApi.create(req)
        const savedMsg = convertingDemandId.value !== null ? t('order.message.createSuccessConverting') : t('order.message.createSuccess')
        ElMessage.success(savedMsg)
        // 若为转采购模式，则关联需求单
        if (convertingDemandId.value !== null) {
          await demandApi.convertToProcurement(convertingDemandId.value, { factoryId: req.factoryId as number })
          ElMessage.success(t('order.message.demandConverted'))
          convertingDemandId.value = null
        }
      } else if (currentRow.value) {
        const req: UpdateProcurementRequest = {
          factoryId: formData.factoryId || undefined,
          productCode: formData.productCode,
          subProductCode: formData.subProductCode || undefined,
          material: formData.material || undefined,
          requiresQc: formData.requiresQc || undefined,
          quantity: formData.quantity,
          priceRmb: formData.priceRmb,
          exchangeRate: formData.exchangeRate,
          taxPoint: formData.taxPoint,
          billingType: formData.billingType || undefined,
          customsRemarks: formData.customsRemarks || undefined,
          instructionManual: formData.instructionManual || undefined,
          actualShipDate: formData.actualShipDate || undefined,
          orderDate: formData.orderDate || undefined,
          factoryShipDate: formData.factoryShipDate || undefined,
          plannedShipDate: formData.plannedShipDate || undefined,
          leadTimeDays: formData.leadTimeDays ?? undefined,
          cartonNotes: formData.cartonNotes || undefined,
          customerCompany: formData.customerCompany || undefined,
          productLead: formData.productLead || undefined,
          japanLead: formData.japanLead || undefined,
          chinaLead: formData.chinaLead || undefined,
          destination: formData.destination || undefined,
          status: formData.status || undefined,
        }
        const updatedId = currentRow.value.id
        await procurementApi.update(updatedId, req)
        ElMessage.success(t('order.message.updateSuccess'))
        // 同步更新表格当前行，避免整体刷新
        const idx = tableRows.value.findIndex(r => r.id === updatedId)
        if (idx !== -1) {
          const { data } = await procurementApi.get(updatedId)
          tableRows.value[idx] = data.data as ProcurementPageVO
        }
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

function statusLabel(status: string): string {
  return t(`order.status.${status}` as any, { default: status })
}

function statusType(status: string): string {
  const statusTypeMap: Record<string, string> = {
    '未定': 'info',
    '発注待': 'warning',
    '永康': 'warning',
    '直送': 'warning',
    '倉庫着': 'primary',
    '現地検品': 'primary',
    '検品': 'primary',
    'エア便': 'success',
    'メーカー直送': 'success',
    '輸出': 'success',
    '国内通関': 'success',
    '通関': 'success',
    '日本着': 'success',
    '日本通関完了': 'success',
    '会計': 'warning',
    '完了': 'info',
    '退货': 'danger',
  }
  return statusTypeMap[status] ?? 'info'
}

function billingTypeLabel(val: string | undefined): string {
  return val ? t(`order.billingType.${val}` as any, { default: val }) : '—'
}

onMounted(() => {
  loadData()
  loadFactories()
  loadDemands()
  // 处理来自 DemandPage "转采购" 的 query params
  if (route.query.demandId) {
    convertingDemandId.value = Number(route.query.demandId)
    dialogMode.value = 'create'
    Object.assign(formData, defaultFormData())
    formData.productCode = (route.query.productCode as string) || ''
    formData.subProductCode = (route.query.subProductCode as string) || ''
    formData.destination = (route.query.destination as string) || ''
    formData.japanLead = (route.query.japanLead as string) || ''
    formData.quantity = Number(route.query.quantity) || 1
    dialogVisible.value = true
    router.replace({ path: '/procurement/order' })
  }
})

defineExpose({ prefillFromDemand })
</script>

<style scoped>
.test-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.3px;
}
.page-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 20px;
  background: var(--color-primary);
  border-radius: 2px;
  margin-right: 10px;
  vertical-align: middle;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.stats-row {
  margin-bottom: 4px;
}

/* ── 统计卡 ── */
.stat-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-card);
  transition: all var(--transition-fast);
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light));
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}
.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: var(--color-primary-pale);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--color-primary-pale);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-icon {
  font-size: 22px;
}

.stat-value {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
  font-weight: 500;
}

.table-card :deep(.el-table__row) {
  cursor: pointer;
}

/* ── 商品代码：橙色 monospace 标签 ── */
.product-code {
  color: var(--color-primary);
  font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace;
  font-size: 12px;
  font-weight: 700;
  background: var(--color-primary-pale);
  padding: 3px 9px;
  border-radius: 5px;
  border: 1px solid rgba(232,101,10,0.2);
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* ── 价格预览 ── */
.price-preview {
  display: flex;
  align-items: baseline;
  gap: 6px;
  color: var(--text-primary);
  line-height: 1;
  background: var(--color-primary-pale);
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(232,101,10,0.15);
}

.price-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}

.price-unit {
  font-size: 13px;
  color: var(--color-primary-dark);
  font-weight: 600;
}

.price-formula {
  font-size: 11px;
  color: var(--text-muted);
  margin-left: 4px;
}

/* ── 抽屉底部按钮区 ── */
.drawer-actions {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  background: #fff;
}

/* ── 工厂选择行 ── */
.factory-select-row {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}
</style>
