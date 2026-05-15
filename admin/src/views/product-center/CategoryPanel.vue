<script setup lang="ts">
// 类目管理面板：首页频道和商品类目共用同一套管理能力。
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Plus, Refresh, SwitchButton } from '@element-plus/icons-vue'
import { saveCategory, updateCategoryStatus } from '@/api/admin'
import type { Category, CategoryUpsertRequest } from '@/types/domain'
import { categoryTypeText, commonStatusLabel, commonStatusTag } from './productCenterState'

const props = defineProps<{
  categories: Category[]
  loading: boolean
}>()

const emit = defineEmits<{
  refresh: []
  changed: []
}>()

const dialogVisible = ref(false)
const saving = ref(false)

const emptyForm = (): CategoryUpsertRequest => ({
  id: '',
  name: '',
  icon: '',
  parentId: null,
  level: 1,
  type: 'PRODUCT',
  status: 'ACTIVE',
  sortOrder: 100,
})

const form = reactive<CategoryUpsertRequest>(emptyForm())

function resetForm(category?: Category) {
  Object.assign(form, emptyForm(), category ?? {})
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(category: Category) {
  resetForm(category)
  dialogVisible.value = true
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写类目名称')
    return
  }
  saving.value = true
  try {
    await saveCategory({
      ...form,
      id: form.id || undefined,
      parentId: form.parentId || null,
    })
    ElMessage.success('类目已保存')
    dialogVisible.value = false
    emit('changed')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(category: Category) {
  const nextStatus = category.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await updateCategoryStatus(category.id, nextStatus)
  ElMessage.success(nextStatus === 'ACTIVE' ? '类目已启用' : '类目已停用')
  emit('changed')
}
</script>

<template>
  <div>
    <div class="dictionary-toolbar">
      <el-button :icon="Refresh" @click="emit('refresh')">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增类目</el-button>
    </div>

    <el-table v-loading="loading" :data="props.categories" row-key="id">
      <el-table-column prop="name" label="类目" min-width="180">
        <template #default="{ row }">
          <strong>{{ row.name }}</strong>
          <div class="muted">{{ row.id }}</div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ categoryTypeText(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="level" label="层级" width="90" />
      <el-table-column prop="parentId" label="父级" min-width="130">
        <template #default="{ row }">{{ row.parentId || '无' }}</template>
      </el-table-column>
      <el-table-column prop="icon" label="图标" min-width="160" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑类目' : '新增类目'" width="560px">
      <el-form label-width="88px">
        <el-form-item label="类目名称">
          <el-input v-model="form.name" maxlength="60" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型">
              <el-select v-model="form.type">
                <el-option label="商品类目" value="PRODUCT" />
                <el-option label="首页频道" value="CHANNEL" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="父级 ID">
              <el-input v-model="form.parentId" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="层级">
              <el-input-number v-model="form.level" :min="1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
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
