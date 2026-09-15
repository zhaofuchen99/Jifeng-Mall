import request from '@/utils/request'
import { makeCrud } from './crud'

const base = makeCrud('/api/orders')

export default {
  ...base,
  /**
   * 订单明细。
   * 注意 OrderEntity 里<b>没有</b> items 字段，订单详情必须另调这个接口
   * （接口文档写「含明细」与实现不符）。
   */
  items: (orderId) => request.get(`/api/order-items/order/${orderId}`),

  /** 发货（仅已支付订单可发货） */
  ship: (id) => request.put(`/api/orders/${id}/ship`),

  /** 取消（仅待付款） */
  cancel: (id) => request.put(`/api/orders/${id}/cancel`),

  /** 发起退款（仅已支付/待收货，且退款状态为「无退款」）→ 退款中 */
  refund: (id) => request.put(`/api/orders/${id}/refund`),

  /** 确认退款（仅退款中）→ 已退款，订单转已取消并回补库存 */
  refundConfirm: (id) => request.put(`/api/orders/${id}/refund/confirm`)
}
