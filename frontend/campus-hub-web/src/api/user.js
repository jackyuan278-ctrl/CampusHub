import { request } from './http'

/** 用户与地址：campus-user-service */
export const userApi = {
  login: (payload) => request({ url: '/users/login', method: 'post', data: payload }),
  register: (payload) => request({ url: '/users/register', method: 'post', data: payload }),
  me: (options = {}) => request({ url: '/users/me', method: 'get' }, options),
  updateMe: (payload) => request({ url: '/users/me', method: 'put', data: payload }),
  changePassword: (payload) => request({ url: '/users/password', method: 'put', data: payload }),

  addresses: (options = {}) => request({ url: '/users/addresses', method: 'get' }, options),
  createAddress: (payload) => request({ url: '/users/addresses', method: 'post', data: payload }),
  updateAddress: (id, payload) => request({ url: `/users/addresses/${id}`, method: 'put', data: payload }),
  removeAddress: (id) => request({ url: `/users/addresses/${id}`, method: 'delete' }),
  setDefaultAddress: (id) => request({ url: `/users/addresses/${id}/default`, method: 'put' })
}

export default userApi
