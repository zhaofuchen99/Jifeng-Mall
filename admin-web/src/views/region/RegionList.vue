<script setup>
import { computed, onMounted, ref } from 'vue'
import { regionApi } from '@/api/region'

/**
 * 地区管理（只读）。
 *
 * region-api 只有查询接口（list / detail / children），没有写接口，
 * 所以这个页面不做新增 / 编辑 / 删除，只做展示。
 *
 * 树形结构用前端组树而不是 el-table 的懒加载：区划数据量很小（种子数据 5 条），
 * 一次 pageSize=0 拿全量（后端 PaginateInfo + PageHelper 里 0 表示不分页），
 * 再按 parentId 组装，比逐级点开少一堆状态。
 */
const list = ref([])
const loading = ref(false)
const keyword = ref('')

const LEVEL_TEXT = { 1: '省', 2: '市', 3: '区' }
const LEVEL_TAG = { 1: 'danger', 2: 'warning', 3: 'success' }

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

async function load() {
  loading.value = true
  try {
    const page = await regionApi.list({ pageNo: 1, pageSize: 0 })
    list.value = page?.list || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-alert
      title="行政区划为只读数据，来自 t_china_region 表"
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

        <template #empty>
          <el-empty :description="keyword ? '没有匹配的区划' : '暂无区划数据'" :image-size="80" />
        </template>
      </el-table>
    </div>
  </div>
</template>
