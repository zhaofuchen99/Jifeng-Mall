import request from '@/utils/request'

/** 下级行政区划（省市区三级联动用；id 传 0 取省级） */
export function getChildren(parentId) {
  return request.get(`/api/regions/parent-id/${parentId}`)
}

export function getRegionById(id) {
  return request.get(`/api/regions/id/${id}`)
}
