<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { InventoryAccount, InventoryTransaction } from '@/types/domain'

const props = defineProps<{
  accounts: InventoryAccount[]
  transactions: InventoryTransaction[]
  loading?: boolean
  adjusting?: boolean
}>()

const emit = defineEmits<{
  refresh: []
  adjust: [skuId: string, payload: { delta: number; reason: string }]
}>()

const dialogVisible = ref(false)
const currentAccount = ref<InventoryAccount | null>(null)
const form = reactive({
  delta: 0,
  reason: '',
})

const lowStockCount = computed(() => props.accounts.filter((item) => item.availableQuantity <= 5).length)
const lockedStockCount = computed(() => props.accounts.reduce((sum, item) => sum + item.lockedQuantity, 0))

function openAdjust(account: InventoryAccount) {
  currentAccount.value = account
  form.delta = 0
  form.reason = ''
  dialogVisible.value = true
}

function submitAdjust() {
  if (!currentAccount.value) return
  if (form.delta === 0) {
    ElMessage.warning('调整数量不能为 0')
    return
  }
  if (!form.reason.trim()) {
    ElMessage.warning('请填写调整原因')
    return
  }
  emit('adjust', currentAccount.value.skuId, {
    delta: form.delta,
    reason: form.reason.trim(),
  })
  dialogVisible.value = false
}

function bizTypeText(value: string) {
  const map: Record<string, string> = {
    ORDER_LOCK: '下单锁定',
    PAYMENT_CONFIRM: '支付确认',
    ORDER_RELEASE: '释放锁定',
    ORDER_RESTOCK: '售后回补',
    ADMIN_ADJUST: '后台调整',
    PRODUCT_STOCK_SYNC: '商品同步',
  }
  return map[value] ?? value
}
</script>

<template>
  <div class="inventory-panel">
    <div class="inventory-toolbar">
      <div class="inventory-metrics">
        <el-statistic title="库存账户" :value="accounts.length" />
        <el-statistic title="低库存 SKU" :value="lowStockCount" />
        <el-statistic title="锁定库存" :value="lockedStockCount" />
      </div>
      <el-button :loading="loading" @click="emit('refresh')">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="accounts" border>
      <el-table-column prop="skuId" label="SKU" min-width="150" />
      <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
      <el-table-column prop="merchantName" label="商家" min-width="150" show-overflow-tooltip />
      <el-table-column prop="availableQuantity" label="可售" width="90" />
      <el-table-column prop="lockedQuantity" label="锁定" width="90" />
      <el-table-column prop="soldQuantity" label="已售" width="90" />
      <el-table-column prop="totalQuantity" label="实物" width="90" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.availableQuantity <= 5 ? 'warning' : 'success'">
            {{ row.availableQuantity <= 5 ? '低库存' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openAdjust(row)">调整</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-divider />

    <el-table v-loading="loading" :data="transactions" border>
      <el-table-column prop="createdAt" label="时间" min-width="170" />
      <el-table-column prop="skuId" label="SKU" min-width="145" />
      <el-table-column prop="productName" label="商品" min-width="170" show-overflow-tooltip />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ bizTypeText(row.bizType) }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column label="可售变化" min-width="120">
        <template #default="{ row }">{{ row.beforeAvailable }} -> {{ row.afterAvailable }}</template>
      </el-table-column>
      <el-table-column label="锁定变化" min-width="120">
        <template #default="{ row }">{{ row.beforeLocked }} -> {{ row.afterLocked }}</template>
      </el-table-column>
      <el-table-column label="已售变化" min-width="120">
        <template #default="{ row }">{{ row.beforeSold }} -> {{ row.afterSold }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
    </el-table>

    <el-dialog v-model="dialogVisible" title="库存调整" width="420px">
      <el-form label-width="84px">
        <el-form-item label="SKU">
          <span>{{ currentAccount?.skuId }}</span>
        </el-form-item>
        <el-form-item label="调整数量">
          <el-input-number v-model="form.delta" :min="-99999" :max="99999" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="120" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" @click="submitAdjust">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.inventory-panel {
  display: grid;
  gap: 16px;
}

.inventory-toolbar {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.inventory-metrics {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(3, minmax(96px, 1fr));
}
</style>
