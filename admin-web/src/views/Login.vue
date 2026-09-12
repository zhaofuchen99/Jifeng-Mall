<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMenuStore } from '@/stores/menu'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const menuStore = useMenuStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  account: '',
  password: ''
})

const rules = {
  account: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  loading.value = true
  try {
    await userStore.doLogin({ ...form })
    // 换了账号，菜单要重新拉（不同角色看到的菜单不一样）
    menuStore.reset()
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || { name: 'dashboard' })
  } catch (e) {
    // 会员账号被前端拦下时抛出的是普通 Error，拦截器不会弹提示，这里补上
    if (e?.code === undefined) ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <div class="login__panel">
      <div class="login__brand">
        <span class="login__mark">极</span>
        <div>
          <h1>极锋商城</h1>
          <p>Jifeng Mall · 管理后台</p>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
        <el-form-item prop="account">
          <el-input v-model="form.account" placeholder="用户名" clearable>
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-button type="primary" size="large" class="login__submit" :loading="loading" @click="onSubmit">
          登 录
        </el-button>
      </el-form>

      <div class="login__hint">
        <p>演示账号：<code>admin / 123456</code></p>
        <p class="text-muted">
          <code>operator / 123456</code> 未分配任何角色，登录后菜单为空
        </p>
      </div>
    </div>

    <div class="login__footer">
      基于 Spring Cloud 的 B2C 微服务秒杀商城 · 演示项目
    </div>
  </div>
</template>

<style scoped>
.login {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(140deg, #1f2733 0%, #22303f 50%, #1b3556 100%);
}

.login__panel {
  width: 380px;
  background: #fff;
  border-radius: 10px;
  padding: 36px 36px 28px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.login__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.login__mark {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--brand), #6f9bf2);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.login__brand h1 {
  font-size: 20px;
  line-height: 1.2;
}

.login__brand p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-3);
}

.login__submit {
  width: 100%;
  letter-spacing: 4px;
  margin-top: 4px;
}

.login__hint {
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
  font-size: 12px;
  color: var(--text-2);
}

.login__hint p {
  margin: 4px 0;
}

.login__hint code {
  background: var(--bg-page);
  padding: 1px 6px;
  border-radius: 4px;
  color: var(--brand);
}

.login__footer {
  margin-top: 24px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}
</style>
