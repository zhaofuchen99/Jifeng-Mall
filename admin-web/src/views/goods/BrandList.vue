<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import brandApi from '@/api/brand'
import { useCrud, useDialog } from '@/composables/useCrud'
import { datetime, onImgError } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'

/**
 * 品牌管理 —— 后台列表页的样板。
 *
 * 通用形状：查询表单 → 表格（含批量选择）→ 分页 → 新增/编辑弹窗。
 * 其余管理页（分类/商品/会员/秒杀/系统管理/地区）都是这个结构，照着写即可。
 *
 * 两个约定：
 * 1. useCrud 返回的都是 ref，解构后模板里直接用名字（Vue 会自动解包），
 *    不要在模板里写 `crud.xxx.value` 那种形式。
 * 2. selection 用 script 里定义的 handler 赋值，避免在模板内联箭头里给 ref 赋值。
 */
const {
  rows, total, loading, selection, query, pageNo, size,
  load, search, reset, remove, onPageChange, onSizeChange
} = useCrud({
  api: brandApi,
  label: '品牌',
  pageSize: 10,
  query: { name: '', company: '' }
})

const dlg = useDialog(() => ({ name: '', company: '', site: '', logo: '', description: '' }))
const { visible, isEdit, submitting, form } = dlg
const formRef = ref()

const rules = {
  name: [
    { required: true, message: '请输入品牌名称', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  company: [{ max: 100, message: '不超过 100 个字符', trigger: 'blur' }],
  site: [{ max: 255, message: '不超过 255 个字符', trigger: 'blur' }]
}

const onSelectionChange = (v) => (selection.value = v)

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await brandApi.update(form.value)
      ElMessage.success('品牌已更新')
    } else {
      await brandApi.save(form.value)
      ElMessage.success('品牌已新增')
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

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 查询 -->
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="品牌名称">
          <el-input
            v-model="query.name"
            placeholder="支持模糊匹配"
            clearable
            style="width: 200px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="公司">
          <el-input
            v-model="query.company"
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
        <span class="table-toolbar__title">品牌列表</span>
        <div class="table-toolbar__actions">
          <el-button
            type="danger"
            plain
            :disabled="selection.length === 0"
            @click="remove(selection.map((r) => r.id))"
          >
            批量删除
          </el-button>
          <el-button type="primary" @click="onAdd">
            <el-icon><Plus /></el-icon> 新增品牌
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="Logo" width="80" align="center">
          <template #default="{ row }">
            <img v-if="row.logo" class="cell-img cell-img--sm" :src="row.logo" @error="onImgError" />
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="品牌名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="company" label="公司" min-width="160" show-overflow-tooltip />
        <el-table-column label="官网" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <a v-if="row.site" :href="row.site" target="_blank" class="site-link">{{ row.site }}</a>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="简介" min-width="160" show-overflow-tooltip />
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
          <el-empty description="没有符合条件的品牌" :image-size="80" />
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

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="visible"
      :title="isEdit ? '编辑品牌' : '新增品牌'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="品牌名称" prop="name">
          <el-input v-model="form.name" placeholder="品牌唯一名称" />
        </el-form-item>
        <el-form-item label="公司" prop="company">
          <el-input v-model="form.company" placeholder="所属公司" />
        </el-form-item>
        <el-form-item label="官网" prop="site">
          <el-input v-model="form.site" placeholder="https://" />
        </el-form-item>
        <el-form-item label="Logo">
          <ImageUpload v-model="form.logo" type="brand" tip="建议正方形，≤ 10MB" />
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

<style scoped>
.site-link {
  color: var(--brand);
}

.site-link:hover {
  text-decoration: underline;
}
</style>
