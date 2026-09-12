import request from '@/utils/request'
import { makeCrud } from './crud'

const base = makeCrud('/api/categories')

export default {
  ...base,
  /** 分类树（根节点带 children），商品/分类的下拉选择用这个 */
  tree: () => request.get('/api/categories/tree')
}
