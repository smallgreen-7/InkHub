import request from '@/utils/request'

// 注册
export function register(data) {
  return request.post('/auth/register', data)
}

// 登录
export function login(data) {
  return request.post('/auth/login', data)
}

// 找回密码：发送邮箱验证码
export function sendResetCode(data) {
  return request.post('/auth/password/code', data)
}

// 找回密码：验证码 + 新密码
export function resetPassword(data) {
  return request.post('/auth/password/reset', data)
}
