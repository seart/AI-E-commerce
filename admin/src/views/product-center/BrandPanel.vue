<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Plus, Refresh, SwitchButton } from '@element-plus/icons-vue'
import { saveBrand, updateBrandStatus } from '@/api/admin'
import type { Brand, BrandUpsertRequest } from '@/types/domain'
import { commonStatusLabel, commonStatusTag } from './productCenterState'

const props = defineProps<{
  brands: Brand[]
  loading: boolean
}>()

const emit = defineEmits<{
  refresh: []
  changed: []
}>()

const dialogVisible = ref(false)
const saving = ref(false)

const emptyForm = (): BrandUpsertRequest => ({
  id: '',
  name: '',
  logo: '',
  description: '',
  status: 'ACTIVE',
  sortOrder: 100,
})

const form = reactive<BrandUpsertRequest>(emptyForm())

function resetForm(brand?: Brand) {
  Object.assign(form, emptyForm(), brand ?? {})
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(brand: Brand) {
  resetForm(brand)
  dialogVisible.value = true
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写品牌名称')
    return
  }
  saving.value = true
  try {
    await saveBrand({
      ...form,
      id: form.id || undefined,
    })
    ElMessage.success('品牌已保存')
    dialogVisible.value = false
    emit('changed')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(brand: Brand) {
  const nextStatus = brand.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await updateBrandStatus(brand.id, nextStatus)
  ElMessage.success(nextStatus === 'ACTIVE' ? '品牌已启用' : '品牌已停用')
  emit('changed')
}
</script>

<template>
  <div>
    <div class="dictionary-toolbar">
      <el-button :icon="Refresh" @click="emit('refresh')">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增品牌</el-button>
    </div>

    <el-table v-loading="loading" :data="props.brands" row-key="id">
      <el-table-column prop="name" label="品牌" min-width="180">
        <template #default="{ row }">
          <strong>{{ row.name }}</strong>
          <div class="muted">{{ row.id }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="logo" label="Logo" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ row.logo || '未设置' }}</template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="commonStatusTag(row.status)">{{ commonStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="88" />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
          <el-button
            size="small"
            :icon="SwitchButton"
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑品牌' : '新增品牌'" width="560px">
      <el-form label-width="88px">
        <el-form-item label="品牌名称">
          <el-input v-model="form.name" maxlength="80" />
        </el-form-item>
        <el-form-item label="Logo">
          <el-input v-model="form.logo" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dictionary-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 16px;
}
</style>
