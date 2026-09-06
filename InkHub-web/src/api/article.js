import request from '@/utils/request'

// 文章流（首页）：latest/hot + 分类/标签/关键词 + 分页
export function getArticles(params) {
  return request.get('/articles', { params })
}

// 文章详情
export function getArticle(id) {
  return request.get(`/articles/${id}`)
}

// 我的文章（含草稿）
export function getMyArticles(pageNum = 1, pageSize = 10) {
  return request.get('/articles/mine', { params: { pageNum, pageSize } })
}

// 发布 / 存草稿
export function createArticle(data) {
  return request.post('/articles', data)
}

// 编辑
export function updateArticle(id, data) {
  return request.put(`/articles/${id}`, data)
}

// 删除
export function deleteArticle(id) {
  return request.delete(`/articles/${id}`)
}

// ===== 点赞 =====
export function likeArticle(id) {
  return request.post(`/articles/${id}/like`)
}
export function unlikeArticle(id) {
  return request.delete(`/articles/${id}/like`)
}
export function getLikeStatus(id) {
  return request.get(`/articles/${id}/like/status`)
}

// ===== 收藏 =====
export function favoriteArticle(id) {
  return request.post(`/articles/${id}/favorite`)
}
export function unfavoriteArticle(id) {
  return request.delete(`/articles/${id}/favorite`)
}
export function getFavoriteStatus(id) {
  return request.get(`/articles/${id}/favorite/status`)
}

// 我的收藏
export function getMyFavorites() {
  return request.get('/user/favorites')
}
// 相关文章推荐
export function getRelatedArticles(id) {
  return request.get(`/articles/${id}/related`)
}
// AI 语义搜索（向量召回相关文章）
export function semanticSearch(keyword) {
  return request.get('/articles/semantic', { params: { keyword } })
}
