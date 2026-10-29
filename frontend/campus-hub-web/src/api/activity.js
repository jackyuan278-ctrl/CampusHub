import { request } from './http'

/** 活动：campus-activity-service（活动 / 报名 / 签到同一服务） */
export const activityApi = {
  /** 分页列表：keyword、status、page、pageSize */
  page: (params, options = {}) => request({ url: '/activities', method: 'get', params }, options),
  /** ES 全文检索：关键词为空时后端返回空页 */
  search: (params, options = {}) => request({ url: '/activities/search', method: 'get', params }, options),
  detail: (id, options = {}) => request({ url: `/activities/${id}`, method: 'get' }, options),
  publish: (payload) => request({ url: '/activities', method: 'post', data: payload }),
  cancel: (id) => request({ url: `/activities/${id}/cancel`, method: 'put' })
}

/** 报名与候补 */
export const signupApi = {
  signup: (activityId) => request({ url: '/signups', method: 'post', data: { activityId } }),
  joinWaitlist: (activityId) => request({ url: '/signups/waitlist', method: 'post', data: { activityId } }),
  cancel: (activityId) => request({ url: `/signups/${activityId}`, method: 'delete' }),
  mine: (options = {}) => request({ url: '/signups/mine', method: 'get' }, options)
}

/** 每日签到（Redis Bitmap） */
export const checkinApi = {
  checkin: () => request({ url: '/checkins', method: 'post' }),
  mine: (options = {}) => request({ url: '/checkins/mine', method: 'get' }, options)
}

export default activityApi
