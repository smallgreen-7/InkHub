import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: () => import('@/views/Home.vue') },
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
    { path: '/forgot', name: 'forgot', component: () => import('@/views/Forgot.vue') },
    {
      path: '/article/:id',
      name: 'article-detail',
      component: () => import('@/views/ArticleDetail.vue'),
    },
    // 写文章：需要登录
    {
      path: '/editor',
      name: 'editor',
      component: () => import('@/views/Editor.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/editor/:id',
      name: 'editor-edit',
      component: () => import('@/views/Editor.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/Profile.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/Settings.vue'),
      meta: { requiresAuth: true },
    },
    // 管理端：需要管理员
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '', redirect: '/admin/articles' },
        { path: 'articles', component: () => import('@/views/admin/AdminArticle.vue') },
        { path: 'comments', component: () => import('@/views/admin/AdminComment.vue') },
        { path: 'categories', component: () => import('@/views/admin/AdminCategory.vue') },
        { path: 'stats', component: () => import('@/views/admin/AdminStats.vue') },
      ],
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('@/views/Notifications.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

// 全局前置守卫：登录校验 + 管理员校验（前端防呆，后端 SecurityConfig 才是真正防线）
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return { path: '/' }
  }
  return true
})

export default router
