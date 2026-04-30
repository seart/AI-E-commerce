<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Plus, Refresh, SwitchButton } from '@element-plus/icons-vue'
import { saveSpecGroup, saveSpecOption, updateSpecOptionStatus } from '@/api/admin'
import type { SpecGroup, SpecGroupUpsertRequest, SpecOption, SpecOptionUpsertRequest } from '@/types/domain'
import { commonStatusLabel, commonStatusTag } from './productCenterState'

const props = defineProps<{
  specGroups: SpecGroup[]
  loading: boolean
}>()

const emit = defineEmits<{
  refresh: []
  changed: []
}>()

const groupDialogVisible = ref(false)
const optionDialogVisible = ref(false)
const saving = ref(false)
const optionGroupId = ref('')

const emptyGroupForm = (): SpecGroupUpsertRequest => ({
  id: '',
  name: '',
  status: 'ACTIVE',
  sortOrder: 100,
})

const emptyOptionForm = (): SpecOptionUpsertRequest => ({
  id: '',
  name: '',
  status: 'ACTIVE',
  sortOrder: 100,
})

const groupForm = reactive<SpecGroupUpsertRequest>(emptyGroupForm())
const optionForm = reactive<SpecOptionUpsertRequest>(emptyOptionForm())

function openCreateGroup() {
  Object.assign(groupForm, emptyGroupForm())
  groupDialogVisible.value = true
}

function openEditGroup(group: SpecGroup) {
  Object.assign(groupForm, emptyGroupForm(), group)
  groupDialogVisible.value = true
}

function openCreateOption(group: SpecGroup) {
  optionGroupId.value = group.id
  Object.assign(optionForm, emptyOptionForm())
  optionDialogVisible.value = true
}

function openEditOption(option: SpecOption) {
  optionGroupId.value = option.groupId
  Object.assign(optionForm, emptyOptionForm(), option)
  optionDialogVisible.value = true
}

async function submitGroup() {
  if (!groupForm.name.trim()) {
    ElMessage.warning('请填写规格组名称')
    return
  }
  saving.value = true
  try {
    await saveSpecGroup({
      ...groupForm,
      id: groupForm.id || undefined,
    })
    ElMessage.success('规格组已保存')
    groupDialogVisible.value = false
    emit('changed')
  } finally {
    saving.value = false
  }
}

async function submitOption() {
  if (!optionGroupId.value) {
    ElMessage.warning('请选择规格组')
    return
  }
  if (!optionForm.name.trim()) {
    ElMessage.warning('请填写规格值名称')
    return
  }
  saving.value = true
  try {
    await saveSpecOption(optionGroupId.value, {
      ...optionForm,
      id: optionForm.id || undefined,
    })
    ElMessage.success('规格值已保存')
    optionDialogVisible.value = false
    emit('changed')
  } finally {
    saving.value = false
  }
}

async function toggleGroupStatus(group: SpecGroup) {
  const nextStatus = group.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await saveSpecGroup({
    id: group.id,
    name: group.name,
    status: nextStatus,
    sortOrder: group.sortOrder,
  })
  ElMessage.success(nextStatus === 'ACTIVE' ? '规格组已启用' : '规格组已停用')
  emit('changed')
}

async function toggleOptionStatus(option: SpecOption) {
  const nextStatus = option.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await updateSpecOptionStatus(option.id, nextStatus)
  ElMessage.success(nextStatus === 'ACTIVE' ? '规格值已启用' : '规格值已停用')
  emit('changed')
}
</script>

<template>
  <div>
    <div class="dictionary-toolbar">
      <el-button :icon="Refresh" @click="emit('refresh')">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreateGroup">新增规格组</el-button>
    </div>

    <el-table v-loading="loading" :data="props.specGroups" row-key="id">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="option-table-wrap">
            <div class="option-toolbar">
              <strong>规格值</strong>
              <el-button size="small" type="primary" plain :icon="Plus" @click="openCreateOption(row)">新增规格值</el-button>
            </div>
            <el-table :data="row.options" row-key="id" size="small">
              <el-table-column prop="name" label="名称" min-width="160">
                <template #default="{ row: option }">
                  <strong>{{ option.name }}</strong>
                  <div class="muted">{{ option.id }}</div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row: option }">
                  <el-tag :type="commonStatusTag(option.status)">{{ commonStatusLabel(option.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sortOrder" label="排序" width="88" />
              <el-table-column label="操作" width="210" fixed="right">
                <template #default="{ row: option }">
                  <el-button size="small" :icon="Edit" @click="openEditOption(option)">编辑</el-button>
                  <el-button
                    size="small"
                    :icon="SwitchButton"
                    :type="option.status === 'ACTIVE' ? 'warning' : 'success'"
                    @click="toggleOptionStatus(option)"
                  >
                    {{ option.status === 'ACTIVE' ? '停用' : '启用' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="规格组" min-width="180">
        <template #default="{ row }">
          <strong>{{ row.name }}</strong>
          <div class="muted">{{ row.id }}</div>
        </template>
      </el-table-column>
      <el-table-column label="规格值数量" width="120">
        <template #default="{ row }">{{ row.options.length }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="commonStatusTag(row.status)">{{ commonStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="88" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Edit" @click="openEditGroup(row)">编辑</el-button>
          <el-button size="small" type="primary" plain :icon="Plus" @click="openCreateOption(row)">规格值</el-button>
          <el-button
            size="small"
            :icon="SwitchButton"
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            @click="toggleGroupStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="groupDialogVisible" :title="groupForm.id ? '编辑规格组' : '新增规格组'" width="520px">
      <el-form label-width="88px">
        <el-form-item label="规格组名">
          <el-input v-model="groupForm.name" maxlength="60" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="groupForm.status">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="groupForm.sortOrder" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitGroup">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="optionDialogVisible" :title="optionForm.id ? '编辑规格值' : '新增规格值'" width="520px">
      <el-form label-width="88px">
        <el-form-item label="规格值名">
          <el-input v-model="optionForm.name" maxlength="60" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="optionForm.status">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="optionForm.sortOrder" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="optionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitOption">保存</el-button>
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

.option-table-wrap {
  padding: 10px 28px 18px;
  background: #f8fafc;
}

.option-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
</style>
