<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAddresses, getAddressesByAccount, setDefaultAddress } from '@/api/address'
import { useUserStore } from '@/stores/user'
import { useRegionStore } from '@/stores/region'
import AddressDialog from '@/components/AddressDialog.vue'

const userStore = useUserStore()
const regionStore = useRegionStore()

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)

function regionText(addr) {
  // 后端返回的 address 只有区县一级（region-api 的 findById 没有 join 父级），
  // 省市区全名用本地缓存的 nameMap 拼。
  return regionStore.labelOf(addr.addrId) || addr.address?.name || ''
}

async function load() {
  loading.value = true
  try {
    await regionStore.ensureLoaded()
    list.value = (await getAddressesByAccount(userStore.account)) || []
  } finally {
    loading.value = false
  }
}

function onAdd() {
  editing.value = null
  dialogVisible.value = true
}

function onEdit(addr) {
  editing.value = addr
  dialogVisible.value = true
}

async function onDelete(addr) {
  const ok = await ElMessageBox.confirm(
    `确定要删除「${addr.receiver}」的这条收货地址吗？`,
    '删除确认',
    { type: 'warning' }
  ).catch(() => false)
  if (!ok) return
  await deleteAddresses([addr.id])
  ElMessage.success('已删除')
  await load()
}

async function onSetDefault(addr) {
  if (addr.isDefault) return
  await setDefaultAddress(addr.id, userStore.account)
  ElMessage.success('已设为默认地址')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="flex-between mb-16">
      <h4>收货地址</h4>
      <el-button type="primary" @click="onAdd">
        <el-icon><Plus /></el-icon> 新增地址
      </el-button>
    </div>

    <div v-loading="loading" class="list">
      <div v-for="a in list" :key="a.id" class="item" :class="{ 'is-default': a.isDefault }">
        <div class="item__main">
          <div class="item__head">
            <b>{{ a.receiver }}</b>
            <span class="text-muted">{{ a.phone }}</span>
            <el-tag v-if="a.isDefault" type="danger" size="small" effect="dark">默认</el-tag>
          </div>
          <div class="item__addr">{{ regionText(a) }} {{ a.addrDetail }}</div>
        </div>

        <div class="item__ops">
          <el-button v-if="!a.isDefault" link type="primary" @click="onSetDefault(a)">设为默认</el-button>
          <el-button link type="primary" @click="onEdit(a)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(a)">删除</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && list.length === 0" description="还没有收货地址">
        <el-button type="primary" @click="onAdd">新增收货地址</el-button>
      </el-empty>
    </div>

    <AddressDialog v-model="dialogVisible" :address="editing" @saved="load" />
  </div>
</template>

<style scoped>
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  transition: border-color 0.15s;
}

.item:hover {
  border-color: var(--brand-light);
}

.item.is-default {
  border-color: var(--brand);
  background: #fffafa;
}

.item__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.item__addr {
  color: var(--text-2);
  font-size: 13px;
  line-height: 1.6;
}

.item__ops {
  flex-shrink: 0;
}
</style>
