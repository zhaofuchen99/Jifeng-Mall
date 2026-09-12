<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import goodApi from '@/api/good'
import orderApi from '@/api/order'
import memberApi from '@/api/member'
import { seckillApi } from '@/api/seckill'
import { money, datetime } from '@/utils/format'
import { ORDER_STATUS, ORDER_STATUS_TAG } from '@/utils/dict'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const stats = ref({ goods: 0, orders: 0, members: 0, seckills: 0 })
const recentOrders = ref([])
const statusCount = ref({})

/**
 * 概览统计。
 *
 * 后端没有 dashboard 聚合接口，这里用各列表接口的 `total` 拿总数——
 * 传 pageSize=1 即可，PageHelper 仍会返回真实的 total，不必把数据全捞回来。
 */
async function countOf(call) {
  const page = await call({ pageNo: 1, pageSize: 1 }).catch(() => null)
  return page?.total ?? 0
}

onMounted(async () => {
  try {
    const [goods, orders, members, seckills] = await Promise.all([
      countOf((p) => goodApi.list({ ...p, isDel: false })),
      countOf((p) => orderApi.list(p)),
      countOf((p) => memberApi.list(p)),
      countOf((p) => seckillApi.list(p))
    ])
    stats.value = { goods, orders, members, seckills }

    // 最近订单：OrderMapper 是写死的 order by id 升序，
    // 拿"最新"只能全量取回再自己倒序，这里量小无所谓。
    const all = await orderApi.list({ pageNo: 1, pageSize: 0 }).catch(() => null)
    const rows = (all?.list || []).slice().sort((a, b) => Number(b.id) - Number(a.id))
    recentOrders.value = rows.slice(0, 6)

    const counts = {}
    ORDER_STATUS.forEach((s) => (counts[s] = 0))
    rows.forEach((o) => {
      if (counts[o.status] !== undefined) counts[o.status] += 1
    })
    statusCount.value = counts
  } finally {
    loading.value = false
  }
})

const cards = computed(() => [
  { key: 'goods', label: '在售商品', value: stats.value.goods, icon: 'Goods', color: '#2f6feb', to: '/goods/good' },
  { key: 'orders', label: '订单总数', value: stats.value.orders, icon: 'Document', color: '#e6a23c', to: '/orders' },
  { key: 'members', label: '注册会员', value: stats.value.members, icon: 'User', color: '#67c23a', to: '/members' },
  { key: 'seckills', label: '秒杀活动', value: stats.value.seckills, icon: 'Timer', color: '#e63946', to: '/seckills/activity' }
])

const shortcuts = [
  { title: '新增商品', desc: '维护商品主图、价格与库存', icon: 'Plus', to: '/goods/good' },
  { title: '待发货订单', desc: '查看已支付等待发货的订单', icon: 'Van', to: '/orders' },
  { title: '配置秒杀', desc: '新建活动并关联秒杀商品', icon: 'Timer', to: '/seckills/activity' },
  { title: '角色授权', desc: '维护用户组、角色与资源授权', icon: 'Key', to: '/system/role' }
]

const maxStatus = computed(() => Math.max(1, ...Object.values(statusCount.value)))
</script>

<template>
  <div class="page" v-loading="loading">
    <!-- 欢迎条 -->
    <div class="welcome">
      <div>
        <h2>你好，{{ userStore.displayName }}</h2>
        <p>欢迎使用极锋商城管理后台。今天是 {{ datetime(new Date()) }}。</p>
      </div>
      <el-icon class="welcome__icon"><DataLine /></el-icon>
    </div>

    <!-- 统计卡 -->
    <div class="cards">
      <div v-for="c in cards" :key="c.key" class="card" @click="router.push(c.to)">
        <div class="card__icon" :style="{ background: c.color + '1a', color: c.color }">
          <el-icon :size="22"><component :is="c.icon" /></el-icon>
        </div>
        <div class="card__body">
          <div class="card__value">{{ c.value }}</div>
          <div class="card__label">{{ c.label }}</div>
        </div>
      </div>
    </div>

    <div class="grid">
      <!-- 最近订单 -->
      <div class="table-card">
        <div class="table-toolbar">
          <span class="table-toolbar__title">最近订单</span>
          <el-button link type="primary" @click="router.push('/orders')">全部订单</el-button>
        </div>

        <el-table :data="recentOrders" size="small" :show-header="true">
          <el-table-column prop="orderNo" label="订单号" min-width="170" show-overflow-tooltip />
          <el-table-column prop="memberAccount" label="会员" width="100" />
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }">
              <span class="price">¥{{ money(row.totalPay) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="ORDER_STATUS_TAG[row.status] || 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="160">
            <template #default="{ row }">{{ datetime(row.checkoutTime) }}</template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无订单" :image-size="70" />
          </template>
        </el-table>
      </div>

      <div class="side-col">
        <!-- 订单状态分布 -->
        <div class="table-card">
          <div class="table-toolbar">
            <span class="table-toolbar__title">订单状态分布</span>
          </div>
          <div v-for="s in ORDER_STATUS" :key="s" class="bar-row">
            <span class="bar-row__label">{{ s }}</span>
            <div class="bar-row__track">
              <div
                class="bar-row__fill"
                :style="{
                  width: ((statusCount[s] || 0) / maxStatus) * 100 + '%',
                  background: { 待付款: '#e6a23c', 已支付: '#2f6feb', 待收货: '#909399', 已确认: '#67c23a', 已取消: '#f56c6c' }[s]
                }"
              ></div>
            </div>
            <span class="bar-row__value">{{ statusCount[s] || 0 }}</span>
          </div>
        </div>

        <!-- 快捷入口 -->
        <div class="table-card mt-16">
          <div class="table-toolbar">
            <span class="table-toolbar__title">快捷操作</span>
          </div>
          <div
            v-for="s in shortcuts"
            :key="s.title"
            class="shortcut"
            @click="router.push(s.to)"
          >
            <el-icon class="shortcut__icon"><component :is="s.icon" /></el-icon>
            <div>
              <b>{{ s.title }}</b>
              <p>{{ s.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(120deg, #1f2733, #27405e);
  color: #fff;
  border-radius: var(--radius);
  padding: 22px 26px;
  margin-bottom: 16px;
}

.welcome h2 {
  font-size: 20px;
  margin-bottom: 6px;
}

.welcome p {
  margin: 0;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.welcome__icon {
  font-size: 48px;
  color: rgba(255, 255, 255, 0.18);
}

/* 统计卡 */
.cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.card {
  background: #fff;
  border-radius: var(--radius);
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: transform 0.15s, box-shadow 0.15s;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.card__icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.card__value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.card__label {
  font-size: 13px;
  color: var(--text-3);
  margin-top: 2px;
}

/* 两栏 */
.grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
  align-items: start;
}

.side-col {
  min-width: 0;
}

/* 状态分布条 */
.bar-row {
  display: grid;
  grid-template-columns: 56px 1fr 36px;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  font-size: 13px;
}

.bar-row__label {
  color: var(--text-2);
}

.bar-row__track {
  height: 8px;
  border-radius: 4px;
  background: var(--bg-page);
  overflow: hidden;
}

.bar-row__fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s;
}

.bar-row__value {
  text-align: right;
  color: var(--text-2);
  font-variant-numeric: tabular-nums;
}

/* 快捷入口 */
.shortcut {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: var(--radius);
  cursor: pointer;
  transition: background 0.15s;
}

.shortcut:hover {
  background: var(--bg-hover);
}

.shortcut__icon {
  font-size: 18px;
  color: var(--brand);
}

.shortcut b {
  font-size: 13px;
}

.shortcut p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-3);
}

@media (max-width: 1200px) {
  .cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
