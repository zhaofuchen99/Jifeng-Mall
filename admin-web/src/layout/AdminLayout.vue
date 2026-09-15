<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMenuStore } from '@/stores/menu'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const menuStore = useMenuStore()

const collapsed = ref(false)

/**
 * 菜单表里的 icon 是 element-ui 时代的名字（el-icon-house 这种），
 * Element Plus 的图标是组件名，映射一下。
 */
const ICON_MAP = {
  'el-icon-house': 'HomeFilled',
  'el-icon-goods': 'Goods',
  'el-icon-document': 'Document',
  'el-icon-user': 'User',
  'el-icon-timer': 'Timer',
  'el-icon-setting': 'Setting',
  'el-icon-location': 'Location'
}

function iconOf(name, fallback = 'Menu') {
  if (!name) return fallback
  return ICON_MAP[name] || name
}

/** 当前展开的父级菜单：/goods/brand → /goods */
const activeMenu = computed(() => route.path)

const openedMenus = ref([])

const breadcrumbs = computed(() =>
  route.matched.filter((r) => r.meta?.title).map((r) => r.meta.title)
)

onMounted(async () => {
  await menuStore.load()
  // 默认把所有分组展开，后台页面不多，折叠反而多点一次
  openedMenus.value = menuStore.menus.filter((m) => m.children?.length).map((m) => m.url)
})

function onCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        userStore.logout()
        menuStore.reset()
        ElMessage.success('已退出登录')
        router.push({ name: 'login' })
      })
      .catch(() => {})
    return
  }
  router.push({ name: cmd })
}

function refreshMenus() {
  menuStore.load().then(() => ElMessage.success('菜单已刷新'))
}

/** 模板作用域里没有 window，必须包成方法 */
function openMall() {
  window.open('http://localhost:5173', '_blank')
}
</script>

<template>
  <div class="admin">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ 'is-collapsed': collapsed }">
      <div class="sidebar__logo">
        <span class="sidebar__mark">极</span>
        <span v-show="!collapsed" class="sidebar__name">极锋商城<em>管理后台</em></span>
      </div>

      <el-scrollbar class="sidebar__scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="transparent"
          text-color="#b6bdc9"
          active-text-color="#fff"
          unique-opened
          router
        >
          <!-- 动态菜单：来自 /api/menus/mine -->
          <template v-for="m in menuStore.menus" :key="m.id">
            <!-- 有子页面的栏目渲染成子菜单 -->
            <el-sub-menu v-if="m.children?.length" :index="m.url">
              <template #title>
                <el-icon><component :is="iconOf(m.icon)" /></el-icon>
                <span>{{ m.name }}</span>
              </template>
              <el-menu-item v-for="c in m.children" :key="c.path" :index="c.path">
                <el-icon><component :is="c.icon" /></el-icon>
                <span>{{ c.title }}</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 单页菜单直接跳 -->
            <el-menu-item v-else :index="m.url">
              <el-icon><component :is="iconOf(m.icon)" /></el-icon>
              <template #title>{{ m.name }}</template>
            </el-menu-item>
          </template>
        </el-menu>

        <!-- operator 这类没授权的账号会拿到空菜单，给个明确提示 -->
        <div v-if="menuStore.loaded && menuStore.menus.length === 0" class="sidebar__empty">
          <el-icon><InfoFilled /></el-icon>
          <p>当前账号没有分配任何菜单</p>
          <p class="text-muted">需要管理员在「系统管理」里授权</p>
        </div>
      </el-scrollbar>
    </aside>

    <!-- 右侧主体 -->
    <div class="main">
      <header class="header">
        <el-icon class="header__toggle" @click="collapsed = !collapsed">
          <component :is="collapsed ? 'Expand' : 'Fold'" />
        </el-icon>

        <el-breadcrumb class="header__crumb" separator="/">
          <el-breadcrumb-item v-for="(t, i) in breadcrumbs" :key="i">{{ t }}</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="header__right">
          <el-tooltip content="刷新菜单" placement="bottom">
            <el-icon class="header__icon" @click="refreshMenus"><Refresh /></el-icon>
          </el-tooltip>
          <el-tooltip content="前台商城" placement="bottom">
            <el-icon class="header__icon" @click="openMall"><Monitor /></el-icon>
          </el-tooltip>

          <el-dropdown @command="onCommand">
            <span class="header__user">
              <el-avatar :size="28">{{ (userStore.displayName || 'A').slice(0, 1) }}</el-avatar>
              <span>{{ userStore.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin {
  display: flex;
  height: 100%;
}

/* ---------- 侧边栏 ---------- */
.sidebar {
  width: var(--sidebar-width);
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.2s;
}

.sidebar.is-collapsed {
  width: 64px;
}

.sidebar__logo {
  height: var(--header-height);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
  overflow: hidden;
  white-space: nowrap;
}

.sidebar__mark {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--brand), #6f9bf2);
  color: #fff;
  font-weight: 700;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.sidebar__name {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.sidebar__name em {
  font-style: normal;
  font-size: 11px;
  font-weight: 400;
  color: var(--sidebar-text);
}

.sidebar__scroll {
  flex: 1;
}

.sidebar :deep(.el-menu) {
  border-right: none;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: var(--sidebar-active) !important;
}

.sidebar :deep(.el-menu-item:hover),
.sidebar :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
}

.sidebar__empty {
  padding: 32px 20px;
  color: var(--sidebar-text);
  text-align: center;
  font-size: 13px;
}

.sidebar__empty .el-icon {
  font-size: 26px;
  margin-bottom: 8px;
}

.sidebar__empty p {
  margin: 4px 0;
}

/* ---------- 右侧 ---------- */
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header {
  height: var(--header-height);
  background: #fff;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 16px;
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
  z-index: 10;
}

.header__toggle {
  font-size: 18px;
  cursor: pointer;
  color: var(--text-2);
}

.header__toggle:hover {
  color: var(--brand);
}

.header__crumb {
  flex: 1;
}

.header__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header__icon {
  font-size: 17px;
  cursor: pointer;
  color: var(--text-2);
}

.header__icon:hover {
  color: var(--brand);
}

.header__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text-1);
  outline: none;
}

.content {
  flex: 1;
  overflow: auto;
}
</style>
