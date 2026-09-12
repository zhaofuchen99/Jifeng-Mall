import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 后台列表页的通用「查询 + 分页 + 删除」逻辑。
 *
 * <p>后台近 20 个页面是这个形状：顶部查询表单 → 表格 → 分页 → 新增/编辑弹窗。
 * 把重复的那部分抽出来，每个页面只写自己的列和表单字段。</p>
 *
 * @param {object}   opts
 * @param {object}   opts.api      { list, save, update, remove } 四个接口函数
 * @param {object}   opts.query    初始查询条件（会被 reset 用来还原）
 * @param {number}   opts.pageSize 每页条数
 * @param {string}   opts.label    实体中文名，用于删除确认的文案
 * @param {Function} opts.afterLoad 查询完成后的回调（可选）
 */
export function useCrud({ api, query: initialQuery = {}, pageSize = 10, label = '记录', afterLoad }) {
  const rows = ref([])
  const total = ref(0)
  const loading = ref(false)
  const selection = ref([])
  const pageNo = ref(1)
  const size = ref(pageSize)
  const query = reactive({ ...initialQuery })

  async function load() {
    loading.value = true
    try {
      const page = await api.list({ ...query, pageNo: pageNo.value, pageSize: size.value })
      rows.value = page?.list || []
      total.value = page?.total ?? rows.value.length
      if (afterLoad) await afterLoad(rows.value)
    } finally {
      loading.value = false
    }
  }

  /** 条件变了要回到第一页，否则会停在一个空页上 */
  function search() {
    pageNo.value = 1
    return load()
  }

  function reset() {
    Object.assign(query, initialQuery)
    return search()
  }

  function onPageChange(p) {
    pageNo.value = p
    return load()
  }

  function onSizeChange(s) {
    size.value = s
    pageNo.value = 1
    return load()
  }

  async function remove(ids) {
    const list = Array.isArray(ids) ? ids : [ids]
    if (list.length === 0) {
      ElMessage.warning('请先勾选要删除的数据')
      return false
    }
    const ok = await ElMessageBox.confirm(
      list.length > 1 ? `确定要删除选中的 ${list.length} 条${label}吗？` : `确定要删除该${label}吗？`,
      '删除确认',
      { type: 'warning' }
    ).catch(() => false)
    if (!ok) return false

    await api.remove(list)
    ElMessage.success('已删除')
    // 整页被删空时往前退一页，否则会停在空白页
    if (rows.value.length === list.length && pageNo.value > 1) {
      pageNo.value -= 1
    }
    await load()
    return true
  }

  return {
    rows, total, loading, selection, query, pageNo, size,
    load, search, reset, remove, onPageChange, onSizeChange
  }
}

/**
 * 新增/编辑弹窗的状态。表单对象用响应式对象承载，页面直接往上面挂字段。
 *
 * <pre>
 * const dlg = useDialog(() => ({ enabled: true, sort: 0 }))
 * dlg.open(row)   // 传 row = 编辑，不传 = 新增
 * </pre>
 */
export function useDialog(makeEmpty = () => ({})) {
  const visible = ref(false)
  const isEdit = ref(false)
  const submitting = ref(false)
  const form = ref(makeEmpty())

  function open(row) {
    isEdit.value = !!row
    // 浅拷贝，避免直接改到表格里的那一行
    form.value = row ? { ...row } : makeEmpty()
    visible.value = true
  }

  function close() {
    visible.value = false
  }

  return { visible, isEdit, submitting, form, open, close }
}
