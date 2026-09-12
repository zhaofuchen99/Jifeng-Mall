import request from '@/utils/request'

/**
 * 行政区划（只读，region-api 没有写接口）。
 * 注意 GET /api/regions/id/{id} 的 SQL 没 join 父级，返回的 parent 是 null。
 */
export const regionApi = {
  list: (params) => request.get('/api/regions', { params }),
  detail: (id) => request.get(`/api/regions/id/${id}`),
  /** 下级区划；id 传 0 取省级 */
  children: (parentId) => request.get(`/api/regions/parent-id/${parentId}`)
}
