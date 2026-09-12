import request from '@/utils/request'

/**
 * 商品分页查询。
 *
 * <p><b>注意后端实际支持的筛选参数</b>（以 GoodMapper.xml 为准）：</p>
 * <ul>
 *   <li>模糊匹配：name / alias / spuNo / description</li>
 *   <li>精确匹配：categoryId / brandId / isHot / isTakeDown / isDel / isSeckill</li>
 * </ul>
 * <p>{@code docs/接口文档.md} 里写的 keyword / priceLow / priceHigh / sort <b>后端并不支持</b>，
 * Spring 会静默忽略未知查询参数。所以价格区间筛选与价格排序只能在前端做
 * （见 views/goods/GoodList.vue 的说明）。</p>
 *
 * @param {object} params 查询条件 + pageNo/pageSize/full
 */
export function getGoods(params) {
  return request.get('/api/goods', { params })
}

/** 商品详情；full=true 时带上品牌与分类对象 */
export function getGoodById(id, full = false) {
  return request.get(`/api/goods/id/${id}`, { params: { full } })
}
