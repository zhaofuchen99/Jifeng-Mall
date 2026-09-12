import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { grab } from '@/api/seckill'
import { getOrderBySeckillNo } from '@/api/order'

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

/**
 * 秒杀抢购 + 结果轮询。
 *
 * <p>抢购接口是<b>异步</b>的：它只把请求丢进 Redis + MQ 就返回 orderNo，
 * 订单要等消费者处理完才落库。所以前端必须凭 orderNo 轮询订单，
 * 不能指望 grab 的返回值里有订单（接口文档 5.3 第 5 步）。</p>
 */
export function useSeckillGrab() {
  const grabbing = ref(false)

  /**
   * @param {number} seckillGoodId 秒杀商品行 id（不是商品 id）
   * @returns {{result: object, order: object|null}}
   */
  async function doGrab(seckillGoodId, { rounds = 10, interval = 800 } = {}) {
    grabbing.value = true
    try {
      // 失败时后端返回 JsonResp.fail(code,msg)，拦截器会 reject；这里自己提示
      const result = await grab(seckillGoodId).catch((e) => {
        ElMessage.error(e.message || '抢购失败')
        return null
      })
      if (!result) return { result: null, order: null }

      ElMessage.success(result.msg || '抢购提交成功，正在生成订单…')

      const order = await pollOrder(result.orderNo, rounds, interval)
      if (order) {
        ElMessage.success('订单已生成，请尽快完成支付')
      } else {
        ElMessage.warning('订单还在生成中，可稍后到「我的订单」查看')
      }
      return { result, order }
    } finally {
      grabbing.value = false
    }
  }

  async function pollOrder(seckillNo, rounds, interval) {
    if (!seckillNo) return null
    for (let i = 0; i < rounds; i++) {
      await sleep(interval)
      try {
        const order = await getOrderBySeckillNo(seckillNo)
        if (order && order.id) return order
      } catch {
        // 订单尚未生成（后端返回 null 或 404），继续轮询
      }
    }
    return null
  }

  return { grabbing, doGrab }
}
