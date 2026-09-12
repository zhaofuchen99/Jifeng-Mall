<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, confirmReceipt, getOrderById, getOrderItems } from '@/api/order'
import { datetime, money, orderStatusTag } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const items = ref([])
const loading = ref(true)
const acting = ref(false)

const orderId = computed(() => route.params.id)

/** 状态流转时间线（需求 5.8.3：订单号与各状态时间） */
const timeline = computed(() => {
  const o = order.value
  if (!o) return []
  const nodes = [
    { label: '提交订单', time: o.checkoutTime },
    { label: '支付成功', time: o.payTime },
    { label: '商家发货', time: o.shipTime },
    { label: '确认收货', time: o.acceptTime }
  ]
  return nodes.map((n) => ({ ...n, done: !!n.time }))
})

const activeStep = computed(() => timeline.value.filter((n) => n.done).length)

async function onCancel() {
  const ok = await ElMessageBox.confirm('确定要取消该订单吗？取消后库存会回补。', '提示', {
    type: 'warning'
  }).catch(() => false)
  if (!ok) return
  acting.value = true
  try {
    await cancelOrder(orderId.value)
    ElMessage.success('订单已取消')
    await load()
  } finally {
    acting.value = false
  }
}

async function onConfirm() {
  const ok = await ElMessageBox.confirm('请确认已收到商品，确认后订单完成。', '确认收货', {
    type: 'warning'
  }).catch(() => false)
  if (!ok) return
  acting.value = true
  try {
    await confirmReceipt(orderId.value)
    ElMessage.success('已确认收货')
    await load()
  } finally {
    acting.value = false
  }
}

async function load() {
  loading.value = true
  try {
    order.value = await getOrderById(orderId.value)
    items.value = (await getOrderItems(orderId.value)) || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="panel">
      <div class="flex-between mb-16">
        <div class="flex-center gap-12">
          <h4>订单详情</h4>
          <el-tag v-if="order" :type="orderStatusTag(order.status)" size="small">{{ order.status }}</el-tag>
        </div>
        <el-button link type="primary" @click="router.push({ name: 'userOrders' })">返回订单列表</el-button>
      </div>

      <template v-if="order">
        <!-- 状态流转 -->
        <el-steps :active="activeStep" align-center class="mb-16">
          <el-step v-for="n in timeline" :key="n.label" :title="n.label" :description="datetime(n.time)" />
        </el-steps>

        <el-alert
          v-if="order.status === '已取消'"
          class="mb-16"
          type="info"
          show-icon
          :closable="false"
          title="该订单已取消"
          description="若为超时未支付自动关闭，库存已回补；若为手动取消，同样已回补库存。"
        />

        <!-- 基本信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ datetime(order.checkoutTime) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">
            <span class="price">¥{{ money(order.totalPay) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ order.payType || '—' }}</el-descriptions-item>
          <el-descriptions-item label="交易流水号">{{ order.alipayTradeNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="退款状态">{{ order.refundStatus || '无退款' }}</el-descriptions-item>
          <el-descriptions-item v-if="order.seckillNo" label="秒杀流水号" :span="2">
            {{ order.seckillNo }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </div>

    <!-- 收货信息 -->
    <div v-if="order" class="panel">
      <h4 class="mb-16">收货信息</h4>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="收货人">{{ order.receiverName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ order.receiverPhone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">
          {{ order.receiverAddrDetail || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="订单备注" :span="2">
          {{ order.orderComment || '无' }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 商品明细 -->
    <div class="panel">
      <h4 class="mb-16">商品明细</h4>
      <div class="row row--head">
        <div>商品</div>
        <div>成交价</div>
        <div>数量</div>
        <div>小计</div>
      </div>
      <div v-for="it in items" :key="it.id" class="row">
        <div class="cell-good">
          <img
            class="thumb"
            :src="it.goodPic || '/img-placeholder.svg'"
            @error="(e) => (e.target.src = '/img-placeholder.svg')"
          />
          <router-link :to="{ name: 'goodDetail', params: { id: it.goodId } }">
            {{ it.goodName }}
          </router-link>
        </div>
        <div class="cell-center price">¥{{ money(it.dealPrice) }}</div>
        <div class="cell-center">×{{ it.count }}</div>
        <div class="cell-center price">¥{{ money(Number(it.dealPrice) * it.count) }}</div>
      </div>
      <el-empty v-if="items.length === 0 && !loading" description="无商品明细" />
    </div>

    <!-- 操作 -->
    <div v-if="order" class="panel actions">
      <el-button
        v-if="order.status === '待付款'"
        type="primary"
        @click="router.push({ name: 'pay', params: { id: orderId } })"
      >
        去支付
      </el-button>
      <el-button v-if="order.status === '待付款'" :loading="acting" @click="onCancel">取消订单</el-button>
      <el-button v-if="order.status === '待收货'" type="primary" :loading="acting" @click="onConfirm">
        确认收货
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.row {
  display: grid;
  grid-template-columns: 1fr 140px 100px 140px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}

.row--head {
  padding: 10px 0;
  background: #fafafa;
  color: var(--text-3);
  font-size: 13px;
  border-radius: 4px;
}

.row--head > div:first-child {
  padding-left: 16px;
}

.cell-good {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  width: 56px;
  height: 56px;
  object-fit: contain;
  border-radius: 6px;
  background: #fff;
}

.cell-center {
  text-align: center;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
