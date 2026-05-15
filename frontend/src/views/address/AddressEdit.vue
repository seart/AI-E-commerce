<template>
  <!-- 收货地址编辑页：新增和编辑共用同一套表单。 -->
  <div class="address-edit">
    <div class="header">
      <el-icon class="back-icon" @click="goBack"><ArrowLeft /></el-icon>
      <h2>{{ isEdit ? '编辑收货地址' : '新建收货地址' }}</h2>
      <span class="action-text" @click="save">保存</span>
    </div>

    <div class="form-container">
      <el-form label-width="96px" class="edit-form" label-position="left">
        <el-form-item label="城市">
          <el-input v-model.trim="form.city" placeholder="如北京市" />
        </el-form-item>
        <el-form-item label="区县">
          <el-input v-model.trim="form.district" placeholder="如朝阳区" />
        </el-form-item>
        <el-form-item label="街道/园区">
          <el-input v-model.trim="form.street" placeholder="如大望路商务区" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model.trim="form.detail" placeholder="楼层、门牌号等详细信息" />
        </el-form-item>
        <el-form-item label="收货人">
          <el-input v-model.trim="form.contactName" placeholder="请填写收货人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model.trim="form.phone" placeholder="请填写收货手机号" />
        </el-form-item>
        <el-form-item label="地址标签">
          <el-input v-model.trim="form.tag" placeholder="如公司、家、学校" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>

      <el-button type="primary" class="save-btn" @click="save">保存</el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useAddressStore } from '@/stores/address'

defineOptions({ name: 'AddressEdit' })

const router = useRouter()
const route = useRoute()
const addressStore = useAddressStore()

const id = computed(() => String(route.query.id ?? 'new'))
const isEdit = computed(() => id.value !== 'new')

const form = reactive({
  city: '',
  district: '',
  street: '',
  detail: '',
  contactName: '',
  phone: '',
  tag: '公司',
  isDefault: false,
})

const phonePattern = /^1\d{10}$/

onMounted(async () => {
  await addressStore.loadAddresses()

  if (!isEdit.value) {
    return
  }

  const match = addressStore.getAddressById(id.value)
  if (!match) {
    ElMessage.warning('未找到对应地址')
    router.replace('/address')
    return
  }

  form.city = match.city
  form.district = match.district
  form.street = match.street
  form.detail = match.detail
  form.contactName = match.contactName
  form.phone = match.phone
  form.tag = match.tag
  form.isDefault = match.isDefault
})

function goBack() {
  router.back()
}

async function save() {
  if (!form.city || !form.district || !form.street || !form.detail || !form.contactName) {
    ElMessage.warning('请填写完整的地址和联系人信息')
    return
  }

  if (!phonePattern.test(form.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  try {
    if (isEdit.value) {
      await addressStore.updateAddress(id.value, { ...form })
      ElMessage.success('地址修改成功')
    } else {
      await addressStore.addAddress({ ...form })
      ElMessage.success('地址新增成功')
    }

    router.push('/address')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存地址失败')
  }
}
</script>

<style scoped>
.address-edit {
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

.form-container {
  margin-top: 12px;
  background: #fff;
  padding: 12px 18px;
}

.edit-form :deep(.el-form-item__label) {
  color: #333;
}

.edit-form :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border-bottom: 1px solid #eee;
  border-radius: 0;
  padding-left: 0;
}

.save-btn {
  width: 100%;
  margin-top: 32px;
  height: 44px;
  border-radius: 22px;
  font-size: 16px;
}
</style>
