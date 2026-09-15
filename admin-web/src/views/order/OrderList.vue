<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import orderApi from '@/api/order'
import { useCrud } from '@/composables/useCrud'
import { ORDER_STATUS, ORDER_STATUS_TAG, REFUND_STATUS_TAG, refundStatusText } from '@/utils/dict'
import { datetime, money, onImgError } from '@/utils/format'

/**
 * 订单管理。
 *
 * 订单不走 useCrud 的 remove：后台不提供删除，只能按状态发货 / 取消（取消会回补库存）。
 *
 * 退款是模拟的，分两步（与模拟支付的「发起 → 确认」对称）：
 *   发起退款 已支付/待收货 + 无退款 → 退款中
 *   确认退款 退款中                  → 已退款，订单转「已取消」并回补库存
 * 所以这两个按钮互斥出现，同一时刻只会看到一个。
 *
 * ⚠️ OrderEntity 里<b>没有</b> items 字段（接口文档写「含明细」与实现不符），
 * 所以订单明细必须另调 orderApi.items(orderId)，不能指望详情里带出来。
 */
const {
  rows, total, loading, query, pageNo, size,
  load, search, reset, onPageChange, onSizeChange
} = useCrud({
  api: orderApi,
  pageSize: 10,
  query: { orderNo: '', memberAccount: '', status: '', receiverPhone: '' }
})

const detailVisible = ref(false)
const detail = ref({})
const items = ref([])
const itemsLoading = ref(false)

async function onDetail(row) {
  detail.value = row
  detailVisible.value = true
  items.value = []
  itemsLoading.value = true
  try {
    items.value = (await orderApi.items(row.id)) || []
  } finally {
    itemsLoading.value = false
  }
}

async function onShip(row) {
  const ok = await ElMessageBox.confirm(
    `确定要为订单「${row.orderNo}」发货吗？`,
    '发货确认',
    { type: 'warning' }
  ).catch(() => false)
  if (!ok) return

  await orderApi.ship(row.id)
  ElMessage.success('已发货')
  await load()
}

async function onCancel(row) {
  const ok = await ElMessageBox.confirm(
    `确定要取消订单「${row.orderNo}」吗？取消后占用的库存会回补。`,
    '取消确认',
    { type: 'warning' }
  ).catch(() => false)
  if (!ok) return

  await orderApi.cancel(row.id)
  ElMessage.success('订单已取消')
  await load()
}

/** 该订单当前能否发起退款 */
function canRefund(row) {
  const notRefunded = !row.refundStatus || row.refundStatus === '无退款'
  return notRefunded && (row.status === '已支付' || row.status === '待收货')
}

async function onRefund(row) {
  const ok = await ElMessageBox.confirm(
    `确定为订单「${row.orderNo}」发起退款吗？\n` +
      `金额 ¥${money(row.totalPay)} 将模拟原路退回，订单进入「退款中」。`,
    '发起退款',
    { type: 'warning' }
  ).catch(() => false)
  if (!ok) return

  await orderApi.refund(row.id)
  ElMessage.success('已发起退款，等待确认')
  await load()
}

async function onRefundConfirm(row) {
  const ok = await ElMessageBox.confirm(
    `确认订单「${row.orderNo}」退款到账吗？\n` +
      `确认后订单转为「已取消」，占用的库存会回补。`,
    '确认退款',
    { type: 'warning' }
  ).catch(() => false)
  if (!ok) return

  await orderApi.refundConfirm(row.id)
  ElMessage.success('退款已完成')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 查询 -->
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="订单号">
          <el-input
            v-model="query.orderNo"
            placeholder="支持模糊匹配"
            clearable
            style="width: 200px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="会员账号">
          <el-input
            v-model="query.memberAccount"
            placeholder="支持模糊匹配"
            clearable
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="s in ORDER_STATUS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="收货人手机">
          <el-input
            v-model="query.receiverPhone"
            placeholder="支持模糊匹配"
            clearable
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <span class="table-toolbar__title">订单列表</span>
        <div class="table-toolbar__actions">
          <el-button @click="load">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="memberAccount" label="会员账号" width="120" show-overflow-tooltip />
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">
            <span class="price">{{ money(row.totalPay) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS_TAG[row.status]" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退款状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="REFUND_STATUS_TAG[refundStatusText(row.refundStatus)]" effect="light">
              {{ refundStatusText(row.refundStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="是否秒杀单" width="110" align="center">
          <template #default="{ row }">
            <!-- 秒杀单才有 seckillNo，普通单是 null -->
            <el-tag v-if="row.seckillNo" type="danger" effect="light">秒杀</el-tag>
            <span v-else class="text-muted">否</span>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="160">
          <template #default="{ row }">{{ datetime(row.checkoutTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="onDetail(row)">详情</el-button>
            <el-button v-if="row.status === '已支付'" link type="primary" @click="onShip(row)">
              发货
            </el-button>
            <el-button v-if="row.status === '待付款'" link type="danger" @click="onCancel(row)">
              取消
            </el-button>
            <el-button v-if="canRefund(row)" link type="danger" @click="onRefund(row)">
              退款
            </el-button>
            <el-button
              v-if="row.refundStatus === '退款中'"
              link
              type="danger"
              @click="onRefundConfirm(row)"
            >
              确认退款
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有符合条件的订单" :image-size="80" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="820px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="ORDER_STATUS_TAG[detail.status]" effect="light">{{ detail.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="会员账号">{{ detail.memberAccount || '—' }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">
          <span class="price">{{ money(detail.totalPay) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="是否秒杀单">
          {{ detail.seckillNo ? '是' : '否' }}
        </el-descriptions-item>
        <el-descriptions-item label="秒杀编号">{{ detail.seckillNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ detail.payType || '—' }}</el-descriptions-item>
        <el-descriptions-item label="退款状态">
          <el-tag
            :type="REFUND_STATUS_TAG[refundStatusText(detail.refundStatus)]"
            effect="light"
            size="small"
          >
            {{ refundStatusText(detail.refundStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付宝交易号" :span="2">
          {{ detail.alipayTradeNo || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ datetime(detail.checkoutTime) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ datetime(detail.payTime) }}</el-descriptions-item>
        <el-descriptions-item label="发货时间">{{ datetime(detail.shipTime) }}</el-descriptions-item>
        <el-descriptions-item label="确认收货时间">{{ datetime(detail.acceptTime) }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ detail.receiverName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="收货人手机">{{ detail.receiverPhone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">
          {{ detail.receiverAddrDetail || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="收货地址编号">
          {{ detail.receiverAddrId ?? '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="是否已删除">{{ detail.isDel ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="客户备注" :span="2">
          {{ detail.orderComment || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="平台备注" :span="2">
          {{ detail.description || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="记录 ID">{{ detail.id }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">订单明细</el-divider>

      <el-table v-loading="itemsLoading" :data="items" border size="small">
        <el-table-column label="图片" width="66" align="center">
          <template #default="{ row }">
            <img v-if="row.goodPic" class="cell-img cell-img--sm" :src="row.goodPic" @error="onImgError" />
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="goodName" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="goodId" label="商品编号" width="100" align="center" />
        <el-table-column label="成交价" width="100" align="right">
          <template #default="{ row }">{{ money(row.dealPrice) }}</template>
        </el-table-column>
        <el-table-column prop="count" label="数量" width="70" align="center" />
        <el-table-column label="小计" width="110" align="right">
          <template #default="{ row }">
            <span class="price">{{ money((row.dealPrice || 0) * (row.count || 0)) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="goodDesc" label="商品摘要" min-width="140" show-overflow-tooltip />

        <template #empty>
          <el-empty description="该订单没有明细" :image-size="60" />
        </template>
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
