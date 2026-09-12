<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCategoryTree } from '@/api/category'
import { getGoods } from '@/api/good'
import { getActiveSeckills } from '@/api/seckill'
import { seckillStatus, parseTime } from '@/utils/format'
import GoodCard from '@/components/GoodCard.vue'
import CountDown from '@/components/CountDown.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const categories = ref([])
const hotGoods = ref([])
const seckills = ref([])
const loading = ref(true)

/**
 * 轮播。演示项目没有运维配图，用 CSS 渐变代替 <img>——
 * 免得指向不存在的图片文件、整块区域变成破图。
 */
const banners = [
  {
    title: '新品首发',
    subtitle: 'iPhone 16 Pro 现已开售',
    cta: '立即选购',
    to: { name: 'goods', query: { categoryId: 1 } },
    bg: 'linear-gradient(120deg, #2b2f36 0%, #4a3a3d 60%, #6d2029 100%)'
  },
  {
    title: '限时秒杀',
    subtitle: '每周三秒杀 · 低至 6 折',
    cta: '进入会场',
    to: { name: 'seckill' },
    bg: 'linear-gradient(120deg, #6d2029 0%, #b82e38 60%, #ff8a3c 100%)'
  },
  {
    title: '品牌直营',
    subtitle: 'Apple / Huawei 官方好货',
    cta: '去看看',
    to: { name: 'goods' },
    bg: 'linear-gradient(120deg, #1f2a44 0%, #2f4b7c 60%, #3d6fb5 100%)'
  }
]

/** 首页只露出主活动：第一个「进行中」的，没有就退而取第一个 */
const mainSeckill = computed(() => {
  const list = seckills.value || []
  return list.find((s) => seckillStatus(s) === '进行中') || list[0] || null
})

/** 未开始 → 倒计时到开始时间；进行中 → 倒计时到结束时间 */
const seckillTarget = computed(() => {
  const s = mainSeckill.value
  if (!s) return null
  return seckillStatus(s) === '未开始'
    ? { time: s.startTime, label: '距开始' }
    : { time: s.endTime, label: '距结束' }
})

/** 秒杀场内最快结束的那件商品剩余库存，用来做首页的"剩余"提示 */
const seckillInfo = computed(() => mainSeckill.value)

function toCategory(id) {
  router.push({ name: 'goods', query: { categoryId: id } })
}

onMounted(async () => {
  try {
    // 三个请求互不依赖，并发拿
    const [tree, hot, active] = await Promise.all([
      getCategoryTree(),
      getGoods({ isHot: true, isTakeDown: false, isDel: false, pageNo: 1, pageSize: 8, full: true }),
      getActiveSeckills()
    ])
    categories.value = tree || []
    hotGoods.value = hot?.list || []
    seckills.value = active || []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="container">
      <!-- 首屏：分类 + 轮播 + 秒杀入口 -->
      <div class="hero">
        <aside class="hero__cats">
          <h3 class="hero__cats-title">全部分类</h3>
          <ul>
            <li v-for="c in categories" :key="c.id" @click="toCategory(c.id)">
              <span>{{ c.name }}</span>
              <el-icon><ArrowRight /></el-icon>
            </li>
            <li v-if="!loading && categories.length === 0" class="text-muted">暂无分类</li>
          </ul>
        </aside>

        <el-carousel class="hero__banner" height="360px" :interval="5000" arrow="hover">
          <el-carousel-item v-for="b in banners" :key="b.title">
            <div class="banner" :style="{ background: b.bg }">
              <div class="banner__text">
                <h2>{{ b.title }}</h2>
                <p>{{ b.subtitle }}</p>
                <el-button type="primary" round @click="router.push(b.to)">{{ b.cta }}</el-button>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>

        <aside class="hero__seckill">
          <template v-if="seckillTarget">
            <div class="sk-card__head">
              <el-icon><Timer /></el-icon>
              <span>{{ seckillInfo.name }}</span>
            </div>
            <div class="sk-card__time">
              <CountDown
                :end-time="seckillTarget.time"
                :prefix="seckillTarget.label"
                @finish="$router.go(0)"
              />
            </div>
            <p class="sk-card__desc">{{ seckillInfo.description || '限时限量，先到先得' }}</p>
            <el-button type="primary" class="sk-card__btn" @click="router.push({ name: 'seckill' })">
              立即抢购
            </el-button>
          </template>
          <EmptyState v-else-if="!loading" text="暂无秒杀活动" icon="Timer" />
        </aside>
      </div>

      <!-- 热销推荐 -->
      <section class="mt-24">
        <div class="section-head">
          <h3 class="section-title">热销推荐</h3>
          <router-link :to="{ name: 'goods', query: { isHot: 'true' } }" class="section-more">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>

        <div v-loading="loading" class="good-grid">
          <GoodCard v-for="g in hotGoods" :key="g.id" :good="g" />
        </div>

        <EmptyState v-if="!loading && hotGoods.length === 0" text="暂无热销商品">
          <el-button type="primary" class="mt-16" @click="router.push({ name: 'goods' })">
            去逛逛
          </el-button>
        </EmptyState>
      </section>

      <!-- 服务承诺 -->
      <section class="promise mt-24">
        <div v-for="p in ['正品保障', '极速配送', '七天无理由', '售后无忧']" :key="p" class="promise__item">
          <el-icon :size="26"><CircleCheck /></el-icon>
          <span>{{ p }}</span>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: 200px 1fr 260px;
  gap: 16px;
}

/* 分类侧栏 */
.hero__cats {
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  padding: 12px 0;
}

.hero__cats-title {
  padding: 0 16px 10px;
  font-size: 15px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 6px;
}

.hero__cats ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.hero__cats li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 16px;
  cursor: pointer;
  color: var(--text-2);
  transition: all 0.15s;
}

.hero__cats li:hover {
  background: var(--brand-bg);
  color: var(--brand);
  padding-left: 20px;
}

/* 轮播 */
.hero__banner {
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.banner {
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 56px;
}

.banner__text h2 {
  font-size: 40px;
  color: #fff;
  letter-spacing: 3px;
  margin-bottom: 12px;
}

.banner__text p {
  color: rgba(255, 255, 255, 0.75);
  font-size: 16px;
  margin: 0 0 28px;
}

/* 秒杀卡片 */
.hero__seckill {
  background: linear-gradient(170deg, #fff 0%, #fff5f5 100%);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  padding: 20px;
  display: flex;
  flex-direction: column;
  border: 1px solid #ffe3e5;
}

.sk-card__head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 17px;
  font-weight: 600;
  color: var(--brand);
  margin-bottom: 14px;
}

.sk-card__time {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 14px;
}

.sk-card__desc {
  flex: 1;
  color: var(--text-3);
  font-size: 13px;
  margin: 0 0 16px;
  line-height: 1.7;
}

.sk-card__btn {
  width: 100%;
  letter-spacing: 2px;
}

/* 服务承诺 */
.promise {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  padding: 24px 0;
}

.promise__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--text-2);
  border-right: 1px solid var(--line);
}

.promise__item:last-child {
  border-right: none;
}

.promise__item .el-icon {
  color: var(--brand);
}

@media (max-width: 1220px) {
  .hero {
    grid-template-columns: 180px 1fr;
  }
  .hero__seckill {
    display: none;
  }
}
</style>
