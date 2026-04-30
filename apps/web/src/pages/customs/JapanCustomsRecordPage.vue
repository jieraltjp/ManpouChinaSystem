<template>
  <div class="page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#6366F1"><Document /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">{{ $t('japanCustoms.stat.total') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#F59E0B"><Clock /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.PENDING }}</div><div class="stat-label">{{ $t('japanCustoms.stat.pending') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#3B82F6"><Loading /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.IN_PROGRESS }}</div><div class="stat-label">{{ $t('japanCustoms.stat.inProgress') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.CLEARED }}</div><div class="stat-label">{{ $t('japanCustoms.stat.cleared') }}</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('japanCustoms.filter.containerNo')">
          <el-input v-model="filterForm.containerNo" :placeholder="$t('japanCustoms.filter.containerNoPlaceholder')" clearable style="width:170px" />
        </el-form-item>
        <el-form-item :label="$t('japanCustoms.filter.entryNo')">
          <el-input v-model="filterForm.keyword" :placeholder="$t('japanCustoms.filter.entryNoPlaceholder')" clearable style="width:170px" />
        </el-form-item>
        <el-form-item :label="$t('japanCustoms.filter.domesticCustomsId')">
          <el-input-number v-model="filterForm.domesticCustomsId" :min="1" style="width:130px" clearable />
        </el-form-item>
        <el-form-item :label="$t('japanCustoms.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('common.all')" clearable style="width:140px">
            <el-option value="PENDING" :label="$t('japanCustoms.status.pending')" />
            <el-option value="IN_PROGRESS" :label="$t('japanCustoms.status.inProgress')" />
            <el-option value="CLEARED" :label="$t('japanCustoms.status.cleared')" />
            <el-option value="FAILED" :label="$t('japanCustoms.status.failed')" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearchFromButton">{{ $t('common.search') }}</el-button>
          <el-button @click="onReset">{{ $t('common.reset') }}</el-button>
          <el-button type="primary" @click="onNew">
            <el-icon><Plus /></el-icon> {{ $t('japanCustoms.action.newCustoms') }}
          </el-button>
          <el-button type="success" @click="onBatchOpen">
            <el-icon><Box /></el-icon> {{ $t('japanCustoms.action.batchImport') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="list">{{ $t('japanCustoms.viewMode.list') }}</el-radio-button>
            <el-radio-button value="group">{{ $t('japanCustoms.viewMode.byContainer') }}</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 列表视图 -->
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="containerNo" :label="$t('japanCustoms.column.containerNo')" min-width="160">
          <template #default="{ row }">
            <el-link v-if="row.containerNo" type="primary" @click.stop="onJumpToDomestic(row)">{{ row.containerNo }}</el-link>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="customsEntryNo" :label="$t('japanCustoms.column.entryNo')" min-width="180">
          <template #default="{ row }">
            <span class="code-badge">{{ row.customsEntryNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="domesticCustomsId" :label="$t('japanCustoms.column.domesticCustomsId')" min-width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.domesticCustomsId">{{ row.domesticCustomsId }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="procurementId" :label="$t('japanCustoms.column.procurementId')" min-width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.procurementId">{{ row.procurementId }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" :label="$t('japanCustoms.column.productCode')" min-width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.productCode">{{ row.productCode }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="subProductCode" :label="$t('japanCustoms.column.subProductCode')" min-width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.subProductCode">{{ row.subProductCode }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="arrivalPort" :label="$t('japanCustoms.column.arrivalPort')" min-width="90" />
        <el-table-column prop="arrivalDate" :label="$t('japanCustoms.column.arrivalDate')" min-width="120" />
        <el-table-column prop="customsBroker" :label="$t('japanCustoms.column.broker')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="importDutyPaid" :label="$t('japanCustoms.column.importDuty')" min-width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.importDutyPaid != null" class="money">{{ row.importDutyPaid?.toLocaleString('ja-JP') }} {{ $t('common.units.jpy') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="consumptionTaxPaid" :label="$t('japanCustoms.column.consumptionTax')" min-width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.consumptionTaxPaid != null" class="money">{{ row.consumptionTaxPaid?.toLocaleString('ja-JP') }} {{ $t('common.units.jpy') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="clearanceDate" :label="$t('japanCustoms.column.clearanceDate')" min-width="120" />
        <el-table-column prop="status" :label="$t('japanCustoms.column.status')" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('japanCustoms.column.action')" min-width="200" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('japanCustoms.action.detail') }}</el-button>
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" size="small" :loading="actionLoading === row.id + '-start'" @click.stop="onStart(row)">{{ $t('japanCustoms.action.start') }}</el-button>
              <el-button link type="danger" size="small" :loading="actionLoading === row.id + '-delete'" @click.stop="onDelete(row)">{{ $t('common.delete') }}</el-button>
            </template>
            <template v-else-if="row.status === 'IN_PROGRESS'">
              <el-button link type="success" size="small" :loading="actionLoading === row.id + '-complete'" @click.stop="onComplete(row)">{{ $t('japanCustoms.action.complete') }}</el-button>
              <el-button link type="danger" size="small" :loading="actionLoading === row.id + '-fail'" @click.stop="onFail(row)">{{ $t('japanCustoms.action.fail') }}</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分组视图 -->
      <el-collapse v-loading="loading" v-if="viewMode === 'group'" class="container-group-collapse">
        <el-collapse-item v-for="group in groupedData" :key="group.containerNo">
          <template #title>
            <div class="group-header">
              <el-icon class="group-icon"><Box /></el-icon>
              <span class="group-title">
                {{ group.containerNo || $t('japanCustoms.group.ungrouped') }}
              </span>
              <el-tag size="small" type="info" class="group-count">{{ group.records.length }} {{ $t('japanCustoms.group.records') }}</el-tag>
              <el-tag v-if="group.statusCount.PENDING" size="small" type="warning" class="group-badge">{{ group.statusCount.PENDING }} {{ $t('japanCustoms.status.pending') }}</el-tag>
              <el-tag v-if="group.statusCount.IN_PROGRESS" size="small" type="primary" class="group-badge">{{ group.statusCount.IN_PROGRESS }} {{ $t('japanCustoms.status.inProgress') }}</el-tag>
              <el-tag v-if="group.statusCount.CLEARED" size="small" type="success" class="group-badge">{{ group.statusCount.CLEARED }} {{ $t('japanCustoms.status.cleared') }}</el-tag>
              <el-tag v-if="group.statusCount.FAILED" size="small" type="danger" class="group-badge">{{ group.statusCount.FAILED }} {{ $t('japanCustoms.status.failed') }}</el-tag>
            </div>
          </template>
          <el-table :data="group.records" stripe>
            <el-table-column prop="containerNo" :label="$t('japanCustoms.column.containerNo')" min-width="160">
              <template #default="{ row }">
                <el-link v-if="row.containerNo" type="primary" @click.stop="onJumpToDomestic(row)">{{ row.containerNo }}</el-link>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="customsEntryNo" :label="$t('japanCustoms.column.entryNo')" min-width="180">
              <template #default="{ row }">
                <span class="code-badge">{{ row.customsEntryNo }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="domesticCustomsId" :label="$t('japanCustoms.column.domesticCustomsId')" min-width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.domesticCustomsId">{{ row.domesticCustomsId }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="productCode" :label="$t('japanCustoms.column.productCode')" min-width="80" align="center">
              <template #default="{ row }">
                <span v-if="row.productCode">{{ row.productCode }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="subProductCode" :label="$t('japanCustoms.column.subProductCode')" min-width="80" align="center">
              <template #default="{ row }">
                <span v-if="row.subProductCode">{{ row.subProductCode }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="arrivalPort" :label="$t('japanCustoms.column.arrivalPort')" min-width="90" />
            <el-table-column prop="arrivalDate" :label="$t('japanCustoms.column.arrivalDate')" min-width="120" />
            <el-table-column prop="importDutyPaid" :label="$t('japanCustoms.column.importDuty')" min-width="120" align="right">
              <template #default="{ row }">
                <span v-if="row.importDutyPaid != null" class="money">{{ row.importDutyPaid?.toLocaleString('ja-JP') }} {{ $t('common.units.jpy') }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="$t('japanCustoms.column.status')" min-width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('japanCustoms.column.action')" min-width="200" align="center">
              <template #default="{ row }">
                <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('japanCustoms.action.detail') }}</el-button>
                <template v-if="row.status === 'PENDING'">
                  <el-button link type="success" size="small" :loading="actionLoading === row.id + '-start'" @click.stop="onStart(row)">{{ $t('japanCustoms.action.start') }}</el-button>
                  <el-button link type="danger" size="small" :loading="actionLoading === row.id + '-delete'" @click.stop="onDelete(row)">{{ $t('common.delete') }}</el-button>
                </template>
                <template v-else-if="row.status === 'IN_PROGRESS'">
                  <el-button link type="success" size="small" :loading="actionLoading === row.id + '-complete'" @click.stop="onComplete(row)">{{ $t('japanCustoms.action.complete') }}</el-button>
                  <el-button link type="danger" size="small" :loading="actionLoading === row.id + '-fail'" @click.stop="onFail(row)">{{ $t('japanCustoms.action.fail') }}</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>

      <div v-if="viewMode === 'list'" class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="onSearch"
          @current-change="onSearch"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('japanCustoms.drawerTitle')" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('japanCustoms.column.containerNo')">{{ currentRow.containerNo ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.entryNo')">
          <span class="code-badge">{{ currentRow.customsEntryNo }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.domesticCustomsId')">{{ currentRow.domesticCustomsId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.productCode')">{{ currentRow.productCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.status')">
          <el-tag :type="statusTagType(currentRow.status)" size="small">{{ statusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.subProductCode')">{{ currentRow.subProductCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.arrivalPort')">{{ currentRow.arrivalPort ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.arrivalDate')">{{ currentRow.arrivalDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.broker')">{{ currentRow.customsBroker ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.brokerPhone')">{{ currentRow.brokerPhone ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.brokerContact')">{{ currentRow.brokerContact ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.weight')">
          <span v-if="currentRow.declaredWeightKg !== null">{{ currentRow.declaredWeightKg }} {{ $t('common.units.kg') }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.volume')">
          <span v-if="currentRow.declaredVolumeCbm !== null">{{ currentRow.declaredVolumeCbm }} {{ $t('common.units.m3') }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.importDuty')">
          <span v-if="currentRow.importDutyPaid != null" class="money">{{ currentRow.importDutyPaid?.toLocaleString('ja-JP') }} {{ $t('common.units.jpy') }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.consumptionTax')">
          <span v-if="currentRow.consumptionTaxPaid != null" class="money">{{ currentRow.consumptionTaxPaid?.toLocaleString('ja-JP') }} {{ $t('common.units.jpy') }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.clearanceDate')">{{ currentRow.clearanceDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.createBy')">{{ currentRow.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.createTime')">{{ currentRow.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 完成清关弹窗 -->
    <el-dialog v-model="completeDialogVisible" :title="$t('japanCustoms.completeDialogTitle')" width="480px">
      <el-form :model="completeForm" label-width="140px">
        <el-form-item :label="$t('japanCustoms.completeDialog.importDuty')">
          <el-input-number v-model="completeForm.importDutyPaid" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('japanCustoms.completeDialog.consumptionTax')">
          <el-input-number v-model="completeForm.consumptionTaxPaid" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('japanCustoms.completeDialog.clearanceDate')">
          <el-date-picker v-model="completeForm.clearanceDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="success" :loading="actionLoading.startsWith('complete-')" @click="onCompleteConfirm">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 失败原因弹窗 -->
    <el-dialog v-model="failDialogVisible" :title="$t('japanCustoms.failDialogTitle')" width="420px">
      <el-form>
        <el-form-item :label="$t('japanCustoms.failDialog.reasonLabel')">
          <el-input v-model="failReason" type="textarea" :rows="3" :placeholder="$t('japanCustoms.failDialog.reasonPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="failDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="danger" :loading="actionLoading.startsWith('fail-')" @click="onFailConfirm">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 新规清关弹窗 -->
    <el-dialog v-model="createDialogVisible" :title="$t('japanCustoms.newDialogTitle')" width="760px" destroy-on-close>
      <!-- 第一步：选择国内报关记录 -->
      <div v-if="!selectedDomestic" class="domestic-select-section">
        <div class="domestic-select-header">
          <span class="domestic-select-label">{{ $t('japanCustoms.dialog.selectDomesticTip') }}</span>
          <el-select
            v-model="domesticSearchContainer"
            filterable
            remote
            reserve-keyword
            :placeholder="$t('japanCustoms.dialog.containerNoPlaceholder')"
            :remote-method="searchDomesticContainers"
            :loading="domesticContainerLoading"
            clearable
            style="width:260px"
          >
            <el-option
              v-for="item in domesticContainerOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </div>
        <el-table
          v-loading="domesticTableLoading"
          :data="domesticTableOptions"
          max-height="280"
          stripe
          highlight-current-row
          @row-click="onDomesticRowClick"
        >
          <el-table-column :label="$t('japanCustoms.column.domesticCustomsId')" prop="id" width="100" align="center" />
          <el-table-column :label="$t('japanCustoms.column.containerNo')" prop="containerNo" min-width="140">
            <template #default="{ row }">{{ row.containerNo ?? '-' }}</template>
          </el-table-column>
          <el-table-column :label="$t('japanCustoms.column.productCode')" prop="productCode" min-width="130">
            <template #default="{ row }"><span class="product-code">{{ row.productCode }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('japanCustoms.column.subProductCode')" prop="subProductCode" min-width="110">
            <template #default="{ row }">{{ row.subProductCode ?? '-' }}</template>
          </el-table-column>
          <el-table-column :label="$t('japanCustoms.column.procurementId')" prop="procurementId" width="100" align="center">
            <template #default="{ row }">{{ row.procurementId ?? '-' }}</template>
          </el-table-column>
          <el-table-column :label="$t('customs.column.status')" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'CLEARED' ? 'success' : 'info'" size="small">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="domesticTableOptions.length === 0 && !domesticTableLoading" class="domestic-empty-tip">
          {{ $t('japanCustoms.dialog.noDomesticTip') }}
        </div>
      </div>

      <!-- 第二步：已选国内报关，自动填充表单 -->
      <div v-else class="form-section">
        <div class="selected-domestic-banner">
          <span class="banner-label">{{ $t('japanCustoms.dialog.selectedDomestic') }}：</span>
          <el-tag type="success" size="small">{{ selectedDomestic.customsCode }}</el-tag>
          <span class="banner-detail">{{ selectedDomestic.containerNo ?? '-' }} / {{ selectedDomestic.productCode }}</span>
          <el-button text size="small" type="primary" @click="onChangeDomestic">{{ $t('japanCustoms.dialog.changeDomestic') }}</el-button>
        </div>
        <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="130px">
          <el-form-item :label="$t('japanCustoms.column.containerNo')" prop="containerNo">
            <el-input v-model="createForm.containerNo" maxlength="32" />
          </el-form-item>
          <el-form-item :label="$t('japanCustoms.column.domesticCustomsId')" prop="domesticCustomsId">
            <el-input-number v-model="createForm.domesticCustomsId" :min="1" style="width:100%" disabled />
          </el-form-item>
          <el-form-item :label="$t('japanCustoms.column.procurementId')">
            <el-input-number v-model="createForm.procurementId" :min="1" style="width:100%" />
          </el-form-item>
          <el-form-item :label="$t('japanCustoms.column.productCode')">
            <el-input v-model="createForm.productCode" maxlength="32" />
          </el-form-item>
          <el-form-item :label="$t('japanCustoms.column.subProductCode')">
            <el-input v-model="createForm.subProductCode" maxlength="64" />
          </el-form-item>
          <el-form-item :label="$t('japanCustoms.column.arrivalDate')">
            <el-date-picker v-model="createForm.arrivalDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
          <el-form-item :label="$t('japanCustoms.column.arrivalPort')">
            <el-input v-model="createForm.arrivalPort" maxlength="64" />
          </el-form-item>
          <el-form-item :label="$t('japanCustoms.column.broker')">
            <el-input v-model="createForm.customsBroker" maxlength="128" />
          </el-form-item>
          <el-form-item :label="$t('japanCustoms.column.weight')">
            <el-input-number v-model="createForm.declaredWeightKg" :min="0" :precision="3" style="width:100%" />
          </el-form-item>
          <el-form-item :label="$t('japanCustoms.column.volume')">
            <el-input-number v-model="createForm.declaredVolumeCbm" :min="0" :precision="4" style="width:100%" />
          </el-form-item>
          <el-form-item :label="$t('japanCustoms.column.remarks')">
            <el-input v-model="createForm.remarks" type="textarea" :rows="2" maxlength="512" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button v-if="!selectedDomestic" disabled type="primary">{{ $t('common.confirm') }}</el-button>
        <el-button v-else type="primary" :loading="createSubmitting" @click="onCreate">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗（按货柜） -->
    <el-dialog v-model="batchDialogVisible" :title="$t('japanCustoms.batchDialogTitle')" width="720px" destroy-on-close>
      <div class="batch-header">
        <span class="batch-label">{{ $t('japanCustoms.batchDialog.containerNo') }}：</span>
        <el-select
          v-model="batchForm.containerNo"
          filterable
          remote
          reserve-keyword
          :placeholder="$t('japanCustoms.batchDialog.containerNoPlaceholder')"
          :remote-method="searchBatchContainers"
          :loading="batchContainerLoading"
          clearable
          style="flex:1; min-width:200px"
          @change="onBatchContainerSelect"
        >
          <el-option
            v-for="item in batchContainerOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
        <span v-if="batchForm.containerNo" class="batch-hint">
          {{ $t('japanCustoms.batchDialog.selectedCount', { n: batchSelectedIds.length }) }}
        </span>
      </div>

      <div v-if="batchForm.containerNo" class="batch-plan-table">
        <el-table
          :data="batchDomesticList"
          v-loading="batchTableLoading"
          max-height="280"
          stripe
          @selection-change="onBatchSelectionChange"
        >
          <el-table-column type="selection" width="40" />
          <el-table-column :label="$t('japanCustoms.column.domesticCustomsId')" prop="id" width="100" align="center" />
          <el-table-column :label="$t('japanCustoms.column.productCode')" min-width="130">
            <template #default="{ row }"><span class="product-code">{{ row.productCode }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('japanCustoms.column.subProductCode')" prop="subProductCode" min-width="110">
            <template #default="{ row }">{{ row.subProductCode ?? '-' }}</template>
          </el-table-column>
          <el-table-column :label="$t('japanCustoms.column.procurementId')" prop="procurementId" width="100" align="center">
            <template #default="{ row }">{{ row.procurementId ?? '-' }}</template>
          </el-table-column>
          <el-table-column :label="$t('customs.column.status')" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'CLEARED' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-if="batchDomesticList.length === 0 && batchForm.containerNo && !batchTableLoading" class="domestic-empty-tip">
        {{ $t('japanCustoms.batchDialog.noDataTip') }}
      </div>

      <template #footer>
        <el-button @click="batchDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button
          type="primary"
          :loading="batchSubmitting"
          :disabled="batchSelectedIds.length === 0"
          @click="onBatchSubmit"
        >
          {{ $t('japanCustoms.batchDialog.createButton', { n: batchSelectedIds.length }) }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Clock, Loading, CircleCheck, Plus, Box } from '@element-plus/icons-vue'
import { japanCustomsApi, type JapanCustomsVO, type JapanCustomsStatus } from '@/api/japanCustoms'
import { customsApi, type CustomsVO } from '@/api/customs'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const drawerVisible = ref(false)
const completeDialogVisible = ref(false)
const failDialogVisible = ref(false)
const createDialogVisible = ref(false)
const createSubmitting = ref(false)
const createFormRef = ref()
const actionLoading = ref('')
const failReason = ref('')
const completingRowId = ref<number | null>(null)
const failingRowId = ref<number | null>(null)
const viewMode = ref<'list' | 'group'>('list')

const currentRow = ref<JapanCustomsVO | null>(null)
const filterForm = reactive({
  containerNo: '',
  keyword: '',
  domesticCustomsId: undefined as number | undefined,
  status: '' as JapanCustomsStatus | '',
})
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<JapanCustomsVO[]>([])
const completeForm = reactive({
  importDutyPaid: 0,
  consumptionTaxPaid: 0,
  clearanceDate: '',
})

const createForm = reactive({
  containerNo: '',
  domesticCustomsId: undefined as number | undefined,
  procurementId: undefined as number | undefined,
  productCode: '',
  subProductCode: '',
  arrivalDate: '',
  arrivalPort: '',
  customsBroker: '',
  declaredWeightKg: undefined as number | undefined,
  declaredVolumeCbm: undefined as number | undefined,
  remarks: '',
})

// 新规清关联动国内报关
const selectedDomestic = ref<CustomsVO | null>(null)
const domesticSearchContainer = ref('')
const domesticContainerLoading = ref(false)
const domesticContainerOptions = ref<string[]>([])
const domesticTableLoading = ref(false)
const domesticTableOptions = ref<CustomsVO[]>([])

// 批量导入
const batchDialogVisible = ref(false)
const batchForm = reactive({ containerNo: '' })
const batchContainerLoading = ref(false)
const batchContainerOptions = ref<string[]>([])
const batchTableLoading = ref(false)
const batchDomesticList = ref<CustomsVO[]>([])
const batchSelectedIds = ref<number[]>([])
const batchSubmitting = ref(false)

const createRules = computed(() => ({
  containerNo: [{ required: true, message: t('japanCustoms.validation.containerNoRequired'), trigger: 'blur' }],
  domesticCustomsId: [{ required: true, message: t('japanCustoms.validation.domesticCustomsIdRequired'), trigger: 'blur' }],
}))

const { t } = useI18n()

const statusCount = computed(() => {
  const counts: Record<string, number> = { PENDING: 0, IN_PROGRESS: 0, CLEARED: 0, FAILED: 0 }
  tableData.value.forEach(r => { if (r.status in counts) counts[r.status]++ })
  return counts
})

const groupedData = computed(() => {
  const groups = new Map<string, { containerNo: string | null; records: JapanCustomsVO[]; statusCount: Record<string, number> }>()
  tableData.value.forEach(record => {
    const key = record.containerNo ?? '__UNGROUPED__'
    if (!groups.has(key)) {
      groups.set(key, { containerNo: record.containerNo ?? null, records: [], statusCount: { PENDING: 0, IN_PROGRESS: 0, CLEARED: 0, FAILED: 0 } })
    }
    const group = groups.get(key)!
    group.records.push(record)
    if (record.status in group.statusCount) group.statusCount[record.status]++
  })
  return Array.from(groups.values()).sort((a, b) => {
    if (a.containerNo === null) return 1
    if (b.containerNo === null) return -1
    return a.containerNo.localeCompare(b.containerNo)
  })
})

function statusLabel(status?: string): string {
  const map: Record<string, string> = {
    PENDING: t('japanCustoms.status.pending'),
    IN_PROGRESS: t('japanCustoms.status.inProgress'),
    CLEARED: t('japanCustoms.status.cleared'),
    FAILED: t('japanCustoms.status.failed'),
  }
  return map[status ?? ''] ?? status ?? '-'
}

function statusTagType(status?: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning',
    IN_PROGRESS: 'primary',
    CLEARED: 'success',
    FAILED: 'danger',
  }
  return map[status ?? ''] ?? 'info'
}

function onNew() {
  createFormRef.value?.resetFields()
  createForm.containerNo = ''
  createForm.domesticCustomsId = undefined
  createForm.procurementId = undefined
  createForm.productCode = ''
  createForm.subProductCode = ''
  createForm.arrivalDate = ''
  createForm.arrivalPort = ''
  createForm.customsBroker = ''
  createForm.declaredWeightKg = undefined
  createForm.declaredVolumeCbm = undefined
  createForm.remarks = ''
  selectedDomestic.value = null
  domesticSearchContainer.value = ''
  domesticContainerOptions.value = []
  domesticTableOptions.value = []
  createDialogVisible.value = true
  // 初始加载所有 CLEARED 的国内报关记录
  loadDomesticTable('')
}

async function loadDomesticTable(containerNo: string) {
  domesticTableLoading.value = true
  try {
    const res = await customsApi.list({
      status: 'CLEARED',
      containerNo: containerNo || undefined,
      page: 0,
      pageSize: 50,
    })
    domesticTableOptions.value = res.data.data?.content ?? []
  } catch (e) {
    console.error('[JapanCustomsRecordPage] load domestic customs failed', e)
  } finally {
    domesticTableLoading.value = false
  }
}

async function searchDomesticContainers(keyword: string) {
  if (!keyword) {
    domesticContainerOptions.value = []
    return
  }
  domesticContainerLoading.value = true
  try {
    const res = await customsApi.list({
      status: 'CLEARED',
      containerNo: keyword,
      page: 0,
      pageSize: 20,
    })
    const list: CustomsVO[] = res.data.data?.content ?? []
    // 去重 containerNo
    const seen = new Set<string>()
    domesticContainerOptions.value = list
      .filter(r => r.containerNo && !seen.has(r.containerNo) && seen.add(r.containerNo))
      .map(r => r.containerNo as string)
  } catch (e) {
    console.error('[JapanCustomsRecordPage] search domestic containers failed', e)
  } finally {
    domesticContainerLoading.value = false
  }
}

watch(domesticSearchContainer, (val) => {
  loadDomesticTable(val ?? '')
})

function onDomesticRowClick(row: CustomsVO) {
  selectedDomestic.value = row
  createForm.containerNo = row.containerNo ?? ''
  createForm.domesticCustomsId = row.id
  createForm.procurementId = row.procurementId ?? undefined
  createForm.productCode = row.productCode
  createForm.subProductCode = row.subProductCode ?? ''
}

function onChangeDomestic() {
  selectedDomestic.value = null
  createForm.domesticCustomsId = undefined
  createForm.containerNo = ''
  createForm.procurementId = undefined
  createForm.productCode = ''
  createForm.subProductCode = ''
}

function onBatchOpen() {
  batchForm.containerNo = ''
  batchContainerOptions.value = []
  batchDomesticList.value = []
  batchSelectedIds.value = []
  batchDialogVisible.value = true
}

async function searchBatchContainers(keyword: string) {
  if (!keyword) {
    batchContainerOptions.value = []
    return
  }
  batchContainerLoading.value = true
  try {
    const res = await customsApi.list({ status: 'CLEARED', containerNo: keyword, page: 0, pageSize: 20 })
    const list: CustomsVO[] = res.data.data?.content ?? []
    const seen = new Set<string>()
    batchContainerOptions.value = list
      .filter(r => r.containerNo && !seen.has(r.containerNo) && seen.add(r.containerNo))
      .map(r => r.containerNo as string)
  } catch (e) {
    console.error('[JapanCustomsRecordPage] search batch containers failed', e)
  } finally {
    batchContainerLoading.value = false
  }
}

async function onBatchContainerSelect(val: string) {
  if (!val) {
    batchDomesticList.value = []
    return
  }
  batchTableLoading.value = true
  try {
    const res = await customsApi.list({ status: 'CLEARED', containerNo: val, page: 0, pageSize: 50 })
    batchDomesticList.value = res.data.data?.content ?? []
  } catch (e) {
    console.error('[JapanCustomsRecordPage] load batch domestic list failed', e)
  } finally {
    batchTableLoading.value = false
  }
}

function onBatchSelectionChange(rows: CustomsVO[]) {
  batchSelectedIds.value = rows.map(r => r.id)
}

async function onBatchSubmit() {
  if (batchSelectedIds.value.length === 0) return
  batchSubmitting.value = true
  try {
    await japanCustomsApi.batchCreate({
      containerNo: batchForm.containerNo,
      domesticCustomsIds: batchSelectedIds.value,
    })
    ElMessage.success(t('japanCustoms.batchDialog.createSuccess', { n: batchSelectedIds.value.length }))
    batchDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[JapanCustomsRecordPage] batch create failed', e)
    ElMessage.error(t('japanCustoms.message.actionFailed'))
  } finally {
    batchSubmitting.value = false
  }
}

async function onCreate() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  createSubmitting.value = true
  try {
    await japanCustomsApi.create({
      containerNo: createForm.containerNo,
      domesticCustomsId: createForm.domesticCustomsId,
      procurementId: createForm.procurementId,
      productCode: createForm.productCode || undefined,
      subProductCode: createForm.subProductCode || undefined,
      arrivalDate: createForm.arrivalDate || undefined,
      arrivalPort: createForm.arrivalPort || undefined,
      customsBroker: createForm.customsBroker || undefined,
      declaredWeightKg: createForm.declaredWeightKg,
      declaredVolumeCbm: createForm.declaredVolumeCbm,
      remarks: createForm.remarks || undefined,
    })
    ElMessage.success(t('japanCustoms.message.createSuccess'))
    createDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[JapanCustomsRecordPage] create failed', e)
    ElMessage.error(t('japanCustoms.message.createFailed'))
  } finally {
    createSubmitting.value = false
  }
}

function onJumpToDomestic(row: JapanCustomsVO) {
  if (row.domesticCustomsId) {
    window.location.hash = `#/procurement/domestic-customs?id=${row.domesticCustomsId}`
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await japanCustomsApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      status: filterForm.status || undefined,
      containerNo: filterForm.containerNo || undefined,
      domesticCustomsId: filterForm.domesticCustomsId,
    })
    const data = res.data.data
    tableData.value = data?.content ?? []
    pagination.total = data?.totalElements ?? 0
  } catch (e: unknown) {
    console.error('[JapanCustomsRecordPage] loadData failed', e)
    ElMessage.error(t('japanCustoms.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onSearch() { loadData() }
function onSearchFromButton() { pagination.page = 1; loadData() }

function onReset() {
  filterForm.containerNo = ''
  filterForm.keyword = ''
  filterForm.domesticCustomsId = undefined
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

function onView(row: JapanCustomsVO) {
  currentRow.value = row
  drawerVisible.value = true
}

async function onStart(row: JapanCustomsVO) {
  actionLoading.value = `${row.id}-start`
  try {
    await japanCustomsApi.start(row.id)
    ElMessage.success(t('japanCustoms.message.startSuccess'))
    loadData()
  } catch (e) {
    console.error('[JapanCustomsRecordPage] start failed', e)
    ElMessage.error(t('japanCustoms.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
}

function onComplete(row: JapanCustomsVO) {
  completingRowId.value = row.id
  completeForm.importDutyPaid = 0
  completeForm.consumptionTaxPaid = 0
  completeForm.clearanceDate = ''
  completeDialogVisible.value = true
}

async function onCompleteConfirm() {
  if (!completingRowId.value) return
  actionLoading.value = `complete-${completingRowId.value}`
  try {
    await japanCustomsApi.complete(completingRowId.value, {
      importDutyPaid: completeForm.importDutyPaid,
      consumptionTaxPaid: completeForm.consumptionTaxPaid,
      clearanceDate: completeForm.clearanceDate,
    })
    ElMessage.success(t('japanCustoms.message.completeSuccess'))
    completeDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[JapanCustomsRecordPage] complete failed', e)
    ElMessage.error(t('japanCustoms.message.actionFailed'))
  } finally {
    actionLoading.value = ''
    completingRowId.value = null
  }
}

function onFail(row: JapanCustomsVO) {
  failingRowId.value = row.id
  failReason.value = ''
  failDialogVisible.value = true
}

async function onFailConfirm() {
  if (!failingRowId.value) return
  actionLoading.value = `fail-${failingRowId.value}`
  try {
    await japanCustomsApi.fail(failingRowId.value, { reason: failReason.value })
    ElMessage.success(t('japanCustoms.message.failSuccess'))
    failDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[JapanCustomsRecordPage] fail failed', e)
    ElMessage.error(t('japanCustoms.message.actionFailed'))
  } finally {
    actionLoading.value = ''
    failingRowId.value = null
  }
}

async function onDelete(row: JapanCustomsVO) {
  try {
    await ElMessageBox.confirm(t('japanCustoms.message.deleteConfirm'), t('common.warning'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
  } catch { return }
  actionLoading.value = `${row.id}-delete`
  try {
    await japanCustomsApi.delete(row.id)
    ElMessage.success(t('japanCustoms.message.deleteSuccess'))
    loadData()
  } catch (e) {
    console.error('[JapanCustomsRecordPage] delete failed', e)
    ElMessage.error(t('japanCustoms.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
}

onMounted(() => loadData())

// 修正 el-table 空状态时 empty-block 宽度超出列宽
watch(tableData, () => {
  nextTick(() => {
    const headerTable = document.querySelector('.el-table__header') as HTMLElement
    const scrollView = document.querySelector('.el-scrollbar__view') as HTMLElement
    const emptyBlock = document.querySelector('.el-table__empty-block') as HTMLElement
    if (headerTable) {
      const headerW = headerTable.offsetWidth
      if (scrollView) scrollView.style.width = headerW + 'px'
      if (emptyBlock) emptyBlock.style.width = headerW + 'px'
    }
  })
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.stats-row { margin-bottom: 0; }
.stat-card { border-radius: var(--radius-md); border: 1px solid var(--border-color); box-shadow: var(--shadow-card); position: relative; overflow: hidden; transition: all var(--transition-fast); }
.stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light)); border-radius: var(--radius-md) var(--radius-md) 0 0; }
.stat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.stat-content { display: flex; align-items: center; gap: 14px; }
.stat-icon-wrap { width: 48px; height: 48px; border-radius: 50%; background: var(--color-primary-pale); display: flex; align-items: center; justify-content: center; }
.stat-icon { font-size: 22px; }
.stat-value { font-size: 26px; font-weight: 800; color: var(--text-primary); line-height: 1; font-variant-numeric: tabular-nums; }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.code-badge { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.money { color: #16A34A; font-weight: 600; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.table-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.container-group-collapse { margin-top: 8px; }
.group-header { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.group-icon { font-size: 18px; color: var(--color-primary); }
.group-title { font-weight: 600; font-size: 14px; color: var(--text-primary); }
.group-count { margin-left: 4px; }
.group-badge { margin-left: 4px; }
.domestic-select-section { display: flex; flex-direction: column; gap: 12px; }
.domestic-select-header { display: flex; align-items: center; gap: 12px; }
.domestic-select-label { font-weight: 600; font-size: 13px; color: var(--text-secondary); white-space: nowrap; }
.domestic-empty-tip { text-align: center; color: var(--text-placeholder); font-size: 13px; padding: 16px 0; }
.selected-domestic-banner { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; padding: 10px 14px; background: #f0f9eb; border: 1px solid #e1f3d8; border-radius: 6px; }
.banner-label { font-weight: 600; font-size: 13px; color: #67c23a; }
.banner-detail { font-size: 12px; color: #999; }
.form-section { margin-top: 4px; }
.batch-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.batch-label { font-weight: 600; font-size: 13px; color: var(--text-secondary); white-space: nowrap; }
.batch-hint { font-size: 12px; color: #67c23a; font-weight: 600; white-space: nowrap; }
.batch-plan-table { margin-top: 8px; }
</style>
