<template>
  <div class="detail-page page-container" v-if="article">
    <div class="detail-card ink-card">
      <!-- 文章头 -->
      <div class="article-head">
        <h1 class="title">
          <el-tag v-if="article.top" type="danger" size="small" effect="dark" class="top-tag"
            >置顶</el-tag
          >
          {{ article.title }}
        </h1>
        <div class="meta">
          <span class="meta-item author">
            <span class="author-avatar">{{
              (article.authorName || 'U').slice(0, 1).toUpperCase()
            }}</span>
            {{ article.authorName }}
          </span>
          <span class="meta-divider" />
          <span v-if="article.categoryName" class="meta-item">
            <svg
              viewBox="0 0 24 24"
              width="14"
              height="14"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path
                d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.83z"
              />
              <line x1="7" y1="7" x2="7.01" y2="7" />
            </svg>
            {{ article.categoryName }}
          </span>
          <span class="meta-item">{{ formatTime(article.publishTime) }}</span>
          <span class="meta-item views">
            <svg
              viewBox="0 0 24 24"
              width="14"
              height="14"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            {{ article.viewCount }} 次阅读
          </span>
        </div>
      </div>

      <!-- Markdown 正文 -->
      <div class="article-body">
        <div class="markdown-body" v-html="html"></div>
      </div>

      <!-- 互动区 -->
      <div class="actions">
        <el-button class="action-btn" :class="{ 'is-liked': liked }" round @click="toggleLike">
          <svg
            v-if="!liked"
            viewBox="0 0 24 24"
            width="16"
            height="16"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path
              d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"
            />
          </svg>
          <svg
            v-else
            viewBox="0 0 24 24"
            width="16"
            height="16"
            fill="currentColor"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path
              d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"
            />
          </svg>
          点赞 {{ article.likeCount }}
        </el-button>
        <el-button
          class="action-btn"
          :class="{ 'is-faved': favorited }"
          round
          @click="toggleFavorite"
        >
          <svg
            v-if="!favorited"
            viewBox="0 0 24 24"
            width="16"
            height="16"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polygon
              points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
            />
          </svg>
          <svg
            v-else
            viewBox="0 0 24 24"
            width="16"
            height="16"
            fill="currentColor"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polygon
              points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
            />
          </svg>
          收藏 {{ article.favoriteCount }}
        </el-button>
      </div>

      <!-- 相关阅读 -->
      <div v-if="related.length" class="related">
        <h3 class="related-title">相关阅读</h3>
        <div class="related-grid">
          <div
            v-for="r in related"
            :key="r.id"
            class="related-card ink-card ink-card--hover"
            @click="$router.push('/article/' + r.id)"
          >
            <h4 class="related-name">{{ r.title }}</h4>
            <p class="related-meta">浏览 {{ r.viewCount }} · 赞 {{ r.likeCount }}</p>
          </div>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="comments">
        <div class="comments-head">
          <h3>
            评论 <span class="count">{{ article.commentCount }}</span>
          </h3>
        </div>

        <div v-if="userStore.isLogin" class="comment-input ink-card">
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="3"
            :placeholder="replyParentId ? '回复 ' + replyTargetName + '…' : '写下你的想法…'"
          />
          <div class="input-foot">
            <el-button v-if="replyParentId" link type="info" @click="cancelReply"
              >取消回复</el-button
            >
            <el-button type="primary" class="ink-gradient-btn" @click="submitComment"
              >发表评论</el-button
            >
          </div>
        </div>
        <div v-else class="login-tip ink-card">
          <span>登录后即可参与评论</span>
          <el-button size="small" @click="$router.push('/login')">去登录</el-button>
        </div>

        <div v-if="comments.length" class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <span class="c-avatar">{{ (c.nickname || 'U').slice(0, 1).toUpperCase() }}</span>
            <div class="c-main">
              <div class="c-head">
                <span class="c-nick">{{ c.nickname }}</span>
                <span class="c-time">{{ formatTime(c.createTime) }}</span>
              </div>
              <div class="c-content">{{ c.content }}</div>
              <div class="c-reply">
                <el-button link type="primary" size="small" @click="replyTo(c)">回复</el-button>
                <el-button
                  v-if="c.userId === userStore.userInfo?.id"
                  link
                  type="danger"
                  size="small"
                  @click="delComment(c.id)"
                  >删除</el-button
                >
              </div>
              <!-- 二级回复 -->
              <div v-if="repliesOf(c.id).length" class="reply-list">
                <div v-for="r in repliesOf(c.id)" :key="r.id" class="reply-item">
                  <span class="r-nick">{{ r.nickname }}</span>
                  <span class="r-content">{{ r.content }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="还没有评论，来抢沙发～" :image-size="90" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getArticle,
  getRelatedArticles,
  likeArticle,
  unlikeArticle,
  favoriteArticle,
  unfavoriteArticle,
  getLikeStatus,
  getFavoriteStatus,
} from '@/api/article'
import { getComments, addComment, deleteComment } from '@/api/comment'
import { renderMarkdown } from '@/utils/markdown'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const article = ref(null)
const related = ref([])
const html = computed(() => renderMarkdown(article.value?.contentMd || ''))
const liked = ref(false)
const favorited = ref(false)
const comments = ref([])
const commentText = ref('')
const replyParentId = ref(null)
const replyTargetName = ref('')

function formatTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}
// 某条一级评论的回复（parentId === 该评论 id）
function repliesOf(parentId) {
  return comments.value.filter((c) => c.parentId === parentId)
}

async function load() {
  const id = route.params.id
  article.value = await getArticle(id)
  related.value = await getRelatedArticles(id)
  comments.value = await getComments(id)
  // 登录了才查点赞/收藏状态
  if (userStore.isLogin) {
    try {
      liked.value = await getLikeStatus(id)
      favorited.value = await getFavoriteStatus(id)
    } catch {
      /* 忽略 */
    }
  }
}

async function toggleLike() {
  if (!userStore.isLogin) return ElMessage.warning('请先登录')
  if (liked.value) {
    await unlikeArticle(article.value.id)
    article.value.likeCount--
  } else {
    await likeArticle(article.value.id)
    article.value.likeCount++
  }
  liked.value = !liked.value
}
async function toggleFavorite() {
  if (!userStore.isLogin) return ElMessage.warning('请先登录')
  if (favorited.value) {
    await unfavoriteArticle(article.value.id)
    article.value.favoriteCount--
  } else {
    await favoriteArticle(article.value.id)
    article.value.favoriteCount++
  }
  favorited.value = !favorited.value
}

async function submitComment() {
  if (!commentText.value.trim()) return ElMessage.warning('评论不能为空')
  await addComment(article.value.id, commentText.value.trim(), replyParentId.value)
  ElMessage.success('评论成功')
  commentText.value = ''
  replyParentId.value = null
  replyTargetName.value = ''
  comments.value = await getComments(article.value.id)
  article.value.commentCount++
}
function replyTo(c) {
  replyParentId.value = c.id
  replyTargetName.value = c.nickname
  commentText.value = `@${c.nickname} `
}
function cancelReply() {
  replyParentId.value = null
  replyTargetName.value = ''
  commentText.value = ''
}
async function delComment(id) {
  await deleteComment(id)
  ElMessage.success('已删除')
  comments.value = await getComments(article.value.id)
  article.value.commentCount--
}

onMounted(load)
</script>

<style scoped>
.detail-page {
  max-width: 860px;
}

.detail-card {
  padding: 44px 48px 36px;
}

/* 文章头 */
.article-head {
  margin-bottom: 28px;
}
.title {
  margin: 0 0 16px;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.35;
  letter-spacing: -0.5px;
  color: var(--ink-ink);
  display: flex;
  align-items: center;
  gap: 10px;
}
.top-tag {
  flex-shrink: 0;
  border: none !important;
}
.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--ink-faint);
  font-size: 13.5px;
  flex-wrap: wrap;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.author {
  color: var(--ink-muted);
  font-weight: 600;
}
.author-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--ink-deep);
  color: var(--ink-accent-glow);
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.meta-divider {
  width: 1px;
  height: 14px;
  background: var(--ink-border);
}
.views {
  color: var(--ink-faint);
}

/* 正文 */
.article-body {
  padding-bottom: 8px;
}

/* 互动区 */
.actions {
  display: flex;
  gap: 12px;
  margin: 34px 0 40px;
}
.action-btn {
  border-radius: 999px !important;
  padding: 10px 24px !important;
  font-size: 14.5px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid var(--ink-border) !important;
  background: #fff !important;
  color: var(--ink-muted) !important;
  transition: all 0.2s ease !important;
}
.action-btn:hover {
  border-color: var(--ink-primary-300) !important;
  color: var(--ink-primary) !important;
  transform: translateY(-1px);
}
.action-btn.is-liked {
  background: var(--ink-accent) !important;
  border-color: transparent !important;
  color: #fff !important;
  box-shadow: 0 4px 14px rgba(29, 159, 192, 0.3);
}
.action-btn.is-faved {
  background: var(--ink-navy) !important;
  border-color: transparent !important;
  color: var(--ink-foam) !important;
  box-shadow: 0 4px 14px rgba(20, 48, 74, 0.3);
}

/* 评论区 */
.comments {
  border-top: 1px solid var(--ink-border-light);
  padding-top: 26px;
}
.comments-head h3 {
  margin: 0 0 18px;
  font-size: 17px;
  color: var(--ink-ink);
  display: flex;
  align-items: center;
  gap: 8px;
}
.comments-head .count {
  font-size: 12.5px;
  background: var(--ink-primary-50);
  color: var(--ink-primary);
  border-radius: 999px;
  padding: 1px 10px;
  font-weight: 600;
}

.comment-input {
  padding: 14px;
  margin-bottom: 20px;
  border-radius: var(--ink-radius-sm);
}
.input-foot {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.login-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  color: var(--ink-muted);
  font-size: 14px;
  border-radius: var(--ink-radius-sm);
  margin-bottom: 20px;
}

.comment-list {
  display: flex;
  flex-direction: column;
}
.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--ink-border-light);
}
.c-avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--ink-deep);
  color: var(--ink-accent-glow);
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.c-main {
  flex: 1;
  min-width: 0;
}
.c-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 4px;
}
.c-nick {
  font-weight: 600;
  color: var(--ink-text);
  font-size: 14px;
}
.c-time {
  color: var(--ink-faint);
  font-size: 12px;
}
.c-content {
  color: var(--ink-text);
  font-size: 14.5px;
  line-height: 1.7;
}
.c-reply {
  margin-top: 6px;
}

.reply-list {
  margin-top: 10px;
  background: var(--ink-bg);
  border-radius: var(--ink-radius-sm);
  padding: 6px 14px;
}
.reply-item {
  padding: 8px 0;
  font-size: 14px;
  line-height: 1.6;
  border-bottom: 1px dashed var(--ink-border-light);
}
.reply-item:last-child {
  border-bottom: none;
}
.r-nick {
  font-weight: 600;
  color: var(--ink-primary);
  margin-right: 8px;
}
.r-content {
  color: var(--ink-text);
}

@media (max-width: 720px) {
  .detail-card {
    padding: 28px 22px;
  }
  .title {
    font-size: 24px;
  }
}
.related {
  margin-top: 36px;
  padding-top: 26px;
  border-top: 1px solid var(--ink-border-light);
}
.related-title {
  margin: 0 0 14px;
  font-size: 17px;
  color: var(--ink-ink);
}
.related-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}
.related-card {
  padding: 16px;
  cursor: pointer;
}
.related-name {
  margin: 0 0 8px;
  font-size: 14.5px;
  font-weight: 700;
  color: var(--ink-ink);
  line-height: 1.5;
  display: -webkit-box;
 -webkit-line-clamp: 2; 
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.related-meta {
  margin: 0;
  color: var(--ink-faint);
  font-size: 12.5px;
}
</style>
