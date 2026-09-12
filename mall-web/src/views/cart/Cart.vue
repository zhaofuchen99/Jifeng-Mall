<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { money } from '@/utils/format'

const router = useRouter()
const cartStore = useCartStore()

/** 勾选状态只在前端维护（后端没有购物车勾选字段） */
const checked = ref([])

const items = computed(() => cartStore.items)

const checkedItems = computed(() => items.value.filter((it) => checked.value.includes(it.id)))

const totalPrice = computed(() =>
  checkedItems.value.reduce((sum, it) => sum + Number(it.good?.price || 0) * it.qty, 0)
)

const totalCount = computed(() => checkedItems.value.reduce((sum, it) => sum + it.qty, 0))

const allChecked = computed({
  get: () => items.value.length > 0 && checked.value.length === items.value.length,
  set: (v) => {
    checked.value = v ? items.value.map((it) => it.id) : []
  }
})

/** 下架/删除/无库存的行不能结算 */
function rowDisabled(row) {
  const g = row.good
  return !g || g.isTakeDown === true || g.isDel === true
}

function rowMax(row) {
  return Math.max(1, row.good?.qty ?? 1)
}

async function onChangeQty(row) {
  if (row.qty > rowMax(row)) {
    ElMessage.warning(`库存只有 ${rowMax(row)} 件`)
    row.qty = rowMax(row)
  }
  if (row.qty < 1) row.qty = 1
  await cartStore.updateQty(row.id, row.qty)
}

async function onRemove(ids) {
  const ok = await ElMessageBox.confirm(
    ids.length > 1 ? `确定要删除这 ${ids.length} 件商品吗？` : '确定要删除该商品吗？',
    '提示',
    { type: 'warning' }
  ).catch(() => false)
  if (!ok) return
  await cartStore.remove(ids)
  checked.value = checked.value.filter((id) => !ids.includes(id))
  ElMessage.success('已删除')
}

function toCheckout() {
  const valid = checkedItems.value.filter((it) => !rowDisabled(it))
  if (valid.length === 0) {
    ElMessage.warning('请先勾选要结算的商品')
    return
  }
  if (valid.length !== checkedItems.value.length) {
    ElMessage.warning('已自动排除下架商品')
  }
  router.push({
    name: 'orderConfirm',
    query: { cartIds: valid.map((it) => it.id).join(',') }
  })
}

onMounted(async () => {
  await cartStore.refresh()
  checked.value = items.value.filter((it) => !rowDisabled(it)).map((it) => it.id)
})
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="section-head">
        <h3 class="section-title">我的购物车</h3>
        <span class="text-muted">共 {{ items.length }} 种商品</span>
      </div>

      <div v-loading="cartStore.loading" class="panel">
        <template v-if="items.length > 0">
          <!-- 表头 -->
          <div class="row row--head">
            <div class="col-check">
              <el-checkbox v-model="allChecked">全选</el-checkbox>
            </div>
            <div class="col-good">商品</div>
            <div class="col-price">单价</div>
            <div class="col-qty">数量</div>
            <div class="col-sub">小计</div>
            <div class="col-op">操作</div>
          </div>

          <!-- 商品行 -->
          <div v-for="row in items" :key="row.id" class="row" :class="{ 'is-disabled': rowDisabled(row) }">
            <div class="col-check">
              <el-checkbox v-model="checked" :value="row.id" :disabled="rowDisabled(row)" />
            </div>

            <div class="col-good">
              <img
                class="thumb"
                :src="row.good?.pic || '/img-placeholder.svg'"
                @error="(e) => (e.target.src = '/img-placeholder.svg')"
              />
              <div class="good-info">
                <router-link
                  :to="{ name: 'goodDetail', params: { id: row.goodId } }"
                  class="good-name"
                >
                  {{ row.good?.name || `商品 #${row.goodId}` }}
                </router-link>
                <el-tag v-if="rowDisabled(row)" type="info" size="small">已下架</el-tag>
                <span v-else class="text-muted">库存 {{ row.good?.qty ?? 0 }}</span>
              </div>
            </div>

            <div class="col-price price">¥{{ money(row.good?.price) }}</div>

            <div class="col-qty">
              <el-input-number
                v-model="row.qty"
                size="small"
                :min="1"
                :max="rowMax(row)"
                :disabled="rowDisabled(row)"
                @change="onChangeQty(row)"
              />
            </div>

            <div class="col-sub price">
              ¥{{ money(Number(row.good?.price || 0) * row.qty) }}
            </div>

            <div class="col-op">
              <el-button link type="danger" @click="onRemove([row.id])">删除</el-button>
            </div>
          </div>

          <!-- 结算栏 -->
          <div class="bar">
            <div class="bar__left">
              <el-checkbox v-model="allChecked">全选</el-checkbox>
              <el-button link type="danger" :disabled="checked.length === 0" @click="onRemove(checked)">
                批量删除
              </el-button>
            </div>
            <div class="bar__right">
              <span class="text-muted">已选 <b class="text-brand">{{ totalCount }}</b> 件</span>
              <span class="bar__total">
                合计：<span class="price">¥{{ money(totalPrice) }}</span>
              </span>
              <el-button type="primary" size="large" :disabled="checked.length === 0" @click="toCheckout">
                去结算
              </el-button>
            </div>
          </div>
        </template>

        <!-- 空态 -->
        <el-empty v-else-if="!cartStore.loading" description="购物车还是空的">
          <el-button type="primary" @click="router.push({ name: 'goods' })">去逛逛</el-button>
          <el-button @click="router.push({ name: 'seckill' })">看看秒杀</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<style scoped>
.row {
  display: grid;
  grid-template-columns: 60px 1fr 120px 150px 120px 80px;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
}

.row--head {
  padding: 12px 0;
  background: #fafafa;
  color: var(--text-3);
  font-size: 13px;
  border-radius: 4px;
}

.row.is-disabled {
  opacity: 0.55;
}

.col-check {
  display: flex;
  justify-content: center;
}

.col-good {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  width: 72px;
  height: 72px;
  object-fit: contain;
  border-radius: 6px;
  background: #fff;
  flex-shrink: 0;
}

.good-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}

.good-name {
  font-size: 14px;
  line-height: 1.4;
}

.good-name:hover {
  color: var(--brand);
}

.col-price,
.col-sub {
  text-align: center;
  font-size: 14px;
}

.col-sub {
  font-size: 16px;
}

.col-qty,
.col-op {
  text-align: center;
}

/* 结算栏 */
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding: 14px 16px;
  background: #fff8f8;
  border: 1px solid #ffe3e5;
  border-radius: var(--radius);
}

.bar__left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.bar__right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.bar__total .price {
  font-size: 24px;
}

.text-brand {
  color: var(--brand);
}
</style>
