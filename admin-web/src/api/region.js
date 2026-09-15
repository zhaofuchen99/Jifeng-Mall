import request from '@/utils/request'
import { makeCrud } from './crud'

/**
 * 行政区划（FR-210）。写接口是后补的，形状与其它资源一致（见 makeCrud）。
 *
 * 注意两点：
 * 1. `GET /api/regions/id/{id}` 的 SQL 没 join 父级，返回的 parent 是 null；
 * 2. `DELETE /api/regions` 是**级联删除**，会连同所有下级一起删掉，
 *    返回的 data 是实际删除的行数（含被级联的），不等于传入的 id 个数。
 */
export const regionApi = {
  ...makeCrud('/api/regions'),
  /** 下级区划；id 传 0 取省级 */
  children: (parentId) => request.get(`/api/regions/parent-id/${parentId}`)
}
