import request from '@/utils/request'

/** 分类树（公开，根节点带 children） */
export function getCategoryTree() {
  return request.get('/api/categories/tree')
}

export function getCategoryById(id) {
  return request.get(`/api/categories/id/${id}`)
}

export function getCategories(params) {
  return request.get('/api/categories', { params })
}
