<template>
  <div class="tab-bar">
    <div 
      class="tab-item" 
      :class="{ active: currentPath === '/' }"
      @click="navigateTo('/')"
    >
      <el-icon><HomeFilled /></el-icon>
      <span>首页</span>
    </div>
    <div 
      class="tab-item" 
      :class="{ active: currentPath === '/cart' }"
      @click="navigateTo('/cart')"
    >
      <el-icon><ShoppingCart /></el-icon>
      <span>购物车</span>
    </div>
    <div 
      class="tab-item" 
      :class="{ active: currentPath === '/orders' }"
      @click="navigateTo('/orders')"
    >
      <el-icon><Document /></el-icon>
      <span>订单</span>
    </div>
    <div 
      class="tab-item" 
      :class="{ active: currentPath === '/profile' }"
      @click="navigateTo('/profile')"
    >
      <el-icon><User /></el-icon>
      <span>我的</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, ShoppingCart, Document, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const currentPath = computed(() => route.path)

const navigateTo = (path: string) => {
  if (currentPath.value !== path) {
    router.push(path)
  }
}
</script>

<style scoped>
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  height: 50px;
  background-color: #fff;
  border-top: 1px solid #f1f1f1;
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding-bottom: env(safe-area-inset-bottom);
  box-sizing: content-box;
  z-index: 100;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #333;
  cursor: pointer;
  flex: 1;
}

.tab-item .el-icon {
  font-size: 20px;
  margin-bottom: 2px;
}

.tab-item.active {
  color: #1f9af8;
}
</style>
