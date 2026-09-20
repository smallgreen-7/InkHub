<template>
  <div class="editor-page page-container">
    <div class="editor-head">
      <h2 class="page-title">{{ isEdit ? '编辑文章' : '写文章' }}</h2>
      <div class="head-actions">
        <el-button size="large" @click="save(0)">存草稿</el-button>
        <el-button type="primary" size="large" class="ink-gradient-btn" @click="save(1)"
          >发布</el-button
        >
      </div>
    </div>

    <!-- 标题（大输入） -->
    <input v-model="form.title" class="title-input" maxlength="100" placeholder="输入标题…" />

    <div class="editor-meta ink-card">
      <div class="meta-grid">
        <div class="meta-cell">
          <label class="meta-label">摘要
            <el-button
              v-if="form.contentMd && form.contentMd.trim()"
              link
              type="primary"
              size="small"
              :loading="summarizing"
              style="margin-left: 6px"
              @click="aiSummarize"
            >AI 生成</el-button>
          </label>
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            maxlength="255"
            placeholder="选填，展示在列表页的简介"
          />
        </div>
        <div class="meta-cell">
          <label class="meta-label">分类</label>
          <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
        <div class="meta-cell">
          <label class="meta-label">标签</label>
          <el-select
            v-model="form.tagIds"
            multiple
            placeholder="选择标签（可多选）"
            clearable
            style="width: 100%"
          >
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </div>
        <div class="meta-cell">
          <label class="meta-label">封面图</label>
          <div class="cover-row">
            <el-upload :show-file-list="false" :http-request="doUploadCover" accept="image/*">
              <el-button size="small">上传封面</el-button>
            </el-upload>
            <img v-if="form.cover" :src="form.cover" class="cover-preview" alt="封面预览" />
            <el-button v-if="form.cover" link type="danger" size="small" @click="form.cover = ''"
              >移除</el-button
            >
          </div>
        </div>
      </div>
    </div>

    <!-- md-editor-v3：v-model 绑定 Markdown 原文，双栏编辑+预览 -->
    <div class="editor-box ink-card">
      <MdEditor
        v-model="form.contentMd"
        :toolbars="toolbars"
        :on-upload-img="onUploadImg"
        style="height: 560px"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { createArticle, updateArticle, getArticle } from '@/api/article'
import { getCategories } from '@/api/category'
import { getTags } from '@/api/category'
import { uploadFile } from '@/api/upload'
import { summarize as aiSummarizeApi } from '@/api/ai'

const route = useRoute()
const router = useRouter()
const isEdit = !!route.params.id
const form = ref({ title: '', summary: '', categoryId: null, tagIds: [], contentMd: '' })
const summarizing = ref(false)   // AI 生成摘要中
const categories = ref([])
const tags = ref([])

// 编辑器工具栏：常用功能（md-editor-v3 是字符串数组配置）
const toolbars = [
  'bold',
  'underline',
  'italic',
  'strikeThrough',
  'title',
  'quote',
  'unorderedList',
  'orderedList',
  'codeRow',
  'code',
  'link',
  'image',
  'table',
  'revoke',
  'next',
  'preview',
  'fullscreen',
]

async function save(status) {
  if (!form.value.title.trim()) return ElMessage.warning('标题不能为空')
  if (!form.value.contentMd.trim()) return ElMessage.warning('内容不能为空')
  const payload = { ...form.value, status }
  if (isEdit) {
    await updateArticle(route.params.id, payload)
    ElMessage.success('保存成功')
    router.push('/profile')
  } else {
    const id = await createArticle(payload)
    ElMessage.success(status === 1 ? '发布成功' : '已存草稿')
    router.push('/article/' + id)
  }
}

// AI 生成摘要：正文交给大模型，生成 ≤80 字摘要填入表单
async function aiSummarize() {
  if (!form.value.title.trim()) return ElMessage.warning('先生成标题再生成摘要哦')
  if (!form.value.contentMd.trim()) return ElMessage.warning('正文还是空的')
  summarizing.value = true
  try {
    const summary = await aiSummarizeApi({ title: form.value.title, contentMd: form.value.contentMd })
    form.value.summary = summary
    ElMessage.success('摘要已生成，可手动微调')
  } catch {
    /* 拦截器已提示 */
  } finally {
    summarizing.value = false
  }
}

async function doUploadCover(option) {
  const url = await uploadFile(option.file)
  form.value.cover = url
  ElMessage.success('封面上传成功')
}

async function onUploadImg(files, callback) {
  const url = await uploadFile(files[0])
  callback([url])
}

onMounted(async () => {
  categories.value = await getCategories()
  tags.value = await getTags()
  if (isEdit) {
    // 编辑时回填（后端 ArticleDetailVO 有 contentMd/tagIds）
    const detail = await getArticle(route.params.id)
    form.value = {
      title: detail.title,
      summary: detail.summary,
      categoryId: detail.categoryId,
      tagIds: detail.tagIds || [],
      contentMd: detail.contentMd,
    }
  }
})
</script>

<style scoped>
.editor-page {
  max-width: 1100px;
}

.editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: var(--ink-ink);
}
.head-actions {
  display: flex;
  gap: 10px;
}

/* 大标题输入 */
.title-input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-family: inherit;
  font-size: 32px;
  font-weight: 800;
  color: var(--ink-ink);
  letter-spacing: -0.5px;
  padding: 4px 2px 16px;
  border-bottom: 2px solid transparent;
  transition: border-color 0.2s ease;
}
.title-input::placeholder {
  color: var(--ink-faint);
}
.title-input:focus {
  border-bottom-color: var(--ink-primary-200);
}

/* 元信息卡片 */
.editor-meta {
  padding: 18px 20px;
  margin: 18px 0;
  border-radius: var(--ink-radius-sm);
}
.meta-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1.4fr;
  gap: 18px;
}
.meta-label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-faint);
  margin-bottom: 6px;
  letter-spacing: 0.5px;
}

/* 编辑器容器 */
.editor-box {
  padding: 6px;
  border-radius: var(--ink-radius);
  overflow: hidden;
}
.editor-box :deep(.md-editor) {
  border-radius: var(--ink-radius-sm);
}

@media (max-width: 860px) {
  .meta-grid {
    grid-template-columns: 1fr;
  }
}
.cover-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cover-preview {
  width: 72px;
  height: 48px;
  object-fit: cover;
  border-radius: 6px;
}
</style>
