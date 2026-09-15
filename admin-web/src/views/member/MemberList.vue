<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import memberApi from '@/api/member'
import { useCrud, useDialog } from '@/composables/useCrud'
import { enabledTag, enabledText } from '@/utils/dict'
import { datetime } from '@/utils/format'

/**
 * 会员管理（需求 FR-207）。
 *
 * 只读 + 有限编辑：会员是前台自己注册的，后台不提供「新增会员」，也不提供删除，
 * 编辑只开放几个真正需要人工修正的字段。
 *
 * 「重置密码」是**独立的一次动作**，不并进编辑表单：编辑表单是「照原样改几个字段」，
 * 而重置密码是「把凭证换掉」，混在一起容易误提交（见下面 EDIT_FIELDS 的说明）。
 * 后端不另开接口，仍是 `PUT /api/members` 带上 password —— 详细设计 4.5 就是这么定的。
 */
function listWithoutBlankEnabled(params) {
  // 状态下拉的「全部」是空串，但后端是 <if test="enabled != null">，
  // 空串会被拼成 enabled = ''（等价于 0），一条都查不出来，所以请求前先剔掉空值。
  const out = { ...params }
  if (out.enabled === '' || out.enabled === null) delete out.enabled
  return out
}

const pageApi = { ...memberApi, list: (params) => memberApi.list(listWithoutBlankEnabled(params)) }

const {
  rows, total, loading, query, pageNo, size,
  load, search, reset, onPageChange, onSizeChange
} = useCrud({
  api: pageApi,
  pageSize: 10,
  query: { account: '', name: '', phone: '', enabled: '' }
})

/** 后台「编辑会员」允许修改的字段。password 绝不在其中——改密码走上面的「重置密码」动作 */
const EDIT_FIELDS = ['name', 'sex', 'phone', 'email', 'qq', 'wechat', 'enabled', 'description']

const dlg = useDialog(() => ({ name: '', sex: '', phone: '', email: '', qq: '', wechat: '', enabled: true, description: '' }))
const { visible, submitting, form } = dlg
const formRef = ref()

const detailVisible = ref(false)
const detail = ref({})
/** 只用于编辑弹窗里回显账号（不可改，也不该出现在提交体里） */
const editAccount = ref('')

const rules = {
  name: [{ max: 50, message: '不超过 50 个字符', trigger: 'blur' }],
  phone: [{ max: 20, message: '不超过 20 个字符', trigger: 'blur' }],
  email: [{ max: 100, message: '不超过 100 个字符', trigger: 'blur' }],
  qq: [{ max: 20, message: '不超过 20 个字符', trigger: 'blur' }],
  wechat: [{ max: 50, message: '不超过 50 个字符', trigger: 'blur' }]
}

function onDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function onEdit(row) {
  dlg.open(row)
  // useDialog 的 open 会浅拷贝整行，password 也在里面；这里重建表单对象，只留可改字段
  const picked = { id: row.id }
  EDIT_FIELDS.forEach((k) => (picked[k] = row[k]))
  form.value = picked
  editAccount.value = row.account
  formRef.value?.clearValidate()
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    // form 里只有 EDIT_FIELDS + id，password 不会被带上去（不传即不改密码）
    await memberApi.update(form.value)
    ElMessage.success('会员信息已更新')
    visible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

// ---------------- 重置密码（FR-207） ----------------

const resetVisible = ref(false)
const resetting = ref(false)
const resetFormRef = ref()
const resetForm = ref({ id: null, account: '', password: '', confirmPassword: '' })

const resetRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    // 与前台注册页保持一致（Register.vue 是 6-32 位）
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== resetForm.value.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

function onResetPassword(row) {
  resetForm.value = { id: row.id, account: row.account, password: '', confirmPassword: '' }
  resetVisible.value = true
  resetFormRef.value?.clearValidate()
}

async function onSubmitReset() {
  const ok = await resetFormRef.value.validate().catch(() => false)
  if (!ok) return

  const confirmed = await ElMessageBox.confirm(
    `确定要把会员「${resetForm.value.account}」的密码重置掉吗？\n` +
      '重置后原密码立即失效，会员必须用新密码登录。',
    '重置密码',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return

  resetting.value = true
  try {
    // 只提交 id + password：update 是条件更新，其余列不传就不会被改动
    await memberApi.update({ id: resetForm.value.id, password: resetForm.value.password })
    ElMessage.success('密码已重置，该会员的登录失败锁定也已解除')
    resetVisible.value = false
  } finally {
    resetting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 查询 -->
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="会员账号">
          <el-input
            v-model="query.account"
            placeholder="支持模糊匹配"
            clearable
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input
            v-model="query.name"
            placeholder="支持模糊匹配"
            clearable
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="query.phone"
            placeholder="支持模糊匹配"
            clearable
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.enabled" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
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
        <span class="table-toolbar__title">会员列表</span>
        <div class="table-toolbar__actions">
          <el-button @click="load">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="account" label="会员账号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="sex" label="性别" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.sex">{{ row.sex }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="enabledTag(row.enabled)" effect="light">{{ enabledText(row.enabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录时间" width="160">
          <template #default="{ row }">{{ datetime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="onDetail(row)">详情</el-button>
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onResetPassword(row)">重置密码</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有符合条件的会员" :image-size="80" />
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

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="会员详情" width="760px" destroy-on-close>
      <!-- 会员实体有三十多个字段，这里只挑后台常用的展示 -->
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="会员账号">{{ detail.account || '—' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detail.name || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="enabledTag(detail.enabled)" effect="light">{{ enabledText(detail.enabled) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="性别">{{ detail.sex || '—' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ detail.birthday || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
        <el-descriptions-item label="QQ">{{ detail.qq || '—' }}</el-descriptions-item>
        <el-descriptions-item label="微信">{{ detail.wechat || '—' }}</el-descriptions-item>
        <el-descriptions-item label="身高(cm)">{{ detail.height ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="体重(kg)">{{ detail.weight ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="最后登录时间">{{ datetime(detail.lastLoginTime) }}</el-descriptions-item>
        <el-descriptions-item label="最后登录 IP">{{ detail.lastLoginIp || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.description || '—' }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑 -->
    <el-dialog v-model="visible" title="编辑会员" width="580px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="会员账号">
          <!-- 账号是登录凭据，不允许后台改 -->
          <el-input :model-value="editAccount" disabled />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="sex">
              <!-- sex 是自由文本列，allow-create 兼容库里已有的非 男/女 取值 -->
              <el-select
                v-model="form.sex"
                placeholder="未填写"
                clearable
                filterable
                allow-create
                default-first-option
                style="width: 100%"
              >
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="11 位手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="name@example.com" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="QQ" prop="qq">
              <el-input v-model="form.qq" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="微信" prop="wechat">
              <el-input v-model="form.wechat" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>

        <el-form-item label="备注">
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

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置会员密码" width="460px" destroy-on-close>
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        class="mb-16"
        title="重置后原密码立即失效"
        description="请把新密码告知会员，并提醒其登录后自行修改。已签发的登录令牌在有效期内仍然可用。"
      />
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="90px">
        <el-form-item label="会员账号">
          <el-input :model-value="resetForm.account" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="resetForm.password"
            type="password"
            placeholder="6-32 位"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="resetForm.confirmPassword"
            type="password"
            placeholder="再输一遍"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="resetVisible = false">取消</el-button>
          <el-button type="primary" :loading="resetting" @click="onSubmitReset">确认重置</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
