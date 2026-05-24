<template>
  <el-image
    v-if="url"
    :src="url"
    fit="cover"
    style="width:40px;height:40px;border-radius:4px;cursor:pointer"
    :preview-src-list="[url]"
    preview-teleported
  />
  <span v-else style="color:#d0d5dd;font-size:12px">—</span>
</template>

<script setup lang="ts">
/**
 * 商品图片单元格组件。
 *
 * 用法：
 *   import ProductImageCell from '@/components/ProductImageCell.vue'
 *   const { imageMap, loadImageMap } = useProductImage()
 *
 *   <el-table-column label="图片" width="70" align="center">
 *     <template #default="{ row }">
 *       <ProductImageCell :product-code="row.productCode" :image-map="imageMap" />
 *     </template>
 *   </el-table-column>
 *
 * 对应 Lesson: Lesson 77（COS URL 干净，无 query param）
 */
const props = defineProps<{
  productCode: string | null | undefined
  imageMap: Record<string, string>
}>()

const url = computed(() =>
  props.productCode ? props.imageMap[props.productCode] : undefined,
)
</script>

<script lang="ts">
import { computed } from 'vue'
export default { name: 'ProductImageCell' }
</script>
