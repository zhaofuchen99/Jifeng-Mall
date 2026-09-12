<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { resourceApi } from '@/api/rbac'
import { useCrud, useDialog } from '@/composables/useCrud'
import { datetime } from '@/utils/format'
import { RESOURCE_TYPES } from '@/utils/dict'

/**
 * 资源管理。资源是网关鉴权的最小单位：接口用 Ant 风格路径匹配，按钮用标识串。
 */
const {
  rows, total, loading, selection, query, pageNo, size,
  load, search, reset, remove, onPageChange, onSizeChange
} = useCrud({
  api: resourceApi,
  label: '资源',
  pageSize: 10,
  query: { name: '', type: '' }
})

/** 仅用于表格配色，(type, value) 才是库里的唯一键 */
const TYPE_TAG = { 菜单: 'primary', 接口: 'success', 按钮: 'warning' }

const dlg = useDialog(() => ({ name: '', type: '接口', value: '', description: '' }))
const { visible, isEdit, submitting, form } = dlg
const formRef = ref()

const rules = {
  name: [
    { required: true, message: '请输入资源名称', trigger: 'blur' },
    { max: 100, message: '不超过 100 个字符', trigger: 'blur' }
  ],
  type: [{ required: true, message: '请选择资源类型', trigger: 'change' }],
  value: [
    { required: true, message: '请输入资源值', trigger: 'blur' },
    { max: 255, message: '不超过 255 个字符', trigger: 'blur' }
  ]
}

const onSelectionChange = (v) => (selection.value = v)

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await resourceApi.update(form.value)
      ElMessage.success('资源已更新')
    } else {
      await resourceApi.save(form.value)
      ElMessage.success('资源已新增')
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
        <el-form-item label="资源名称">
          <el-input
            v-model="query.name"
            placeholder="支持模糊匹配"
            clearable
            style="width: 200px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="t in RESOURCE_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
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
        <span class="table-toolbar__title">资源列表</span>
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
            <el-icon><Plus /></el-icon> 新增资源
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="资源名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="TYPE_TAG[row.type] || 'info'">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="value" label="资源值" min-width="220" show-overflow-tooltip />
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
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
          <el-empty description="没有符合条件的资源" :image-size="80" />
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
      :title="isEdit ? '编辑资源' : '新增资源'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="资源名称" prop="name">
          <el-input v-model="form.name" placeholder="如：商品查询" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="t in RESOURCE_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源值" prop="value">
          <!-- 模板里的 form 已解包，这里的 form.value 是资源实体自己的 value 列，不是 ref 的 .value -->
          <el-input v-model="form.value" placeholder="如：/api/goods/** 或 btn:good:add" />
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
