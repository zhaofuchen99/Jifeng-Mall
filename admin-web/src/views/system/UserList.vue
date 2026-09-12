<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { groupApi, userApi } from '@/api/rbac'
import { userGroupApi } from '@/api/relation'
import { useCrud, useDialog } from '@/composables/useCrud'
import { datetime, onImgError } from '@/utils/format'
import { enabledTag, enabledText } from '@/utils/dict'
import ImageUpload from '@/components/ImageUpload.vue'

/**
 * 后台用户管理。
 *
 * 密码只有两条规则：新增必填（后端 save 时 BCrypt 加密），编辑留空表示不修改
 * —— user 的 update 语句是条件更新，password 为空会被整列跳过。
 */
const {
  rows, total, loading, selection, query, pageNo, size,
  load, search, reset, remove, onPageChange, onSizeChange
} = useCrud({
  api: userApi,
  label: '用户',
  pageSize: 10,
  query: { username: '' }
})

/**
 * ⚠️ status / loginTimes 不在表单里，但必须带着值提交：user 表的 insert 显式列了这两列，
 * 而它们都是 NOT NULL（status 默认 1、login_times 默认 0），少传一个就是 NULL 报错。
 * status 只在库里做标记，登录链路不校验它，所以照种子数据取 1。
 */
const dlg = useDialog(() => ({
  username: '',
  password: '',
  avatarUrl: '',
  enabled: true,
  status: 1,
  locked: false,
  loginTimes: 0,
  // 必须是 null 不能是空串：后端这两个字段是 LocalDateTime 且没有 @JsonFormat，
  // 收到 "" 会因为反序列化失败把整个请求打回
  userExpireTime: null,
  credentialExpireTime: null,
  description: ''
}))
const { visible, isEdit, submitting, form } = dlg
const formRef = ref()

/**
 * 到期时间没有 @JsonFormat（只有审计字段有），序列化出来是 ISO 带 T 的格式；
 * 所以 value-format 也用带 [T] 的 ISO，和后端对得上（[T] 是 dayjs 的字面量转义）。
 */
const EXPIRE_FORMAT = 'YYYY-MM-DD[T]HH:mm:ss'

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  password: [
    {
      validator: (rule, value, cb) => {
        if (isEdit.value || value) cb()
        else cb(new Error('请输入密码'))
      },
      trigger: 'blur'
    }
  ]
}

const onSelectionChange = (v) => (selection.value = v)

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    const payload = { ...form.value }
    // 编辑时留空的不传，后端 update 条件更新会跳过 password 列
    if (!payload.password) delete payload.password

    if (isEdit.value) {
      await userApi.update(payload)
      ElMessage.success('用户已更新')
    } else {
      await userApi.save(payload)
      ElMessage.success('用户已新增')
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
  // 列表查询会把 password 密文一并带出来，不清掉就会被原样回传（虽然后端认得 BCrypt 不会二次加密）
  form.value.password = ''
  formRef.value?.clearValidate()
}

/* ---------- 用户组关联 ---------- */

const relVisible = ref(false)
const relLoading = ref(false)
const relSaving = ref(false)
const relUser = ref(null)
const relRows = ref([])
const groups = ref([])
const relPickId = ref()

/** 关联表只存外键，组名要靠全部用户组回显 */
const groupNames = computed(() => new Map(groups.value.map((g) => [g.id, g.name])))
/** 已加入的组不再进候选，否则会撞 (user_id, group_id) 唯一键 */
const groupCandidates = computed(() =>
  groups.value.filter((g) => !relRows.value.some((r) => r.groupId === g.id))
)

async function loadGroups() {
  groups.value = (await groupApi.list({ pageNo: 1, pageSize: 0 }))?.list || []
}

async function loadRel(userId) {
  relLoading.value = true
  try {
    relRows.value = (await userGroupApi.byUser(userId)) || []
  } finally {
    relLoading.value = false
  }
}

function openRel(row) {
  relUser.value = row
  relPickId.value = undefined
  relVisible.value = true
  return loadRel(row.id)
}

async function onRelAdd() {
  if (!relPickId.value) {
    ElMessage.warning('请先选择用户组')
    return
  }
  relSaving.value = true
  try {
    await userGroupApi.save({ userId: relUser.value.id, groupId: relPickId.value })
    ElMessage.success('已加入用户组')
    relPickId.value = undefined
    await loadRel(relUser.value.id)
  } finally {
    relSaving.value = false
  }
}

async function onRelRemove(row) {
  await userGroupApi.remove([row.id])
  ElMessage.success('已移出用户组')
  await loadRel(relUser.value.id)
}

onMounted(() => {
  load()
  loadGroups()
})
</script>

<template>
  <div class="page">
    <!-- 查询 -->
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="用户名">
          <el-input
            v-model="query.username"
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
        <span class="table-toolbar__title">用户列表</span>
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
            <el-icon><Plus /></el-icon> 新增用户
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="头像" width="70" align="center">
          <template #default="{ row }">
            <img v-if="row.avatarUrl" class="cell-img cell-img--sm" :src="row.avatarUrl" @error="onImgError" />
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="140" show-overflow-tooltip />
        <el-table-column label="启用状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="enabledTag(row.enabled)">{{ enabledText(row.enabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="锁定" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.locked ? 'danger' : 'info'">{{ row.locked ? '已锁定' : '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loginTimes" label="登录次数" width="100" align="center" />
        <el-table-column label="最近登录时间" width="170">
          <template #default="{ row }">{{ datetime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRel(row)">用户组</el-button>
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有符合条件的用户" :image-size="80" />
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
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="580px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空表示不修改密码' : '新增必填，入库前自动加密'"
          />
        </el-form-item>
        <el-form-item label="头像">
          <ImageUpload v-model="form.avatarUrl" type="common" tip="建议正方形，≤ 10MB" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="启用">
              <el-switch v-model="form.enabled" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="锁定">
              <el-switch v-model="form.locked" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="账号过期">
              <el-date-picker
                v-model="form.userExpireTime"
                type="datetime"
                :value-format="EXPIRE_FORMAT"
                placeholder="留空表示不过期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="凭证过期">
              <el-date-picker
                v-model="form.credentialExpireTime"
                type="datetime"
                :value-format="EXPIRE_FORMAT"
                placeholder="留空表示不过期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

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

    <!-- 用户组关联 -->
    <el-dialog v-model="relVisible" :title="`用户组 — ${relUser?.username || ''}`" width="560px" destroy-on-close>
      <div class="flex gap-8 mb-8">
        <el-select
          v-model="relPickId"
          placeholder="选择要加入的用户组"
          filterable
          clearable
          style="flex: 1"
        >
          <el-option v-for="g in groupCandidates" :key="g.id" :label="g.name" :value="g.id" />
        </el-select>
        <el-button type="primary" :loading="relSaving" @click="onRelAdd">加入</el-button>
      </div>

      <el-table v-loading="relLoading" :data="relRows" border size="small">
        <el-table-column prop="id" label="关联ID" width="100" />
        <el-table-column label="用户组" min-width="160">
          <template #default="{ row }">{{ groupNames.get(row.groupId) || `#${row.groupId}` }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="onRelRemove(row)">移除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="尚未加入任何用户组" :image-size="60" />
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
