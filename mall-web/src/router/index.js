import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
import MallLayout from '@/layout/MallLayout.vue'

const routes = [
  {
    path: '/',
    component: MallLayout,
    children: [
      { path: '', name: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
      { path: 'goods', name: 'goods', component: () => import('@/views/goods/GoodList.vue'), meta: { title: '商品列表' } },
      { path: 'goods/:id', name: 'goodDetail', component: () => import('@/views/goods/GoodDetail.vue'), meta: { title: '商品详情' } },
      { path: 'seckill', name: 'seckill', component: () => import('@/views/seckill/SeckillFloor.vue'), meta: { title: '秒杀会场' } },
      { path: 'help', name: 'help', component: () => import('@/views/Help.vue'), meta: { title: '帮助中心' } },
      {
        path: 'cart',
        name: 'cart',
        component: () => import('@/views/cart/Cart.vue'),
        meta: { title: '购物车', requiresAuth: true }
      },
      {
        path: 'order/confirm',
        name: 'orderConfirm',
        component: () => import('@/views/order/OrderConfirm.vue'),
        meta: { title: '确认订单', requiresAuth: true }
      },
      {
        path: 'pay/:id',
        name: 'pay',
        component: () => import('@/views/order/Pay.vue'),
        meta: { title: '收银台', requiresAuth: true }
      },
      {
        path: 'pay/result/:id',
        name: 'payResult',
        component: () => import('@/views/order/PayResult.vue'),
        meta: { title: '支付结果', requiresAuth: true }
      },
      {
        path: 'user',
        component: () => import('@/views/user/UserCenter.vue'),
        meta: { requiresAuth: true },
        children: [
          { path: '', redirect: { name: 'userProfile' } },
          { path: 'profile', name: 'userProfile', component: () => import('@/views/user/Profile.vue'), meta: { title: '个人信息' } },
          { path: 'address', name: 'userAddress', component: () => import('@/views/user/AddressList.vue'), meta: { title: '收货地址' } },
          { path: 'orders', name: 'userOrders', component: () => import('@/views/user/MyOrders.vue'), meta: { title: '我的订单' } },
          { path: 'orders/:id', name: 'userOrderDetail', component: () => import('@/views/user/OrderDetail.vue'), meta: { title: '订单详情' } },
          { path: 'password', name: 'userPassword', component: () => import('@/views/user/ChangePassword.vue'), meta: { title: '修改密码' } }
        ]
      }
    ]
  },
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { title: '会员登录' } },
  { path: '/register', name: 'register', component: () => import('@/views/Register.vue'), meta: { title: '会员注册' } },
  { path: '/:pathMatch(.*)*', name: 'notFound', component: () => import('@/views/NotFound.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 带 #锚点 时滚到对应小节（帮助中心的各栏目靠它定位），否则回到页首。
  // Help.vue 是懒加载的，首次跳过去时目标元素可能还没渲染，所以要重试几次再放弃。
  scrollBehavior: (to) => {
    if (!to.hash) return { top: 0 }
    return new Promise((resolve) => {
      let tries = 0
      const attempt = () => {
        const el = document.querySelector(to.hash)
        if (el) {
          resolve({ el: to.hash, behavior: 'smooth', top: 80 })
        } else if (tries++ > 20) {
          resolve({ top: 0 })
        } else {
          setTimeout(attempt, 50)
        }
      }
      attempt()
    })
  }
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 极锋商城` : '极锋商城'

  if (to.meta.requiresAuth && !getToken()) {
    ElMessage.warning('请先登录')
    // 带上来路，登录后回跳（需求 5.2）
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
