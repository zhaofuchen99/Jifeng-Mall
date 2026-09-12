import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMyMenus } from '@/api/auth'
import { MENU_CHILDREN } from '@/config/menu'

/**
 * 动态菜单。
 *
 * <p>后端 `/api/menus/mine` 按「用户 → 组 → 角色 → 权限 → 资源 → 菜单」整条链路算，
 * 种子数据里只有 admin 挂了完整授权；operator 没有任何授权，会拿到空数组，
 * 侧边栏就是空的——这是数据问题不是 bug，界面上要给出提示。</p>
 */
export const useMenuStore = defineStore('menu', () => {
  const menus = ref([])
  const loaded = ref(false)
  const loading = ref(false)

  async function load() {
    loading.value = true
    try {
      const list = (await getMyMenus()) || []
      // 后端只给顶层菜单（parentId=0）。每个顶层菜单下的具体页面由前端的
      // MENU_CHILDREN 映射补齐——菜单表里的 url 是 /goods 这种栏目地址，
      // 而栏目下真正有品牌/分类/商品三个页面。
      menus.value = list
        .map((m) => ({
          ...m,
          children: MENU_CHILDREN[m.url] || []
        }))
        .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
      loaded.value = true
      return menus.value
    } finally {
      loading.value = false
    }
  }

  function reset() {
    menus.value = []
    loaded.value = false
  }

  return { menus, loaded, loading, load, reset }
})
