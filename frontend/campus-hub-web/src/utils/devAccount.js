/**
 * 开发/演示用默认账号。
 * 走前端本地会话，不请求 /users/login，所以后端没起也能进去。
 * 正式上线前删掉本文件，并清掉 auth.js / http.js / LoginView.vue 里对它的引用即可。
 */
export const DEV_ACCOUNT = { username: 'admin', password: '123456' }

const DEV_TOKEN_PREFIX = 'dev-local-'

export function isDevCredential(username, password) {
  return (
    String(username || '').trim().toLowerCase() === DEV_ACCOUNT.username &&
    String(password || '') === DEV_ACCOUNT.password
  )
}

/** 假 token 打头，用来区分「本地会话」和真实 JWT */
export function isDevToken(token) {
  return String(token || '').startsWith(DEV_TOKEN_PREFIX)
}

export function createDevSession() {
  return {
    token: `${DEV_TOKEN_PREFIX}${Date.now().toString(36)}`,
    user: {
      id: 'dev-admin',
      username: DEV_ACCOUNT.username,
      nickname: '测试管理员',
      avatar: '',
      bio: '前端本地测试账号',
      balance: 100000
    }
  }
}
