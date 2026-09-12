<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderById, getOrderItems, payOrder, confirmPay, cancelOrder } from '@/api/order'
import { money, parseTime } from '@/utils/format'
import CountDown from '@/components/CountDown.vue'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const items = ref([])
const loading = ref(true)
const paying = ref(false)

const orderId = computed(() => route.params.id)

/** 待付款订单 30 分钟未支付自动关单（需求 5.7.2 / 7.4） */
const closeDeadline = computed(() => {
  const t = parseTime(order.value?.checkoutTime)
  if (!t) return ''
  const d = new Date(t + 30 * 60 * 1000)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
    d.getMinutes()
  )}:${pad(d.getSeconds())}`
})

/** 订单已不是待付款时不允许再支付 */
const payable = computed(() => order.value?.status === '待付款')

async function onPay() {
  if (paying.value) return
  paying.value = true
  try {
    // 1) 发起支付：后端只做状态校验，返回 true 表示可以支付
    await payOrder(orderId.value)
    // 2) 模拟支付确认：这一步才真正把订单置为「已支付」
    await confirmPay(orderId.value)
    ElMessage.success('支付成功')
    router.replace({ name: 'payResult', params: { id: orderId.value } })
  } catch {
    // 订单状态不允许等错误由拦截器提示
    await load()
  } finally {
    paying.value = false
  }
}

async function onCancel() {
  const ok = await ElMessageBox.confirm('确定要取消这笔订单吗？取消后库存会回补。', '提示', {
    type: 'warning'
  }).catch(() => false)
  if (!ok) return
  await cancelOrder(orderId.value)
  ElMessage.success('订单已取消')
  router.replace({ name: 'userOrders' })
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
  <div class="page">
    <div class="container">
      <div class="section-head">
        <h3 class="section-title">模拟收银台</h3>
      </div>

      <div v-loading="loading">
        <template v-if="order">
          <!-- 状态条 -->
          <div class="panel pay-head">
            <div class="pay-head__left">
              <el-icon class="pay-head__icon" :class="{ 'is-ok': !payable }">
                <component :is="payable ? 'Wallet' : 'CircleCheckFilled'" />
              </el-icon>
              <div>
                <h2 class="pay-head__title">
                  {{ payable ? '订单提交成功，请完成支付' : `订单当前状态：${order.status}` }}
                </h2>
                <p class="text-muted">订单号：{{ order.orderNo }}</p>
              </div>
            </div>

            <div class="pay-head__right">
              <div class="pay-head__amount">
                <span class="text-muted">应付金额</span>
                <span class="price amount">
                  <span class="symbol">¥</span>{{ money(order.totalPay) }}
                </span>
              </div>
              <div v-if="payable && closeDeadline" class="pay-head__countdown">
                <CountDown :end-time="closeDeadline" prefix="支付剩余时间" />
              </div>
            </div>
          </div>

          <!-- 支付方式 -->
          <div class="panel">
            <h4 class="mb-16">选择支付方式</h4>
            <div class="methods">
              <div class="method is-active">
                <el-icon :size="26"><CreditCard /></el-icon>
                <div>
                  <b>模拟支付</b>
                  <p class="text-muted">演示环境，不接入真实支付渠道</p>
                </div>
                <el-icon class="method__check"><CircleCheckFilled /></el-icon>
              </div>
            </div>

            <el-alert
              class="mt-16"
              type="info"
              :closable="false"
              show-icon
              title="这是模拟支付"
              description="点击「确认支付」后订单会直接变为「已支付」，并生成一个 MOCK 开头的模拟交易号。"
            />
          </div>

          <!-- 商品清单 -->
          <div class="panel">
            <h4 class="mb-16">订单商品</h4>
            <div v-for="it in items" :key="it.id" class="line">
              <img
                class="thumb"
                :src="it.goodPic || '/img-placeholder.svg'"
                @error="(e) => (e.target.src = '/img-placeholder.svg')"
              />
              <span class="line__name">{{ it.goodName }}</span>
              <span class="text-muted">¥{{ money(it.dealPrice) }} × {{ it.count }}</span>
              <span class="price">¥{{ money(Number(it.dealPrice) * it.count) }}</span>
            </div>
            <el-empty v-if="items.length === 0 && !loading" description="无订单明细" />
          </div>

          <!-- 操作 -->
          <div class="panel actions">
            <el-button @click="router.push({ name: 'userOrders' })">稍后再付</el-button>
            <el-button v-if="payable" type="danger" plain @click="onCancel">取消订单</el-button>
            <el-button
              v-if="payable"
              type="primary"
              size="large"
              :loading="paying"
              @click="onPay"
            >
              确认支付
            </el-button>
            <el-button v-else type="primary" @click="router.push({ name: 'userOrderDetail', params: { id: orderId } })">
              查看订单详情
            </el-button>
          </div>
        </template>

        <el-result v-else-if="!loading" icon="warning" title="订单不存在" sub-title="请从「我的订单」重新进入">
          <template #extra>
            <el-button type="primary" @click="router.push({ name: 'userOrders' })">我的订单</el-button>
          </template>
        </el-result>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pay-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
}

.pay-head__left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.pay-head__icon {
  font-size: 40px;
  color: var(--accent);
}

.pay-head__icon.is-ok {
  color: #67c23a;
}

.pay-head__title {
  font-size: 20px;
  margin-bottom: 4px;
}

.pay-head__right {
  text-align: right;
}

.pay-head__amount {
  display: flex;
  align-items: baseline;
  gap: 10px;
  justify-content: flex-end;
}

.amount {
  font-size: 34px;
}

.pay-head__countdown {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-2);
}

/* 支付方式 */
.methods {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.method {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  cursor: pointer;
}

.method.is-active {
  border-color: var(--brand);
  background: #fff8f8;
}

.method p {
  margin: 2px 0 0;
  font-size: 12px;
}

.method__check {
  position: absolute;
  right: 10px;
  top: 10px;
  color: var(--brand);
}

/* 明细行 */
.line {
  display: grid;
  grid-template-columns: 56px 1fr 180px 120px;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
}

.line:last-child {
  border-bottom: none;
}

.thumb {
  width: 56px;
  height: 56px;
  object-fit: contain;
  border-radius: 6px;
  background: #fff;
}

.line__name {
  font-size: 14px;
}

.line > .price {
  text-align: right;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
