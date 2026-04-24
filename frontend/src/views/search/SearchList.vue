<template>
  <div class="search-list">
    <div class="search-header">
      <el-icon class="back-icon" @click="goBack"><ArrowLeft /></el-icon>
      <el-input
        v-model.trim="keyword"
        class="search-input"
        :prefix-icon="Search"
        clearable
        placeholder="搜索商家或商品"
        @keyup.enter="runSearch"
      />
    </div>

    <el-skeleton :loading="searchLoading" animated :rows="5">
      <template #default>
        <div class="list-container" v-if="merchants.length > 0">
          <div
            class="merchant-card"
            v-for="item in merchants"
            :key="item.id"
            @click="goToMerchant(item.id)"
          >
            <div class="merchant-img" :style="{ background: item.heroColor }">
              <div class="img-placeholder">{{ item.logoText }}</div>
            </div>
            <div class="merchant-info">
              <div class="merchant-name">{{ item.name }}</div>
              <div class="merchant-sales">
                月售{{ item.monthlySales }} | 起送¥{{ item.minOrderAmount }} | 配送费¥{{ item.deliveryFee }}
              </div>
              <div class="merchant-tags">
                <el-tag
                  v-for="tag in item.tags"
                  :key="tag"
                  type="danger"
                  size="small"
                  effect="plain"
                  class="custom-tag"
                >
                  {{ tag }}
                </el-tag>
              </div>
              <div class="merchant-desc">{{ item.description }}</div>
            </div>
          </div>
        </div>

        <el-empty v-else description="未搜索到相关商家" />
      </template>
    </el-skeleton>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Search } from '@element-plus/icons-vue'
import { useCatalogStore } from '@/stores/catalog'

defineOptions({ name: 'SearchList' })

const route = useRoute()
const router = useRouter()
const catalogStore = useCatalogStore()
const { merchants, searchLoading } = storeToRefs(catalogStore)
const keyword = ref('')

async function runSearch() {
  await catalogStore.searchMerchants(keyword.value)
  catalogStore.addSearchHistory(keyword.value)
}

onMounted(async () => {
  keyword.value = String(route.query.q ?? '')
  await runSearch()
})

watch(
  () => route.query.q,
  async (value) => {
    const nextKeyword = String(value ?? '')
    if (nextKeyword === keyword.value) {
      return
    }

    keyword.value = nextKeyword
    await runSearch()
  },
)

function goBack() {
  router.back()
}

function goToMerchant(id: string) {
  router.push(`/merchant/${id}`)
}
</script>

<style scoped>
.search-list {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 12px 18px;
}

.search-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.back-icon {
  font-size: 20px;
  color: #333;
  margin-right: 12px;
  cursor: pointer;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 18px;
  background: #fff;
  box-shadow: none;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.merchant-card {
  background: #fff;
  border-radius: 16px;
  padding: 12px;
  display: flex;
  gap: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.03);
  cursor: pointer;
}

.merchant-img {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 16px;
  overflow: hidden;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #fff;
  font-weight: 700;
}

.merchant-info {
  flex: 1;
}

.merchant-name {
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

.merchant-sales,
.merchant-desc {
  font-size: 12px;
  color: #666;
  margin-top: 6px;
}

.merchant-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.custom-tag {
  border-radius: 10px;
}
</style>
