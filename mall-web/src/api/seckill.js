import request from '@/utils/request'

/** 进行中/即将开始的秒杀活动（公开） */
export function getActiveSeckills() {
  return request.get('/api/seckills/active')
}

export function getSeckillById(id) {
  return request.get(`/api/seckills/id/${id}`)
}

/** 某活动下的秒杀商品（含秒杀价与剩余库存） */
export function getSeckillGoods(seckillId) {
  return request.get(`/api/seckill-goods/seckill/${seckillId}`)
}

export function getSeckillGoodById(id) {
  return request.get(`/api/seckill-goods/id/${id}`)
}

/**
 * 按商品 id 反查它的秒杀配置（商品详情页判断"这件商品有没有在秒杀"）。
 * 后端没有专门的接口，但 SeckillGoodSearchBean 支持 goodId 精确匹配。
 */
export function getSeckillGoodsByGood(goodId) {
  return request.get('/api/seckill-goods', { params: { goodId, pageNo: 1, pageSize: 20 } })
}

/**
 * 立即秒杀。结果是异步的——返回 {code, msg, orderNo, success}，
 * 前端拿 orderNo 轮询订单是否生成，而不是等这个接口返回订单。
 */
export function grab(seckillGoodId) {
  return request.post('/api/seckills/grab', { seckillGoodId }, { silent: true })
}
