<template>
  <div class="notifications-page page-container">
    <div class="page-head">
      <h2 class="page-title">我的通知</h2>
      <el-button v-if="list.length" link type="primary" @click="readAll">全部已读</el-button>
    </div>

    <div v-if="list.length" class="notice-list ink-card">
      <div
        v-for="n in list"
        :key="n.id"
        class="notice-item"
        :class="{ unread: n.isRead === 0 }"
        @click="open(n)"
      >
        <span class="dot" :class="typeClass(n.type)"></span>
        <div class="notice-main">
          <p class="notice-content">{{ n.content }}</p>
          <p class="notice-time">{{ formatTime(n.createTime) }}</p>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无通知" :image-size="90" />

    <div v-if="total > pageSize" class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNotifications, readNotification, readAllNotifications } from '@/api/notification'

const router = useRouter()
const list = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)

function formatTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}
function typeClass(type) {
  return { 1: 'reply', 2: 'like', 3: 'fav' }[type] || 'reply'
}

async function load(page = 1) {
  pageNum.value = page
  const data = await getNotifications(page, pageSize)
  list.value = data.records
  total.value = data.total
}

async function open(n) {
  if (n.isRead === 0) {
    await readNotification(n.id)
    n.isRead = 1
  }
  if (n.articleId) router.push('/article/' + n.articleId)
}

async function readAll() {
  await readAllNotifications()
  list.value.forEach((n) => {
    n.isRead = 1
  })
  ElMessage.success('已全部标记为已读')
}

onMounted(() => load(1))
</script>

<style scoped>
.notifications-page {
  max-width: 760px;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--ink-ink);
}
.notice-list {
  padding: 6px 18px;
  border-radius: var(--ink-radius);
}
.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--ink-border-light);
  cursor: pointer;
}
.notice-item:last-child {
  border-bottom: none;
}
.notice-item.unread .notice-content {
  font-weight: 600;
  color: var(--ink-ink);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 7px;
  flex-shrink: 0;
}
.dot.reply {
  background: var(--ink-primary);
}
.dot.like {
  background: #f59e0b;
}
.dot.fav {
  background: #10b981;
}
.notice-main {
  flex: 1;
}
.notice-content {
  margin: 0;
  font-size: 14.5px;
  color: var(--ink-text);
  line-height: 1.6;
}
.notice-time {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-faint);
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
