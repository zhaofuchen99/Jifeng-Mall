<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import AuthLayout from '@/layout/AuthLayout.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  account: '',
  password: ''
})

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  loading.value = true
  try {
    await userStore.doLogin({ ...form })
    ElMessage.success('登录成功')
    // 游客被拦截时带了 redirect，登录后回跳原页面（需求 5.2）
    router.replace(route.query.redirect || { name: 'home' })
  } catch {
    // 错误提示已由 axios 响应拦截器统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <div class="tabs">
      <span class="is-active">登录</span>
      <router-link :to="{ name: 'register', query: route.query }">注册</router-link>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      size="large"
      label-position="top"
      @keyup.enter="onSubmit"
    >
      <el-form-item label="账号" prop="account">
        <el-input v-model="form.account" placeholder="请输入账号" clearable>
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password>
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>
      </el-form-item>

      <el-button type="primary" size="large" class="submit" :loading="loading" @click="onSubmit">
        登 录
      </el-button>
    </el-form>

    <div class="hint">
      <p>演示账号：<code>member / 123456</code></p>
      <p class="text-muted">
        还没有账号？<router-link :to="{ name: 'register' }">立即注册</router-link>
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
  color: var(--text-2);
}

.hint p {
  margin: 6px 0;
}

.hint code {
  background: var(--bg-page);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--brand);
}

.hint a {
  color: var(--brand);
}
</style>
