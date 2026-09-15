<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, confirmReceipt, getOrderItems, getOrdersByAccount } from '@/api/order'
import { datetime, money, orderStatusTag } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const TABS = ['全部', '待付款', '已支付', '待收货', '已确认', '已取消']

const orders = ref([])
const loading = ref(false)
const activeTab = ref('全部')
const pageNo = ref(1)
const pageSize = 5

/** 订单号 -> 明细数组。只为当前页的订单去拉，避免一次打 N 个请求。 */
const itemsMap = ref({})

const filtered = computed(() => {
  if (activeTab.value === '全部') return orders.value
  return orders.value.filter((o) => o.status === activeTab.value)
})

const paged = computed(() => {
  const start = (pageNo.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

function countOf(tab) {
  if (tab === '全部') return orders.value.length
  return orders.value.filter((o) => o.status === tab).length
}

async function loadItems(rows) {
  // 订单主体不含明细（接口文档写"含明细"与实现不符），要单独查
  const results = await Promise.all(
    rows.map((o) => getOrderItems(o.id).catch(() => []))
  )
  const next = { ...itemsMap.value }
  rows.forEach((o, i) => {
    next[o.id] = results[i] || []
  })
  itemsMap.value = next
}

async function load() {
  loading.value = true
  try {
    orders.value = (await getOrdersByAccount(userStore.account)) || []
    // 新订单排前面
    orders.value.sort((a, b) => (b.checkoutTime || '').localeCompare(a.checkoutTime || ''))
    await loadItems(paged.value)
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  pageNo.value = 1
  loadItems(paged.value)
}

function onPageChange(p) {
  pageNo.value = p
  loadItems(paged.value)
}

async function onCancel(order) {
  const ok = await ElMessageBox.confirm('确定要取消该订单吗？取消后库存会回补。', '提示', {
    type: 'warning'
  }).catch(() => false)
  if (!ok) return
  await cancelOrder(order.id)
  ElMessage.success('订单已取消')
  await load()
}

async function onConfirm(order) {
  const ok = await ElMessageBox.confirm('请确认已收到商品，确认后订单完成。', '确认收货', {
    type: 'warning'
  }).catch(() => false)
  if (!ok) return
  await confirmReceipt(order.id)
  ElMessage.success('已确认收货')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="flex-between mb-16">
      <h4>我的订单</h4>
      <el-button link type="primary" @click="load">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane v-for="t in TABS" :key="t" :name="t">
        <template #label>
          <span>{{ t }}<em v-if="countOf(t) > 0" class="tab-count">{{ countOf(t) }}</em></span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div v-loading="loading" class="orders">
      <div v-for="o in paged" :key="o.id" class="order">
        <!-- 单头 -->
        <div class="order__head">
          <span class="text-muted">{{ datetime(o.checkoutTime) }}</span>
          <span class="text-muted">订单号：{{ o.orderNo }}</span>
          <el-tag v-if="o.seckillNo" type="danger" size="small" effect="plain">秒杀</el-tag>
          <!-- 退款是后台发起的，会员这边只做展示：退款中 / 已退款 -->
          <el-tag
            v-if="o.refundStatus && o.refundStatus !== '无退款'"
            :type="o.refundStatus === '已退款' ? 'danger' : 'warning'"
            size="small"
            effect="plain"
          >
            {{ o.refundStatus }}
          </el-tag>
          <el-tag :type="orderStatusTag(o.status)" size="small">{{ o.status }}</el-tag>
        </div>

        <!-- 商品行 -->
        <div class="order__body">
          <div class="order__goods">
            <div v-for="it in itemsMap[o.id] || []" :key="it.id" class="goods-line">
              <img
                class="thumb"
                :src="it.goodPic || '/img-placeholder.svg'"
                @error="(e) => (e.target.src = '/img-placeholder.svg')"
              />
              <div class="goods-line__info">
                <span class="goods-line__name">{{ it.goodName }}</span>
                <span class="text-muted">¥{{ money(it.dealPrice) }} × {{ it.count }}</span>
              </div>
            </div>
            <span v-if="(itemsMap[o.id] || []).length === 0" class="text-muted">加载中…</span>
          </div>

          <div class="order__amount">
            <span class="text-muted">实付</span>
            <span class="price">¥{{ money(o.totalPay) }}</span>
          </div>

          <div class="order__ops">
            <el-button
              v-if="o.status === '待付款'"
              type="primary"
              size="small"
              @click="router.push({ name: 'pay', params: { id: o.id } })"
            >
              去支付
            </el-button>
            <el-button
              v-if="o.status === '待付款'"
              size="small"
              @click="onCancel(o)"
            >
              取消
            </el-button>
            <el-button
              v-if="o.status === '待收货'"
              type="primary"
              size="small"
              @click="onConfirm(o)"
            >
              确认收货
            </el-button>
            <el-button size="small" @click="router.push({ name: 'userOrderDetail', params: { id: o.id } })">
              详情
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && paged.length === 0" :description="`没有${activeTab === '全部' ? '' : activeTab}订单`">
        <el-button type="primary" @click="router.push({ name: 'goods' })">去逛逛</el-button>
      </el-empty>
    </div>

    <el-pagination
      v-if="filtered.length > pageSize"
      background
      layout="prev, pager, next, total"
      :total="filtered.length"
      :page-size="pageSize"
      :current-page="pageNo"
      @current-change="onPageChange"
    />
  </div>
</template>

<style scoped>
.tab-count {
  display: inline-block;
  margin-left: 4px;
  padding: 0 5px;
  border-radius: 8px;
  background: var(--bg-page);
  color: var(--text-3);
  font-size: 11px;
  font-style: normal;
  line-height: 16px;
}

.orders {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.order {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
}

.order__head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  background: #fafafa;
  font-size: 12px;
}

.order__body {
  display: grid;
  grid-template-columns: 1fr 140px 200px;
  align-items: center;
  gap: 16px;
  padding: 16px;
}

.order__goods {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.goods-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.thumb {
  width: 52px;
  height: 52px;
  object-fit: contain;
  border-radius: 6px;
  background: #fff;
  flex-shrink: 0;
}

.goods-line__info {
  display: flex;
  flex-direction: column;
  font-size: 13px;
}

.goods-line__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 260px;
}

.order__amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 12px;
}

.order__amount .price {
  font-size: 18px;
}

.order__ops {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-end;
}

.order__ops .el-button {
  margin-left: 0;
}
</style>
