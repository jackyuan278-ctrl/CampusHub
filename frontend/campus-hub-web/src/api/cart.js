import { request } from './http'

/** 购物车：campus-cart-service（按登录用户隔离，数据存 Redis Hash） */
export const cartApi = {
  list: (options = {}) => request({ url: '/cart', method: 'get' }, options),
  add: (itemId, num = 1) => request({ url: '/cart', method: 'post', data: { itemId, num } }),
  updateNum: (itemId, num) => request({ url: `/cart/${itemId}`, method: 'put', data: { num } }),
  remove: (itemId) => request({ url: `/cart/${itemId}`, method: 'delete' }),
  clear: () => request({ url: '/cart', method: 'delete' })
}

export default cartApi
