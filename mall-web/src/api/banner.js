import request from '@/utils/request'

/**
 * 首页轮播（需求 5.3 / FR-102）。
 *
 * 接口在商品中心里，路径是 `GET /api/goods/banners` —— 详细设计说明书没有给轮播
 * 单独建服务与表，本期并入了 good-api（理由见 docs/接口文档.md 的轮播一节）。
 *
 * - `enabled=true` 只取启用中的，后端已按 `sort_no, id` 排好序
 * - `pageSize=0` = 取全部（Nacos 里 `pagehelper.page-size-zero: on`）
 * - `silent`：首页拿不到轮播不该弹错误框，页面自己回退到内置渐变
 */
export function getBanners() {
  return request.get('/api/goods/banners', {
    params: { enabled: true, pageNo: 1, pageSize: 0 },
    silent: true
  })
}
