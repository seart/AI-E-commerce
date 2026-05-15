<template>
  <!-- 注册页：创建用户账号，注册成功后引导回登录页。 -->
  <div class="register-page">
    <div class="register-card">
      <div class="register-avatar" aria-hidden="true">
        <div class="avatar-head"></div>
        <div class="avatar-body"></div>
      </div>

      <div class="register-title">创建企业用户账号</div>
      <div class="register-subtitle">注册后即可接入真实鉴权接口或使用 mock 联调</div>

      <el-form :model="form" class="register-form" @submit.prevent="submit">
        <el-form-item>
          <el-input v-model.trim="form.mobile" placeholder="请输入手机号" type="tel" clearable />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.password"
            placeholder="请输入密码"
            type="password"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.confirmPassword"
            placeholder="确认密码"
            type="password"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="register-button"
            :loading="submitting"
            @click="submit"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <el-button type="info" link class="register-link" @click="goLogin">已有账号去登录</el-button>
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

defineOptions({ name: 'RegisterView' })

const router = useRouter()
const authStore = useAuthStore()
const { submitting } = storeToRefs(authStore)

const form = reactive({
  mobile: '',
  password: '',
  confirmPassword: '',
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

  if (form.password.length < 6) {
    ElMessage.warning('密码长度至少 6 位')
    return
  }

  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  try {
    await authStore.register({ ...form })
    ElMessage.success('注册成功，请登录')
    router.push({ name: 'Login' })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败，请稍后重试')
  }
}

function goLogin() {
  router.push({ name: 'Login' })
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top, rgba(31, 154, 248, 0.12), transparent 30%),
    linear-gradient(180deg, #f7fbff 0%, #ffffff 100%);
}

.register-card {
  width: 380px;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 28px;
  box-shadow: 0 28px 72px rgba(31, 126, 252, 0.12);
  padding: 32px 28px;
  display: flex;
  flex-direction: column;
}

.register-avatar {
  position: relative;
  width: 66px;
  height: 66px;
  margin: 0 auto 24px;
  border-radius: 50%;
  background: linear-gradient(180deg, #1f9af8 0%, #0e85f0 100%);
}

.avatar-head {
  position: absolute;
  top: 21px;
  left: 50%;
  width: 20px;
  height: 20px;
  margin-left: -10px;
  border-radius: 50%;
  background: linear-gradient(180deg, #eef4f7 0%, #d6dde2 100%);
}

.avatar-body {
  position: absolute;
  left: 50%;
  bottom: 13px;
  width: 40px;
  height: 20px;
  margin-left: -20px;
  border-radius: 20px 20px 18px 18px;
  background: linear-gradient(180deg, #b7dbef 0%, #7db8e5 100%);
}

.register-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: #222;
  margin-bottom: 8px;
}

.register-subtitle {
  text-align: center;
  font-size: 13px;
  line-height: 1.6;
  color: #8c8f99;
  margin-bottom: 24px;
}

.register-form .el-form-item {
  margin-bottom: 16px;
}

.register-button {
  width: 100%;
  height: 48px;
  border-radius: 24px;
  margin-top: 8px;
  font-size: 16px;
}

.register-footer {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.register-link {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.58);
}
</style>
