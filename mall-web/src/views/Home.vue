<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getBanners } from '@/api/banner'
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
 * 轮播。图片与跳转链接都由后台配置（需求 5.3「轮播 Banner（可配置图片与跳转链接）」），
 * 后台「商品管理 - 轮播管理」维护，接口见 api/banner.js。
 *
 * 这里的 FALLBACK 是兜底：后端没配轮播、或接口挂了的时候用。
 * 它同时也是图片加载失败时的垫底背景（img 的 onerror 会把它藏起来，
 * 底下的渐变就露出来了，不会出现破图）。
 */
const FALLBACK_BANNERS = [
  {
    id: 'f1',
    title: '新品首发',
    linkUrl: '/goods',
    bg: 'linear-gradient(120deg, #2b2f36 0%, #4a3a3d 60%, #6d2029 100%)'
  },
  {
    id: 'f2',
    title: '限时秒杀',
    linkUrl: '/seckill',
    bg: 'linear-gradient(120deg, #6d2029 0%, #b82e38 60%, #ff8a3c 100%)'
  },
  {
    id: 'f3',
    title: '品牌直营',
    linkUrl: '/goods',
    bg: 'linear-gradient(120deg, #1f2a44 0%, #2f4b7c 60%, #3d6fb5 100%)'
  }
]

const banners = ref(FALLBACK_BANNERS)

/** 后台配了就用后端的，一条都没有（或请求失败）就用兜底 */
async function loadBanners() {
  try {
    const page = await getBanners()
    const list = page?.list || []
    if (list.length > 0) {
      banners.value = list
    }
  } catch (e) {
    // 首页不该因为轮播挂了就报错，静默用兜底
  }
}

/** 点击轮播：站内路由直接跳，外链新开页 */
function goBanner(b) {
  const url = b.linkUrl
  if (!url) return
  if (/^https?:\/\//i.test(url)) {
    window.open(url, '_blank', 'noopener')
  } else {
    router.push(url)
  }
}

/** 图片不存在时藏掉，露出底下的渐变兜底，避免破图 */
function onBannerImgError(e) {
  e.target.style.display = 'none'
}

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
    // 四个请求互不依赖，并发拿（轮播内部自己 try/catch，不会拖垮整块）
    const [tree, hot, active] = await Promise.all([
      getCategoryTree(),
      getGoods({ isHot: true, isTakeDown: false, isDel: false, pageNo: 1, pageSize: 8, full: true }),
      getActiveSeckills(),
      loadBanners()
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
          <el-carousel-item v-for="(b, i) in banners" :key="b.id ?? i">
            <div
              class="banner"
              :class="{ 'banner--clickable': !!b.linkUrl }"
              :style="{ background: b.bg || FALLBACK_BANNERS[i % FALLBACK_BANNERS.length].bg }"
              @click="goBanner(b)"
            >
              <img
                v-if="b.imageUrl"
                class="banner__img"
                :src="b.imageUrl"
                :alt="b.title"
                @error="onBannerImgError"
              />
              <div class="banner__text">
                <h2>{{ b.title }}</h2>
                <el-button v-if="b.linkUrl" type="primary" round>立即查看</el-button>
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
  position: relative;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 56px;
  overflow: hidden;
}

/* 后台配的轮播图铺满整块。这里用 cover 是对的——轮播是背景板，
   裁切不影响信息；商品图才必须 contain（见 商品图来源.md 的约定）。
   img 加载失败时 onerror 会把它藏掉，露出底下的渐变，不会出现破图。 */
.banner__img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 文字压在图上，加个左侧渐变压一层，保证任何配图下都读得清 */
.banner__text {
  position: relative;
  z-index: 1;
  padding: 8px 24px 8px 0;
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.42) 0%, rgba(0, 0, 0, 0) 100%);
  border-radius: 8px;
}

.banner--clickable {
  cursor: pointer;
}

.banner__text h2 {
  font-size: 40px;
  color: #fff;
  letter-spacing: 3px;
  margin-bottom: 12px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.35);
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
