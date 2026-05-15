<script setup lang="ts">
// 商品编辑抽屉：把 SPU 主信息和 SKU 规格库存组合成一次保存请求。
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Select } from '@element-plus/icons-vue'
import type { Brand, Category, Merchant, ProductSpu, ProductSpuUpsertRequest, SpecGroup } from '@/types/domain'
import {
  makeProductDraft,
  makeSkuDraft,
  productDraftToRequest,
  skuStatusText,
  specKey,
  splitSpecKey,
  type ProductSpuDraft,
} from './productCenterState'

const props = defineProps<{
  modelValue: boolean
  product: ProductSpu | null
  merchants: Merchant[]
  categories: Category[]
  brands: Brand[]
  specGroups: SpecGroup[]
  saving: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  submit: [payload: ProductSpuUpsertRequest]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive<ProductSpuDraft>(
  makeProductDraft(null, {
    merchantId: '',
    categoryId: '',
    brandId: '',
  }),
)

const activeMerchants = computed(() => props.merchants.filter((item) => item.status === 'ACTIVE'))
const selectableCategories = computed(() => props.categories.filter((item) => item.status === 'ACTIVE'))
const selectableBrands = computed(() => props.brands.filter((item) => item.status === 'ACTIVE'))
const specOptions = computed(() =>
  props.specGroups.flatMap((group) =>
    group.options.map((option) => ({
      key: specKey(group.id, option.id),
      groupId: group.id,
      label: `${group.name} / ${option.name}`,
      disabled: group.status !== 'ACTIVE' || option.status !== 'ACTIVE',
    })),
  ),
)

function resetForm(product: ProductSpu | null) {
  const defaultMerchantId = activeMerchants.value[0]?.id ?? props.merchants[0]?.id
  const defaultCategoryId = selectableCategories.value.find((item) => item.type === 'PRODUCT')?.id ?? selectableCategories.value[0]?.id
  const next = makeProductDraft(product, {
    merchantId: defaultMerchantId,
    categoryId: defaultCategoryId,
    brandId: selectableBrands.value[0]?.id,
  })
  Object.assign(form, next)
}

watch(
  () => props.modelValue,
  (nextVisible) => {
    if (nextVisible) resetForm(props.product)
  },
  { immediate: true },
)

watch(
  () => props.product,
  (product) => {
    if (props.modelValue) resetForm(product)
  },
)

function addSku() {
  form.skus.push(makeSkuDraft())
}

function hasDuplicateSpecGroup(keys: string[]) {
  const groupIds = keys.map((key) => splitSpecKey(key).groupId).filter(Boolean)
  return new Set(groupIds).size !== groupIds.length
}

function validateForm() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写商品名称')
    return false
  }
  if (!form.merchantId) {
    ElMessage.warning('请选择所属商家')
    return false
  }
  if (!form.categoryId) {
    ElMessage.warning('请选择商品类目')
    return false
  }
  if (form.status === 'ON_SHELF' && !form.skus.some((sku) => sku.status === 'ON_SHELF')) {
    ElMessage.warning('上架商品至少需要一个上架 SKU')
    return false
  }
  for (const sku of form.skus) {
    if (sku.originalPrice < sku.price) {
      ElMessage.warning('SKU 原价不能小于现价')
      return false
    }
    if (sku.stock < 0) {
      ElMessage.warning('SKU 库存不能小于 0')
      return false
    }
    if (hasDuplicateSpecGroup(sku.selectedSpecKeys)) {
      ElMessage.warning('同一个 SKU 不能选择同一规格组下的多个规格值')
      return false
    }
  }
  return true
}

function submit() {
  if (!validateForm()) return
  emit('submit', productDraftToRequest(form, props.specGroups))
}
</script>

<template>
  <el-drawer v-model="visible" :title="form.id ? '编辑商品' : '新增商品'" size="780px" destroy-on-close>
    <div class="drawer-body">
      <el-form label-width="96px">
        <el-form-item label="商品名称">
          <el-input v-model="form.name" maxlength="80" placeholder="例如：有机鲜牛奶" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" maxlength="120" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="所属商家">
              <el-select v-model="form.merchantId" filterable placeholder="请选择商家">
                <el-option
                  v-for="merchant in props.merchants"
                  :key="merchant.id"
                  :label="merchant.name"
                  :value="merchant.id"
                  :disabled="merchant.status !== 'ACTIVE'"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="商品类目">
              <el-select v-model="form.categoryId" filterable placeholder="请选择类目">
                <el-option
                  v-for="category in props.categories"
                  :key="category.id"
                  :label="`${category.name} · ${category.type === 'PRODUCT' ? '商品' : '频道'}`"
                  :value="category.id"
                  :disabled="category.status !== 'ACTIVE'"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="品牌">
              <el-select v-model="form.brandId" clearable filterable placeholder="不设置品牌">
                <el-option
                  v-for="brand in props.brands"
                  :key="brand.id"
                  :label="brand.name"
                  :value="brand.id"
                  :disabled="brand.status !== 'ACTIVE'"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="上架" value="ON_SHELF" />
                <el-option label="下架" value="OFF_SHELF" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="主图文案">
              <el-input v-model="form.mainImage" maxlength="80" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详情">
          <el-input v-model="form.detail" type="textarea" :rows="3" maxlength="1000" />
        </el-form-item>
        <el-form-item label="详情图片">
          <el-input v-model="form.detailImagesText" type="textarea" :rows="3" placeholder="每行一个图片地址或文案" />
        </el-form-item>
      </el-form>

      <div class="sku-header">
        <h3>SKU</h3>
        <el-button type="primary" plain :icon="Plus" @click="addSku">新增 SKU</el-button>
      </div>

      <el-table :data="form.skus" row-key="clientId" class="sku-table">
        <el-table-column label="编码" min-width="130">
          <template #default="{ row }">
            <el-input v-model="row.skuCode" placeholder="自动生成" />
          </template>
        </el-table-column>
        <el-table-column label="规格" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.selectedSpecKeys" multiple collapse-tags collapse-tags-tooltip filterable placeholder="选择规格">
              <el-option
                v-for="option in specOptions"
                :key="option.key"
                :label="option.label"
                :value="option.key"
                :disabled="option.disabled"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="现价" width="132">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="原价" width="132">
          <template #default="{ row }">
            <el-input-number v-model="row.originalPrice" :min="0" :precision="2" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="库存" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.stock" :min="0" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="单位" width="96">
          <template #default="{ row }">
            <el-input v-model="row.unit" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-select v-model="row.status">
              <el-option v-for="(label, value) in skuStatusText" :key="value" :label="label" :value="value" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :icon="Select" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.drawer-body {
  padding-right: 8px;
}

.sku-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 10px 0 12px;
}

.sku-header h3 {
  margin: 0;
  font-size: 16px;
}

.sku-table :deep(.el-input-number) {
  width: 100%;
}

.sku-table :deep(.el-select) {
  width: 100%;
}
</style>
