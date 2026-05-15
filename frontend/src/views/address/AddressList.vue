<template>
  <!-- 收货地址列表页：结算页可跳转到这里选择或维护地址。 -->
  <div class="address-list">
    <div class="header">
      <el-icon class="back-icon" @click="goBack"><ArrowLeft /></el-icon>
      <h2>管理收货地址</h2>
      <span class="action-text" @click="goToEdit('new')">新建</span>
    </div>

    <div class="list-container" v-if="addresses.length > 0">
      <div class="address-card" v-for="address in addresses" :key="address.id">
        <div class="user-desc">
          <div class="name-phone">
            <span class="name">{{ address.contactName }}</span>
            <span class="phone">{{ address.phone }}</span>
            <el-tag size="small" type="danger" v-if="address.isDefault" style="margin-left: 8px">
              默认
            </el-tag>
            <el-tag size="small" effect="plain" style="margin-left: 8px">{{ address.tag }}</el-tag>
          </div>
          <div class="detail">{{ formatAddress(address) }}</div>
        </div>
        <el-icon class="edit-icon" @click="goToEdit(address.id)"><ArrowRight /></el-icon>
      </div>
    </div>

    <el-empty v-else description="暂无地址，请先新增收货地址" />
  </div>
</template>

<script lang="ts" setup>
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { useAddressStore } from '@/stores/address'
import { formatAddress } from '@/utils/format'

defineOptions({ name: 'AddressList' })

const router = useRouter()
const addressStore = useAddressStore()
const { addresses } = storeToRefs(addressStore)

onMounted(async () => {
  await addressStore.loadAddresses(true)
})

function goBack() {
  router.back()
}

function goToEdit(id: string) {
  router.push(`/address/edit?id=${id}`)
}
</script>

<style scoped>
.address-list {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  background: #fff;
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  border-bottom: 1px solid #f0f0f0;
}

.back-icon {
  font-size: 20px;
  color: #333;
  cursor: pointer;
}

.header h2 {
  flex: 1;
  text-align: center;
  font-size: 16px;
  margin: 0;
  color: #333;
}

.action-text {
  font-size: 14px;
  color: #333;
  cursor: pointer;
}

.list-container {
  padding: 12px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.address-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  display: flex;
  align-items: center;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.03);
}

.user-desc {
  flex: 1;
}

.name-phone {
  margin-bottom: 8px;
  color: #999;
  font-size: 14px;
}

.name {
  color: #333;
  font-size: 16px;
  font-weight: 700;
  margin-right: 12px;
}

.detail {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
}

.edit-icon {
  font-size: 16px;
  color: #ccc;
  cursor: pointer;
}
</style>
