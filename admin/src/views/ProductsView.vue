<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMerchants, getProducts, saveProduct } from '@/api/admin'
import type { Merchant, Product } from '@/types/domain'

const products = ref<Product[]>([])
const merchants = ref<Merchant[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)

const emptyForm = () => ({
  id: '',
  merchantId: '',
  categoryId: 'general',
  name: '',
  sales: 0,
  price: 0,
  originalPrice: 0,
  imageText: '商品',
  unit: '件',
  description: '',
  stock: 0,
  status: 'ON_SHELF' as Product['status'],
  sortOrder: 100,
})

const form = reactive<Partial<Product>>(emptyForm())

const activeMerchants = computed(() => merchants.value.filter((item) => item.status === 'ACTIVE'))

function resetForm(product?: Product) {
  Object.assign(form, emptyForm(), product ?? {})
}

async function load() {
  loading.value = true
  try {
    const [merchantRows, productRows] = await Promise.all([getMerchants(), getProducts()])
    merchants.value = merchantRows
    products.value = productRows
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  form.merchantId = activeMerchants.value[0]?.id ?? merchants.value[0]?.id ?? ''
  dialogVisible.value = true
}

function openEdit(product: Product) {
  resetForm(product)
  dialogVisible.value = true
}

async function submit() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写商品名称')
    return
  }
  if (!form.merchantId) {
    ElMessage.warning('请选择所属商家')
    return
  }
  saving.value = true
  try {
    await saveProduct(form)
    ElMessage.success('商品已保存')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(product: Product) {
  const nextStatus = product.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
  await saveProduct({ ...product, status: nextStatus })
  ElMessage.success(nextStatus === 'ON_SHELF' ? '商品已上架' : '商品已下架')
  await load()
}

onMounted(load)
</script>

<template>
  <el-card class="page-card">
    <div class="toolbar">
      <div>
        <h2>商品管理</h2>
        <p class="page-hint">维护商品上下架、库存、价格与所属商家。</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增商品</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="products" row-key="id">
      <el-table-column prop="name" label="商品" min-width="180">
        <template #default="{ row }">
          <strong>{{ row.name }}</strong>
          <div class="muted">{{ row.imageText }} / {{ row.unit }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="merchantName" label="商家" min-width="150" />
      <el-table-column label="价格" width="130">
        <template #default="{ row }">￥{{ Number(row.price).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="100" />
      <el-table-column prop="sales" label="销量" width="100" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ON_SHELF' ? 'success' : 'info'">
            {{ row.status === 'ON_SHELF' ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 'ON_SHELF' ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 'ON_SHELF' ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="680px">
    <el-form label-width="96px">
      <el-form-item label="商品名称">
        <el-input v-model="form.name" placeholder="例如：有机鲜牛奶" />
      </el-form-item>
      <el-form-item label="所属商家">
        <el-select v-model="form.merchantId" filterable placeholder="请选择商家">
          <el-option v-for="merchant in merchants" :key="merchant.id" :label="merchant.name" :value="merchant.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类 ID">
        <el-input v-model="form.categoryId" />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="现价">
            <el-input-number v-model="form.price" :min="0" :precision="2" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="原价">
            <el-input-number v-model="form.originalPrice" :min="0" :precision="2" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="库存">
            <el-input-number v-model="form.stock" :min="0" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="销量">
            <el-input-number v-model="form.sales" :min="0" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="上架" value="ON_SHELF" />
              <el-option label="下架" value="OFF_SHELF" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="展示文案">
            <el-input v-model="form.imageText" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位">
            <el-input v-model="form.unit" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>
