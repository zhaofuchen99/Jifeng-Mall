<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="nf">
    <div class="nf__code">404</div>
    <p class="nf__text">页面不存在或已被移除</p>
    <el-button type="primary" @click="router.replace({ name: 'dashboard' })">返回工作台</el-button>
  </div>
</template>

<style scoped>
.nf {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.nf__code {
  font-size: 88px;
  font-weight: 700;
  line-height: 1;
  color: var(--brand);
}

.nf__text {
  color: var(--text-3);
  margin: 0 0 8px;
}
</style>
