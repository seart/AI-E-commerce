<script setup lang="ts">
// 后台登录页：只允许 ADMIN / OPERATOR 登录后进入后台路由。
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/admin'
import { useSessionStore } from '@/stores/session'

const router = useRouter()
const session = useSessionStore()
const mobile = ref('13900000000')
const password = ref('123456')
const loading = ref(false)

async function submit() {
  loading.value = true
  try {
    const data = await login(mobile.value, password.value)
    if (data.user.role !== 'ADMIN' && data.user.role !== 'OPERATOR') {
      ElMessage.error('当前账号没有后台访问权限')
      return
    }
    session.setSession(data)
    router.replace('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <p class="eyebrow">JINGDONG ENTERPRISE</p>
      <h1>运营后台</h1>
      <p class="hint">管理订单、商品、商家、用户和审计日志</p>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="管理员手机号">
          <el-input v-model="mobile" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" size="large" type="password" show-password />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" class="submit" @click="submit">
          登录后台
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 20% 20%, rgba(225, 37, 27, 0.2), transparent 32%),
    linear-gradient(135deg, #101828 0%, #1f2937 100%);
}

.login-panel {
  width: min(420px, calc(100vw - 32px));
  padding: 42px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.28);
}

.eyebrow {
  color: #e1251b;
  font-weight: 800;
  letter-spacing: 0.16em;
}

h1 {
  margin: 0;
  font-size: 42px;
}

.hint {
  color: #667085;
  margin-bottom: 28px;
}

.submit {
  width: 100%;
}
</style>
