import { request } from './http'

/**
 * 二手商品：campus-secondhand-service
 * 列表支持两种分页：page + pageSize（前 5 页）与 cursorId 游标（第 6 页起，滚动加载用）。
 */
export const itemApi = {
  page: (params, options = {}) => request({ url: '/items', method: 'get', params }, options),
  hot: (options = {}) => request({ url: '/items/hot', method: 'get' }, options),
  detail: (id, options = {}) => request({ url: `/items/${id}`, method: 'get' }, options),
  categories: (options = {}) => request({ url: '/items/categories', method: 'get' }, options)
}

export default itemApi
