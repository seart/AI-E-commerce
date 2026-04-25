<script setup lang="ts">
import {
  DataBoard,
  DocumentChecked,
  Goods,
  Memo,
  Shop,
  User,
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { logout } from '@/api/admin'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

async function signOut() {
  await logout().catch(() => undefined)
  session.clear()
  router.replace('/login')
}
</script>

<template>
  <el-container class="shell">
    <el-aside width="238px" class="aside">
      <div class="brand">
        <strong>JD Admin</strong>
        <span>企业商城运营台</span>
      </div>
      <el-menu router :default-active="route.path" background-color="#111827" text-color="#cbd5e1" active-text-color="#fff">
        <el-menu-item index="/dashboard"><el-icon><DataBoard /></el-icon>数据概览</el-menu-item>
        <el-menu-item index="/orders"><el-icon><DocumentChecked /></el-icon>订单管理</el-menu-item>
        <el-menu-item index="/products"><el-icon><Goods /></el-icon>商品管理</el-menu-item>
        <el-menu-item index="/merchants"><el-icon><Shop /></el-icon>商家管理</el-menu-item>
        <el-menu-item index="/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
        <el-menu-item index="/audit-logs"><el-icon><Memo /></el-icon>审计日志</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div>
          <strong>{{ session.user?.nickname }}</strong>
          <span>{{ session.user?.role }}</span>
        </div>
        <el-button @click="signOut">退出登录</el-button>
      </el-header>
      <el-main>
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.shell {
  min-height: 100vh;
}

.aside {
  background: #111827;
}

.brand {
  height: 86px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 24px;
  color: #fff;
}

.brand strong {
  font-size: 24px;
}

.brand span {
  color: #94a3b8;
  margin-top: 4px;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 0 rgba(17, 24, 39, 0.08);
}

.header div {
  display: flex;
  gap: 10px;
  align-items: baseline;
}

.header span {
  color: #667085;
}
</style>
