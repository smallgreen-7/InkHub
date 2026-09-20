<template>
  <div class="app">
    <header class="nav">
      <div class="nav-inner page-container">
        <router-link to="/" class="logo">
          <span class="logo-mark">
            <svg
              viewBox="0 0 24 24"
              width="20"
              height="20"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
            </svg>
          </span>
          <span class="logo-text">Ink<em>Hub</em></span>
        </router-link>

        <nav class="nav-links">
          <router-link to="/" class="nav-link" exact-active-class="active">首页</router-link>
          <router-link v-if="userStore.isLogin" to="/profile" class="nav-link" active-class="active"
            >我的</router-link
          >
          <router-link
            v-if="userStore.isAdmin"
            to="/admin/articles"
            class="nav-link"
            active-class="active"
            >管理后台</router-link
          >
          <router-link to="/notifications" class="nav-link" active-class="active">通知</router-link>
        </nav>

        <div class="nav-right">
          <el-button class="write-btn ink-gradient-btn" size="small" @click="goEditor">
            <svg
              viewBox="0 0 24 24"
              width="14"
              height="14"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M12 5v14M5 12h14" />
            </svg>
            写文章
          </el-button>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="bell-badge">
            <el-dropdown trigger="click" @command="onBellCommand">
              <span class="bell">
                <svg
                  viewBox="0 0 24 24"
                  width="20"
                  height="20"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                  <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                </svg>
              </span>
              <template #dropdown>
                <el-dropdown-menu class="bell-menu">
                  <template v-if="notices.length">
                    <el-dropdown-item
                      v-for="n in notices"
                      :key="n.id"
                      :command="'go:' + n.id"
                      class="bell-item"
                    >
                      <div class="bell-text">
                        <span :class="{ unread: n.isRead === 0 }">{{ n.content }}</span>
                        <span class="bell-time">{{ formatTime(n.createTime) }}</span>
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item divided :command="'readall'" class="bell-all"
                      >全部已读</el-dropdown-item
                    >
                  </template>
                  <el-dropdown-item v-else disabled>暂无通知</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-badge>
          <template v-if="userStore.isLogin">
            <el-dropdown trigger="click" @command="onCommand">
              <span class="user-chip">
                <el-avatar :size="30" class="avatar" :src="userStore.userInfo?.avatar">{{ initial }}</el-avatar>
                <span class="nickname">{{
                  userStore.userInfo?.nickname || userStore.userInfo?.username
                }}</span>
                <svg
                  class="chevron"
                  viewBox="0 0 24 24"
                  width="14"
                  height="14"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M6 9l6 6 6-6" />
                </svg>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin"
                    >管理后台</el-dropdown-item
                  >
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <el-button v-else class="login-btn" size="small" @click="$router.push('/login')"
            >登录</el-button
          >
        </div>
      </div>
    </header>

    <main class="content">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <footer class="footer">
      <div class="page-container">
        <span>InkHub · 用文字记录思考</span>
        <span class="dot">·</span>
        <span>一个 Markdown 知识社区</span>
      </div>
    </footer>

    <!-- 全局悬浮 AI 助手 -->
    <AiFloating />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AiFloating from '@/components/AiFloating.vue'
import {
  getNotifications,
  getUnreadCount,
  readNotification,
  readAllNotifications,
} from '@/api/notification'

const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)
const notices = ref([])

const initial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || 'U'
  return name.slice(0, 1).toUpperCase()
})

function goEditor() {
  // 未登录先去登录页，登录成功后 redirect 回编辑器
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: '/editor' } })
  } else {
    router.push('/editor')
  }
}
function onCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/')
  } else {
    router.push('/' + cmd)
  }
}
function formatTime(t) {
  return t ? t.replace('T', ' ').slice(5, 16) : ''
}

async function loadNotices() {
  const data = await getNotifications(1, 5) // 下拉只取最近 5 条
  notices.value = data.records
  unreadCount.value = await getUnreadCount()
}

// 页面加载即拉取通知
onMounted(loadNotices)

async function onBellCommand(cmd) {
  if (cmd === 'readall') {
    await readAllNotifications()
    unreadCount.value = 0
    notices.value.forEach((n) => {
      n.isRead = 1
    })
  } else if (cmd.startsWith('go:')) {
    const id = Number(cmd.slice(3))
    const n = notices.value.find((x) => x.id === id)
    await readNotification(id) // 点开即已读
    if (n.isRead === 0) {
      n.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
    if (n.articleId) router.push('/article/' + n.articleId)
  }
}
</script>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部导航：固定 + 玻璃拟态 */
/* 顶部导航：深海面 + 轻微玻璃感 */
.nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(10, 22, 36, 0.86);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: var(--ink-shadow-nav);
}
.nav-inner {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 32px;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 9px;
}
.logo-mark {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ink-accent-glow);
  color: var(--ink-abyss);
  border-radius: 10px;
  box-shadow: 0 0 18px rgba(90, 209, 230, 0.35);
}
.logo-text {
  font-family: var(--ink-serif);
  font-size: 21px;
  font-weight: 600;
  color: var(--ink-foam);
  letter-spacing: 0.02em;
}
.logo-text em {
  font-style: normal;
  color: var(--ink-accent-glow);
}

/* 导航链接 */
.nav-links {
  display: flex;
  gap: 4px;
  flex: 1;
}
.nav-link {
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 14.5px;
  font-weight: 500;
  color: var(--ink-foam-dim);
  transition: all 0.2s ease;
}
.nav-link:hover {
  color: var(--ink-accent-glow);
  background: rgba(90, 209, 230, 0.1);
}
.nav-link.active {
  color: var(--ink-accent-glow);
  background: rgba(90, 209, 230, 0.12);
  font-weight: 600;
}

/* 右侧 */
.nav-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.write-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #fff !important;
  border-radius: 8px;
}
.login-btn {
  border-radius: 8px;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: all 0.2s ease;
  outline: none;
}
.user-chip:hover {
  border-color: var(--ink-accent-glow);
  background: rgba(90, 209, 230, 0.12);
}
.avatar {
  background: var(--ink-accent-glow);
  font-weight: 700;
  font-size: 13px;
  color: var(--ink-abyss);
}
.nickname {
  font-size: 14px;
  font-weight: 500;
  color: var(--ink-foam);
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chevron {
  color: var(--ink-foam-dim);
}

/* 内容区 */
.content {
  flex: 1;
  padding: 40px 0 64px;
}

/* 页脚：深海面 */
.footer {
  background: linear-gradient(180deg, var(--ink-deep), var(--ink-abyss));
  padding: 44px 0;
  color: var(--ink-foam-dim);
  font-size: 13px;
}
.footer .page-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
}
.footer strong {
  font-family: var(--ink-serif);
  font-size: 17px;
  font-weight: 600;
  color: var(--ink-foam);
  letter-spacing: 0.02em;
}
.dot {
  color: rgba(255, 255, 255, 0.18);
}
.bell {
  display: inline-flex;
  padding: 6px;
  border-radius: 8px;
  color: var(--ink-foam-dim);
  cursor: pointer;
  transition: all 0.2s;
}
.bell:hover {
  color: var(--ink-accent-glow);
  background: rgba(90, 209, 230, 0.12);
}
.bell-menu {
  min-width: 280px;
  max-width: 320px;
}
.bell-item {
  white-space: normal;
  height: auto !important;
  padding: 8px 12px !important;
}
.bell-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.bell-text .unread {
  font-weight: 600;
  color: var(--ink-ink);
}
.bell-time {
  font-size: 12px;
  color: var(--ink-faint);
}
.bell-all {
  justify-content: center;
  color: var(--ink-primary);
}
</style>
