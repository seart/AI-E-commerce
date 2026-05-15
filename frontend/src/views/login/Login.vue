<template>
  <!-- 登录页：成功后写入 auth store，并由路由守卫放行受保护页面。 -->
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="brand">企业版商城</div>
        <div class="title">欢迎登录</div>
        <div class="subtitle">支持真实 API 对接，默认开发模式可切换 mock 联调</div>
      </div>

      <el-form :model="form" class="login-form" @submit.prevent="submit">
        <el-form-item>
          <el-input v-model.trim="form.mobile" placeholder="请输入手机号" clearable />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            clearable
          />
        </el-form-item>

        <div class="helper-text">开发联调用例：`13800000000 / 123456`</div>

        <el-form-item>
          <el-button
            type="primary"
            class="login-button"
            :loading="submitting"
            @click="submit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span class="login-link" @click="onRegister">立即注册</span>
        <span class="login-split">|</span>
        <span class="login-link" @click="onForgot">忘记密码</span>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'LoginView' })

const router = useRouter()
const authStore = useAuthStore()
const { submitting } = storeToRefs(authStore)

const form = reactive({
  mobile: '',
  password: '',
})

const phonePattern = /^1\d{10}$/

async function submit() {
  if (!phonePattern.test(form.mobile)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  if (!form.password.trim()) {
    ElMessage.warning('请输入密码')
    return
  }

  try {
    await authStore.login({
      mobile: form.mobile,
      password: form.password,
    })
    ElMessage.success('登录成功')
    router.push({ name: 'Home' })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败，请稍后重试')
  }
}

function onRegister() {
  router.push({ name: 'Register' })
}

function onForgot() {
  ElMessage.info('请联系企业管理员或对接短信找回接口')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top, rgba(225, 37, 27, 0.12), transparent 28%),
    linear-gradient(180deg, #fff5f2 0%, #ffffff 100%);
}

.login-card {
  width: 380px;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 28px;
  box-shadow: 0 28px 72px rgba(196, 56, 23, 0.12);
  padding: 34px 28px;
  border: 1px solid rgba(225, 37, 27, 0.08);
}

.login-header {
  margin-bottom: 28px;
}

.brand {
  font-size: 26px;
  font-weight: 700;
  color: #222222;
  margin-bottom: 10px;
}

.title {
  font-size: 18px;
  color: #333333;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 13px;
  line-height: 1.6;
  color: #8c8f99;
}

.helper-text {
  font-size: 12px;
  color: #8c8f99;
  margin-bottom: 16px;
}

.login-form .el-form-item {
  margin-bottom: 16px;
}

.login-button {
  width: 100%;
  height: 48px;
  border-radius: 24px;
  font-size: 16px;
}

.login-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 18px;
  color: #8f92a1;
  font-size: 14px;
}

.login-link {
  color: #e1251b;
  cursor: pointer;
}

.login-split {
  color: #c9c9d0;
}
</style>
