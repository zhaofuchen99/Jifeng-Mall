<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import AuthLayout from '@/layout/AuthLayout.vue'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  account: '',
  name: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]{4,20}$/,
      message: '4-20 位，仅限字母、数字、下划线',
      trigger: 'blur'
    }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  loading.value = true
  try {
    // 注册成功后端直接签发会员令牌，这里就等于自动登录（需求 5.2）
    await userStore.doRegister({
      account: form.account,
      password: form.password,
      name: form.name,
      phone: form.phone
    })
    ElMessage.success('注册成功，已自动登录')
    router.replace({ name: 'home' })
  } catch {
    // 账号已存在等错误由拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout slogan="注册新账号&#10;开启购物之旅">
    <div class="tabs">
      <router-link :to="{ name: 'login' }">登录</router-link>
      <span class="is-active">注册</span>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" size="large" label-position="top">
      <el-form-item label="账号" prop="account">
        <el-input v-model="form.account" placeholder="字母、数字或下划线" clearable>
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
      </el-form-item>

      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" placeholder="请输入姓名" clearable>
          <template #prefix><el-icon><Postcard /></el-icon></template>
        </el-input>
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" clearable maxlength="11">
          <template #prefix><el-icon><Iphone /></el-icon></template>
        </el-input>
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="6-32 位" show-password>
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password>
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>
      </el-form-item>

      <el-button type="primary" size="large" class="submit" :loading="loading" @click="onSubmit">
        注 册
      </el-button>
    </el-form>

    <div class="hint">
      <p class="text-muted">
        已有账号？<router-link :to="{ name: 'login' }">直接登录</router-link>
      </p>
      <p class="text-muted"><router-link :to="{ name: 'home' }">← 返回商城首页</router-link></p>
    </div>
  </AuthLayout>
</template>

<style scoped>
.tabs {
  display: flex;
  gap: 24px;
  margin-bottom: 32px;
  font-size: 22px;
}

.tabs .is-active {
  font-weight: 700;
  color: var(--text-1);
}

.tabs a {
  color: var(--text-3);
}

.tabs a:hover {
  color: var(--brand);
}

.submit {
  width: 100%;
  margin-top: 8px;
  letter-spacing: 4px;
}

.hint {
  margin-top: 24px;
  font-size: 13px;
}

.hint p {
  margin: 6px 0;
}

.hint a {
  color: var(--brand);
}
</style>
