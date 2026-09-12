<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useRegionStore } from '@/stores/region'

const route = useRoute()
const userStore = useUserStore()
const regionStore = useRegionStore()

const member = ref(null)

const MENUS = [
  { name: 'userProfile', label: '个人信息', icon: 'User' },
  { name: 'userAddress', label: '收货地址', icon: 'Location' },
  { name: 'userOrders', label: '我的订单', icon: 'List' },
  { name: 'userPassword', label: '修改密码', icon: 'Lock' }
]

/** 子页在「我的订单」下时，侧栏该项要保持高亮 */
const activeMenu = computed(() => {
  if (route.name === 'userOrderDetail') return 'userOrders'
  return route.name
})

onMounted(async () => {
  regionStore.ensureLoaded()
  member.value = await userStore.fetchProfile()
})
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="layout">
        <!-- 侧栏 -->
        <aside class="side">
          <div class="side__user">
            <el-avatar :size="56" :src="member?.portrait || ''">
              {{ (userStore.displayName || 'U').slice(0, 1) }}
            </el-avatar>
            <div class="side__user-info">
              <b>{{ member?.name || userStore.displayName }}</b>
              <span class="text-muted">{{ userStore.account }}</span>
            </div>
          </div>

          <nav class="side__nav">
            <router-link
              v-for="m in MENUS"
              :key="m.name"
              :to="{ name: m.name }"
              class="side__item"
              :class="{ 'is-active': activeMenu === m.name }"
            >
              <el-icon><component :is="m.icon" /></el-icon>
              {{ m.label }}
            </router-link>
          </nav>
        </aside>

        <!-- 子页 -->
        <section class="content">
          <router-view :member="member" @refresh="() => userStore.fetchProfile().then((m) => (member = m))" />
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 16px;
  align-items: start;
}

.side {
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.side__user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  background: linear-gradient(120deg, #fff5f5, #fff);
  border-bottom: 1px solid var(--line);
}

.side__user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.side__user-info b {
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side__user-info span {
  font-size: 12px;
}

.side__nav {
  padding: 8px 0;
}

.side__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  color: var(--text-2);
  font-size: 14px;
  border-left: 3px solid transparent;
}

.side__item:hover {
  background: var(--bg-page);
  color: var(--brand);
}

.side__item.is-active {
  background: var(--brand-bg);
  color: var(--brand);
  border-left-color: var(--brand);
  font-weight: 600;
}

.content {
  min-width: 0;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
