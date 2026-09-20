import request from '@/utils/request'

/** AI 生成摘要（编辑器用） */
export function summarize(data) {
  return request.post('/ai/summarize', data)
}

/**
 * 统一 SSE 流式对话（RAG + Agent 共用解析逻辑）
 * EventSource 不支持 POST/Header，用 fetch + ReadableStream 手写解析
 * 服务端事件：data: {"type":"delta"|"sources"|"done"|"error", ...}
 * @param mode 'rag' | 'agent'
 * @returns AbortController（停止回答用）
 */
export function chatStream({
  mode = 'rag',
  question,
  sessionId,
  contextType,
  articleId,
  history,
  onDelta,
  onSources,
  onDone,
  onError,
}) {
  const token = localStorage.getItem('token')
  const controller = new AbortController()
  const url = mode === 'agent' ? '/api/ai/agent/chat' : '/api/ai/chat'
  const body =
    mode === 'agent' ? { question, sessionId } : { question, contextType, articleId, history }

  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
    signal: controller.signal,
  })
    .then(async (resp) => {
      if (resp.status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
        return
      }
      if (!resp.ok || !resp.body) {
        let msg = '请求失败'
        try {
          const data = await resp.json()
          msg = data.msg || msg
        } catch {
          /* ignore */
        }
        onError?.(msg)
        return
      }
      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      const handleEvent = (raw) => {
        const line = raw.split('\n').find((l) => l.startsWith('data:'))
        if (!line) return
        try {
          const ev = JSON.parse(line.slice(5).trim())
          if (ev.type === 'delta') onDelta?.(ev.text || '')
          else if (ev.type === 'sources') onSources?.(ev.sources || [])
          else if (ev.type === 'done') onDone?.()
          else if (ev.type === 'error') onError?.(ev.msg || '出错了')
        } catch {
          /* 跳过碎片 */
        }
      }
      for (;;) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        let idx
        while ((idx = buffer.indexOf('\n\n')) >= 0) {
          handleEvent(buffer.slice(0, idx))
          buffer = buffer.slice(idx + 2)
        }
      }
      if (buffer.trim()) handleEvent(buffer)
    })
    .catch((err) => {
      if (err.name !== 'AbortError') onError?.(err.message || '网络错误')
    })

  return controller
}
