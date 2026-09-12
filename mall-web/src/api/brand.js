import request from '@/utils/request'

/**
 * 品牌分页查询。
 * 注意：logo 虽然字段存在，但 BrandMapper 没把它接成查询条件，传了也没用。
 */
export function getBrands(params) {
  return request.get('/api/brands', { params })
}

export function getBrandById(id) {
  return request.get(`/api/brands/id/${id}`)
}
