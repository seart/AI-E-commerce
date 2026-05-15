<template>
  <!-- 移动端首页：展示地址、搜索入口、频道、商家和推荐商品。 -->
  <div class="home-page">
    <div class="header">
      <el-icon class="location-icon"><Location /></el-icon>
      <span class="location-text">
        {{ defaultAddress ? formatAddress(defaultAddress) : '点击配置收货地址' }}
      </span>
      <el-icon class="bell-icon"><Bell /></el-icon>
    </div>

    <div class="search-wrap" @click="goToSearch">
      <el-input
        v-model="searchKeyword"
        class="search-input"
        placeholder="搜索商品、商家或活动"
        :prefix-icon="Search"
        readonly
      />
    </div>

    <el-skeleton :loading="homeLoading" animated :rows="6">
      <template #template>
        <div class="banner-skeleton"></div>
      </template>

      <template #default>
        <div class="banner" v-if="home">
          <el-carousel height="140px" :show-indicators="true" class="custom-carousel">
            <el-carousel-item v-for="banner in home.banners" :key="banner.id">
              <div class="banner-item" :style="{ background: banner.background }">
                <div class="banner-title">{{ banner.title }}</div>
                <div class="banner-subtitle">{{ banner.subtitle }}</div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>

        <div class="category-grid" v-if="home?.categories?.length">
          <div v-for="item in home.categories" :key="item.id" class="category-item">
            <img :src="item.icon" alt="category" class="category-img" />
            <span class="category-name">{{ item.name }}</span>
          </div>
        </div>

        <div class="section">
          <div class="section-head">
            <div class="section-title">优选商家</div>
            <div class="section-subtitle">接入真实库存与活动价格的商家列表</div>
          </div>

          <div class="merchant-list" v-if="home?.featuredMerchants?.length">
            <div
              v-for="merchant in home.featuredMerchants"
              :key="merchant.id"
              class="merchant-card"
              @click="goToMerchant(merchant.id)"
            >
              <div class="merchant-cover" :style="{ background: merchant.heroColor }">
                <span>{{ merchant.logoText }}</span>
              </div>
              <div class="merchant-info">
                <div class="merchant-title-row">
                  <div class="merchant-name">{{ merchant.name }}</div>
                  <div class="merchant-rating">{{ merchant.rating.toFixed(1) }}</div>
                </div>
                <div class="merchant-meta">
                  月售 {{ merchant.monthlySales }} | {{ merchant.etaMinutes }} 分钟送达
                </div>
                <div class="merchant-tags">
                  <el-tag
                    v-for="tag in merchant.tags.slice(0, 3)"
                    :key="tag"
                    type="danger"
                    size="small"
                    effect="plain"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
                <div class="merchant-desc">{{ merchant.description }}</div>
              </div>
            </div>
          </div>

          <el-empty v-else description="暂无可用商家数据" />
        </div>
      </template>
    </el-skeleton>

    <TabBar />
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { Location, Bell, Search } from '@element-plus/icons-vue'
import TabBar from '@/components/TabBar.vue'
import { useAddressStore } from '@/stores/address'
import { useCatalogStore } from '@/stores/catalog'
import { formatAddress } from '@/utils/format'

defineOptions({ name: 'HomeView' })

const router = useRouter()
const addressStore = useAddressStore()
const catalogStore = useCatalogStore()
const { defaultAddress } = storeToRefs(addressStore)
const { home, homeLoading } = storeToRefs(catalogStore)

const searchKeyword = ref('')

onMounted(async () => {
  await Promise.all([catalogStore.loadHomePage(), addressStore.loadAddresses()])
})

function goToSearch() {
  router.push('/search')
}

function goToMerchant(merchantId: string) {
  router.push(`/merchant/${merchantId}`)
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff4ef 0%, #f5f6f8 22%, #f5f5f5 100%);
  padding: 16px 18px 76px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  color: #333;
}

.location-icon,
.bell-icon {
  font-size: 20px;
}

.location-text {
  flex: 1;
  font-size: 15px;
  margin: 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-wrap {
  margin-bottom: 14px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 20px rgba(225, 37, 27, 0.06);
}

.search-input :deep(.el-input__inner) {
  height: 36px;
  font-size: 14px;
}

.banner,
.banner-skeleton {
  margin-bottom: 16px;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.05);
}

.banner-skeleton {
  height: 140px;
  background: linear-gradient(90deg, #f2f2f2 25%, #fafafa 37%, #f2f2f2 63%);
}

.banner-item {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 24px;
  color: #fff;
}

.banner-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 10px;
}

.banner-subtitle {
  font-size: 14px;
  opacity: 0.92;
}

.category-grid {
  display: flex;
  flex-wrap: wrap;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 18px;
  padding: 14px 0 4px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.04);
  margin-bottom: 18px;
}

.category-item {
  width: 20%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 14px;
}

.category-img {
  width: 42px;
  height: 42px;
  margin-bottom: 8px;
  object-fit: contain;
}

.category-name {
  font-size: 12px;
  color: #333;
}

.section-head {
  margin-bottom: 12px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #202124;
}

.section-subtitle {
  font-size: 12px;
  color: #8c8f99;
  margin-top: 4px;
}

.merchant-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.merchant-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 18px;
  padding: 14px;
  display: flex;
  gap: 14px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.04);
}

.merchant-cover {
  width: 68px;
  height: 68px;
  border-radius: 18px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

.merchant-info {
  flex: 1;
}

.merchant-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.merchant-name {
  font-size: 16px;
  font-weight: 700;
  color: #202124;
}

.merchant-rating {
  font-size: 14px;
  color: #e1251b;
  font-weight: 600;
}

.merchant-meta,
.merchant-desc {
  font-size: 12px;
  color: #6b7280;
  margin-top: 6px;
}

.merchant-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
</style>
