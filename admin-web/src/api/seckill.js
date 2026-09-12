import request from '@/utils/request'
import { makeCrud } from './crud'

export const seckillApi = {
  ...makeCrud('/api/seckills'),
  /** 启用中且未结束的活动（前台会场用的就是它） */
  active: () => request.get('/api/seckills/active')
}

export const seckillGoodApi = {
  ...makeCrud('/api/seckill-goods'),
  /** 某活动下的全部秒杀商品 */
  bySeckill: (seckillId) => request.get(`/api/seckill-goods/seckill/${seckillId}`)
}
