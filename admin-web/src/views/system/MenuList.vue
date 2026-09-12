<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { menuApi, resourceApi } from '@/api/rbac'
import { useCrud, useDialog } from '@/composables/useCrud'
import { datetime } from '@/utils/format'

/**
 * 菜单管理。
 *
 * ⚠️ /api/menus 的分页查询返回的是扁平列表（children 恒为 null），只有 /api/menus/mine
 * 才是树；表格又要树形展示，所以在前端按 parentId 组装。父 id 为 0 或 null 的是根。
 *
 * 菜单就几十条，这里靠 pageSize 0（PageHelper 的「不分页」约定）一次取全，不做分页条，
 * 否则分页会把父子菜单截断在不同页里。
 */
const tree = ref([])

const { loading, selection, query, load, search, reset, remove } = useCrud({
  api: menuApi,
  label: '菜单',
  pageSize: 0,
  query: { name: '' },
  afterLoad: (list) => (tree.value = buildTree(list))
})

/**
 * 扁平列表 → 树。按 id 建索引后一遍挂接，比每层 filter 一遍快，也不会漏掉乱序的数据。
 * 名称搜索命中子菜单时它的父级不在结果里，这个节点会退化成根节点，是预期行为。
 */
function buildTree(list) {
  const nodes = (list || []).map((m) => ({ ...m, children: [] }))
  const byId = new Map(nodes.map((n) => [n.id, n]))
  const roots = []

  nodes.forEach((n) => {
    const parent = n.parentId ? byId.get(n.parentId) : null
    if (parent) parent.children.push(n)
    else roots.push(n)
  })

  // 叶子节点要把空的 children 删掉，否则表格会给它也画一个展开箭头
  const prune = (arr) =>
    arr.forEach((n) => (n.children.length ? prune(n.children) : delete n.children))
  prune(roots)

  return roots
}

const dlg = useDialog(() => ({
  parentId: 0,
  resourceId: null,
  name: '',
  icon: '',
  url: '',
  sort: 0,
  description: ''
}))
const { visible, isEdit, submitting, form } = dlg
const formRef = ref()

const rules = {
  name: [
    { required: true, message: '请输入菜单名称', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  icon: [{ max: 100, message: '不超过 100 个字符', trigger: 'blur' }],
  url: [{ max: 255, message: '不超过 255 个字符', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序值', trigger: 'blur' }]
}

const resources = ref([])

/** 编辑时自己和自己的子孙都不能当父级，否则会把菜单挂成一个环 */
const bannedIds = computed(() => {
  const out = new Set()
  if (!form.value.id) return out

  const findSelf = (list) => {
    for (const n of list) {
      if (n.id === form.value.id) return n
      const hit = n.children ? findSelf(n.children) : null
      if (hit) return hit
    }
    return null
  }
  const collect = (n) => {
    out.add(n.id)
    ;(n.children || []).forEach(collect)
  }

  const self = findSelf(tree.value)
  if (self) collect(self)
  return out
})

/** 父级下拉的树数据；外挂一个 id 0 的虚拟根，让「顶级菜单」也能选 */
const parentOptions = computed(() => {
  const mark = (list) =>
    list.map((m) => ({
      id: m.id,
      name: m.name,
      disabled: bannedIds.value.has(m.id),
      children: m.children?.length ? mark(m.children) : undefined
    }))
  return [{ id: 0, name: '顶级菜单', children: mark(tree.value) }]
})

const onSelectionChange = (v) => (selection.value = v)

async function loadResources() {
  resources.value = (await resourceApi.list({ pageNo: 1, pageSize: 0 }))?.list || []
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    const payload = { ...form.value }
    // parent_id / sort 在库里是 NOT NULL（insert 显式带了这两列），不能送 null
    if (payload.parentId == null) payload.parentId = 0
    if (payload.sort == null) payload.sort = 0

    if (isEdit.value) {
      await menuApi.update(payload)
      ElMessage.success('菜单已更新')
    } else {
      await menuApi.save(payload)
      ElMessage.success('菜单已新增')
    }
    visible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

function onAdd() {
  dlg.open(null)
  formRef.value?.clearValidate()
}

function onEdit(row) {
  dlg.open(row)
  formRef.value?.clearValidate()
}

onMounted(() => {
  load()
  loadResources()
})
</script>

<template>
  <div class="page">
    <!-- 查询 -->
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="菜单名称">
          <el-input
            v-model="query.name"
            placeholder="支持模糊匹配"
            clearable
            style="width: 200px"
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
        <span class="table-toolbar__title">菜单树</span>
        <div class="table-toolbar__actions">
          <el-button @click="load">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
          <el-button
            type="danger"
            plain
            :disabled="selection.length === 0"
            @click="remove(selection.map((r) => r.id))"
          >
            批量删除
          </el-button>
          <el-button type="primary" @click="onAdd">
            <el-icon><Plus /></el-icon> 新增菜单
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tree"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="菜单名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="图标" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <!-- 存的是 el-icon-xxx 这个名字，后台没有按名字渲染图标的必要，直接当文本看 -->
            <span v-if="row.icon" class="text-muted">{{ row.icon }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="url" label="路径" min-width="150" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="关联资源ID" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.resourceId">{{ row.resourceId }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">{{ datetime(row.updatedTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有符合条件的菜单" :image-size="80" />
        </template>
      </el-table>
    </div>

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="visible"
      :title="isEdit ? '编辑菜单' : '新增菜单'"
      width="580px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单">
          <!-- 必须开 check-strictly：否则点击有子节点的行只会展开，选不中它当父级 -->
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', disabled: 'disabled' }"
            node-key="id"
            check-strictly
            default-expand-all
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="form.name" placeholder="如：商品管理" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="图标" prop="icon">
              <el-input v-model="form.icon" placeholder="如：el-icon-goods" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="icon-tip">
          可用图标：el-icon-house / el-icon-goods / el-icon-document / el-icon-user /
          el-icon-timer / el-icon-setting
        </div>

        <el-form-item label="路径" prop="url">
          <el-input v-model="form.url" placeholder="如：/goods/brand" />
        </el-form-item>

        <el-form-item label="关联资源">
          <el-select
            v-model="form.resourceId"
            placeholder="可不填，选了就受该资源的鉴权约束"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="r in resources"
              :key="r.id"
              :label="`#${r.id} ${r.name}（${r.type}）`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
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

<style scoped>
.icon-tip {
  margin: -8px 0 12px 100px;
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.5;
}
</style>
