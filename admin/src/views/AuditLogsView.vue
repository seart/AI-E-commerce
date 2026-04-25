<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getAuditLogs } from '@/api/admin'
import type { AuditLog } from '@/types/domain'

const logs = ref<AuditLog[]>([])
const loading = ref(false)
const filters = reactive({
  actorId: '',
  action: '',
  range: [] as string[],
})

async function load() {
  loading.value = true
  try {
    logs.value = await getAuditLogs({
      actorId: filters.actorId || undefined,
      action: filters.action || undefined,
      from: filters.range[0],
      to: filters.range[1],
    })
  } finally {
    loading.value = false
  }
}

function reset() {
  filters.actorId = ''
  filters.action = ''
  filters.range = []
  load()
}

onMounted(load)
</script>

<template>
  <el-card class="page-card">
    <div class="toolbar">
      <div>
        <h2>审计日志</h2>
        <p class="page-hint">追踪登录、下单、退款、后台变更等关键操作。</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-form class="filter-bar" inline>
      <el-form-item label="操作者">
        <el-input v-model="filters.actorId" clearable placeholder="用户 ID" />
      </el-form-item>
      <el-form-item label="动作">
        <el-input v-model="filters.action" clearable placeholder="例如 ADMIN_SAVE_PRODUCT" />
      </el-form-item>
      <el-form-item label="时间">
        <el-date-picker
          v-model="filters.range"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">筛选</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="logs" row-key="id">
      <el-table-column prop="createdAt" label="时间" min-width="190" />
      <el-table-column prop="actorId" label="操作者" min-width="160">
        <template #default="{ row }">{{ row.actorId || '匿名/登录前' }}</template>
      </el-table-column>
      <el-table-column prop="actorRole" label="角色" width="110">
        <template #default="{ row }">{{ row.actorRole || '-' }}</template>
      </el-table-column>
      <el-table-column prop="action" label="动作" min-width="210" />
      <el-table-column label="对象" min-width="170">
        <template #default="{ row }">{{ row.targetType }} / {{ row.targetId || '-' }}</template>
      </el-table-column>
      <el-table-column prop="detail" label="详情" min-width="180" />
      <el-table-column prop="requestId" label="Request ID" min-width="220" />
    </el-table>
  </el-card>
</template>
