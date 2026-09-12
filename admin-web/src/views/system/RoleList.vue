<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { permApi, roleApi } from '@/api/rbac'
import { rolePermApi } from '@/api/relation'
import { useCrud, useDialog } from '@/composables/useCrud'
import { datetime } from '@/utils/format'
import { enabledTag, enabledText } from '@/utils/dict'

/**
 * 角色管理。
 *
 * ⚠️ 同用户组：t_rbac_role.enabled 是 NOT NULL 且 insert 显式带上该列，
 * 新增时 JSON 里必须显式传 enabled，表单默认 true 且提交时不删这个字段。
 */
const {
  rows, total, loading, selection, query, pageNo, size,
  load, search, reset, remove, onPageChange, onSizeChange
} = useCrud({
  api: roleApi,
  label: '角色',
  pageSize: 10,
  query: { name: '' }
})

const dlg = useDialog(() => ({ name: '', enabled: true, description: '' }))
const { visible, isEdit, submitting, form } = dlg
const formRef = ref()

const rules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ]
}

const onSelectionChange = (v) => (selection.value = v)

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await roleApi.update(form.value)
      ElMessage.success('角色已更新')
    } else {
      await roleApi.save(form.value)
      ElMessage.success('角色已新增')
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

/* ---------- 权限关联 ---------- */

const relVisible = ref(false)
const relLoading = ref(false)
const relSaving = ref(false)
const relRole = ref(null)
const relRows = ref([])
const perms = ref([])
const relPickId = ref()

const permNames = computed(() => new Map(perms.value.map((p) => [p.id, p.name])))
/** 已关联的权限不再进候选，否则会撞 (role_id, perm_id) 唯一键 */
const permCandidates = computed(() =>
  perms.value.filter((p) => !relRows.value.some((row) => row.permId === p.id))
)

async function loadPerms() {
  perms.value = (await permApi.list({ pageNo: 1, pageSize: 0 }))?.list || []
}

async function loadRel(roleId) {
  relLoading.value = true
  try {
    relRows.value = (await rolePermApi.byRole(roleId)) || []
  } finally {
    relLoading.value = false
  }
}

function openRel(row) {
  relRole.value = row
  relPickId.value = undefined
  relVisible.value = true
  return loadRel(row.id)
}

async function onRelAdd() {
  if (!relPickId.value) {
    ElMessage.warning('请先选择权限')
    return
  }
  relSaving.value = true
  try {
    await rolePermApi.save({ roleId: relRole.value.id, permId: relPickId.value })
    ElMessage.success('已关联权限')
    relPickId.value = undefined
    await loadRel(relRole.value.id)
  } finally {
    relSaving.value = false
  }
}

async function onRelRemove(row) {
  await rolePermApi.remove([row.id])
  ElMessage.success('已移除权限')
  await loadRel(relRole.value.id)
}

onMounted(() => {
  load()
  loadPerms()
})
</script>

<template>
  <div class="page">
    <!-- 查询 -->
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="角色名称">
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
        <span class="table-toolbar__title">角色列表</span>
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
            <el-icon><Plus /></el-icon> 新增角色
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="角色名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="启用状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="enabledTag(row.enabled)">{{ enabledText(row.enabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">{{ datetime(row.updatedTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRel(row)">权限</el-button>
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有符合条件的角色" :image-size="80" />
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
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="角色唯一名称" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="说明">
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

    <!-- 权限关联 -->
    <el-dialog v-model="relVisible" :title="`权限 — ${relRole?.name || ''}`" width="560px" destroy-on-close>
      <div class="flex gap-8 mb-8">
        <el-select v-model="relPickId" placeholder="选择要关联的权限" filterable clearable style="flex: 1">
          <el-option v-for="p in permCandidates" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-button type="primary" :loading="relSaving" @click="onRelAdd">关联</el-button>
      </div>

      <el-table v-loading="relLoading" :data="relRows" border size="small">
        <el-table-column prop="id" label="关联ID" width="100" />
        <el-table-column label="权限" min-width="160">
          <template #default="{ row }">{{ permNames.get(row.permId) || `#${row.permId}` }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="onRelRemove(row)">移除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="尚未关联任何权限" :image-size="60" />
        </template>
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="relVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
