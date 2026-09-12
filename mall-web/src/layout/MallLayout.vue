<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

const keyword = ref(route.query.name || '')

// 从搜索页跳走再回来时，输入框要跟着 URL 走
watch(
  () => route.query.name,
  (v) => {
    if (route.name === 'goods') keyword.value = v || ''
  }
)

onMounted(() => {
  // 刷新页面后顶栏角标要能恢复
  cartStore.refresh()
})

// 登录态变化时同步购物车（登录后立刻出现角标，退出后清空）
watch(
  () => userStore.isLoggedIn,
  () => cartStore.refresh()
)

function onSearch() {
  const name = keyword.value.trim()
  router.push({ name: 'goods', query: name ? { name } : {} })
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        userStore.logout()
        cartStore.reset()
        ElMessage.success('已退出登录')
        router.push({ name: 'home' })
      })
      .catch(() => {})
    return
  }
  router.push({ name: cmd })
}
</script>

<template>
  <div class="mall">
    <!-- 顶栏 -->
    <header class="header">
      <div class="header__top">
        <div class="container header__top-inner">
          <span v-if="userStore.isLoggedIn">你好，{{ userStore.displayName }}</span>
          <span v-else>欢迎来到 极锋商城</span>
          <div class="header__links">
            <router-link v-if="userStore.isLoggedIn" :to="{ name: 'userOrders' }">我的订单</router-link>
            <router-link v-else :to="{ name: 'login' }">登录</router-link>
            <span class="sep">|</span>
            <router-link v-if="!userStore.isLoggedIn" :to="{ name: 'register' }">免费注册</router-link>
            <router-link v-else :to="{ name: 'userProfile' }">个人中心</router-link>
          </div>
        </div>
      </div>

      <div class="header__main">
        <div class="container header__main-inner">
          <router-link :to="{ name: 'home' }" class="logo">
            <span class="logo__mark">极</span>
            <span class="logo__text">极锋<em>商城</em></span>
          </router-link>

          <div class="search">
            <el-input
              v-model="keyword"
              placeholder="搜索商品名称"
              size="large"
              clearable
              @keyup.enter="onSearch"
            >
              <template #append>
                <el-button :icon="'Search'" @click="onSearch">搜索</el-button>
              </template>
            </el-input>
          </div>

          <div class="header__actions">
            <router-link :to="{ name: 'cart' }" class="cart-entry">
              <el-badge :value="cartStore.totalQty" :hidden="cartStore.totalQty === 0" :max="99">
                <el-icon :size="22"><ShoppingCart /></el-icon>
              </el-badge>
              <span>购物车</span>
            </router-link>

            <el-dropdown v-if="userStore.isLoggedIn" @command="onCommand">
              <span class="user-entry">
                <el-icon><User /></el-icon>
                {{ userStore.displayName }}
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="userProfile">个人信息</el-dropdown-item>
                  <el-dropdown-item command="userAddress">收货地址</el-dropdown-item>
                  <el-dropdown-item command="userOrders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="userPassword">修改密码</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>

      <nav class="nav">
        <div class="container nav__inner">
          <router-link :to="{ name: 'home' }" class="nav__item" active-class="is-active" exact-active-class="is-active">
            首页
          </router-link>
          <router-link :to="{ name: 'goods' }" class="nav__item" active-class="is-active">全部商品</router-link>
          <router-link :to="{ name: 'seckill' }" class="nav__item nav__item--hot" active-class="is-active">
            <el-icon><Timer /></el-icon>
            秒杀会场
          </router-link>
          <router-link :to="{ name: 'userOrders' }" class="nav__item" active-class="is-active">我的订单</router-link>
        </div>
      </nav>
    </header>

    <!-- 主内容 -->
    <main class="main">
      <router-view />
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="container">
        <div class="footer__cols">
          <div class="footer__col">
            <h4>购物指南</h4>
            <router-link :to="{ name: 'register' }">会员注册</router-link>
            <router-link :to="{ name: 'help', hash: '#flow' }">购物流程</router-link>
            <router-link :to="{ name: 'help', hash: '#faq' }">常见问题</router-link>
          </div>
          <div class="footer__col">
            <h4>配送方式</h4>
            <router-link :to="{ name: 'help', hash: '#delivery-area' }">配送范围</router-link>
            <router-link :to="{ name: 'help', hash: '#delivery-time' }">配送时效</router-link>
            <router-link :to="{ name: 'help', hash: '#delivery-fee' }">运费说明</router-link>
          </div>
          <div class="footer__col">
            <h4>支付方式</h4>
            <router-link :to="{ name: 'help', hash: '#pay' }">模拟支付</router-link>
            <router-link :to="{ name: 'help', hash: '#pay' }">支付说明</router-link>
          </div>
          <div class="footer__col">
            <h4>售后服务</h4>
            <router-link :to="{ name: 'help', hash: '#refund' }">退换货政策</router-link>
            <router-link :to="{ name: 'help', hash: '#cancel' }">取消订单</router-link>
          </div>
          <div class="footer__brand">
            <div class="footer__brand-name">极锋商城</div>
            <p class="text-muted">基于 Spring Cloud 的 B2C 微服务秒杀商城</p>
            <p class="text-muted">演示项目 · 支付为模拟支付，不产生真实交易</p>
          </div>
        </div>
        <div class="footer__copy">© 2026 极锋商城 Jifeng Mall · 仅用于教学演示</div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.mall {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

/* ---------- 顶栏 ---------- */
.header {
  background: #fff;
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header__top {
  background: #2b2f36;
  color: #c9cdd4;
  font-size: 12px;
  line-height: 32px;
}

.header__top-inner {
  display: flex;
  justify-content: space-between;
}

.header__links a:hover {
  color: #fff;
}

.header__links .sep {
  margin: 0 8px;
  color: #4e5969;
}

.header__main-inner {
  display: flex;
  align-items: center;
  gap: 32px;
  height: 88px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.logo__mark {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--accent), var(--brand));
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  display: grid;
  place-items: center;
}

.logo__text {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.logo__text em {
  font-style: normal;
  color: var(--brand);
}

.search {
  flex: 1;
  max-width: 520px;
}

.header__actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-left: auto;
}

.cart-entry {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-2);
}

.cart-entry:hover {
  color: var(--brand);
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: var(--text-2);
  outline: none;
}

.user-entry:hover {
  color: var(--brand);
}

.nav {
  border-top: 1px solid var(--line);
}

.nav__inner {
  display: flex;
  gap: 4px;
  height: 46px;
  align-items: center;
}

.nav__item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 18px;
  height: 46px;
  line-height: 46px;
  font-size: 15px;
  color: var(--text-2);
  border-bottom: 2px solid transparent;
}

.nav__item:hover {
  color: var(--brand);
}

.nav__item.is-active {
  color: var(--brand);
  border-bottom-color: var(--brand);
  font-weight: 600;
}

.nav__item--hot {
  color: var(--brand);
}

/* ---------- 主体 ---------- */
.main {
  flex: 1;
}

/* ---------- 页脚 ---------- */
.footer {
  background: #2b2f36;
  color: #c9cdd4;
  padding: 40px 0 20px;
  margin-top: 40px;
}

.footer__cols {
  display: grid;
  grid-template-columns: repeat(4, 1fr) 1.6fr;
  gap: 24px;
}

.footer__col h4 {
  color: #fff;
  font-size: 14px;
  margin-bottom: 12px;
}

.footer__col a {
  display: block;
  width: fit-content;
  margin: 6px 0;
  font-size: 13px;
  color: #c9cdd4;
  transition: color 0.15s;
}

.footer__col a:hover {
  color: #fff;
}

.footer__brand-name {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}

.footer__brand .text-muted {
  color: #86909c;
  font-size: 13px;
  margin: 4px 0;
}

.footer__copy {
  margin-top: 28px;
  padding-top: 16px;
  border-top: 1px solid #3a3f47;
  text-align: center;
  font-size: 12px;
  color: #86909c;
}
</style>
