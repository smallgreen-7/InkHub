<template>
  <div class="ai-floating">
    <!-- 悬浮球 -->
    <button class="fab" :class="{ open: panelOpen }" @click="togglePanel" title="AI 助手">
      <svg
        v-if="!panelOpen"
        viewBox="0 0 24 24"
        width="22"
        height="22"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path
          d="M12 3l1.9 5.7a2 2 0 0 0 1.3 1.3L21 12l-5.8 1.9a2 2 0 0 0-1.3 1.3L12 21l-1.9-5.8a2 2 0 0 0-1.3-1.3L3 12l5.8-1.9a2 2 0 0 0 1.3-1.3z"
        />
      </svg>
      <svg
        v-else
        viewBox="0 0 24 24"
        width="22"
        height="22"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <line x1="18" y1="6" x2="6" y2="18" />
        <line x1="6" y1="6" x2="18" y2="18" />
      </svg>
    </button>

    <!-- 对话浮层 -->
    <div v-if="panelOpen" class="panel">
      <div class="panel-head">
        <span class="panel-title">
          <svg
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
              d="M12 3l1.9 5.7a2 2 0 0 0 1.3 1.3L21 12l-5.8 1.9a2 2 0 0 0-1.3 1.3L12 21l-1.9-5.8a2 2 0 0 0-1.3-1.3L3 12l5.8-1.9a2 2 0 0 0 1.3-1.3z"
            />
          </svg>
          AI 助手 · 墨墨
        </span>
        <span v-if="ctx.article" class="ctx-chip" title="当前在文章页，可针对这篇文章提问">
          📄 当前文章
        </span>
        <el-button v-if="messages.length" link type="info" size="small" @click="clearChat"
          >清空</el-button
        >
      </div>

      <div ref="bodyRef" class="panel-body">
        <!-- 空态引导 -->
        <div v-if="!messages.length" class="welcome">
          <p>我是「墨墨」，已通读站内文章，可以直接问我 👇</p>
          <div class="chips">
            <button v-for="q in samples" :key="q" class="chip" @click="ask(q)">{{ q }}</button>
          </div>
          <p v-if="ctx.article" class="ctx-tip">你正在看文章，问它相关的问题效果更好</p>
        </div>

        <div v-for="m in messages" :key="m.id" class="msg" :class="m.role">
          <div v-if="m.role === 'ai'" class="avatar">墨</div>
          <div class="bubble">
            <div class="text">{{ m.content }}<span v-if="m.streaming" class="cursor">▍</span></div>
            <div v-if="m.sources && m.sources.length" class="sources">
              <span class="src-label">参考</span>
              <router-link
                v-for="s in m.sources"
                :key="s.articleId"
                :to="'/article/' + s.articleId"
                class="src-chip"
              >
                {{ s.title }}
              </router-link>
            </div>
            <div v-if="m.error" class="err-text">{{ m.error }}</div>
          </div>
        </div>
      </div>

      <div class="panel-input">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="问点站内文章相关的问题…（Enter 发送，Shift+Enter 换行）"
          @keydown="onKeydown"
        />
        <el-button
          class="send-btn ink-gradient-btn"
          :loading="streaming"
          @click="streaming ? stop() : send()"
        >
          {{ streaming ? '停止' : '发送' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { chatStream } from '@/api/ai'

const route = useRoute()
const userStore = useUserStore()

const samples = ['这个站有哪些文章？', '站内文章讲了什么内容？', '这个项目怎么部署的？']

const panelOpen = ref(false)
const messages = ref([])
const draft = ref('')
const streaming = ref(false)
const controller = ref(null)
const bodyRef = ref(null)

// 页面上下文：文章详情页时带上"当前文章"
const ctx = computed(() => {
  const m = route.path.match(/^\/article\/(\d+)/)
  return { article: m ? m[1] : null }
})

const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 7)

function scrollBottom() {
  nextTick(() => {
    const el = bodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
watch([messages, streaming], scrollBottom, { deep: true })

function togglePanel() {
  if (!userStore.isLogin) {
    ElMessage.warning('登录后就能和 AI 对话啦')
    window.location.href = '/login'
    return
  }
  panelOpen.value = !panelOpen.value
}

function ask(q) {
  draft.value = q
  send()
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    send()
  }
}

function send() {
  const q = draft.value.trim()
  if (!q || streaming.value) return
  draft.value = ''
  messages.value.push({ id: uid(), role: 'user', content: q })

  // 👇 先记录这条 AI 消息在数组中的位置
  const aiIndex = messages.value.length
  messages.value.push({
    id: uid(),
    role: 'ai',
    content: '',
    sources: [],
    streaming: true,
    error: '',
  })
  streaming.value = true

  // 最近 6 轮作为多轮上下文
  const history = messages.value
    .slice(0, -2)
    .filter((m) => m.content && !m.error)
    .slice(-6)
    .map((m) => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.content }))

  controller.value = chatStream({
    question: q,
    contextType: ctx.value.article ? 'article' : 'normal',
    articleId: ctx.value.article ? Number(ctx.value.article) : null,
    history,
    onDelta: (t) => {
      // 👇 改这里：用索引找到消息，修改后重新赋值整个数组
      const msg = messages.value[aiIndex]
      if (msg) {
        msg.content += t
        messages.value = [...messages.value]
      }
    },
    onSources: (s) => {
      // 👇 改这里
      const msg = messages.value[aiIndex]
      if (msg) {
        msg.sources = s
        messages.value = [...messages.value]
      }
    },
    onDone: () => {
      // 👇 改这里
      const msg = messages.value[aiIndex]
      if (msg) {
        msg.streaming = false
        messages.value = [...messages.value]
      }
      streaming.value = false
    },
    onError: (errMsg) => {
      const msg = messages.value[aiIndex]
      if (msg) {
        msg.streaming = false
        msg.error = errMsg
        if (!msg.content) msg.content = '（这次没有生成回答）'
        messages.value = [...messages.value]
      }
      streaming.value = false
    },
  })
}

function stop() {
  controller.value?.abort()
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'ai') last.streaming = false
  streaming.value = false
}

function clearChat() {
  if (streaming.value) stop()
  messages.value = []
}
</script>

<style scoped>
.ai-floating {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
}
.fab {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 18px rgba(79, 70, 229, 0.4);
  transition: transform 0.2s;
}
.fab:hover {
  transform: scale(1.06);
}
.fab.open {
  background: #2a2d4a;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.25);
}

.panel {
  position: fixed;
  right: 24px;
  bottom: 90px;
  width: 400px;
  height: min(640px, 72vh);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(20, 22, 40, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e6e8f0;
}
.panel-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #eef0f6;
  background: #fafbff;
}
.panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  color: #16182b;
  font-size: 14px;
  flex: 1;
}
.panel-title svg {
  color: #4f46e5;
}
.ctx-chip {
  background: #eef0ff;
  color: #4f46e5;
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 11px;
  white-space: nowrap;
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
}
.welcome {
  text-align: center;
  padding: 40px 18px 10px;
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 12px;
}
.chip {
  border: 1px solid #d6d9e6;
  background: #f7f8ff;
  color: #3f3f66;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 12px;
}
.chip:hover {
  border-color: #4f46e5;
  color: #4f46e5;
}
.ctx-tip {
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 14px;
}

.msg {
  display: flex;
  gap: 8px;
  padding: 8px 14px;
}
.msg.user {
  justify-content: flex-end;
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bubble {
  max-width: 82%;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-wrap;
}
.msg.user .bubble {
  background: linear-gradient(135deg, #16182b, #2a2d4a);
  color: #fff;
  border-top-right-radius: 3px;
}
.msg.ai .bubble {
  background: #f4f5fb;
  color: #303133;
  border-top-left-radius: 3px;
}
.cursor {
  color: #4f46e5;
  animation: blink 0.9s infinite;
}
@keyframes blink {
  50% {
    opacity: 0;
  }
}
.err-text {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}
.sources {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 6px;
  align-items: center;
}
.src-label {
  font-size: 11px;
  color: #909399;
}
.src-chip {
  display: inline-block;
  padding: 2px 9px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid #dcdff0;
  color: #4f46e5;
  font-size: 11px;
  text-decoration: none;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.src-chip:hover {
  background: #eef0ff;
}
.panel-input {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid #eef0f6;
  align-items: flex-end;
}
.panel-input :deep(.el-textarea__inner) {
  border-radius: 10px;
}
.send-btn {
  border: none;
  border-radius: 10px;
  height: 44px;
  padding: 0 18px;
}
@media (max-width: 480px) {
  .panel {
    width: calc(100vw - 24px);
    right: 12px;
  }
}
</style>
