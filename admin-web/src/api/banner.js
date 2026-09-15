import { makeCrud } from './crud'

/**
 * 首页轮播（需求 5.3 / FR-102「可配置图片与跳转链接」）。
 *
 * ⚠️ 路径是 `/api/goods/banners` 而不是 `/api/banners`：详细设计说明书没有给轮播
 * 建服务与表，本期并入了商品中心。挂在 `/api/goods/**` 下可以直接复用网关已有的
 * 「GET 公开」白名单与商品管理权限资源。详见 docs/接口文档.md 的轮播一节。
 */
export default {
  ...makeCrud('/api/goods/banners')
}
