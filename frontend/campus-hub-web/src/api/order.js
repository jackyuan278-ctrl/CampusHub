import { request } from './http'

/** 订单：campus-order-service */
export const orderApi = {
  create: (payload) => request({ url: '/orders', method: 'post', data: payload }),
  mine: (options = {}) => request({ url: '/orders', method: 'get' }, options),
  detail: (id, options = {}) => request({ url: `/orders/${id}`, method: 'get' }, options)
}

/** 支付：campus-payment-service；payType 取 balance（余额）/ mock（模拟扫码） */
export const payApi = {
  pay: (orderId, payType) => request({ url: `/pay/orders/${orderId}/pay`, method: 'post', data: { payType } })
}

export default orderApi
