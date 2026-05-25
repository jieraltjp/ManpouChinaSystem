<template>
  <div class="page">
    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('legacyProcurement.filter.code')">
          <el-input v-model="filterForm.code" :placeholder="$t('legacyProcurement.filter.codePlaceholder')" clearable style="width:160px" />
        </el-form-item>
        <el-form-item :label="$t('legacyProcurement.filter.container')">
          <el-input v-model="filterForm.container" :placeholder="$t('legacyProcurement.filter.containerPlaceholder')" clearable style="width:200px" />
        </el-form-item>
        <el-form-item :label="$t('legacyProcurement.filter.itemName')">
          <el-input v-model="filterForm.itemName" :placeholder="$t('legacyProcurement.filter.itemNamePlaceholder')" clearable style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">{{ $t('legacyProcurement.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('legacyProcurement.filter.reset') }}</el-button>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="filterForm.overdueOnly">
            <span>{{ $t('legacyProcurement.filter.overdueOnly') }}</span>
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onNew" v-if="hasPermission('legacy_procurement:create')">
            {{ $t('legacyProcurement.dialog.createTitle') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px;">
          <el-radio-group v-model="excelViewMode" size="small">
            <el-radio-button value="table">{{ $t('common.viewMode.table') }}</el-radio-button>
            <el-radio-button value="copy">{{ $t('common.viewMode.excel') }}</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table v-if="excelViewMode === 'table'" v-loading="loading" :data="displayData" stripe style="width:100%" :row-class-name="getRowClassName">
        <el-table-column type="selection" width="40" align="center" />
        <el-table-column prop="legacyId" :label="$t('legacyProcurement.column.legacyId')" width="70" align="center" />
        <el-table-column prop="code" :label="$t('legacyProcurement.column.code')" min-width="110" show-overflow-tooltip />
        <el-table-column prop="subCode" :label="$t('legacyProcurement.column.subCode')" min-width="80" show-overflow-tooltip />
        <el-table-column prop="infoFile1" :label="$t('legacyProcurement.column.infoFile1')" min-width="100" show-overflow-tooltip />
        <el-table-column prop="infoFile2" :label="$t('legacyProcurement.column.infoFile2')" min-width="100" show-overflow-tooltip />
        <el-table-column prop="material" :label="$t('legacyProcurement.column.material')" min-width="90" show-overflow-tooltip />
        <el-table-column prop="note" :label="$t('legacyProcurement.column.note')" min-width="130" show-overflow-tooltip />
        <el-table-column :label="$t('legacyProcurement.column.image')" width="70" align="center">
          <template #default="{ row }">
            <ProductImageCell :product-code="row.code" :image-map="imageMap" />
          </template>
        </el-table-column>
        <el-table-column prop="itemName" :label="$t('legacyProcurement.column.itemName')" min-width="130" show-overflow-tooltip />
        <el-table-column prop="orderGroup" :label="$t('legacyProcurement.column.orderGroup')" min-width="110" show-overflow-tooltip />
        <el-table-column prop="orderCount" :label="$t('legacyProcurement.column.orderCount')" min-width="80" align="right" />
        <el-table-column prop="inspectCount" :label="$t('legacyProcurement.column.inspectCount')" min-width="80" align="right" />
        <el-table-column prop="yoyakuHasoubi" :label="$t('legacyProcurement.column.yoyakuHasoubi')" min-width="110" align="center">
          <template #default="{ row }">
            {{ row.yoyakuHasoubi ?? $t('common.format.dash') }}
          </template>
        </el-table-column>
        <el-table-column prop="arrivalDepo" :label="$t('legacyProcurement.column.arrivalDepo')" min-width="90" show-overflow-tooltip />
        <el-table-column prop="departure" :label="$t('legacyProcurement.column.departure')" min-width="100" align="center">
          <template #default="{ row }">
            {{ row.departure ?? $t('common.format.dash') }}
          </template>
        </el-table-column>
        <el-table-column prop="arrival" :label="$t('legacyProcurement.column.arrival')" min-width="130" align="center">
          <template #default="{ row }">
            {{ row.arrival ?? $t('common.format.dash') }}
          </template>
        </el-table-column>
        <el-table-column prop="arrivalJikan" :label="$t('legacyProcurement.column.arrivalJikan')" width="120" align="center">
          <template #default="{ row }">
            {{ row.arrivalJikan != null ? String(row.arrivalJikan).padStart(2, '0') + ':00' : $t('common.format.dash') }}
          </template>
        </el-table-column>
        <el-table-column prop="receive" :label="$t('legacyProcurement.column.receive')" min-width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.receive == '13'" class="receive-13">到着済</span>
            <span v-else-if="row.receive == '11'">{{ $t('legacyProcurement.receive.transiting') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="neStock" :label="$t('legacyProcurement.column.neStock')" min-width="90" show-overflow-tooltip />
        <el-table-column prop="houkoku" :label="$t('legacyProcurement.column.houkoku')" min-width="70" show-overflow-tooltip />
        <el-table-column prop="kaitsuke" :label="$t('legacyProcurement.column.kaitsuke')" min-width="90" align="right">
          <template #default="{ row }">
            {{ formatNum(row.kaitsuke) }}
          </template>
        </el-table-column>
        <el-table-column prop="kanpu" :label="$t('legacyProcurement.column.kanpu')" min-width="70" show-overflow-tooltip />
        <el-table-column prop="hyoten" :label="$t('legacyProcurement.column.hyoten')" min-width="70" align="right">
          <template #default="{ row }">
            {{ formatNum(row.hyoten) }}
          </template>
        </el-table-column>
        <el-table-column prop="unitCh" :label="$t('legacyProcurement.column.unitCh')" min-width="90" align="right">
          <template #default="{ row }">
            {{ formatNum(row.unitCh) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalCh" :label="$t('legacyProcurement.column.totalCh')" min-width="90" align="right">
          <template #default="{ row }">
            {{ formatNum(row.totalCh) }}
          </template>
        </el-table-column>
        <el-table-column prop="unitJp" :label="$t('legacyProcurement.column.unitJp')" min-width="90" align="right">
          <template #default="{ row }">
            {{ formatNum(row.unitJp) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalJp" :label="$t('legacyProcurement.column.totalJp')" min-width="100" align="right">
          <template #default="{ row }">
            {{ row.totalJp != null ? row.totalJp.toLocaleString() : $t('common.format.dash') }}
          </template>
        </el-table-column>
        <el-table-column prop="rate" :label="$t('legacyProcurement.column.rate')" min-width="90" align="right">
          <template #default="{ row }">
            {{ formatNum(row.rate) }}
          </template>
        </el-table-column>
        <el-table-column prop="container" :label="$t('legacyProcurement.column.container')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="boxNum" :label="$t('legacyProcurement.column.boxNum')" min-width="90" show-overflow-tooltip />
        <el-table-column :label="$t('legacyProcurement.column.action')" width="160" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onDetail(row)">{{ $t('legacyProcurement.action.detail') }}</el-button>
            <el-button link class="btn-blue" size="small" @click.stop="onEdit(row)" v-if="hasPermission('legacy_procurement:update')">{{ $t('legacyProcurement.action.edit') }}</el-button>
            <el-button link class="btn-danger" size="small" @click.stop="onDelete(row)" v-if="hasPermission('legacy_procurement:delete')">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <ExcelTable v-else :columns="copyColumns" :data="displayData" />

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          background
          :current-page="pagination.page"
          :page-size="pagination.pageSize"
          :page-sizes="[20, 50, 100, 500, 1000]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="$t('legacyProcurement.drawer.title')" size="650px" direction="rtl" body-style="overflow-y: auto">
      <div v-if="currentRow" class="detail-grid">
        <div class="detail-section-title">{{ $t('legacyProcurement.drawer.section.basic') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.legacyId') }}</span><span class="detail-value">{{ currentRow.legacyId }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.code') }}</span><span class="detail-value">{{ currentRow.code ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.subCode') }}</span><span class="detail-value">{{ currentRow.subCode ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.itemName') }}</span><span class="detail-value">{{ currentRow.itemName ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.img') }}</span><span class="detail-value">{{ currentRow.img ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.orderGroup') }}</span><span class="detail-value">{{ currentRow.orderGroup ?? $t('common.format.dash') }}</span></div>

        <div class="detail-section-title">{{ $t('legacyProcurement.drawer.section.quantity') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.orderCount') }}</span><span class="detail-value">{{ currentRow.orderCount ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.inspectCount') }}</span><span class="detail-value">{{ currentRow.inspectCount ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.fbaStock') }}</span><span class="detail-value">{{ currentRow.fbaStock ?? $t('common.format.dash') }}</span></div>

        <div class="detail-section-title">{{ $t('legacyProcurement.drawer.section.schedule') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.yoyakuHasoubi') }}</span><span class="detail-value">{{ currentRow.yoyakuHasoubi ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.arrivalDepo') }}</span><span class="detail-value">{{ currentRow.arrivalDepo ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.departure') }}</span><span class="detail-value">{{ currentRow.departure ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.arrival') }}</span><span class="detail-value">{{ currentRow.arrival ?? $t('common.format.dash') }}</span></div>

        <div class="detail-section-title">{{ $t('legacyProcurement.drawer.section.price') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.unitCh') }}</span><span class="detail-value">{{ formatNum(currentRow.unitCh) }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.totalCh') }}</span><span class="detail-value">{{ formatNum(currentRow.totalCh) }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.unitJp') }}</span><span class="detail-value">{{ formatNum(currentRow.unitJp) }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.totalJp') }}</span><span class="detail-value">{{ currentRow.totalJp?.toLocaleString() ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.rate') }}</span><span class="detail-value">{{ formatNum(currentRow.rate) }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.kaitsuke') }}</span><span class="detail-value">{{ formatNum(currentRow.kaitsuke) }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.hyoten') }}</span><span class="detail-value">{{ formatNum(currentRow.hyoten) }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.kanpu') }}</span><span class="detail-value">{{ currentRow.kanpu ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.houkoku') }}</span><span class="detail-value">{{ currentRow.houkoku || $t('common.format.dash') }}</span></div>

        <div class="detail-section-title">{{ $t('legacyProcurement.drawer.section.logistics') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.container') }}</span><span class="detail-value">{{ currentRow.container ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.boxNum') }}</span><span class="detail-value">{{ currentRow.boxNum ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.boxCount') }}</span><span class="detail-value">{{ currentRow.boxCount ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.kg') }}</span><span class="detail-value">{{ formatNum(currentRow.kg) }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.oneM3') }}</span><span class="detail-value">{{ formatNum(currentRow.oneM3) }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.allM3') }}</span><span class="detail-value">{{ formatNum(currentRow.allM3) }}</span></div>

        <div class="detail-section-title">{{ $t('legacyProcurement.drawer.section.spec') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.material') }}</span><span class="detail-value">{{ currentRow.material ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.materialCh') }}</span><span class="detail-value">{{ currentRow.materialCh ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.height') }}</span><span class="detail-value">{{ formatNum(currentRow.height) }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.width') }}</span><span class="detail-value">{{ formatNum(currentRow.width) }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.depth') }}</span><span class="detail-value">{{ formatNum(currentRow.depth) }}</span></div>
        <div class="detail-item full-width"><span class="detail-label">{{ $t('legacyProcurement.drawer.infoFile1') }}</span><span class="detail-value">{{ currentRow.infoFile1 ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item full-width"><span class="detail-label">{{ $t('legacyProcurement.drawer.infoFile2') }}</span><span class="detail-value">{{ currentRow.infoFile2 ?? $t('common.format.dash') }}</span></div>

        <div class="detail-section-title">{{ $t('legacyProcurement.drawer.section.other') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.neStock') }}</span><span class="detail-value">{{ currentRow.neStock ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.receive') }}</span><span class="detail-value">{{ currentRow.receive ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item full-width"><span class="detail-label">{{ $t('legacyProcurement.drawer.note') }}</span><span class="detail-value">{{ currentRow.note || $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.updater') }}</span><span class="detail-value">{{ currentRow.updater ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('legacyProcurement.drawer.updatetime') }}</span><span class="detail-value">{{ formatTime(currentRow.updatetime) }}</span></div>

        <div class="drawer-footer">
          <el-button @click="detailVisible = false">{{ $t('legacyProcurement.drawer.close') }}</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="currentRow?.legacyId ? $t('legacyProcurement.dialog.editTitle') : $t('legacyProcurement.dialog.createTitle')" width="780px" :close-on-click-modal="false" destroy-on-close>
      <el-tabs>
        <!-- 基本信息 -->
        <el-tab-pane :label="$t('legacyProcurement.dialog.tab.basic')">
          <el-form ref="editFormRef" :model="editForm" label-width="110px">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.code')">
                  <el-input v-model="editForm.code" maxlength="100" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.subCode')">
                  <el-input v-model="editForm.subCode" maxlength="100" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.orderGroup')">
                  <el-input v-model="editForm.orderGroup" maxlength="100" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('legacyProcurement.dialog.itemName')">
                  <el-input v-model="editForm.itemName" maxlength="200" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('legacyProcurement.dialog.infoFile1')">
                  <el-input v-model="editForm.infoFile1" maxlength="200" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('legacyProcurement.dialog.infoFile2')">
                  <el-input v-model="editForm.infoFile2" maxlength="200" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.material')">
                  <el-input v-model="editForm.material" maxlength="200" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.materialCh')">
                  <el-input v-model="editForm.materialCh" maxlength="200" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.neStock')">
                  <el-input v-model="editForm.neStock" maxlength="100" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item :label="$t('legacyProcurement.dialog.note')">
                  <el-input v-model="editForm.note" type="textarea" :rows="2" maxlength="2000" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <!-- 数量信息 -->
        <el-tab-pane :label="$t('legacyProcurement.dialog.tab.quantity')">
          <el-form ref="editFormRef2" :model="editForm" label-width="110px">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.orderCount')">
                  <el-input-number v-model="editForm.orderCount" :min="0" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.inspectCount')">
                  <el-input-number v-model="editForm.inspectCount" :min="0" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.fbaStock')">
                  <el-input-number v-model="editForm.fbaStock" :min="0" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.receive')">
                  <el-input v-model="editForm.receive" maxlength="100" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <!-- 日程信息 -->
        <el-tab-pane :label="$t('legacyProcurement.dialog.tab.schedule')">
          <el-form ref="editFormRef3" :model="editForm" label-width="120px">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.yoyakuHasoubi')">
                  <el-date-picker v-model="editForm.yoyakuHasoubi" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.arrivalDepo')">
                  <el-input v-model="editForm.arrivalDepo" maxlength="100" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.departure')">
                  <el-date-picker v-model="editForm.departure" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.arrival')">
                  <el-date-picker v-model="editForm.arrival" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.arrivalJikan')">
                  <el-input-number v-model="editForm.arrivalJikan" :min="0" :max="23" style="width:100%" placeholder="0-23" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <!-- 价格信息 -->
        <el-tab-pane :label="$t('legacyProcurement.dialog.tab.price')">
          <el-form ref="editFormRef4" :model="editForm" label-width="110px">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.unitCh')">
                  <el-input-number v-model="editForm.unitCh" :min="0" :precision="4" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.totalCh')">
                  <el-input-number v-model="editForm.totalCh" :min="0" :precision="4" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.unitJp')">
                  <el-input-number v-model="editForm.unitJp" :min="0" :precision="4" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.totalJp')">
                  <el-input-number v-model="editForm.totalJp" :min="0" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.rate')">
                  <el-input-number v-model="editForm.rate" :min="0" :precision="6" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.kaitsuke')">
                  <el-input-number v-model="editForm.kaitsuke" :min="0" :precision="2" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.hyoten')">
                  <el-input-number v-model="editForm.hyoten" :min="0" :precision="4" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.kanpu')">
                  <el-input v-model="editForm.kanpu" maxlength="10" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.houkoku')">
                  <el-input v-model="editForm.houkoku" maxlength="50" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <!-- 物流信息 -->
        <el-tab-pane :label="$t('legacyProcurement.dialog.tab.logistics')">
          <el-form ref="editFormRef5" :model="editForm" label-width="110px">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.container')">
                  <el-input v-model="editForm.container" maxlength="200" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.boxNum')">
                  <el-input v-model="editForm.boxNum" maxlength="100" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.boxCount')">
                  <el-input-number v-model="editForm.boxCount" :min="0" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.kg')">
                  <el-input-number v-model="editForm.kg" :min="0" :precision="4" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.oneM3')">
                  <el-input-number v-model="editForm.oneM3" :min="0" :precision="6" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.allM3')">
                  <el-input-number v-model="editForm.allM3" :min="0" :precision="6" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <!-- 规格信息 -->
        <el-tab-pane :label="$t('legacyProcurement.dialog.tab.spec')">
          <el-form ref="editFormRef6" :model="editForm" label-width="110px">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.height')">
                  <el-input-number v-model="editForm.height" :min="0" :precision="4" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.width')">
                  <el-input-number v-model="editForm.width" :min="0" :precision="4" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('legacyProcurement.dialog.depth')">
                  <el-input-number v-model="editForm.depth" :min="0" :precision="4" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="editVisible = false">{{ $t('common.button.cancel') }}</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="onEditSubmit">{{ $t('common.button.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { legacyProcurementApi } from '@/api/legacy-procurement'
import type { LegacyProcurementPageVO } from '@/api/legacy-procurement'
import { useProductImage } from '@/composables/useProductImage'
import ProductImageCell from '@/components/ProductImageCell.vue'
import { usePermission } from '@/composables/usePermission'
import { useOverdue, useOverdueUrlSync } from '@/composables/useOverdue'
import ExcelTable, { type ExcelColDef } from '@/components/ExcelTable.vue'

const { imageMap, loadImageMap } = useProductImage()
const { hasPermission } = usePermission()
const { locale: localeRef } = useI18n()
const { t } = useI18n()

const excelViewMode = ref<'table' | 'copy'>('table')

const copyColumns: ExcelColDef[] = [
  { prop: 'legacyId', label: t('legacyProcurement.column.legacyId') },
  { prop: 'code', label: t('legacyProcurement.column.code') },
  { prop: 'subCode', label: t('legacyProcurement.column.subCode') },
  { prop: 'infoFile1', label: t('legacyProcurement.column.infoFile1') },
  { prop: 'infoFile2', label: t('legacyProcurement.column.infoFile2') },
  { prop: 'material', label: t('legacyProcurement.column.material') },
  { prop: 'note', label: t('legacyProcurement.column.note') },
  { prop: 'itemName', label: t('legacyProcurement.column.itemName') },
  { prop: 'orderGroup', label: t('legacyProcurement.column.orderGroup') },
  { prop: 'orderCount', label: t('legacyProcurement.column.orderCount'), formatter: (row) => row.orderCount != null ? String(row.orderCount) : '' },
  { prop: 'inspectCount', label: t('legacyProcurement.column.inspectCount'), formatter: (row) => row.inspectCount != null ? String(row.inspectCount) : '' },
  { prop: 'yoyakuHasoubi', label: t('legacyProcurement.column.yoyakuHasoubi') },
  { prop: 'arrivalDepo', label: t('legacyProcurement.column.arrivalDepo') },
  { prop: 'departure', label: t('legacyProcurement.column.departure') },
  { prop: 'arrival', label: t('legacyProcurement.column.arrival') },
  { prop: 'arrivalJikan', label: t('legacyProcurement.column.arrivalJikan'), formatter: (row) => row.arrivalJikan != null ? String(row.arrivalJikan).padStart(2, '0') + ':00' : '' },
  { prop: 'receive', label: t('legacyProcurement.column.receive'), formatter: (row) => row.receive == '13' ? '到着済' : row.receive == '11' ? t('legacyProcurement.receive.transiting') : '' },
  { prop: 'neStock', label: t('legacyProcurement.column.neStock') },
  { prop: 'houkoku', label: t('legacyProcurement.column.houkoku') },
  { prop: 'kaitsuke', label: t('legacyProcurement.column.kaitsuke'), formatter: (row) => formatNum(row.kaitsuke) },
  { prop: 'kanpu', label: t('legacyProcurement.column.kanpu') },
  { prop: 'hyoten', label: t('legacyProcurement.column.hyoten'), formatter: (row) => formatNum(row.hyoten) },
  { prop: 'unitCh', label: t('legacyProcurement.column.unitCh'), formatter: (row) => formatNum(row.unitCh) },
  { prop: 'totalCh', label: t('legacyProcurement.column.totalCh'), formatter: (row) => formatNum(row.totalCh) },
  { prop: 'unitJp', label: t('legacyProcurement.column.unitJp'), formatter: (row) => formatNum(row.unitJp) },
  { prop: 'totalJp', label: t('legacyProcurement.column.totalJp'), formatter: (row) => row.totalJp != null ? row.totalJp.toLocaleString() : '' },
  { prop: 'rate', label: t('legacyProcurement.column.rate'), formatter: (row) => formatNum(row.rate) },
  { prop: 'container', label: t('legacyProcurement.column.container') },
  { prop: 'boxNum', label: t('legacyProcurement.column.boxNum') },
]

function formatTime(ts: string | undefined | null): string {
  if (!ts) return '-'
  return new Date(ts).toLocaleString(localeRef.value === 'ja' ? 'ja-JP' : 'zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

function formatNum(v: number | undefined | null): string {
  if (v == null || v === 0) return '-'
  return v.toLocaleString(localeRef.value === 'ja' ? 'ja-JP' : 'zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 4 })
}

const loading = ref(false)
const tableData = ref<LegacyProcurementPageVO[]>([])
const overdueLoaded = ref(false) // overdue 全量数据是否已加载
const detailVisible = ref(false)
const currentRow = ref<LegacyProcurementPageVO | null>(null)

const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const filterForm = reactive({ code: '', container: '', itemName: '', overdueOnly: false })

const { getRowClassName, filterOverdue } = useOverdue(tableData)

const displayData = computed(() => {
  if (!filterForm.overdueOnly) return tableData.value
  const filtered = filterOverdue(tableData.value)
  const start = (pagination.page - 1) * pagination.pageSize
  return filtered.slice(start, start + pagination.pageSize)
})

// URL 持久化：overdueOnly ↔ ?overdue=true
const syncToQuery = useOverdueUrlSync(
  toRef(filterForm, 'overdueOnly'),
  () => { pagination.page = 1 },
)
watch(() => filterForm.overdueOnly, (v: boolean) => {
  syncToQuery(v)
  pagination.page = 1
  overdueLoaded.value = false
  loadData()
})

// Edit dialog
const editVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref()
const editForm = reactive({
  code: '',
  subCode: '',
  itemName: '',
  orderGroup: '',
  orderCount: undefined as number | undefined,
  inspectCount: undefined as number | undefined,
  yoyakuHasoubi: '',
  arrivalDepo: '',
  departure: '',
  arrival: '',
  arrivalJikan: undefined as number | undefined,
  unitCh: undefined as number | undefined,
  totalCh: undefined as number | undefined,
  unitJp: undefined as number | undefined,
  totalJp: undefined as number | undefined,
  rate: undefined as number | undefined,
  fbaStock: undefined as number | undefined,
  houkoku: '',
  kaitsuke: undefined as number | undefined,
  hyoten: undefined as number | undefined,
  kanpu: '',
  neStock: '',
  container: '',
  boxNum: '',
  boxCount: undefined as number | undefined,
  kg: undefined as number | undefined,
  oneM3: undefined as number | undefined,
  allM3: undefined as number | undefined,
  material: '',
  materialCh: '',
  height: undefined as number | undefined,
  width: undefined as number | undefined,
  depth: undefined as number | undefined,
  infoFile1: '',
  infoFile2: '',
  note: '',
  receive: '',
})

async function loadData() {
  loading.value = true
  try {
    if (filterForm.overdueOnly) {
      // overdue 数据已加载则跳过，重新加载只有在首次或切换模式时
      if (overdueLoaded.value && tableData.value.length > 0) return
      const res = await legacyProcurementApi.overdue()
      const data = res.data as LegacyProcurementPageVO[] | undefined
      tableData.value = data ?? []
      pagination.total = filterOverdue(tableData.value).length
      overdueLoaded.value = true
    } else {
      const params: Record<string, unknown> = { page: pagination.page - 1, pageSize: pagination.pageSize }
      if (filterForm.code) params.code = filterForm.code
      if (filterForm.container) params.container = filterForm.container
      if (filterForm.itemName) params.itemName = filterForm.itemName

      const res = await legacyProcurementApi.list(params as Record<string, string | number>)
      const data = res.data as { content?: LegacyProcurementPageVO[]; totalElements?: number } | undefined
      tableData.value = data?.content ?? []
      pagination.total = data?.totalElements ?? 0
    }
    await loadImageMap(tableData.value, 'code')
  } catch (err: unknown) {
    console.error('[LegacyProcurement] loadData error:', err)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pagination.page = 1
  loadData()
}

function onReset() {
  filterForm.code = ''
  filterForm.container = ''
  filterForm.itemName = ''
  filterForm.overdueOnly = false
  pagination.page = 1
  loadData()
}

function onPageChange(page: number) {
  pagination.page = page
  loadData()
}

function onSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  loadData()
}

function onNew() {
  currentRow.value = null
  // Reset form
  Object.assign(editForm, {
    code: '', subCode: '', itemName: '', orderGroup: '',
    orderCount: undefined, inspectCount: undefined,
    yoyakuHasoubi: '', arrivalDepo: '', departure: '', arrival: '',
    arrivalJikan: undefined,
    unitCh: undefined, totalCh: undefined, unitJp: undefined, totalJp: undefined,
    rate: undefined, fbaStock: undefined, houkoku: '', kaitsuke: undefined,
    hyoten: undefined, kanpu: '', neStock: '',
    container: '', boxNum: '', boxCount: undefined,
    kg: undefined, oneM3: undefined, allM3: undefined,
    material: '', materialCh: '',
    height: undefined, width: undefined, depth: undefined,
    infoFile1: '', infoFile2: '',
    note: '', receive: '',
  })
  editVisible.value = true
}

function onDetail(row: LegacyProcurementPageVO) {
  currentRow.value = row
  detailVisible.value = true
}

function onEdit(row: LegacyProcurementPageVO) {
  currentRow.value = row
  editForm.code = row.code ?? ''
  editForm.subCode = row.subCode ?? ''
  editForm.itemName = row.itemName ?? ''
  editForm.orderGroup = row.orderGroup ?? ''
  editForm.orderCount = row.orderCount
  editForm.inspectCount = row.inspectCount
  editForm.yoyakuHasoubi = row.yoyakuHasoubi ?? ''
  editForm.arrivalDepo = row.arrivalDepo ?? ''
  editForm.departure = row.departure ?? ''
  editForm.arrival = row.arrival ?? ''
  editForm.arrivalJikan = row.arrivalJikan
  editForm.unitCh = row.unitCh
  editForm.totalCh = row.totalCh
  editForm.unitJp = row.unitJp
  editForm.totalJp = row.totalJp
  editForm.rate = row.rate
  editForm.fbaStock = row.fbaStock
  editForm.houkoku = row.houkoku ?? ''
  editForm.kaitsuke = row.kaitsuke
  editForm.hyoten = row.hyoten
  editForm.kanpu = row.kanpu ?? ''
  editForm.neStock = row.neStock ?? ''
  editForm.container = row.container ?? ''
  editForm.boxNum = row.boxNum ?? ''
  editForm.boxCount = row.boxCount
  editForm.kg = row.kg
  editForm.oneM3 = row.oneM3
  editForm.allM3 = row.allM3
  editForm.material = row.material ?? ''
  editForm.materialCh = row.materialCh ?? ''
  editForm.height = row.height
  editForm.width = row.width
  editForm.depth = row.depth
  editForm.infoFile1 = row.infoFile1 ?? ''
  editForm.infoFile2 = row.infoFile2 ?? ''
  editForm.note = row.note ?? ''
  editForm.receive = row.receive ?? ''
  editVisible.value = true
}

async function onEditSubmit() {
  editSubmitting.value = true
  try {
    if (currentRow.value?.legacyId) {
      await legacyProcurementApi.update(currentRow.value.legacyId, { ...editForm })
      ElMessage.success(t('legacyProcurement.message.updateSuccess'))
    } else {
      await legacyProcurementApi.create({ ...editForm })
      ElMessage.success(t('legacyProcurement.message.createSuccess'))
    }
    editVisible.value = false
    await loadData()
  } catch {
    // error handled by interceptor
  } finally {
    editSubmitting.value = false
  }
}

async function onDelete(row: LegacyProcurementPageVO) {
  if (!row.legacyId) return
  try {
    await ElMessageBox.confirm(
      t('legacyProcurement.message.deleteConfirm'),
      t('legacyProcurement.message.deleteConfirmTitle'),
      { confirmButtonText: t('common.button.confirm'), cancelButtonText: t('common.button.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await legacyProcurementApi.delete(row.legacyId)
    ElMessage.success(t('legacyProcurement.message.deleteSuccess'))
    await loadData()
  } catch {}
}

loadData()
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filter-card {
  margin-bottom: 0;
}
.table-card {
  margin-bottom: 0;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 20px;
  padding: 0 4px;
}
.detail-section-title {
  grid-column: 1 / -1;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding: 12px 0 6px 0;
  margin-top: 4px;
  border-bottom: 1px solid #ebeef5;
}
.detail-section-title:first-child {
  margin-top: 0;
  padding-top: 4px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 0;
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
}
.drawer-footer {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  margin-top: 8px;
  border-top: 1px solid #ebeef5;
}
.btn-blue {
  color: #E8650A;
}
.btn-danger {
  color: #F56C6C;
}
.receive-13 {
  color: #67C23A;
  font-weight: 600;
}
:deep(.el-table .overdue-row) {
  background-color: #fde2e2 !important;
}
:deep(.el-table .overdue-row:hover > td) {
  background-color: #fbd5d5 !important;
}
:deep(.el-table .overdue-row td) {
  background-color: inherit !important;
}
:deep(.el-table__header-wrapper),
:deep(.el-table__header th.el-table__cell) {
  position: sticky !important;
  top: 0 !important;
  z-index: 10 !important;
  background: inherit;
}
</style>