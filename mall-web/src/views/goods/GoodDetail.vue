<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getGoodById } from '@/api/good'
import { getSeckillGoodsByGood, getSeckillById } from '@/api/seckill'
import { money, seckillStatus } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useSeckillGrab } from '@/composables/useSeckillGrab'
import CountDown from '@/components/CountDown.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const { grabbing, doGrab } = useSeckillGrab()

const good = ref(null)
const loading = ref(true)
const notFound = ref(false)

const qty = ref(1)
const adding = ref(false)

/** 该商品正在进行的秒杀（没有则为 null） */
const seckill = ref(null)

const goodId = computed(() => Number(route.params.id))

/** 下架或已删除都不能买（需求 7.1-3） */
const offShelf = computed(() => {
  const g = good.value
  return !g || g.isTakeDown === true || g.isDel === true
})

const noStock = computed(() => (good.value?.qty ?? 0) <= 0)

const canBuy = computed(() => !offShelf.value && !noStock.value)

/** 主图 + 次图 + 详情图，拼成图集 */
const gallery = computed(() => {
  const g = good.value
  if (!g) return []
  const extra = (g.detailPics || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  return [g.pic, g.pic2, ...extra].filter(Boolean)
})

const activePic = ref(0)

const seckillStatusText = computed(() => (seckill.value ? seckillStatus(seckill.value.activity) : ''))

const seckillTarget = computed(() => {
  const s = seckill.value
  if (!s) return null
  return seckillStatusText.value === '未开始'
    ? { time: s.activity.startTime, label: '距开始' }
    : { time: s.activity.endTime, label: '距结束' }
})

const seckillRemain = computed(() => {
  const sg = seckill.value?.seckillGood
  if (!sg) return 0
  return Math.max(0, (sg.stock ?? 0) - (sg.sold ?? 0))
})

function requireLogin() {
  if (userStore.isLoggedIn) return true
  ElMessage.warning('请先登录')
  router.push({ name: 'login', query: { redirect: route.fullPath } })
  return false
}

async function onAddCart() {
  if (!requireLogin()) return
  adding.value = true
  try {
    await cartStore.add(goodId.value, qty.value)
    ElMessage.success('已加入购物车')
  } finally {
    adding.value = false
  }
}

function onBuyNow() {
  if (!requireLogin()) return
  if (!canBuy.value) return
  router.push({
    name: 'orderConfirm',
    query: { goodId: goodId.value, qty: qty.value }
  })
}

async function onGrabNow() {
  if (!requireLogin()) return
  const { order } = await doGrab(seckill.value.seckillGood.id)
  // 抢到了就直接进收银台，符合需求 5.9「抢购成功 → 进入支付流程」
  if (order) router.push({ name: 'pay', params: { id: order.id } })
}

async function load() {
  loading.value = true
  notFound.value = false
  seckill.value = null
  activePic.value = 0
  qty.value = 1
  try {
    const g = await getGoodById(goodId.value, true)
    if (!g || !g.id) {
      notFound.value = true
      return
    }
    good.value = g

    // 反查这件商品有没有配秒杀；有就顺带把活动时间窗取回来做倒计时
    const page = await getSeckillGoodsByGood(goodId.value).catch(() => null)
    const rows = page?.list || []
    for (const sg of rows) {
      if ((sg.stock ?? 0) - (sg.sold ?? 0) <= 0) continue
      const activity = await getSeckillById(sg.seckillId).catch(() => null)
      if (activity && seckillStatus(activity) !== '已结束' && seckillStatus(activity) !== '已禁用') {
        seckill.value = { seckillGood: sg, activity }
        break
      }
    }
  } finally {
    loading.value = false
  }
}

watch(goodId, load)
onMounted(load)

function onImgError(e) {
  e.target.src = '/img-placeholder.svg'
}
</script>

<template>
  <div class="page">
    <div class="container">
      <el-breadcrumb class="crumb" separator="/">
        <el-breadcrumb-item :to="{ name: 'home' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'goods' }">全部商品</el-breadcrumb-item>
        <el-breadcrumb-item v-if="good?.category">
          <a @click.prevent="router.push({ name: 'goods', query: { categoryId: good.categoryId } })">
            {{ good.category.name }}
          </a>
        </el-breadcrumb-item>
        <el-breadcrumb-item>{{ good?.name || '商品详情' }}</el-breadcrumb-item>
      </el-breadcrumb>

      <!-- 商品不存在 -->
      <div v-if="notFound" class="panel">
        <el-result icon="warning" title="商品不存在" sub-title="该商品可能已被删除">
          <template #extra>
            <el-button type="primary" @click="router.push({ name: 'goods' })">返回商品列表</el-button>
          </template>
        </el-result>
      </div>

      <template v-else>
        <!-- 下架提示（需求 5.5） -->
        <el-alert
          v-if="!loading && offShelf && good"
          class="mb-16"
          type="warning"
          show-icon
          :closable="false"
          title="该商品已下架或已删除，无法购买"
        />

        <div v-loading="loading" class="detail panel">
          <!-- 图集 -->
          <div class="gallery">
            <div class="gallery__main">
              <img
                :src="gallery[activePic] || '/img-placeholder.svg'"
                :alt="good?.name"
                @error="onImgError"
              />
            </div>
            <div v-if="gallery.length > 1" class="gallery__thumbs">
              <img
                v-for="(p, i) in gallery"
                :key="i"
                :src="p"
                :class="{ 'is-active': i === activePic }"
                @mouseenter="activePic = i"
                @error="onImgError"
              />
            </div>
          </div>

          <!-- 右侧信息 -->
          <div class="info">
            <h1 class="info__name">{{ good?.name }}</h1>
            <p class="info__summary">{{ good?.summary || good?.alias || '暂无商品摘要' }}</p>

            <div class="info__price-box">
              <div class="info__price-row">
                <span class="info__price-label">售价</span>
                <span class="price info__price">
                  <span class="symbol">¥</span>{{ money(good?.price) }}
                </span>
                <span v-if="good?.markPrice" class="price-origin">¥{{ money(good.markPrice) }}</span>
              </div>

              <!-- 秒杀价（需求 5.5） -->
              <div v-if="seckill" class="info__seckill">
                <span class="info__price-label">秒杀</span>
                <span class="price info__seckill-price">
                  <span class="symbol">¥</span>{{ money(seckill.seckillGood.seckillPrice) }}
                </span>
                <el-tag type="danger" size="small" effect="dark">限时秒杀</el-tag>
                <CountDown
                  v-if="seckillTarget"
                  class="info__seckill-time"
                  :end-time="seckillTarget.time"
                  :prefix="seckillTarget.label"
                />
                <span class="info__seckill-stock">仅剩 {{ seckillRemain }} 件</span>
              </div>
            </div>

            <ul class="info__meta">
              <li><span>品牌</span>{{ good?.brand?.name || '—' }}</li>
              <li><span>分类</span>{{ good?.category?.name || '—' }}</li>
              <li><span>编号</span>{{ good?.spuNo || '—' }}</li>
              <li>
                <span>库存</span>
                <b v-if="noStock" class="text-danger">已售罄</b>
                <b v-else>{{ good?.qty }} 件</b>
              </li>
            </ul>

            <div class="info__qty">
              <span class="info__price-label">数量</span>
              <el-input-number
                v-model="qty"
                :min="1"
                :max="Math.max(1, good?.qty || 1)"
                :disabled="!canBuy"
              />
              <span class="text-muted">（最多 {{ good?.qty ?? 0 }} 件）</span>
            </div>

            <div class="info__actions">
              <el-button
                v-if="seckill"
                type="danger"
                size="large"
                :loading="grabbing"
                :disabled="offShelf || seckillRemain <= 0"
                @click="onGrabNow"
              >
                <el-icon><Timer /></el-icon>
                立即秒杀
              </el-button>
              <template v-else>
                <el-button
                  type="primary"
                  size="large"
                  :loading="adding"
                  :disabled="!canBuy"
                  @click="onAddCart"
                >
                  <el-icon><ShoppingCart /></el-icon>
                  加入购物车
                </el-button>
                <el-button type="danger" plain size="large" :disabled="!canBuy" @click="onBuyNow">
                  立即购买
                </el-button>
              </template>
            </div>

            <p v-if="noStock && !offShelf" class="text-danger mt-8">
              <el-icon><WarningFilled /></el-icon> 该商品暂时缺货
            </p>
          </div>
        </div>

        <!-- 详情区 -->
        <div class="panel mt-16">
          <div class="section-head">
            <h3 class="section-title">商品详情</h3>
          </div>
          <!-- detail 是后台维护的富文本，这里直接渲染 -->
          <div v-if="good?.detail" class="rich" v-html="good.detail"></div>
          <div v-else-if="good?.description" class="rich">{{ good.description }}</div>
          <el-empty v-else description="暂无详细描述" />
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.crumb {
  margin-bottom: 16px;
}

.detail {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 32px;
}

/* 图集 */
.gallery__main {
  width: 400px;
  height: 400px;
  border-radius: var(--radius);
  overflow: hidden;
  background: #fff;
}

.gallery__main img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.gallery__thumbs {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.gallery__thumbs img {
  width: 64px;
  height: 64px;
  object-fit: contain;
  border-radius: 4px;
  border: 2px solid transparent;
  cursor: pointer;
  background: #fff;
}

.gallery__thumbs img.is-active {
  border-color: var(--brand);
}

/* 信息区 */
.info__name {
  font-size: 22px;
  line-height: 1.4;
  margin-bottom: 8px;
}

.info__summary {
  color: var(--text-3);
  font-size: 13px;
  margin: 0 0 20px;
}

.info__price-box {
  background: linear-gradient(100deg, #fff5f5, #fff);
  border-radius: var(--radius);
  padding: 16px;
  margin-bottom: 20px;
}

.info__price-row,
.info__seckill {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.info__seckill {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ffd7da;
}

.info__price-label {
  color: var(--text-3);
  font-size: 13px;
  flex-shrink: 0;
}

.info__price {
  font-size: 32px;
}

.info__seckill-price {
  font-size: 26px;
}

.info__seckill-time {
  font-size: 12px;
  color: var(--text-2);
}

.info__seckill-stock {
  font-size: 12px;
  color: var(--brand);
}

.info__meta {
  list-style: none;
  padding: 0;
  margin: 0 0 20px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 24px;
}

.info__meta li {
  color: var(--text-2);
  font-size: 13px;
}

.info__meta span {
  display: inline-block;
  width: 48px;
  color: var(--text-3);
}

.info__qty {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.info__actions {
  display: flex;
  gap: 12px;
}

.text-danger {
  color: var(--brand);
}

/* 富文本区 */
.rich {
  line-height: 1.9;
  color: var(--text-2);
  word-break: break-word;
}

.rich :deep(img) {
  max-width: 100%;
}

@media (max-width: 900px) {
  .detail {
    grid-template-columns: 1fr;
  }
  .gallery__main {
    width: 100%;
    height: auto;
    aspect-ratio: 1;
  }
}
</style>
