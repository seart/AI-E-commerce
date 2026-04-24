<template>
  <div class="search-view">
    <div class="search-header">
      <el-icon class="back-icon" @click="goBack"><ArrowLeft /></el-icon>
      <el-input
        v-model.trim="keyword"
        class="search-input"
        placeholder="请输入您想要的商品或商家"
        :prefix-icon="Search"
        clearable
        autofocus
        @keyup.enter="doSearch"
      />
      <span class="cancel-text" @click="goBack">取消</span>
    </div>

    <div class="search-section">
      <div class="section-title">
        <span>历史搜索</span>
        <el-icon class="delete-icon" @click="clearHistory"><Delete /></el-icon>
      </div>
      <div class="tags-group" v-if="searchHistory.length > 0">
        <el-tag
          v-for="item in searchHistory"
          :key="item"
          type="info"
          class="search-tag"
          @click="quickSearch(item)"
        >
          {{ item }}
        </el-tag>
      </div>
      <el-empty v-else description="暂无历史搜索" :image-size="72" />
    </div>

    <div class="search-section">
      <div class="section-title">
        <span>热门搜索</span>
      </div>
      <div class="tags-group">
        <el-tag
          v-for="item in popularKeywords"
          :key="item"
          type="danger"
          effect="plain"
          class="search-tag"
          @click="quickSearch(item)"
        >
          {{ item }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { ArrowLeft, Search, Delete } from '@element-plus/icons-vue'
import { useCatalogStore } from '@/stores/catalog'

defineOptions({ name: 'SearchView' })

const router = useRouter()
const catalogStore = useCatalogStore()
const { searchHistory } = storeToRefs(catalogStore)

const keyword = ref('')
const { popularKeywords } = catalogStore

function goBack() {
  router.back()
}

function doSearch() {
  if (!keyword.value.trim()) {
    return
  }

  catalogStore.addSearchHistory(keyword.value)
  router.push({ path: '/search-list', query: { q: keyword.value } })
}

function quickSearch(value: string) {
  keyword.value = value
  doSearch()
}

async function clearHistory() {
  if (searchHistory.value.length === 0) {
    return
  }

  try {
    await ElMessageBox.confirm('确认清空历史搜索记录吗？', '提示', {
      type: 'warning',
    })
    catalogStore.clearSearchHistory()
  } catch {
    return
  }
}
</script>

<style scoped>
.search-view {
  min-height: 100vh;
  background-color: #fff;
  padding: 12px 18px;
}

.search-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
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
  background: #f5f7fa;
  box-shadow: none;
}

.cancel-text {
  font-size: 14px;
  color: #333;
  margin-left: 12px;
  cursor: pointer;
}

.search-section {
  margin-bottom: 32px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 700;
  color: #333;
  margin-bottom: 12px;
}

.delete-icon {
  color: #999;
  cursor: pointer;
}

.tags-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.search-tag {
  cursor: pointer;
  border-radius: 999px;
  padding: 0 12px;
}
</style>
