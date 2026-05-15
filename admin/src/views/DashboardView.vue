<script setup lang="ts">
// 数据概览页：展示后台首页的核心经营指标。
import { onMounted, ref } from 'vue'
import { getDashboard } from '@/api/admin'
import type { DashboardResponse } from '@/types/domain'

const data = ref<DashboardResponse['summary']>()

onMounted(async () => {
  data.value = (await getDashboard()).summary
})
</script>

<template>
  <div>
    <h2>数据概览</h2>
    <el-row :gutter="18">
      <el-col :span="6">
        <el-card class="page-card metric"><span>订单数</span><strong>{{ data?.orderCount ?? '-' }}</strong></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="page-card metric"><span>销售额</span><strong>￥{{ data?.salesAmount ?? '-' }}</strong></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="page-card metric"><span>商品数</span><strong>{{ data?.productCount ?? '-' }}</strong></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="page-card metric"><span>用户数</span><strong>{{ data?.userCount ?? '-' }}</strong></el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.metric {
  min-height: 132px;
}

.metric span {
  color: #667085;
}

.metric strong {
  display: block;
  margin-top: 18px;
  font-size: 34px;
}
</style>
