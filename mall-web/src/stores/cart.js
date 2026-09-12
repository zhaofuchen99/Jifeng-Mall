import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as cartApi from '@/api/cart'
import { useUserStore } from './user'

/**
 * 购物车。顶栏的角标数量取自这里，所以加入购物车后要 refresh()。
 */
export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const loading = ref(false)

  /** 角标：商品件数合计（不是行数） */
  const totalQty = computed(() => items.value.reduce((sum, it) => sum + (it.qty || 0), 0))

  async function refresh() {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      items.value = []
      return
    }
    loading.value = true
    try {
      items.value = (await cartApi.getCartByMember(userStore.memberId)) || []
    } finally {
      loading.value = false
    }
  }

  /** 加入购物车（同一商品自动累加），成功后刷新角标 */
  async function add(goodId, qty = 1) {
    const userStore = useUserStore()
    const row = await cartApi.addToCart({
      memberId: userStore.memberId,
      goodId,
      qty
    })
    await refresh()
    return row
  }

  async function updateQty(id, qty) {
    await cartApi.updateCartQty({ id, qty })
    const target = items.value.find((it) => it.id === id)
    if (target) target.qty = qty
  }

  async function remove(ids) {
    await cartApi.deleteCarts(ids)
    await refresh()
  }

  /** 下单成功后清掉已结算的那些行 */
  async function clear(ids) {
    await cartApi.clearCarts(ids)
    await refresh()
  }

  function reset() {
    items.value = []
  }

  return { items, loading, totalQty, refresh, add, updateQty, remove, clear, reset }
})
