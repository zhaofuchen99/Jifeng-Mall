<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login as memberLogin, updateMember } from '@/api/member'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const saving = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/** 密码强度：长度 + 是否含字母和数字 */
const strength = computed(() => {
  const p = form.newPassword
  if (!p) return { level: 0, text: '', color: '' }
  let score = 0
  if (p.length >= 6) score++
  if (p.length >= 10) score++
  if (/[A-Za-z]/.test(p) && /\d/.test(p)) score++
  if (/[^A-Za-z0-9]/.test(p)) score++
  const map = [
    { level: 1, text: '弱', color: '#f56c6c' },
    { level: 1, text: '弱', color: '#f56c6c' },
    { level: 2, text: '中', color: '#e6a23c' },
    { level: 3, text: '强', color: '#67c23a' },
    { level: 3, text: '很强', color: '#67c23a' }
  ]
  return map[Math.min(score, 4)]
})

const rules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  if (form.newPassword === form.oldPassword) {
    ElMessage.warning('新密码不能与当前密码相同')
    return
  }

  saving.value = true
  try {
    // 1) 用当前密码走一次登录，验证旧密码是否正确。
    //    后端没有「校验密码」的独立接口，而登录本身就是最可靠的校验（BCrypt 比对在服务端做）。
    await memberLogin({ account: userStore.account, password: form.oldPassword })

    // 2) 写入新密码。PUT /api/members 只更新传了值的字段，所以这里只带 id + password。
    await updateMember({ id: userStore.memberId, password: form.newPassword })

    ElMessage.success('密码修改成功，请用新密码重新登录')
    userStore.logout()
    router.replace({ name: 'login' })
  } catch {
    // 「账号或密码错误」说明旧密码填错了，拦截器已提示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="panel">
    <h4 class="mb-16">修改密码</h4>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="form">
      <el-form-item label="当前密码" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入当前密码" />
      </el-form-item>

      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password placeholder="6-32 位" />
        <div v-if="strength.text" class="strength">
          密码强度：<b :style="{ color: strength.color }">{{ strength.text }}</b>
        </div>
      </el-form-item>

      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="onSubmit">确认修改</el-button>
        <el-button @click="formRef.resetFields()">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="修改成功后需要重新登录"
      description="当前登录令牌会立即失效，请用新密码重新登录。"
    />
  </div>
</template>

<style scoped>
.form {
  max-width: 460px;
}

.strength {
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.8;
}
</style>
