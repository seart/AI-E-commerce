<template>
  <div class="profile-view">
    <div class="profile-header">
      <div class="user-info">
        <div class="avatar">{{ profile?.avatarText ?? '我' }}</div>
        <div class="basic-info">
          <div class="username">{{ profile?.nickname ?? '普通用户' }}</div>
          <div class="vip-tag">
            <el-icon><StarFilled /></el-icon>
            {{ profile?.memberLevel ?? '普通会员' }}
          </div>
          <div class="mobile">{{ profile ? formatMaskedPhone(profile.mobile) : '' }}</div>
        </div>
      </div>

      <div class="data-stats">
        <div class="stat-item">
          <div class="val">{{ profile?.stats.redPackets ?? 0 }}</div>
          <div class="label">红包</div>
        </div>
        <div class="stat-item">
          <div class="val">{{ profile?.stats.coupons ?? 0 }}</div>
          <div class="label">优惠券</div>
        </div>
        <div class="stat-item">
          <div class="val">{{ profile?.stats.points ?? 0 }}</div>
          <div class="label">积分</div>
        </div>
        <div class="stat-item">
          <div class="val">{{ profile?.stats.credit ?? 0 }}</div>
          <div class="label">白条</div>
        </div>
      </div>
    </div>

    <div class="menu-panel">
      <div class="menu-card" @click="goToAddress">
        <div class="card-icon blue"><el-icon><Location /></el-icon></div>
        <div class="card-text">我的地址</div>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </div>

      <div class="menu-card" @click="goToOrders">
        <div class="card-icon yellow"><el-icon><Document /></el-icon></div>
        <div class="card-text">我的订单</div>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </div>

      <div class="menu-card" @click="doLogout">
        <div class="card-icon"><el-icon><SwitchButton /></el-icon></div>
        <div class="card-text">退出登录</div>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </div>
    </div>

    <TabBar />
  </div>
</template>

<script lang="ts" setup>
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  StarFilled,
  Location,
  ArrowRight,
  SwitchButton,
  Document,
} from '@element-plus/icons-vue'
import TabBar from '@/components/TabBar.vue'
import { useAddressStore } from '@/stores/address'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { useOrderStore } from '@/stores/order'
import { useProfileStore } from '@/stores/profile'
import { formatMaskedPhone } from '@/utils/format'

defineOptions({ name: 'ProfileView' })

const router = useRouter()
const authStore = useAuthStore()
const addressStore = useAddressStore()
const cartStore = useCartStore()
const orderStore = useOrderStore()
const profileStore = useProfileStore()
const { profile } = storeToRefs(profileStore)

onMounted(async () => {
  await Promise.all([profileStore.loadProfile(true), addressStore.loadAddresses()])
})

function goToAddress() {
  router.push('/address')
}

function goToOrders() {
  router.push('/orders')
}

async function doLogout() {
  await authStore.logout()
  profileStore.reset()
  addressStore.reset()
  cartStore.reset()
  orderStore.reset()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.profile-view {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 70px;
}

.profile-header {
  background: linear-gradient(180deg, #e1251b 0%, #ff7a18 100%);
  padding: 40px 18px 24px;
  border-bottom-left-radius: 18px;
  border-bottom-right-radius: 18px;
  color: #fff;
  position: relative;
  z-index: 1;
}

.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
  font-weight: 700;
}

.basic-info .username {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 8px;
}

.basic-info .vip-tag {
  display: inline-flex;
  align-items: center;
  background: rgba(0, 0, 0, 0.16);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  gap: 4px;
}

.mobile {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.9;
}

.data-stats {
  display: flex;
  justify-content: space-between;
  background: #fff;
  border-radius: 16px;
  padding: 16px 0;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  position: absolute;
  bottom: -40px;
  left: 18px;
  right: 18px;
  color: #333;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.stat-item::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  height: 20px;
  width: 1px;
  background: #eee;
}

.stat-item:last-child::after {
  display: none;
}

.val {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 4px;
}

.label {
  font-size: 12px;
  color: #666;
}

.menu-panel {
  margin-top: 56px;
  padding: 0 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.menu-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  display: flex;
  align-items: center;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.03);
  cursor: pointer;
}

.card-icon {
  width: 32px;
  height: 32px;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #999;
  margin-right: 12px;
}

.card-icon.blue {
  background: #e6f3ff;
  color: #1f9af8;
}

.card-icon.yellow {
  background: #fff5d6;
  color: #fb8500;
}

.card-text {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.arrow {
  color: #ccc;
  font-size: 14px;
}
</style>
