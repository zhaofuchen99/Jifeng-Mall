import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
import AdminLayout from '@/layout/AdminLayout.vue'

/**
 * 路由与后端菜单表的 url 对齐（/dashboard、/goods、/orders、/members、/seckills、/system），
 * 这样侧边栏拿到菜单就能直接跳，不用再做一层映射。
 * 栏目地址本身重定向到它下面的第一个页面。
 */
const routes = [
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'HomeFilled' }
      },

      // ---- 商品中心 ----
      {
        path: 'goods',
        redirect: '/goods/brand',
        meta: { title: '商品管理', icon: 'Goods' },
        children: [
          { path: 'brand', name: 'goodsBrand', component: () => import('@/views/goods/BrandList.vue'), meta: { title: '品牌管理' } },
          { path: 'category', name: 'goodsCategory', component: () => import('@/views/goods/CategoryList.vue'), meta: { title: '分类管理' } },
          { path: 'good', name: 'goodsGood', component: () => import('@/views/goods/GoodList.vue'), meta: { title: '商品管理' } },
          { path: 'banner', name: 'goodsBanner', component: () => import('@/views/goods/BannerList.vue'), meta: { title: '轮播管理' } }
        ]
      },

      // ---- 交易中心 ----
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', icon: 'Document' }
      },

      // ---- 会员中心 ----
      {
        path: 'members',
        name: 'members',
        component: () => import('@/views/member/MemberList.vue'),
        meta: { title: '会员管理', icon: 'User' }
      },

      // ---- 秒杀中心 ----
      {
        path: 'seckills',
        redirect: '/seckills/activity',
        meta: { title: '秒杀管理', icon: 'Timer' },
        children: [
          { path: 'activity', name: 'seckillActivity', component: () => import('@/views/seckill/SeckillActivity.vue'), meta: { title: '秒杀活动' } },
          { path: 'good', name: 'seckillGood', component: () => import('@/views/seckill/SeckillGood.vue'), meta: { title: '秒杀商品' } }
        ]
      },

      // ---- 系统管理（RBAC）----
      {
        path: 'system',
        redirect: '/system/user',
        meta: { title: '系统管理', icon: 'Setting' },
        children: [
          { path: 'user', name: 'systemUser', component: () => import('@/views/system/UserList.vue'), meta: { title: '用户管理' } },
          { path: 'group', name: 'systemGroup', component: () => import('@/views/system/GroupList.vue'), meta: { title: '用户组' } },
          { path: 'role', name: 'systemRole', component: () => import('@/views/system/RoleList.vue'), meta: { title: '角色管理' } },
          { path: 'perm', name: 'systemPerm', component: () => import('@/views/system/PermList.vue'), meta: { title: '权限管理' } },
          { path: 'resource', name: 'systemResource', component: () => import('@/views/system/ResourceList.vue'), meta: { title: '资源管理' } },
          { path: 'menu', name: 'systemMenu', component: () => import('@/views/system/MenuList.vue'), meta: { title: '菜单管理' } }
        ]
      },

      // ---- 地区（FR-210，菜单来自 t_rbac_menu 的资源 1007）----
      {
        path: 'region',
        name: 'region',
        component: () => import('@/views/region/RegionList.vue'),
        meta: { title: '地区管理', icon: 'Location' }
      },

      // ---- 个人中心 ----
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', icon: 'Postcard' }
      }
    ]
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 极锋商城管理后台` : '极锋商城管理后台'

  // 登录页本身不拦截；其余没令牌就回登录页并记住来路
  if (to.name !== 'login' && !getToken()) {
    if (to.name !== 'notFound') ElMessage.warning('请先登录')
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
