<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { regionApi } from '@/api/region'
import { useDialog } from '@/composables/useCrud'

/**
 * 地区管理（FR-210「行政区划树维护（省/市/区）」）。
 *
 * 树形结构用前端组树，不用 el-table 的懒加载：区划数据量很小（种子 5 条），
 * 一次 pageSize=0 拿全量（后端 PaginateInfo + PageHelper 里 0 表示不分页），
 * 再按 parentId 组装，比逐级点开少一堆状态。
 *
 * 三个由后端决定的规则，前端只做提示、不重复实现：
 * - level 由 parentId 推导（根 1，最深 3 级），表单里是只读展示；
 * - 不能把区划挂到自己或自己的下级下面（后端会拒，前端先把选项禁掉）；
 * - 删除是级联的，会连同所有下级一起删掉。
 */
const list = ref([])
const loading = ref(false)
const keyword = ref('')

const LEVEL_TEXT = { 1: '省', 2: '市', 3: '区' }
const LEVEL_TAG = { 1: 'danger', 2: 'warning', 3: 'success' }

const dlg = useDialog(() => ({
  id: null,
  name: '',
  parentId: 0,
  sortOrder: 0,
  description: ''
}))
const { visible, isEdit, submitting, form } = dlg
const formRef = ref()

const rules = {
  name: [
    { required: true, message: '请输入区划名称', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  // 编码可以留空（交给数据库自增），填了就必须是数字 ——
  // 否则它会以 Long 反序列化失败的形态报 400，提示看不出问题在哪
  id: [
    {
      validator: (rule, value, cb) => {
        if (value === '' || value === null || value === undefined) return cb()
        if (/^\d+$/.test(String(value))) return cb()
        cb(new Error('区划编码必须是数字'))
      },
      trigger: 'blur'
    }
  ]
}

/**
 * 按 parentId 组树。
 * 只给真的有子节点的行挂 children —— 空数组会让 el-table 画出多余的展开箭头。
 */
const tree = computed(() => {
  const map = new Map()
  list.value.forEach((r) => map.set(r.id, { ...r }))

  const roots = []
  map.forEach((node) => {
    const parent = node.parentId ? map.get(node.parentId) : null
    if (parent) {
      if (!parent.children) parent.children = []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
})

/** 按名称过滤（保留命中节点的祖先链，否则树会断） */
const displayTree = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return tree.value

  const filter = (nodes) =>
    nodes
      .map((n) => {
        const kids = filter(n.children || [])
        const copy = { ...n, children: kids }
        // 同上：没有命中子节点就别留空数组
        if (!kids.length) delete copy.children
        return copy
      })
      .filter((n) => n.name.includes(kw) || n.children?.length)

  return filter(tree.value)
})

/**
 * 上级区划下拉的候选：把树拍平成带缩进的选项。
 * 编辑时把自己和整棵子树排除掉 —— 选了自己或自己的下级会成环，后端也会拒。
 */
const parentOptions = computed(() => {
  const out = [{ id: 0, label: '无（作为顶级/省）', level: 0 }]
  const excluded = isEdit.value ? subtreeIds(form.value.id) : new Set()

  const walk = (nodes, depth) => {
    nodes.forEach((n) => {
      if (excluded.has(n.id)) return
      out.push({ id: n.id, label: '　'.repeat(depth) + n.name, level: depth + 1 })
      if (n.children?.length) walk(n.children, depth + 1)
    })
  }
  walk(tree.value, 0)
  return out
})

/** 某个节点及其全部后代的 id 集合 */
function subtreeIds(id) {
  const ids = new Set()
  if (!id) return ids
  const collect = (node) => {
    ids.add(node.id)
    ;(node.children || []).forEach(collect)
  }
  const find = (nodes) => {
    for (const n of nodes) {
      if (n.id === id) {
        collect(n)
        return true
      }
      if (n.children?.length && find(n.children)) return true
    }
    return false
  }
  find(tree.value)
  return ids
}

/** 选中上级后层级就定了，表单里跟着变（真正的值仍由后端推导） */
const formLevel = computed(() => {
  const p = parentOptions.value.find((o) => o.id === form.value.parentId)
  return (p?.level ?? 0) + 1
})

async function load() {
  loading.value = true
  try {
    const page = await regionApi.list({ pageNo: 1, pageSize: 0 })
    list.value = page?.list || []
  } finally {
    loading.value = false
  }
}

function onAdd(parentId = 0) {
  dlg.open(null)
  form.value.parentId = parentId
  formRef.value?.clearValidate()
}

function onEdit(row) {
  dlg.open(row)
  // 列表里的行带着 parent 链和 children，别提交回去（后端也不认这些字段）
  delete form.value.parent
  delete form.value.children
  formRef.value?.clearValidate()
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  if (formLevel.value > 3) {
    ElMessage.warning('行政区划最多三级（省/市/区），不能再往下加')
    return
  }

  submitting.value = true
  try {
    const payload = { ...form.value }
    delete payload.parent
    delete payload.children
    // 新增时不填编码就交给数据库自增，传 null 会覆盖掉 mapper 里的 <if>
    if (payload.id === null || payload.id === '') delete payload.id

    if (isEdit.value) {
      await regionApi.update(payload)
      ElMessage.success('区划已更新')
    } else {
      await regionApi.save(payload)
      ElMessage.success('区划已新增')
    }
    visible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function onDelete(row) {
  // 级联删除，先在后端算一遍子树规模来提示（前端只数得到已加载的部分，这里就直接数本地树）
  const count = subtreeIds(row.id).size
  const tip =
    count > 1
      ? `「${row.name}」下还有 ${count - 1} 个下级区划，删除会一并级联删除。确定继续吗？`
      : `确定要删除区划「${row.name}」吗？`

  const ok = await ElMessageBox.confirm(tip, '删除确认', { type: 'warning' }).catch(() => false)
  if (!ok) return

  const rows = await regionApi.remove([row.id])
  ElMessage.success(`已删除 ${rows} 条区划`)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-alert
      title="行政区划为省/市/区三级树。层级由上级自动推导，删除会连同下级一起删除。"
      type="info"
      :closable="false"
      show-icon
      class="mb-16"
    />

    <div class="filter-bar">
      <!-- 数据量小，过滤在本地算，不需要查询按钮 -->
      <el-form inline @submit.prevent>
        <el-form-item label="区划名称">
          <el-input v-model="keyword" placeholder="输入关键字过滤" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button @click="keyword = ''">清空</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <span class="table-toolbar__title">行政区划</span>
        <div class="table-toolbar__actions">
          <el-button @click="load">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
          <el-button type="primary" @click="onAdd(0)">
            <el-icon><Plus /></el-icon> 新增省级区划
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="displayTree"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="id" label="区划编码" width="130" />
        <el-table-column prop="name" label="名称" min-width="220" />
        <el-table-column label="层级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="LEVEL_TAG[row.level]" effect="light">
              {{ LEVEL_TEXT[row.level] || row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.level >= 3" @click="onAdd(row.id)">
              加下级
            </el-button>
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :description="keyword ? '没有匹配的区划' : '暂无区划数据'" :image-size="80" />
        </template>
      </el-table>
    </div>

    <el-dialog
      v-model="visible"
      :title="isEdit ? '编辑区划' : '新增区划'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级区划">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option
              v-for="o in parentOptions"
              :key="o.id"
              :label="o.label"
              :value="o.id"
              :disabled="o.level >= 3"
            />
          </el-select>
        </el-form-item>

        <!-- 层级由后端按上级推导，这里只是让管理员看到结果 -->
        <el-form-item label="层级">
          <el-tag :type="LEVEL_TAG[formLevel]" effect="light">
            {{ LEVEL_TEXT[formLevel] || '超出范围' }}
          </el-tag>
          <span class="text-muted" style="margin-left: 8px">由上级自动推导</span>
        </el-form-item>

        <el-form-item label="区划编码" prop="id">
          <el-input
            v-model.number="form.id"
            :disabled="isEdit"
            placeholder="行政区划编码，如 440000；留空自动生成"
          />
          <div v-if="isEdit" class="text-muted">编码是主键，创建后不可修改</div>
        </el-form-item>

        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：广东省" />
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>

        <el-form-item label="说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
