import request from '@/utils/request'
import { makeCrud } from './crud'

const base = makeCrud('/api/goods')

export default {
  ...base,
  /**
   * 商品详情。full=true 时后端会通过 Feign 把品牌与分类对象一并填好。
   * 注意 full 只在单查里生效（列表接口也支持，但每行都发 Feign 调用，慢）。
   */
  detailFull: (id) => request.get(`/api/goods/id/${id}`, { params: { full: true } })
}
