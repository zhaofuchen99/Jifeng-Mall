<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { updateMember } from '@/api/member'
import { uploadFile } from '@/api/upload'
import { useUserStore } from '@/stores/user'
import { date } from '@/utils/format'

const props = defineProps({
  member: { type: Object, default: null }
})
const emit = defineEmits(['refresh'])

const userStore = useUserStore()

const formRef = ref()
const saving = ref(false)
const uploading = ref(false)
const editing = ref(false)

const form = reactive({
  id: null,
  account: '',
  name: '',
  sex: '',
  birthday: '',
  phone: '',
  email: '',
  qq: '',
  wechat: '',
  portrait: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const avatarText = computed(() => (form.name || form.account || 'U').slice(0, 1))

function fill(m) {
  if (!m) return
  form.id = m.id
  form.account = m.account
  form.name = m.name || ''
  form.sex = m.sex || ''
  form.birthday = m.birthday ? date(m.birthday) : ''
  form.phone = m.phone || ''
  form.email = m.email || ''
  form.qq = m.qq || ''
  form.wechat = m.wechat || ''
  form.portrait = m.portrait || ''
  form.description = m.description || ''
}

watch(() => props.member, fill, { immediate: true })

function startEdit() {
  editing.value = true
}

function cancelEdit() {
  fill(props.member)
  editing.value = false
  formRef.value?.clearValidate()
}

/** 头像上传：上传成功后立刻把 URL 写回表单，用户再点保存 */
async function onAvatarChange(uploadFileObj) {
  const raw = uploadFileObj.raw
  if (!raw) return
  if (raw.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 10MB')
    return
  }
  uploading.value = true
  try {
    form.portrait = await uploadFile(raw, 'member')
    // 头像单独存一次，避免用户以为已经保存了
    await save({ silent: true })
    ElMessage.success('头像已更新')
  } finally {
    uploading.value = false
  }
}

async function save({ silent = false } = {}) {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  saving.value = true
  try {
    // 注意不要把 password 一起回传——改了才传，见 ChangePassword.vue
    await updateMember({
      id: form.id,
      name: form.name,
      sex: form.sex || null,
      birthday: form.birthday || null,
      phone: form.phone,
      email: form.email,
      qq: form.qq,
      wechat: form.wechat,
      portrait: form.portrait,
      description: form.description
    })
    if (!silent) ElMessage.success('保存成功')
    editing.value = false
    userStore.patchLoginUser({ name: form.name })
    emit('refresh')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="panel">
    <div class="flex-between mb-16">
      <h4>个人信息</h4>
      <div>
        <template v-if="editing">
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save()">保存</el-button>
        </template>
        <el-button v-else type="primary" plain @click="startEdit">
          <el-icon><Edit /></el-icon> 编辑资料
        </el-button>
      </div>
    </div>

    <!-- 头像 -->
    <div class="avatar-row">
      <el-upload
        :show-file-list="false"
        :auto-upload="false"
        accept="image/*"
        :on-change="onAvatarChange"
      >
        <div v-loading="uploading" class="avatar-box">
          <el-avatar :size="88" :src="form.portrait || ''">{{ avatarText }}</el-avatar>
          <div class="avatar-mask"><el-icon><Camera /></el-icon></div>
        </div>
      </el-upload>
      <div class="text-muted">
        <p>点击头像更换（jpg / png / gif / webp，≤ 10MB）</p>
        <p>上传后会立刻保存，无需再点「保存」</p>
      </div>
    </div>

    <!-- 资料表单 -->
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="mt-16">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="账号">
            <el-input :model-value="form.account" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" :disabled="!editing" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="性别">
            <el-select v-model="form.sex" :disabled="!editing" placeholder="请选择" clearable>
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
              <el-option label="保密" value="保密" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="生日">
            <el-date-picker
              v-model="form.birthday"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              :disabled="!editing"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" :disabled="!editing" maxlength="11" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" :disabled="!editing" />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="QQ">
            <el-input v-model="form.qq" :disabled="!editing" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="微信">
            <el-input v-model="form.wechat" :disabled="!editing" />
          </el-form-item>
        </el-col>

        <el-col :span="24">
          <el-form-item label="个人简介">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="2"
              :disabled="!editing"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<style scoped>
.avatar-row {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px;
  background: #fafbfc;
  border-radius: var(--radius);
}

.avatar-row p {
  margin: 2px 0;
  font-size: 12px;
}

.avatar-box {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 22px;
  opacity: 0;
  transition: opacity 0.15s;
}

.avatar-box:hover .avatar-mask {
  opacity: 1;
}
</style>
