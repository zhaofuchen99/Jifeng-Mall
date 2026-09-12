/** 金额：后端 BigDecimal 序列化成数字，统一显示两位小数 */
export function money(value) {
  if (value === null || value === undefined || value === '') return '0.00'
  const n = Number(value)
  return Number.isNaN(n) ? '0.00' : n.toFixed(2)
}

/**
 * 时间显示。
 * 带 @JsonFormat 的字段是 "yyyy-MM-dd HH:mm:ss"；没带的（如会员生日 LocalDate）是 ISO，
 * 可能出现 "2000-01-01T00:00:00"，这里统一裁掉 T。
 */
export function datetime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

/** 只取日期部分 */
export function date(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 10)
}

/** 订单状态 → Element Plus tag 类型 */
export function orderStatusTag(status) {
  return (
    {
      待付款: 'warning',
      已支付: 'primary',
      待收货: 'info',
      已确认: 'success',
      已取消: 'danger'
    }[status] || 'info'
  )
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

/** 把 "yyyy-MM-dd HH:mm:ss" 解析成毫秒（Safari 不认带空格的格式，必须换 T） */
export function parseTime(value) {
  if (!value) return 0
  return new Date(String(value).replace(' ', 'T')).getTime()
}
