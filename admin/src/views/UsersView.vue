<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getUsers, updateUserStatus } from '@/api/admin'
import { useSessionStore } from '@/stores/session'
import type { AdminUser } from '@/types/domain'

const users = ref<AdminUser[]>([])
const loading = ref(false)
const session = useSessionStore()

async function load() {
  loading.value = true
  try {
    users.value = await getUsers()
  } finally {
    loading.value = false
  }
}

async function toggleStatus(user: AdminUser) {
  const nextStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await updateUserStatus(user.id, nextStatus)
  ElMessage.success(nextStatus === 'ACTIVE' ? '用户已启用' : '用户已禁用')
  await load()
}

onMounted(load)
</script>

<template>
  <el-card class="page-card">
    <div class="toolbar">
      <div>
        <h2>用户管理</h2>
        <p class="page-hint">查看用户角色与状态，支持禁用异常账号。</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="users" row-key="id">
      <el-table-column prop="nickname" label="昵称" min-width="150" />
      <el-table-column prop="mobile" label="手机号" width="150" />
      <el-table-column prop="memberLevel" label="会员等级" width="120" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="row.role === 'CUSTOMER' ? 'info' : 'success'">{{ row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginAt" label="最近登录" min-width="190">
        <template #default="{ row }">{{ row.lastLoginAt || '未登录' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            :disabled="row.id === session.user?.id"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
