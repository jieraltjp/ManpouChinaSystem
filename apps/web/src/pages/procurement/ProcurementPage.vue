<template>
  <div class="test-page">
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
        <el-form-item :label="$t('order.filter.productType')">
          <el-select v-model="filterForm.productType" :placeholder="$t('order.filter.all')" clearable style="width: 130px">
            <el-option v-for="opt in productTypeOptions" :key="opt.value" :label="$t(opt.labelKey)" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ $t('order.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('order.filter.reset') }}</el-button>
          <el-button type="primary" @click="onNew" v-if="hasPermission('procurement:create')">
            <el-icon><Plus /></el-icon>{{ $t('order.newButton') }}
          </el-button>
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
        <el-table-column :label="$t('order.column.productCode')" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.column.subProductCode')" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="product-code">{{ row.subProductCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.column.category')" min-width="100" align="center">
          <template #default="{ row }">
            <span>{{ getCategoryLabel(row.productCode) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="factoryName" :label="$t('order.column.factoryName')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="$t('order.column.priceRmb')" min-width="90" align="right">
          <template #default="{ row }">
            {{ row.priceRmb != null ? row.priceRmb.toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.column.exchangeRate')" min-width="80" align="center">
          <template #default="{ row }">
            {{ row.exchangeRate != null ? row.exchangeRate : '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.column.taxPoint')" min-width="80" align="center">
          <template #default="{ row }">
            {{ row.taxPoint != null ? row.taxPoint : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" :label="$t('order.column.quantity')" min-width="80" align="right" />
        <el-table-column :label="$t('order.column.shipmentQuantity')" min-width="80" align="right">
          <template #default="{ row }">
            {{ row.shipmentQuantity != null && row.shipmentQuantity > 0 ? row.shipmentQuantity : '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.column.estimatedPriceJpy')" min-width="100" align="right">
          <template #default="{ row }">
            {{ row.estimatedPriceJpy ? row.estimatedPriceJpy.toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="leadTimeDays" :label="$t('order.column.leadTimeDays')" min-width="100" align="center">
          <template #default="{ row }">{{ formatLeadTime(row.leadTimeDays) }}</template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('order.column.status')" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="hasPermission('procurement:update')"
              :type="statusType(row)"
              size="small"
              :disable-transitions="false"
              class="status-toggle"
              :class="{ 'is-link': !(row.batchCount ?? 0) }"
              @click.stop="onToggleStatus(row)"
            >
              {{ statusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.column.action')" min-width="300" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('order.message.detail') }}</el-button>
            <el-button link type="success" size="small" @click.stop="onOpenShipmentBatches(row)">{{ $t('order.action.shipmentBatch') }}</el-button>
            <el-button link type="warning" size="small" @click.stop="onEdit(row)"
              :disabled="row.status === ORDER_STATUS_SHIPPED" v-if="hasPermission('procurement:update')">{{ $t('demand.action.edit') }}</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)"
              :disabled="!deletableStatuses.includes(row.status)" v-if="hasPermission('procurement:delete')">{{ $t('common.delete') }}</el-button>
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
        <el-descriptions-item :label="$t('order.drawer.category')">{{ getCategoryLabel(currentRow.productCode) }}</el-descriptions-item>
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
          <el-tag :type="statusType(currentRow)" size="small">
            {{ statusLabel(currentRow) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.customerCompany')">{{ currentRow.customerCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.orderDate')">{{ currentRow.orderDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.factoryShipDate')">{{ currentRow.factoryShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.plannedShipDate')">{{ currentRow.plannedShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.actualShipDate')">{{ currentRow.actualShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.leadTimeDays')">{{ formatLeadTime(currentRow.leadTimeDays) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.cartonNotes')">{{ currentRow.cartonNotes || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.afterSalesDeadline')">{{ currentRow.afterSalesDeadline || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.productLead')">{{ currentRow.productLead || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.japanLead')">{{ currentRow.japanLead || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.chinaLead')">{{ currentRow.chinaLead || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.destination')" :span="2">{{ currentRow.destination || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.createBy')">{{ currentRow.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.createTime')">{{ currentRow.createTime ? new Date(currentRow.createTime).toLocaleString(localeRef === 'ja' ? 'ja-JP' : 'zh-CN', {year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit'}) : '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.updateBy')">{{ currentRow.updateBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.drawer.updateTime')">{{ currentRow.updateTime ? new Date(currentRow.updateTime).toLocaleString(localeRef === 'ja' ? 'ja-JP' : 'zh-CN', {year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit'}) : '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="drawer-actions">
        <el-button @click="drawerVisible = false">{{ $t('order.drawer.close') }}</el-button>
        <el-button v-if="hasPermission('procurement:update')" type="primary" @click="onEdit(currentRow)">{{ $t('order.drawer.edit') }}</el-button>
      </div>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? $t('order.newDialogTitle') : $t('order.editDialogTitle')" width="900px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="88px">
        <!-- 商品类型（创建时可选） -->
        <el-form-item v-if="dialogMode === 'create'" :label="$t('order.dialog.productType')">
          <el-radio-group v-model="productType" size="default">
            <el-radio-button v-for="opt in productTypeOptions" :key="opt.value" :value="opt.value">
              {{ $t(opt.labelKey) }}
            </el-radio-button>
          </el-radio-group>
          <div v-if="!isNormalProcurement" class="form-tip">
            {{ $t('order.dialog.productTypeTip') }}
          </div>
        </el-form-item>

        <!-- 关联需求（仅普通采购显示） -->
        <el-form-item v-if="dialogMode === 'create' && isNormalProcurement" :label="$t('order.dialog.linkedDemand')">
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
          <el-col :span="7">
            <el-form-item :label="$t('order.dialog.productCode')" prop="productCode">
              <el-select
                v-model="formData.productCode"
                filterable
                remote
                reserve-keyword
                :remote-method="productCodeRemoteSearch"
                :loading="productCodeLoading"
                :placeholder="$t('order.dialog.productCodeSearchPlaceholder')"
                style="width: 100%"
                @change="onProductCodeSelect"
              >
                <el-option
                  v-for="item in productCodeSearchResults"
                  :key="item.masterCode"
                  :label="`${item.masterCode}  ${item.nameZh || ''}`"
                  :value="item.masterCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item :label="$t('order.dialog.subProductCode')">
              <div class="factory-select-row">
                <el-select
                  v-model="formData.subProductCode"
                  filterable
                  remote
                  reserve-keyword
                  :remote-method="subCodeRemoteSearch"
                  :loading="subCodeLoading"
                  :placeholder="$t('order.dialog.subProductCodePlaceholder')"
                  style="flex:1"
                  :disabled="!formData.productCode"
                  @change="onSubCodeSelect"
                >
                  <el-option
                    v-for="item in subCodeOptions"
                    :key="item.subCode"
                    :label="`${item.subCode}${item.colorName ? ' — ' + item.colorName : ''}`"
                    :value="item.subCode"
                  />
                </el-select>
                <el-button size="small" @click="onSubCodeNew">
                  <el-icon><Plus /></el-icon>{{ $t('order.dialog.subCodeNew') }}
                </el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item :label="$t('order.dialog.category')">
              <el-input v-model="formData.category" :placeholder="$t('order.dialog.categoryPlaceholder')" readonly />
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
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.leadTimeDays')">
              <el-select v-model="formData.leadTimeDays" style="width: 100%" clearable>
                <el-option v-for="opt in leadTimeOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.cartonNotes')">
              <el-input v-model="formData.cartonNotes" :placeholder="$t('order.dialog.cartonNotesPlaceholder')" maxlength="512" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('order.dialog.afterSalesDeadline')">
              <el-date-picker v-model="formData.afterSalesDeadline" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
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

    <!-- 商品快速创建弹窗（发注单中新建商品/子货号，入口统一） -->
    <el-dialog
      v-model="productCreateDialogVisible"
      :title="$t('order.productCreateDialog.title')"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="productCreateFormRef" :model="productCreateForm" :rules="productCreateRules" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.masterCode')" prop="masterCode">
              <el-input
                v-model="productCreateForm.masterCode"
                :placeholder="$t('order.productCreateDialog.masterCodePlaceholder')"
                maxlength="32"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.subCode')">
              <el-input
                v-model="productCreateForm.subCode"
                :placeholder="$t('order.productCreateDialog.subCodePlaceholder')"
                maxlength="64"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('product.dialog.nameZh')" prop="nameZh">
          <el-input v-model="productCreateForm.nameZh" :placeholder="$t('product.dialog.nameZhPlaceholder')" maxlength="255" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.category')">
              <el-select v-model="productCreateForm.category" clearable style="width:100%">
                <el-option value="OEM" :label="$t('product.category.OEM')" />
                <el-option value="ORDINARY" :label="$t('product.category.ORDINARY')" />
                <el-option value="FACTORY_DIRECT" :label="$t('product.category.FACTORY_DIRECT')" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.material')">
              <el-input v-model="productCreateForm.material" :placeholder="$t('product.dialog.materialPlaceholder')" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('product.dialog.requiresQc')">
          <el-switch v-model="productCreateForm.requiresQc" />
        </el-form-item>
      </el-form>
      <div class="product-create-tip">{{ $t('order.productCreateDialog.tip') }}</div>
      <template #footer>
        <el-button @click="productCreateDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="productCreateSubmitting" @click="onProductCreateSubmit">{{ $t('order.productCreateDialog.createAndContinue') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { usePermission } from '@/composables/usePermission'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Plus, Clock, CircleCheck, Warning, Document, Edit } from '@element-plus/icons-vue'
import { procurementApi, type ProcurementPageVO, type CreateProcurementRequest, type UpdateProcurementRequest, BILLING_TYPE_OPTIONS } from '@/api/procurement'
import { factoryApi, type FactoryPageVO, type CreateFactoryRequest, type UpdateFactoryRequest } from '@/api/factory'
import { demandApi, type DemandPageVO } from '@/api/demand'
import { productApi, type MasterCodeSuggestVO, type CreateProductRequest } from '@/api/product'
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

/** 商品类型（发注单维度） */
export type ProductType = 'NORMAL' | 'SAMPLE' | 'SELF_USE' | 'PARTS' | 'INDEPENDENT'
const productTypeOptions: { value: ProductType; labelKey: string }[] = [
  { value: 'NORMAL', labelKey: 'order.productType.normal' },
  { value: 'SAMPLE', labelKey: 'order.productType.sample' },
  { value: 'SELF_USE', labelKey: 'order.productType.selfUse' },
  { value: 'PARTS', labelKey: 'order.productType.parts' },
  { value: 'INDEPENDENT', labelKey: 'order.productType.independent' },
]
const productType = ref<ProductType>('NORMAL')

/** 商品货号搜索下拉 */
const productCodeSearchResults = ref<MasterCodeSuggestVO[]>([])
const productCodeLoading = ref(false)

/** 快速新建商品弹窗 */
const productCreateDialogVisible = ref(false)
const productCreateFormRef = ref<FormInstance>()
const productCreateSubmitting = ref(false)
const productCreateForm = reactive({
  masterCode: '',
  subCode: '',
  nameZh: '',
  category: '' as 'OEM' | 'ORDINARY' | 'FACTORY_DIRECT' | '',
  material: '',
  requiresQc: false,
})
const productCreateRules = {
  nameZh: [{ required: true, message: () => t('product.validation.nameZhRequired'), trigger: 'blur' }],
}

/** pendingCreateProcurement: 商品新建成功后待提交的采购请求数据 */
let pendingCreateReq: CreateProcurementRequest | null = null

/** 子货号搜索下拉 */
const subCodeOptions = ref<{ subCode: string; colorName?: string }[]>([])
const subCodeLoading = ref(false)

async function subCodeRemoteSearch(query: string) {
  if (!formData.productCode) {
    subCodeOptions.value = []
    return
  }
  if (!query) {
    // 空查询时也加载全部子货号
    query = ''
  }
  subCodeLoading.value = true
  try {
    const res = await productApi.suggestSubCodes(formData.productCode)
    subCodeOptions.value = res.data ?? []
  } catch {
    subCodeOptions.value = []
  } finally {
    subCodeLoading.value = false
  }
}

function onSubCodeSelect(subCode: string) {
  if (!subCode) return
  const found = subCodeOptions.value.find(s => s.subCode === subCode)
  if (found?.colorName) {
    // auto-fill colorName if available (future use)
  }
}

/** 新建子货号 → 打开统一商品创建弹窗（带 masterCode 预填） */
function onSubCodeNew() {
  productCreateForm.masterCode = formData.productCode
  productCreateForm.subCode = ''
  productCreateForm.nameZh = ''
  productCreateForm.category = '' as 'OEM' | 'ORDINARY' | 'FACTORY_DIRECT' | ''
  productCreateForm.material = ''
  productCreateForm.requiresQc = false
  productCreateDialogVisible.value = true
}


async function productCodeRemoteSearch(query: string) {
  if (!query || query.length < 1) {
    productCodeSearchResults.value = []
    return
  }
  productCodeLoading.value = true
  try {
    const res = await productApi.suggestMasterCodes(query)
    productCodeSearchResults.value = res.data ?? []
  } catch {
    productCodeSearchResults.value = []
  } finally {
    productCodeLoading.value = false
  }
}

function onProductCodeSelect(masterCode: string) {
  if (!masterCode) return
  // 清空子货号
  formData.subProductCode = ''
  subCodeOptions.value = []
  // 加载 subCode 候选项
  subCodeRemoteSearch('')
  productApi.getByCode(masterCode).then(res => {
    const p = res.data
    if (!p) return
    if (p.category) {
      productCategoryMap.value = { ...productCategoryMap.value, [masterCode]: p.category }
      formData.category = getCategoryLabel(masterCode)
    }
    if (p.material) formData.material = p.material
    if (p.requiresQc != null) formData.requiresQc = p.requiresQc
    if (p.unitPriceRmb != null) formData.priceRmb = p.unitPriceRmb
    if (p.taxPoint != null) formData.taxPoint = p.taxPoint
  }).catch(() => { /* ignore */ })
}

async function onProductCreateSubmit() {
  if (!productCreateFormRef.value) return
  const valid = await productCreateFormRef.value.validate().catch(() => false)
  if (!valid) return
  productCreateSubmitting.value = true
  try {
    const createdMasterCode = productCreateForm.masterCode || productCreateForm.subCode
    await productApi.create({
      masterCode: productCreateForm.masterCode || undefined,
      subCode: productCreateForm.subCode || undefined,
      nameZh: productCreateForm.nameZh || undefined,
      category: (productCreateForm.category || undefined) as any,
      material: productCreateForm.material || undefined,
      requiresQc: productCreateForm.requiresQc || undefined,
    } as CreateProductRequest)
    // 刷新商品分类缓存
    if (createdMasterCode) {
      productCategoryMap.value = { ...productCategoryMap.value, [createdMasterCode]: productCreateForm.category || 'ORDINARY' }
    }
    ElMessage.success(t('order.productCreateDialog.createSuccess'))
    productCreateDialogVisible.value = false
    // 子货号新建 → 刷新子货号下拉并自动选中
    if (productCreateForm.subCode) {
      await subCodeRemoteSearch('')
      formData.subProductCode = productCreateForm.subCode
    }
    // 主货号新建（无子货号）→ 刷新商品下拉并自动选中
    if (productCreateForm.masterCode && !productCreateForm.subCode) {
      await productCodeRemoteSearch(productCreateForm.masterCode)
      formData.productCode = productCreateForm.masterCode
      // 自动填充商品信息
      productApi.getByCode(productCreateForm.masterCode).then(res => {
        const p = res.data
        if (!p) return
        if (p.category) {
          productCategoryMap.value = { ...productCategoryMap.value, [productCreateForm.masterCode]: p.category }
          formData.category = getCategoryLabel(productCreateForm.masterCode)
        }
        if (p.material) formData.material = p.material
        if (p.requiresQc != null) formData.requiresQc = p.requiresQc
        if (p.unitPriceRmb != null) formData.priceRmb = p.unitPriceRmb
        if (p.taxPoint != null) formData.taxPoint = p.taxPoint
      }).catch(() => { /* ignore */ })
    }
    // 继续提交采购单（仅当从 onSubmit 触发时）
    if (pendingCreateReq) {
      await doCreateProcurement(pendingCreateReq)
      pendingCreateReq = null
    }
  } catch {
    // error handled by interceptor
  } finally {
    productCreateSubmitting.value = false
  }
}

/**
 * 直接提交采购单（内部方法，由 onSubmit 或 onProductCreateSubmit 调用）。
 */
async function doCreateProcurement(req: CreateProcurementRequest) {
  const res = await procurementApi.create(req)
  const newProcurementId = res.data as number
  const savedMsg = convertingDemandId.value !== null ? t('order.message.createSuccessConverting') : t('order.message.createSuccess')
  ElMessage.success(savedMsg)
  if (convertingDemandId.value !== null) {
    await demandApi.link(convertingDemandId.value, newProcurementId)
    ElMessage.success(t('order.message.demandConverted'))
    convertingDemandId.value = null
  }
  dialogVisible.value = false
  loadData()
}

/** 是否普通采购（显示需求选择器） */
const isNormalProcurement = computed(() => productType.value === 'NORMAL')
const route = useRoute()
const router = useRouter()
const { t, locale: localeRef } = useI18n()
const { hasPermission } = usePermission()

// 后端 order_status 枚举值（同时作为 i18n key）
const ORDER_STATUS_ORDERED = '已下单'
const ORDER_STATUS_SHIPPED = '已出货'
const ORDER_STATUS_RETURNED = '退货'
const ORDER_STATUS_DRAFT = '未定'       // 未定（可删除）
const ORDER_STATUS_PENDING = '発注待'   // 発注待（可删除）

const leadTimeOptions = [
  { value: 30, label: `30${t('common.units.day')}` },
  { value: 45, label: `45${t('common.units.day')}` },
  { value: 60, label: `60${t('common.units.day')}` },
]

const deletableStatuses = [ORDER_STATUS_DRAFT, ORDER_STATUS_PENDING, ORDER_STATUS_ORDERED]
const ORDER_STATUSES = [ORDER_STATUS_ORDERED, ORDER_STATUS_SHIPPED]

const statusOptionsWithI18n = computed(() =>
  ORDER_STATUSES.map(value => ({ value, label: statusLabelByValue(value) })),
)

const filterForm = reactive({
  productCode: '',
  status: '',
  customerCompany: '',
  productType: '' as ProductType | '',
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
  if (!hasPermission('factory:read')) return
  try {
    const res = await factoryApi.list({ page: 0, pageSize: 200 })
    factoryOptions.value = (res.data as { content: FactoryPageVO[] })?.content ?? []
  } catch { /* handled by interceptor */ }
}

// 需求下拉（仅 PENDING）
const demandOptions = ref<DemandPageVO[]>([])
const selectedDemandId = ref<number | null>(null)
async function loadDemands() {
  if (!hasPermission('demand:read')) return
  try {
    const res = await demandApi.list({ page: 0, pageSize: 200 })
    demandOptions.value = (res.data as { content: DemandPageVO[] })?.content ?? []
  } catch { /* handled by interceptor */ }
}

// 商品分类映射（productCode → category）
const productCategoryMap = ref<Record<string, string>>({})
function getCategoryLabel(code: string): string {
  if (!code) return '-'
  const category = productCategoryMap.value[code]
  if (!category || category === '-') return '-'
  return t('product.category.' + category) ?? category
}
/** 根据 productCode 拉取商品分类，填入 formData.category（关联需求时自动填入） */
async function fetchCategory(productCode: string) {
  if (!productCode) { formData.category = ''; return }
  if (productCategoryMap.value[productCode]) {
    formData.category = getCategoryLabel(productCode)
    return
  }
  try {
    const res = await productApi.getByCode(productCode)
    const cat = res.data?.category || '-'
    productCategoryMap.value = { ...productCategoryMap.value, [productCode]: cat }
    formData.category = getCategoryLabel(productCode)
  } catch {
    formData.category = ''
  }
}
async function fetchProductCategories(rows: ProcurementPageVO[]) {
  const codes = [...new Set(rows.map(r => r.productCode).filter(Boolean))]
  if (!codes.length) return
  try {
    const res = await productApi.batchGetCategories(codes)
    const map: Record<string, string> = {}
    for (const item of (res.data ?? [])) {
      map[item.masterCode] = item.category || '-'
    }
    productCategoryMap.value = { ...productCategoryMap.value, ...map }
  } catch {
  }
}

/** 选中需求 → 自动带入 productCode / subProductCode / destination / japanLead / quantity + category + 强制普通采购 */
function onDemandChange(demandId: number | null) {
  if (!demandId) return
  const d = demandOptions.value.find(x => x.id === demandId)
  if (!d) return
  productType.value = 'NORMAL'
  formData.productCode = d.productCode
  formData.subProductCode = d.subProductCode || ''
  formData.destination = d.destination || ''
  formData.japanLead = d.japanLead || ''
  formData.quantity = d.quantity ?? 0
  fetchCategory(d.productCode)
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
  fetchCategory(demand.productCode)
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
  tableRows.value.filter(r => r.status !== ORDER_STATUS_SHIPPED && r.status !== ORDER_STATUS_RETURNED).length,
)
const completedCount = computed(() =>
  tableRows.value.filter(r => r.status === ORDER_STATUS_SHIPPED).length,
)
const returnedCount = computed(() =>
  tableRows.value.filter(r => r.status === ORDER_STATUS_RETURNED).length,
)

const previewPriceJpy = computed(() => {
  const { priceRmb, taxPoint, exchangeRate } = formData
  if (!priceRmb || !taxPoint || !exchangeRate) return '—'
  const base = (priceRmb / taxPoint * 1.02 * 1.2) * exchangeRate * 1.05
  return Math.round(base * 100) / 100
})

const defaultFormData = (): CreateProcurementRequest & { status?: string; category?: string } => ({
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
  afterSalesDeadline: '',
  customerCompany: '',
  productLead: '',
  japanLead: '',
  chinaLead: '',
  destination: '',
  status: ORDER_STATUS_ORDERED,
  category: '',
})
const formData = reactive<CreateProcurementRequest & { status?: string; category?: string }>(defaultFormData())

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
      productType: filterForm.productType || undefined,
    })
    const payload = res.data as { content: ProcurementPageVO[]; totalElements: number }
    tableRows.value = payload?.content ?? []
    pagination.total = payload?.totalElements ?? 0
    fetchProductCategories(tableRows.value)
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
  filterForm.productType = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  dialogMode.value = 'create'
  productType.value = 'NORMAL'
  selectedDemandId.value = null
  subCodeOptions.value = []
  Object.assign(formData, defaultFormData())
  dialogVisible.value = true
}

function onView(row: ProcurementPageVO) {
  currentRow.value = row
  drawerVisible.value = true
}


function onEdit(row: ProcurementPageVO | null) {
  dialogMode.value = 'update'
  currentRow.value = row
  productType.value = (row as any)?.productType || 'NORMAL'
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
    afterSalesDeadline: row?.afterSalesDeadline ?? '',
    customerCompany: row?.customerCompany ?? '',
    productLead: row?.productLead ?? '',
    japanLead: row?.japanLead ?? '',
    chinaLead: row?.chinaLead ?? '',
    destination: row?.destination ?? '',
    status: row?.status ?? ORDER_STATUS_ORDERED,
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
          productType: productType.value || undefined,
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
          afterSalesDeadline: formData.afterSalesDeadline || undefined,
          customerCompany: formData.customerCompany || undefined,
          productLead: formData.productLead || undefined,
          japanLead: formData.japanLead || undefined,
          chinaLead: formData.chinaLead || undefined,
          destination: formData.destination || undefined,
          // status 不传，使用数据库默认值 '未定'
        }
        // 检查商品目录中是否已存在此货号，不存在则弹出快速新建弹窗
        try {
          await productApi.getByCode(formData.productCode)
        } catch {
          // 商品不存在，弹出快速新建弹窗
          pendingCreateReq = req
          productCreateForm.masterCode = formData.productCode
          productCreateForm.subCode = ''
          productCreateForm.nameZh = ''
          productCreateForm.category = '' as 'OEM' | 'ORDINARY' | 'FACTORY_DIRECT' | ''
          productCreateForm.material = ''
          productCreateForm.requiresQc = false
          productCreateDialogVisible.value = true
          submitting.value = false
          return
        }
        await doCreateProcurement(req)
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
          afterSalesDeadline: formData.afterSalesDeadline || undefined,
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
          tableRows.value[idx] = data as ProcurementPageVO
        }
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

function statusLabel(row: ProcurementPageVO): string {
  // Phase2：根据批次数量派生（batchCount>0 → 已出货，SPEC-B11 §7.1）
  if ((row.batchCount ?? 0) > 0) return t('order.status.已出货')
  return t('order.status.已下单')
}

function statusLabelByValue(status: string): string {
  return t(`order.status.${status}` as any, { default: status })
}

function statusType(row: ProcurementPageVO): string {
  return (row.batchCount ?? 0) > 0 ? 'success' : 'warning'
}

function formatLeadTime(days: number | undefined | null): string {
  if (!days) return '-'
  return `${days}${t('common.units.day')}`
}

async function onToggleStatus(row: ProcurementPageVO) {
  // 有批次 → 无法切换
  if ((row.batchCount ?? 0) > 0) {
    ElMessage.warning(t('order.message.batchLinked'))
    return
  }
  const newStatus = row.status === ORDER_STATUS_ORDERED ? ORDER_STATUS_SHIPPED : ORDER_STATUS_ORDERED
  try {
    await procurementApi.update(row.id, { status: newStatus })
    ElMessage.success(t('order.message.statusUpdated'))
    loadData()
  } catch {
    ElMessage.error(t('order.message.statusUpdateFailed'))
  }
}

function onOpenShipmentBatches(row: ProcurementPageVO) {
  router.push({ path: '/procurement/shipment-batch', query: { procurementId: String(row.id) } })
}

function billingTypeLabel(val: string | undefined): string {
  return val ? t(`order.billingType.${val}` as any, { default: val }) : '—'
}

onMounted(() => {
  loadData()
  // 工厂下拉：仅在有 factory:read 权限时加载
  if (hasPermission('factory:read')) loadFactories()
  // 需求下拉：仅在有 demand:read 权限时加载
  if (hasPermission('demand:read')) loadDemands()
  // 处理来自 DemandPage "转采购" 的 query params
  if (route.query.demandId) {
    convertingDemandId.value = Number(route.query.demandId)
    dialogMode.value = 'create'
    Object.assign(formData, defaultFormData())
    const pc = (route.query.productCode as string) || ''
    formData.productCode = pc
    formData.subProductCode = (route.query.subProductCode as string) || ''
    formData.destination = (route.query.destination as string) || ''
    formData.japanLead = (route.query.japanLead as string) || ''
    formData.quantity = Number(route.query.quantity) || 1
    if (pc) fetchCategory(pc)
    dialogVisible.value = true
    router.replace({ path: '/procurement/procurement' })
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

.filter-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.stats-row {
  margin-bottom: 0;
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
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
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

.status-toggle { }
.status-toggle.is-link { cursor: pointer; }

/* ── 商品快速创建弹窗提示 ── */
.product-create-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.5;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
</style>
