<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGoods } from '@/api/good'
import { getCategoryTree } from '@/api/category'
import { getBrands } from '@/api/brand'
import { money } from '@/utils/format'
import GoodCard from '@/components/GoodCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const brands = ref([])
const allGoods = ref([])
const loading = ref(false)

/**
 * 价格区间与排序的输入态。提交后才写进 URL（避免每敲一个字符就刷新列表）。
 */
const priceForm = reactive({
  min: route.query.minPrice || '',
  max: route.query.maxPrice || ''
})

const SORTS = [
  { key: 'default', label: '默认' },
  { key: 'priceAsc', label: '价格 ↑' },
  { key: 'priceDesc', label: '价格 ↓' },
  { key: 'hot', label: '热销' }
]

/**
 * ⚠️ 后端不支持价格区间与排序。
 *
 * `docs/接口文档.md` 里写的 keyword / priceLow / priceHigh / sort 四个参数
 * **GoodApi / GoodSearchBean / GoodMapper.xml 里都不存在**，Spring 会静默忽略未知参数，
 * SQL 也是写死的 order by id。所以这里一次性把符合「服务端能过滤的条件」的数据取回来，
 * 价格区间、排序、分页全在前端做。
 *
 * 演示数据只有两条商品，这样做完全没问题；真实商品量下应当补后端的查询参数。
 */
const PAGE_SIZE_HINT = 1000

const query = computed(() => ({
  name: route.query.name || undefined,
  categoryId: route.query.categoryId || undefined,
  brandId: route.query.brandId || undefined,
  isHot: route.query.isHot || undefined,
  isTakeDown: false,
  isDel: false
}))

/** 前端筛选 + 排序 */
const filtered = computed(() => {
  let list = allGoods.value

  const min = Number(route.query.minPrice)
  const max = Number(route.query.maxPrice)
  if (route.query.minPrice !== undefined && route.query.minPrice !== '' && !Number.isNaN(min)) {
    list = list.filter((g) => Number(g.price) >= min)
  }
  if (route.query.maxPrice !== undefined && route.query.maxPrice !== '' && !Number.isNaN(max)) {
    list = list.filter((g) => Number(g.price) <= max)
  }

  const sort = route.query.sort
  if (sort === 'priceAsc') {
    list = [...list].sort((a, b) => Number(a.price) - Number(b.price))
  } else if (sort === 'priceDesc') {
    list = [...list].sort((a, b) => Number(b.price) - Number(a.price))
  } else if (sort === 'hot') {
    // 后端没有销量字段，用热销标记 + 库存降序近似
    list = [...list].sort((a, b) => Number(b.isHot) - Number(a.isHot) || (b.qty || 0) - (a.qty || 0))
  }
  return list
})

const pageNo = computed(() => Number(route.query.page) || 1)
const pageSize = 8

const paged = computed(() => {
  const start = (pageNo.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

/** 把筛选条件写进 URL —— 刷新后状态不丢（需求 5.4） */
function applyQuery(patch, resetPage = true) {
  const next = { ...route.query, ...patch }
  if (resetPage) delete next.page
  Object.keys(next).forEach((k) => {
    if (next[k] === '' || next[k] === undefined || next[k] === null) delete next[k]
  })
  router.push({ name: 'goods', query: next })
}

function onPriceFilter() {
  applyQuery({ minPrice: priceForm.min, maxPrice: priceForm.max })
}

function resetAll() {
  priceForm.min = ''
  priceForm.max = ''
  router.push({ name: 'goods' })
}

async function loadGoods() {
  loading.value = true
  try {
    allGoods.value = (await getGoods({ ...query.value, pageNo: 1, pageSize: PAGE_SIZE_HINT }))?.list || []
  } finally {
    loading.value = false
  }
}

// 分类/品牌/关键字变化要重新取数；价格与排序是纯前端，不用重取
watch(
  () => [route.query.categoryId, route.query.brandId, route.query.name, route.query.isHot].join('|'),
  loadGoods
)

onMounted(async () => {
  const [tree, brandPage] = await Promise.all([
    getCategoryTree(),
    getBrands({ pageNo: 1, pageSize: 100 })
  ])
  categories.value = tree || []
  brands.value = brandPage?.list || []
  await loadGoods()
})

const activeCategoryId = computed(() => (route.query.categoryId ? Number(route.query.categoryId) : null))
const activeBrandId = computed(() => (route.query.brandId ? Number(route.query.brandId) : null))

const priceRangeText = computed(() => {
  const { minPrice, maxPrice } = route.query
  if (!minPrice && !maxPrice) return ''
  return `¥${minPrice || '0'} - ¥${maxPrice || '不限'}`
})
</script>

<template>
  <div class="page">
    <div class="container">
      <!-- 面包屑 -->
      <el-breadcrumb class="crumb" separator="/">
        <el-breadcrumb-item :to="{ name: 'home' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>全部商品</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.query.name">搜索「{{ route.query.name }}」</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="layout">
        <!-- 左侧筛选 -->
        <aside class="side">
          <div class="panel side__block">
            <h4 class="side__title">商品分类</h4>
            <ul class="side__list">
              <li :class="{ 'is-active': !activeCategoryId }" @click="applyQuery({ categoryId: '' })">
                全部分类
              </li>
              <li
                v-for="c in categories"
                :key="c.id"
                :class="{ 'is-active': activeCategoryId === c.id }"
                @click="applyQuery({ categoryId: c.id })"
              >
                {{ c.name }}
              </li>
            </ul>
          </div>

          <div class="panel side__block">
            <h4 class="side__title">品牌</h4>
            <ul class="side__list">
              <li :class="{ 'is-active': !activeBrandId }" @click="applyQuery({ brandId: '' })">
                全部品牌
              </li>
              <li
                v-for="b in brands"
                :key="b.id"
                :class="{ 'is-active': activeBrandId === b.id }"
                @click="applyQuery({ brandId: b.id })"
              >
                {{ b.name }}
              </li>
            </ul>
          </div>

          <div class="panel side__block">
            <h4 class="side__title">价格区间</h4>
            <div class="side__price">
              <el-input v-model="priceForm.min" placeholder="最低" size="small" />
              <span class="dash">—</span>
              <el-input v-model="priceForm.max" placeholder="最高" size="small" />
            </div>
            <el-button size="small" type="primary" class="side__price-btn" @click="onPriceFilter">
              确定
            </el-button>
          </div>
        </aside>

        <!-- 右侧商品区 -->
        <section class="main">
          <div class="toolbar">
            <div class="toolbar__sorts">
              <span
                v-for="s in SORTS"
                :key="s.key"
                class="toolbar__sort"
                :class="{ 'is-active': (route.query.sort || 'default') === s.key }"
                @click="applyQuery({ sort: s.key === 'default' ? '' : s.key })"
              >
                {{ s.label }}
              </span>
            </div>
            <div class="toolbar__right">
              <span class="text-muted">共 {{ filtered.length }} 件商品</span>
              <el-button v-if="priceRangeText || route.query.name" link type="primary" @click="resetAll">
                清空筛选
              </el-button>
            </div>
          </div>

          <div v-if="priceRangeText" class="filter-tip">
            已选价格：<b>{{ priceRangeText }}</b>
          </div>

          <div v-loading="loading" class="good-grid">
            <GoodCard v-for="g in paged" :key="g.id" :good="g" />
          </div>

          <EmptyState
            v-if="!loading && filtered.length === 0"
            text="没有找到符合条件的商品，换个条件试试"
          >
            <el-button type="primary" class="mt-16" @click="resetAll">清空筛选条件</el-button>
          </EmptyState>

          <el-pagination
            v-if="filtered.length > pageSize"
            background
            layout="prev, pager, next, jumper, total"
            :total="filtered.length"
            :page-size="pageSize"
            :current-page="pageNo"
            @current-change="(p) => applyQuery({ page: p }, false)"
          />
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.crumb {
  margin-bottom: 16px;
}

.layout {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 16px;
  align-items: start;
}

.side__block {
  padding: 16px;
}

.side__block + .side__block {
  margin-top: 12px;
}

.side__title {
  font-size: 14px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line);
}

.side__list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.side__list li {
  padding: 7px 10px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--text-2);
  font-size: 13px;
}

.side__list li:hover {
  background: var(--bg-page);
  color: var(--brand);
}

.side__list li.is-active {
  background: var(--brand-bg);
  color: var(--brand);
  font-weight: 600;
}

.side__price {
  display: flex;
  align-items: center;
  gap: 6px;
}

.dash {
  color: var(--text-3);
}

.side__price-btn {
  width: 100%;
  margin-top: 10px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  padding: 10px 16px;
  margin-bottom: 16px;
}

.toolbar__sorts {
  display: flex;
  gap: 4px;
}

.toolbar__sort {
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--text-2);
  font-size: 13px;
}

.toolbar__sort:hover {
  color: var(--brand);
}

.toolbar__sort.is-active {
  background: var(--brand);
  color: #fff;
}

.toolbar__right {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.filter-tip {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-2);
}

.filter-tip b {
  color: var(--brand);
}
</style>
