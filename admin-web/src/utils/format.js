/** 金额：后端 BigDecimal 序列化成数字，统一显示两位小数 */
export function money(value) {
  if (value === null || value === undefined || value === '') return '0.00'
  const n = Number(value)
  return Number.isNaN(n) ? '0.00' : n.toFixed(2)
}

/** 时间显示；带 @JsonFormat 的字段是 "yyyy-MM-dd HH:mm:ss"，没带的是 ISO */
export function datetime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

export function date(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 10)
}

/** 把 "yyyy-MM-dd HH:mm:ss" 解析成毫秒（Safari 不认带空格的格式，必须换 T） */
export function parseTime(value) {
  if (!value) return 0
  return new Date(String(value).replace(' ', 'T')).getTime()
}

/** 时间戳 → "yyyy-MM-dd HH:mm:ss"，给 el-date-picker 回填用 */
export function toDateTimeString(d) {
  if (!d) return ''
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(
    d.getMinutes()
  )}:${p(d.getSeconds())}`
}

/** 逗号分隔的图片 URL 字符串 → 数组 */
export function splitUrls(value) {
  return (value || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

/** 图片加载失败时退化成占位图 */
export function onImgError(e) {
  e.target.src = '/img-placeholder.svg'
}
