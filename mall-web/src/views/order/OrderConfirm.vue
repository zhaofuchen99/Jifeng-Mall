<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAddressesByAccount } from '@/api/address'
import { getCartByMember } from '@/api/cart'
import { getGoodById } from '@/api/good'
import { createOrder } from '@/api/order'
import { money } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useRegionStore } from '@/stores/region'
import AddressDialog from '@/components/AddressDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const regionStore = useRegionStore()

const loading = ref(true)
const submitting = ref(false)
const addresses = ref([])
const selectedAddrId = ref(null)
const items = ref([])
const comment = ref('')
const dialogVisible = ref(false)

/** 下单来源：购物车结算 or 立即购买 */
const cartIds = computed(() => {
  const raw = route.query.cartIds
  if (!raw) return []
  return String(raw)
    .split(',')
    .map((s) => Number(s))
    .filter((n) => !Number.isNaN(n))
})
const isCartMode = computed(() => cartIds.value.length > 0)

const totalPrice = computed(() =>
  items.value.reduce((sum, it) => sum + Number(it.price || 0) * it.qty, 0)
)

const totalCount = computed(() => items.value.reduce((sum, it) => sum + it.qty, 0))

const selectedAddress = computed(() => addresses.value.find((a) => a.id === selectedAddrId.value) || null)

function addressText(addr) {
  if (!addr) return ''
  const region = regionStore.labelOf(addr.addrId) || addr.address?.name || ''
  return `${region} ${addr.addrDetail || ''}`.trim()
}

async function loadAddresses() {
  addresses.value = (await getAddressesByAccount(userStore.account)) || []
  // 默认地址预选；没有默认就选第一条（需求 5.7.1）
  const def = addresses.value.find((a) => a.isDefault === true)
  selectedAddrId.value = def?.id ?? addresses.value[0]?.id ?? null
}

async function loadItems() {
  if (isCartMode.value) {
    // 重新拉一次购物车，避免用到过期的本地缓存
    const cart = (await getCartByMember(userStore.memberId)) || []
    items.value = cart
      .filter((row) => cartIds.value.includes(row.id))
      .map((row) => ({
        cartId: row.id,
        goodId: row.goodId,
        name: row.good?.name || `商品 #${row.goodId}`,
        pic: row.good?.pic,
        price: row.good?.price,
        qty: row.qty
      }))
  } else {
    const goodId = Number(route.query.goodId)
    const qty = Number(route.query.qty) || 1
    const g = await getGoodById(goodId, false)
    items.value = g
      ? [{ goodId: g.id, name: g.name, pic: g.pic, price: g.price, qty }]
      : []
  }
}

async function onSubmit() {
  if (submitting.value) return // 防重复提交（需求 5.7.1）
  if (!selectedAddress.value) {
    ElMessage.warning('请先选择收货地址')
    return
  }
  if (items.value.length === 0) {
    ElMessage.warning('没有可结算的商品')
    return
  }

  submitting.value = true
  try {
    const payload = {
      addrId: selectedAddress.value.id,
      comment: comment.value,
      memberAccount: userStore.account
    }
    if (isCartMode.value) {
      payload.cartIds = cartIds.value
    } else {
      payload.goodId = items.value[0].goodId
      payload.qty = items.value[0].qty
    }

    const order = await createOrder(payload)
    ElMessage.success('订单已提交，请尽快完成支付')
    // 下单成功后刷新购物车角标（后端已经清掉了这些行）
    cartStore.refresh()
    router.replace({ name: 'pay', params: { id: order.id } })
  } catch {
    // 「库存不足」等业务错误由拦截器弹出
  } finally {
    submitting.value = false
  }
}

async function onAddressSaved() {
  await loadAddresses()
}

onMounted(async () => {
  try {
    await Promise.all([loadAddresses(), loadItems(), regionStore.ensureLoaded()])
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="section-head">
        <h3 class="section-title">确认订单</h3>
      </div>

      <div v-loading="loading">
        <!-- 收货地址 -->
        <div class="panel">
          <div class="flex-between mb-16">
            <h4>收货地址</h4>
            <el-button link type="primary" @click="dialogVisible = true">
              <el-icon><Plus /></el-icon> 新增地址
            </el-button>
          </div>

          <div v-if="addresses.length > 0" class="addr-list">
            <div
              v-for="a in addresses"
              :key="a.id"
              class="addr"
              :class="{ 'is-active': a.id === selectedAddrId }"
              @click="selectedAddrId = a.id"
            >
              <div class="addr__head">
                <b>{{ a.receiver }}</b>
                <span class="text-muted">{{ a.phone }}</span>
                <el-tag v-if="a.isDefault" size="small" type="danger" effect="plain">默认</el-tag>
              </div>
              <div class="addr__body">{{ addressText(a) }}</div>
              <el-icon v-if="a.id === selectedAddrId" class="addr__check"><CircleCheckFilled /></el-icon>
            </div>
          </div>

          <el-empty v-else description="还没有收货地址，请先新增一个">
            <el-button type="primary" @click="dialogVisible = true">新增收货地址</el-button>
          </el-empty>
        </div>

        <!-- 商品清单（下单快照，结算页不可改动） -->
        <div class="panel">
          <h4 class="mb-16">商品清单</h4>

          <div class="row row--head">
            <div>商品</div>
            <div>单价</div>
            <div>数量</div>
            <div>小计</div>
          </div>

          <div v-for="(it, i) in items" :key="i" class="row">
            <div class="cell-good">
              <img
                class="thumb"
                :src="it.pic || '/img-placeholder.svg'"
                @error="(e) => (e.target.src = '/img-placeholder.svg')"
              />
              <span>{{ it.name }}</span>
            </div>
            <div class="cell-center price">¥{{ money(it.price) }}</div>
            <div class="cell-center">×{{ it.qty }}</div>
            <div class="cell-center price">¥{{ money(Number(it.price || 0) * it.qty) }}</div>
          </div>
        </div>

        <!-- 备注与提交 -->
        <div class="panel">
          <el-form label-width="80px">
            <el-form-item label="订单备注">
              <el-input
                v-model="comment"
                type="textarea"
                :rows="2"
                maxlength="200"
                show-word-limit
                placeholder="选填，如：请尽快发货"
              />
            </el-form-item>
          </el-form>

          <div class="submit-bar">
            <div class="submit-bar__info">
              <span class="text-muted">共 {{ totalCount }} 件商品</span>
              <span>应付金额：<span class="price total">¥{{ money(totalPrice) }}</span></span>
            </div>
            <div>
              <el-button @click="router.back()">返回</el-button>
              <el-button
                type="primary"
                size="large"
                :loading="submitting"
                :disabled="!selectedAddrId || items.length === 0"
                @click="onSubmit"
              >
                提交订单
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <AddressDialog v-model="dialogVisible" @saved="onAddressSaved" />
  </div>
</template>

<style scoped>
/* 地址 */
.addr-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.addr {
  position: relative;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.15s;
}

.addr:hover {
  border-color: var(--brand-light);
}

.addr.is-active {
  border-color: var(--brand);
  background: #fff8f8;
}

.addr__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.addr__body {
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.6;
  padding-right: 20px;
}

.addr__check {
  position: absolute;
  right: 10px;
  bottom: 10px;
  color: var(--brand);
  font-size: 18px;
}

/* 商品行 */
.row {
  display: grid;
  grid-template-columns: 1fr 140px 120px 140px;
  align-items: center;
  padding: 14px 0;
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
  width: 64px;
  height: 64px;
  object-fit: contain;
  border-radius: 6px;
  background: #fff;
}

.cell-center {
  text-align: center;
}

/* 提交栏 */
.submit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding: 16px;
  background: #fff8f8;
  border: 1px solid #ffe3e5;
  border-radius: var(--radius);
}

.submit-bar__info {
  display: flex;
  align-items: center;
  gap: 24px;
}

.total {
  font-size: 26px;
}

@media (max-width: 900px) {
  .addr-list {
    grid-template-columns: 1fr;
  }
}
</style>
