import request from '@/utils/request'

/**
 * 后端绝大多数资源都是同一套 REST 形状：
 *   GET    {base}?pageNo&pageSize&...   分页查询
 *   GET    {base}/id/{id}               详情
 *   POST   {base}                       新增
 *   PUT    {base}                       修改
 *   DELETE {base}   body: [1,2,3]       批量删除（注意是 JSON 数组 body，不是 query 参数）
 *
 * 所以这里用工厂函数生成，各 api 模块只写一行。
 */
export function makeCrud(base) {
  return {
    list: (params) => request.get(base, { params }),
    detail: (id) => request.get(`${base}/id/${id}`),
    save: (data) => request.post(base, data),
    update: (data) => request.put(base, data),
    remove: (ids) => request.delete(base, { data: ids })
  }
}
