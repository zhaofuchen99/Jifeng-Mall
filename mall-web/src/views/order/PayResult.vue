<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderById } from '@/api/order'
import { datetime, money } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const loading = ref(true)

const orderId = computed(() => route.params.id)
const paid = computed(() => order.value && order.value.status !== '待付款' && order.value.status !== '已取消')

onMounted(async () => {
  try {
    order.value = await getOrderById(orderId.value)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="container">
      <div v-loading="loading" class="panel result">
        <el-result
          v-if="order"
          :icon="paid ? 'success' : 'warning'"
          :title="paid ? '支付成功' : `订单状态：${order.status}`"
          :sub-title="paid ? '感谢您的购买，我们会尽快为您发货' : '该订单尚未完成支付'"
        >
          <template #extra>
            <el-button type="primary" @click="router.push({ name: 'userOrders' })">查看我的订单</el-button>
            <el-button @click="router.push({ name: 'goods' })">继续购物</el-button>
          </template>
        </el-result>

        <!-- 交易信息（需求 5.7.2：展示支付交易信息） -->
        <div v-if="order" class="trade">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="paid ? 'success' : 'warning'" size="small">{{ order.status }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="支付金额">
              <span class="price">¥{{ money(order.totalPay) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">{{ order.payType || '—' }}</el-descriptions-item>
            <el-descriptions-item label="交易流水号">
              {{ order.alipayTradeNo || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ datetime(order.payTime) }}</el-descriptions-item>
            <el-descriptions-item label="收货人">{{ order.receiverName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ order.receiverPhone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">
              {{ order.receiverAddrDetail || '—' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-alert
            class="mt-16"
            type="info"
            :closable="false"
            show-icon
            title="这是模拟支付"
            description="交易流水号为后端生成的 MOCK 号，不对应任何真实支付渠道。"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.result {
  padding: 8px 24px 24px;
}

.trade {
  margin-top: 8px;
}
</style>
