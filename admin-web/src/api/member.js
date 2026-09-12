import { makeCrud } from './crud'

/**
 * 会员。会员实体字段很多（三十多个），表格里只展示常用的几个，
 * 编辑弹窗里也只开放真正需要后台改的字段。
 */
export default makeCrud('/api/members')
