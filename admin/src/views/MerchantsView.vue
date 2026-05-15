<script setup lang="ts">
// 商家管理页：维护商家基础资料和启停状态。
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMerchants, saveMerchant } from '@/api/admin'
import type { Merchant } from '@/types/domain'

const merchants = ref<Merchant[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)

const emptyForm = () => ({
  id: '',
  name: '',
  sales: 0,
  minOrderPrice: 0,
  deliveryFee: 0,
  deliveryMinutes: 30,
  description: '',
  notice: '',
  rating: 5,
  logoBackground: 'linear-gradient(180deg, #2f6fed 0%, #194ec8 100%)',
  logoText: '',
  status: 'ACTIVE' as Merchant['status'],
  sortOrder: 100,
})

const form = reactive<Partial<Merchant>>(emptyForm())

function resetForm(merchant?: Merchant) {
  Object.assign(form, emptyForm(), merchant ?? {})
}

async function load() {
  loading.value = true
  try {
    merchants.value = await getMerchants()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(merchant: Merchant) {
  resetForm(merchant)
  dialogVisible.value = true
}

async function submit() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写商家名称')
    return
  }
  if (!form.logoText?.trim()) {
    form.logoText = form.name.slice(0, 2)
  }
  saving.value = true
  try {
    await saveMerchant(form)
    ElMessage.success('商家已保存')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(merchant: Merchant) {
  const nextStatus = merchant.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await saveMerchant({ ...merchant, status: nextStatus })
  ElMessage.success(nextStatus === 'ACTIVE' ? '商家已启用' : '商家已停用')
  await load()
}

onMounted(load)
</script>

<template>
  <el-card class="page-card">
    <div class="toolbar">
      <div>
        <h2>商家管理</h2>
        <p class="page-hint">控制商家资料、配送信息和启停状态。</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增商家</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="merchants" row-key="id">
      <el-table-column prop="name" label="商家" min-width="170">
        <template #default="{ row }">
          <strong>{{ row.name }}</strong>
          <div class="muted">{{ row.description }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="sales" label="销量" width="100" />
      <el-table-column label="起送/配送" width="150">
        <template #default="{ row }">￥{{ row.minOrderPrice }} / ￥{{ row.deliveryFee }}</template>
      </el-table-column>
      <el-table-column prop="deliveryMinutes" label="配送分钟" width="110" />
      <el-table-column prop="rating" label="评分" width="90" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 'ACTIVE' ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商家' : '新增商家'" width="720px">
    <el-form label-width="104px">
      <el-form-item label="商家名称">
        <el-input v-model="form.name" placeholder="例如：到家旗舰店" />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="销量">
            <el-input-number v-model="form.sales" :min="0" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="评分">
            <el-input-number v-model="form.rating" :min="0" :max="5" :step="0.1" :precision="1" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="起送价">
            <el-input-number v-model="form.minOrderPrice" :min="0" :precision="2" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="配送费">
            <el-input-number v-model="form.deliveryFee" :min="0" :precision="2" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="配送分钟">
            <el-input-number v-model="form.deliveryMinutes" :min="1" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="Logo 文案">
            <el-input v-model="form.logoText" maxlength="4" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="启用" value="ACTIVE" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="背景">
            <el-input v-model="form.logoBackground" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="公告">
        <el-input v-model="form.notice" />
      </el-form-item>
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
