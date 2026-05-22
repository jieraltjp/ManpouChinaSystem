<template>
  <div class="excel-wrap">
    <!-- TODO: 后续可能加复制按钮 -->
    <!-- <div class="excel-toolbar">...</div> -->
    <div ref="containerRef" class="excel-container" />
    <div v-if="copied" class="copy-toast">已复制</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import Spreadsheet from 'x-data-spreadsheet'

export interface ExcelColDef {
  prop: string
  label: string
  excluded?: boolean
  numeric?: boolean
  formatter?: (row: any) => string
}

const props = defineProps<{
  columns: ExcelColDef[]
  data: any[]
}>()

const containerRef = ref<HTMLDivElement>()
const copied = ref(false)
let xs: InstanceType<typeof Spreadsheet> | null = null

const visibleCols = computed(() => props.columns.filter(c => !c.excluded))

// 表头样式：加粗 + 灰底
const HEADER_STYLE = {
  bgcolor: '#E8E8E8',
  color: '#1A1A1A',
  align: 'left',
  valign: 'middle',
  font: { name: 'Arial', size: 10, bold: true, italic: false },
  format: 'normal',
}

// 估算单个字符宽度（px），Arial 10pt
function charWidth(ch: string): number {
  const c = ch.charCodeAt(0)
  if (c >= 0x4E00) return 14       // CJK
  if (c >= 0x3040) return 12       // 日语假名
  if (c >= 0xAC00) return 14       // 谚文
  if (c >= 0x0030 && c <= 0x0039) return 6  // 数字
  if (c >= 0x0041 && c <= 0x005A) return 7  // 大写英文
  return 7                          // 小写英文/符号
}

function measureText(text: string): number {
  return [...text].reduce((sum, ch) => sum + charWidth(ch), 0)
}

function calcColWidths() {
  const cols = visibleCols.value
  const colWidths: Record<number, { width: number }> = {}
  cols.forEach((col, ci) => {
    const samples = [col.label]
    props.data.forEach(row => {
      const val = col.formatter ? col.formatter(row) : String(row[col.prop] ?? '')
      if (val) samples.push(val)
    })
    const maxWidth = Math.max(...samples.map(measureText))
    const width = Math.min(Math.max(maxWidth + 24, 60), 300)  // 24px padding, 60~300px
    colWidths[ci] = { width }
  })
  return colWidths
}

function buildSheetData() {
  const cols = visibleCols.value
  const rows: any = {}

  // 第 0 行 = 表头
  const headerCells: any = {}
  cols.forEach((col, ci) => {
    headerCells[ci] = { text: col.label, style: 0 }
  })
  rows[0] = { cells: headerCells, height: 25 }

  // 后续行 = 数据
  props.data.forEach((row, ri) => {
    const cells: any = {}
    cols.forEach((col, ci) => {
      const text = col.formatter ? col.formatter(row) : String(row[col.prop] ?? '')
      cells[ci] = { text }
    })
    rows[ri + 1] = { cells }
  })

  return {
    name: 'Sheet1',
    styles: [HEADER_STYLE],
    rows,
    cols: calcColWidths(),
  }
}

onMounted(() => {
  if (!containerRef.value) return
  xs = new Spreadsheet(containerRef.value, {
    showToolbar: false,
    showBottomBar: false,
    showGrid: true,
    showContextmenu: false,
    mode: 'read',     // read 模式：点击选中单元格，支持 Ctrl+C 复制
    view: {
      height: () => containerRef.value?.clientHeight ?? 400,
      width: () => containerRef.value?.clientWidth ?? 900,
    },
    row: {
      len: props.data.length + 1,
      height: 25,
    },
    col: {
      len: visibleCols.value.length,
      width: 120,
      indexWidth: 60,
      minWidth: 60,
    },
  })

  xs.loadData([buildSheetData()])
})

watch(
  () => [props.data, props.columns],
  () => {
    if (!xs) return
    xs.loadData([buildSheetData()])
  },
  { deep: true }
)

onUnmounted(() => {
  xs = null
})
</script>

<style scoped>
.excel-wrap {
  position: relative;
  border: 1px solid #D4D4D4;
  border-radius: 4px;
  overflow: hidden;
}

.excel-container {
  height: calc(100vh - 240px);
  min-height: 300px;
}

.copy-toast {
  position: absolute;
  top: 8px;
  right: 12px;
  background: #1A1A1A;
  color: #fff;
  font-size: 12px;
  padding: 6px 14px;
  border-radius: 4px;
  pointer-events: none;
  z-index: 10;
}
</style>

<style>
.x-spreadsheet {
  font-family: Arial, "Microsoft YaHei", sans-serif !important;
}
</style>
