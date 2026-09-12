import { makeCrud } from './crud'

/**
 * 品牌。查询条件里 name / company / site / description 是 LIKE，id 是精确匹配。
 * 注意 logo 没被接成查询条件，传了没用。
 */
export default makeCrud('/api/brands')
