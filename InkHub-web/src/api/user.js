import request from '@/utils/request'

// 我的信息
export function getMe() {
  return request.get('/user/me')
}

// 修改资料：传 { nickname } 或 { avatar } 或都传
export function updateProfile(data) {
  return request.put('/user/profile', data)
}

// 修改用户名（需带当前密码）
export function updateUsername(data) {
  return request.put('/user/username', data)
}

// 修改密码（需带原密码）
export function changePassword(data) {
  return request.put('/user/password', data)
}

// 注销账号（软删除，需带当前密码）
export function deactivateAccount(data) {
  return request.delete('/user/account', { data })
}

// 通知偏好
export function getNotifyPref() {
  return request.get('/user/notify-pref')
}

export function updateNotifyPref(data) {
  return request.put('/user/notify-pref', data)
}
