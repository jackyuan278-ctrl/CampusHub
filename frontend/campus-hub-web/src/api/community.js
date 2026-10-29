import { request } from './http'

/** 问答社区：campus-community-service（网关前缀 /api/community，StripPrefix=2） */
export const questionApi = {
  page: (params, options = {}) => request({ url: '/community/questions', method: 'get', params }, options),
  hot: (top = 10, options = {}) => request({ url: '/community/questions/hot', method: 'get', params: { top } }, options),
  detail: (id, options = {}) => request({ url: `/community/questions/${id}`, method: 'get' }, options),
  publish: (payload) => request({ url: '/community/questions', method: 'post', data: payload }),
  like: (id) => request({ url: `/community/questions/${id}/like`, method: 'post' }),
  unlike: (id) => request({ url: `/community/questions/${id}/like`, method: 'delete' })
}

export const answerApi = {
  list: (questionId, options = {}) => request({ url: `/community/questions/${questionId}/answers`, method: 'get' }, options),
  create: (questionId, content) => request({ url: `/community/questions/${questionId}/answers`, method: 'post', data: { content } }),
  like: (id) => request({ url: `/community/answers/${id}/like`, method: 'post' }),
  unlike: (id) => request({ url: `/community/answers/${id}/like`, method: 'delete' })
}

export const commentApi = {
  list: (answerId, options = {}) => request({ url: `/community/answers/${answerId}/comments`, method: 'get' }, options),
  create: (answerId, content) => request({ url: `/community/answers/${answerId}/comments`, method: 'post', data: { content } })
}

export const tagApi = {
  list: (options = {}) => request({ url: '/community/tags', method: 'get' }, options)
}

export const followApi = {
  follow: (userId) => request({ url: `/community/follows/${userId}`, method: 'post' }),
  unfollow: (userId) => request({ url: `/community/follows/${userId}`, method: 'delete' }),
  mine: (options = {}) => request({ url: '/community/follows/mine', method: 'get' }, options)
}

export default questionApi
