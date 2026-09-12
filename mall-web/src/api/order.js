import request from '@/utils/request'

/**
 * 提交订单。
 * 购物车下单传 {cartIds, addrId, comment, memberAccount}；
 * 立即购买传 {goodId, qty, addrId, memberAccount}。
 * @returns OrderEntity（待付款，含 orderNo / totalPay）
 */
export function createOrder(data) {
  return request.post('/api/orders/create', data)
}

/** 我的订单（不分页，返回 List） */
export function getOrdersByAccount(account) {
  return request.get(`/api/orders/member-account/${account}`)
}

/**
 * 订单主体。
 * 注意：返回的 OrderEntity <b>不含</b>订单明细，明细要另外调 getOrderItems(orderId)。
 * （接口文档说「含明细」，与实现不符。）
 */
export function getOrderById(id) {
  return request.get(`/api/orders/id/${id}`)
}

/** 订单明细 */
export function getOrderItems(orderId) {
  return request.get(`/api/order-items/order/${orderId}`)
}

/** 按秒杀流水号查订单（抢购结果轮询用；查不到时后端返回 null） */
export function getOrderBySeckillNo(seckillNo) {
  return request.get(`/api/orders/seckill-no/${seckillNo}`, { silent: true })
}

/** 发起支付——只是个状态校验，返回 true 表示该单可支付 */
export function payOrder(id) {
  return request.post(`/api/orders/${id}/pay`)
}

/** 模拟支付确认：确认后订单变为「已支付」 */
export function confirmPay(id) {
  return request.post(`/api/orders/${id}/pay/confirm`)
}

/** 取消订单（仅待付款） */
export function cancelOrder(id) {
  return request.put(`/api/orders/${id}/cancel`)
}

/** 确认收货（仅待收货） */
export function confirmReceipt(id) {
  return request.put(`/api/orders/${id}/confirm`)
}
