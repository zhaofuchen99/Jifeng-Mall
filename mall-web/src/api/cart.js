import request from '@/utils/request'

/** 某会员的购物车列表（不分页，返回 List） */
export function getCartByMember(memberId) {
  return request.get(`/api/carts/member/${memberId}`)
}

/** 加入购物车：同商品累加数量 */
export function addToCart(data) {
  return request.post('/api/carts/save-merge', data)
}

/** 修改数量 */
export function updateCartQty(data) {
  return request.put('/api/carts', data)
}

/** 删除购物车项（注意：DELETE 的入参是 JSON 数组 body，不是 query） */
export function deleteCarts(ids) {
  return request.delete('/api/carts', { data: ids })
}

/** 下单后清掉已购项 */
export function clearCarts(ids) {
  return request.delete('/api/carts/clear', { data: ids })
}
