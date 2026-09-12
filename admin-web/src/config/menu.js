/**
 * 栏目 → 页面 的映射。
 *
 * 后端菜单表（t_rbac_menu）里存的是**栏目**级的 url：/goods、/orders、/system …
 * 但一个栏目下往往有多个页面（比如 /goods 下有品牌、分类、商品三个）。
 * 这些页面不是数据库数据，是前端路由，所以在这里声明。
 *
 * 侧边栏渲染时拿 /api/menus/mine 的结果去这张表里查子菜单；
 * 查不到就按单页菜单渲染（点击直接跳 url）。
 */
export const MENU_CHILDREN = {
  '/goods': [
    { path: '/goods/brand', title: '品牌管理', icon: 'Flag' },
    { path: '/goods/category', title: '分类管理', icon: 'Files' },
    { path: '/goods/good', title: '商品管理', icon: 'Goods' }
  ],
  '/seckills': [
    { path: '/seckills/activity', title: '秒杀活动', icon: 'Timer' },
    { path: '/seckills/good', title: '秒杀商品', icon: 'ShoppingCartFull' }
  ],
  '/system': [
    { path: '/system/user', title: '用户管理', icon: 'User' },
    { path: '/system/group', title: '用户组', icon: 'UserFilled' },
    { path: '/system/role', title: '角色管理', icon: 'Avatar' },
    { path: '/system/perm', title: '权限管理', icon: 'Key' },
    { path: '/system/resource', title: '资源管理', icon: 'Grid' },
    { path: '/system/menu', title: '菜单管理', icon: 'Menu' }
  ]
}

/**
 * 固定的附加菜单。
 *
 * 地区管理（需求 FR-210）在种子菜单表里没有对应行——它没有挂到任何
 * 权限资源上。为了不擅自改用户的种子数据，这里作为固定项附在动态菜单之后。
 * 如果之后往 t_rbac_menu 里补了记录，把这里删掉即可。
 */
export const STATIC_MENUS = [{ path: '/region', title: '地区管理', icon: 'Location' }]
