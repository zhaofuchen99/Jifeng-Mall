import request from '@/utils/request'

/** 某会员的全部收货地址（不分页，返回 List） */
export function getAddressesByAccount(account) {
  return request.get(`/api/member-addresses/account/${account}`)
}

export function getAddressById(id) {
  return request.get(`/api/member-addresses/id/${id}`)
}

export function addAddress(data) {
  return request.post('/api/member-addresses', data)
}

export function updateAddress(data) {
  return request.put('/api/member-addresses', data)
}

export function deleteAddresses(ids) {
  return request.delete('/api/member-addresses', { data: ids })
}

/** 设为默认（后端会把该会员其它地址的默认标记清掉，保证唯一） */
export function setDefaultAddress(id, memberAccount) {
  return request.put(`/api/member-addresses/default/${id}`, null, {
    params: { memberAccount }
  })
}
