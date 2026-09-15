/**
 * 字典与枚举。值都取自后端实际写入的数据，不是自己编的：
 * 订单状态见 OrderServiceImpl（待付款/已支付/待收货/已确认/已取消），
 * 退款状态见 order 表默认值「无退款」。
 */

export const ORDER_STATUS = ['待付款', '已支付', '待收货', '已确认', '已取消']

export const ORDER_STATUS_TAG = {
  待付款: 'warning',
  已支付: 'primary',
  待收货: 'info',
  已确认: 'success',
  已取消: 'danger'
}

export const REFUND_STATUS = ['无退款', '退款中', '已退款']

/** 退款状态 → tag 颜色。历史数据 refundStatus 可能为 null，按「无退款」显示 */
export const REFUND_STATUS_TAG = {
  无退款: 'info',
  退款中: 'warning',
  已退款: 'danger'
}

export function refundStatusText(v) {
  return v || '无退款'
}

/** 资源类型：种子数据里只有「菜单」和「接口」两种 */
export const RESOURCE_TYPES = ['菜单', '接口', '按钮']

/** 是否启用 → 表格里的 tag */
export function enabledTag(v) {
  return v === true ? 'success' : 'info'
}

export function enabledText(v) {
  return v === true ? '启用' : '禁用'
}

/**
 * 秒杀活动状态：由启用状态与起止时间推导（需求 4.4）。
 * @returns {'未开始'|'进行中'|'已结束'|'已禁用'}
 */
export function seckillStatus(activity) {
  if (!activity) return '已结束'
  if (!activity.enabled) return '已禁用'
  const now = Date.now()
  const start = activity.startTime ? new Date(String(activity.startTime).replace(' ', 'T')).getTime() : 0
  const end = activity.endTime ? new Date(String(activity.endTime).replace(' ', 'T')).getTime() : 0
  if (start && now < start) return '未开始'
  if (end && now > end) return '已结束'
  return '进行中'
}

export const SECKILL_STATUS_TAG = {
  未开始: 'info',
  进行中: 'danger',
  已结束: 'info',
  已禁用: 'warning'
}
