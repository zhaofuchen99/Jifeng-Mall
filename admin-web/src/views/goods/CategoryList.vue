<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import categoryApi from '@/api/category'
import { useDialog } from '@/composables/useCrud'
import { datetime } from '@/utils/format'

/**
 * 分类管理。
 *
 * 用 /api/categories/tree 而不是分页列表：分类是树形结构，分页会把父子拆散。
 * 分类数量很少（种子数据 3 条），一次取回整棵树没问题。
 *
 * ⚠️ 后端 buildTree 里 `parentId` 的根节点判断同时认 null 和 0
 * （DDL 是 NOT NULL DEFAULT 0），所以表单里新增根分类时 parentId 传 0。
 */
const tree = ref([])
const loading = ref(false)
const keyword = ref('')

const dlg = useDialog(() => ({
  parentId: 0,
  name: '',
  title: '',
  tag: '',
  icon: '',
  summary: '',
  sort: 0,
  description: ''
}))
const { visible, isEdit, submitting, form } = dlg
const formRef = ref()

const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  sort: [{ required: true, message: '请输入排序值', trigger: 'blur' }]
}

/** 按名称过滤整棵树（保留命中节点的祖先链，否则树会断） */
const displayTree = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return tree.value

  const filter = (nodes) =>
    nodes
      .map((n) => ({ ...n, children: filter(n.children || []) }))
      .filter((n) => n.name.includes(kw) || (n.children && n.children.length > 0))

  return filter(tree.value)
})

/** 父级下拉的候选（把树拍平成带层级的选项） */
const parentOptions = computed(() => {
  const out = [{ id: 0, label: '顶级分类' }]
  const walk = (nodes, depth) => {
    nodes.forEach((n) => {
      out.push({ id: n.id, label: '　'.repeat(depth) + n.name })
      if (n.children?.length) walk(n.children, depth + 1)
    })
  }
  walk(tree.value, 0)
  return out
})

async function load() {
  loading.value = true
  try {
    tree.value = (await categoryApi.tree()) || []
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
  formRef.value?.clearValidate()
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  // 编辑时不能把自己选成自己的父级
  if (isEdit.value && form.value.parentId === form.value.id) {
    ElMessage.warning('不能把分类的父级设为它自己')
    return
  }

  submitting.value = true
  try {
    const payload = { ...form.value }
    delete payload.children
    delete payload.parent

    if (isEdit.value) {
      await categoryApi.update(payload)
      ElMessage.success('分类已更新')
    } else {
      await categoryApi.save(payload)
      ElMessage.success('分类已新增')
    }
    visible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function onDelete(row) {
  const hasChildren = row.children && row.children.length > 0
  const tip = hasChildren
    ? `「${row.name}」下有 ${row.children.length} 个子分类，删除会一并级联删除。确定继续吗？`
    : `确定要删除分类「${row.name}」吗？`

  const ok = await ElMessageBox.confirm(tip, '删除确认', { type: 'warning' }).catch(() => false)
  if (!ok) return

  await categoryApi.remove([row.id])
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="filter-bar">
      <el-form inline @submit.prevent>
        <el-form-item label="分类名称">
          <el-input
            v-model="keyword"
            placeholder="输入关键字过滤树"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="keyword = ''">清空</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <span class="table-toolbar__title">分类树</span>
        <div class="table-toolbar__actions">
          <el-button @click="load">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
          <el-button type="primary" @click="onAdd(0)">
            <el-icon><Plus /></el-icon> 新增顶级分类
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
        <el-table-column prop="name" label="分类名称" min-width="200" />
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="tag" label="标签" width="110" show-overflow-tooltip />
        <el-table-column label="图标" width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.icon" class="text-muted">{{ row.icon }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="description" label="简介" min-width="150" show-overflow-tooltip />
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">{{ datetime(row.updatedTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="onAdd(row.id)">加子类</el-button>
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :description="keyword ? '没有匹配的分类' : '暂无分类'" :image-size="80" />
        </template>
      </el-table>
    </div>

    <el-dialog
      v-model="visible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="580px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option
              v-for="o in parentOptions"
              :key="o.id"
              :label="o.label"
              :value="o.id"
              :disabled="o.id === form.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：手机数码" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标题">
              <el-input v-model="form.title" placeholder="展示用标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标签">
              <el-input v-model="form.tag" placeholder="如：热销" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标">
              <el-input v-model="form.icon" placeholder="图标名或 URL" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="摘要">
          <el-input v-model="form.summary" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
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
