<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActiveSeckills, getSeckillGoods } from '@/api/seckill'
import { getGoodById } from '@/api/good'
import { money, seckillStatus } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { useSeckillGrab } from '@/composables/useSeckillGrab'
import CountDown from '@/components/CountDown.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { grabbing, doGrab } = useSeckillGrab()

const activities = ref([])
const activeId = ref(null)
const rows = ref([])
const loading = ref(true)
const loadingGoods = ref(false)

const activeActivity = computed(() => activities.value.find((a) => a.id === activeId.value) || null)
const status = computed(() => (activeActivity.value ? seckillStatus(activeActivity.value) : ''))

const countdown = computed(() => {
  const a = activeActivity.value
  if (!a) return null
  return status.value === '未开始'
    ? { time: a.startTime, label: '距开始' }
    : { time: a.endTime, label: '距结束' }
})

function remainOf(row) {
  return Math.max(0, (row.stock ?? 0) - (row.sold ?? 0))
}

function soldPercent(row) {
  const total = row.stock ?? 0
  if (total <= 0) return 100
  return Math.min(100, Math.round(((row.sold ?? 0) / total) * 100))
}

/** 活动是否在可抢购窗口内 */
const canGrabWindow = computed(() => status.value === '进行中')

async function loadGoods(seckillId) {
  loadingGoods.value = true
  rows.value = []
  try {
    const list = (await getSeckillGoods(seckillId)) || []
    // 秒杀商品表里只有 goodId，商品图/名称/原价要另外查
    const goods = await Promise.all(list.map((r) => getGoodById(r.goodId, false).catch(() => null)))
    rows.value = list.map((r, i) => ({ ...r, good: goods[i] }))
  } finally {
    loadingGoods.value = false
  }
}

async function onGrab(row) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再抢购')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (grabbing.value) return // 防重复提交

  const { order } = await doGrab(row.id)
  if (order) {
    router.push({ name: 'pay', params: { id: order.id } })
  } else {
    // 抢没抢到都要刷新，让剩余库存/已售数量跟上
    await loadGoods(activeId.value)
  }
}

function switchActivity(id) {
  activeId.value = id
  loadGoods(id)
}

onMounted(async () => {
  try {
    activities.value = (await getActiveSeckills()) || []
    if (activities.value.length > 0) {
      // 优先落在"进行中"的活动上
      const running = activities.value.find((a) => seckillStatus(a) === '进行中')
      activeId.value = (running || activities.value[0]).id
      await loadGoods(activeId.value)
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="container">
      <!-- 会场头 -->
      <div class="banner">
        <div class="banner__text">
          <h2>限时秒杀</h2>
          <p>每天精选好物，限时限量，先到先得</p>
        </div>
        <div v-if="countdown" class="banner__time">
          <CountDown :end-time="countdown.time" :prefix="countdown.label" />
        </div>
      </div>

      <div v-loading="loading">
        <template v-if="activities.length > 0">
          <!-- 活动切换 -->
          <div class="acts">
            <div
              v-for="a in activities"
              :key="a.id"
              class="act"
              :class="{ 'is-active': a.id === activeId }"
              @click="switchActivity(a.id)"
            >
              <span class="act__name">{{ a.name }}</span>
              <el-tag
                size="small"
                :type="seckillStatus(a) === '进行中' ? 'danger' : 'info'"
                effect="dark"
              >
                {{ seckillStatus(a) }}
              </el-tag>
            </div>
          </div>

          <!-- 活动说明 -->
          <div v-if="activeActivity" class="panel mt-16 act-info">
            <div>
              <h3 class="act-info__name">{{ activeActivity.name }}</h3>
              <p class="text-muted">{{ activeActivity.description || '暂无活动说明' }}</p>
            </div>
            <div class="act-info__time text-muted">
              <p>开始：{{ activeActivity.startTime }}</p>
              <p>结束：{{ activeActivity.endTime }}</p>
            </div>
          </div>

          <el-alert
            v-if="status === '未开始'"
            class="mt-16"
            type="warning"
            show-icon
            :closable="false"
            title="活动尚未开始"
            description="开始后即可抢购，请留意页面倒计时。"
          />
          <el-alert
            v-else-if="status !== '进行中'"
            class="mt-16"
            type="info"
            show-icon
            :closable="false"
            :title="`活动${status}`"
            description="该活动当前不可抢购。"
          />

          <!-- 秒杀商品 -->
          <div v-loading="loadingGoods" class="good-grid mt-16">
            <div v-for="row in rows" :key="row.id" class="sk-card">
              <div class="sk-card__pic" @click="router.push({ name: 'goodDetail', params: { id: row.goodId } })">
                <img
                  :src="row.good?.pic || '/img-placeholder.svg'"
                  @error="(e) => (e.target.src = '/img-placeholder.svg')"
                />
                <span v-if="remainOf(row) <= 0" class="sk-card__sold">已售罄</span>
              </div>

              <div class="sk-card__body">
                <div class="sk-card__name" :title="row.good?.name">
                  {{ row.good?.name || `商品 #${row.goodId}` }}
                </div>

                <div class="sk-card__price">
                  <span class="price sk-price">
                    <span class="symbol">¥</span>{{ money(row.seckillPrice) }}
                  </span>
                  <span v-if="row.good?.price" class="price-origin">
                    ¥{{ money(row.good.price) }}
                  </span>
                </div>

                <div class="sk-card__stock">
                  <el-progress
                    :percentage="soldPercent(row)"
                    :show-text="false"
                    :stroke-width="6"
                    color="#e63946"
                  />
                  <div class="sk-card__stock-text">
                    <span>已抢 {{ row.sold ?? 0 }} 件</span>
                    <span>剩余 <b>{{ remainOf(row) }}</b> 件</span>
                  </div>
                </div>

                <p class="sk-card__limit">每人限购 {{ row.limitPerUser ?? 1 }} 件</p>

                <el-button
                  type="danger"
                  class="sk-card__btn"
                  :loading="grabbing"
                  :disabled="!canGrabWindow || remainOf(row) <= 0"
                  @click="onGrab(row)"
                >
                  {{ remainOf(row) <= 0 ? '已售罄' : status === '未开始' ? '未开始' : '立即秒杀' }}
                </el-button>
              </div>
            </div>
          </div>

          <EmptyState
            v-if="!loadingGoods && rows.length === 0"
            text="该活动暂无秒杀商品"
            icon="Timer"
          />
        </template>

        <EmptyState v-else-if="!loading" text="当前没有进行中的秒杀活动" icon="Timer">
          <el-button type="primary" class="mt-16" @click="router.push({ name: 'goods' })">去逛逛</el-button>
        </EmptyState>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 会场头 */
.banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32px 40px;
  border-radius: var(--radius);
  background: linear-gradient(120deg, #6d2029 0%, #b82e38 60%, #ff8a3c 100%);
  color: #fff;
  margin-bottom: 16px;
}

.banner__text h2 {
  font-size: 34px;
  letter-spacing: 4px;
  margin-bottom: 8px;
}

.banner__text p {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
}

.banner__time {
  background: rgba(0, 0, 0, 0.25);
  padding: 10px 16px;
  border-radius: 24px;
  font-size: 13px;
}

/* 活动切换 */
.acts {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.act {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 22px;
  cursor: pointer;
  transition: all 0.15s;
}

.act:hover {
  border-color: var(--brand-light);
}

.act.is-active {
  border-color: var(--brand);
  background: #fff8f8;
  font-weight: 600;
}

/* 活动信息 */
.act-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.act-info__name {
  font-size: 18px;
  margin-bottom: 4px;
}

.act-info__time p {
  margin: 2px 0;
  font-size: 13px;
}

/* 秒杀卡片 */
.sk-card {
  background: #fff;
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid #ffe3e5;
}

.sk-card__pic {
  position: relative;
  aspect-ratio: 1;
  background: #fff;
  cursor: pointer;
}

.sk-card__pic img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.sk-card__sold {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 20px;
  letter-spacing: 2px;
}

.sk-card__body {
  padding: 12px;
}

.sk-card__name {
  font-size: 14px;
  height: 42px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 8px;
}

.sk-price {
  font-size: 22px;
}

.sk-card__stock {
  margin: 10px 0 6px;
}

.sk-card__stock-text {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-3);
  margin-top: 4px;
}

.sk-card__stock-text b {
  color: var(--brand);
}

.sk-card__limit {
  font-size: 12px;
  color: var(--text-3);
  margin: 0 0 10px;
}

.sk-card__btn {
  width: 100%;
  letter-spacing: 2px;
}
</style>
