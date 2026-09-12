<script setup>
defineProps({
  text: { type: String, default: '暂无数据' },
  icon: { type: String, default: 'Box' }
})
</script>

<template>
  <div class="empty">
    <el-icon class="empty__icon"><component :is="icon" /></el-icon>
    <p>{{ text }}</p>
    <slot />
  </div>
</template>
