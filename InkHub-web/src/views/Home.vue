<template>
  <div class="home">
    <!-- Hero 横幅（全宽） -->
    <div class="hero">
      <div class="hero-inner">
        <div class="eyebrow eyebrow--foam hero-eyebrow">A MARKDOWN KNOWLEDGE COMMUNITY</div>
        <h1 class="hero-title">记录思考，分享洞见</h1>
        <p class="hero-sub">一个属于写作者的 Markdown 知识社区</p>
        <div class="hero-search">
          <svg class="search-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8" />
            <path d="M21 21l-4.35-4.35" />
          </svg>
          <input v-model="keyword" class="search-input" placeholder="搜索你感兴趣的文章…"
                 @keyup.enter="load(1)" />
          <el-button class="search-btn ink-gradient-btn" @click="load(1)">搜索</el-button>
        </div>
        <div class="hero-sort">
          <button class="sort-chip" :class="{ active: sort === 'latest' }" @click="changeSort('latest')">最新</button>
          <button class="sort-chip" :class="{ active: sort === 'hot' }" @click="changeSort('hot')">最热</button>
          <el-button class="hero-write ink-gradient-btn" round @click="goWrite">开始写作</el-button>
        </div>
      </div>
    </div>

    <!-- 宽屏内容区 -->
    <div class="body home-body">
      <!-- 左侧分类 -->
      <aside class="side">
        <div class="side-title">分类</div>
        <div class="cat-list">
          <div class="cat-item" :class="{ active: !categoryId }" @click="selectCat(null)">
            <span class="cat-name">全部文章</span>
            <span class="cat-count">{{ total }}</span>
          </div>
          <div v-for="c in categories" :key="c.id" class="cat-item"
               :class="{ active: categoryId === c.id }" @click="selectCat(c.id)">
            <span class="cat-name">{{ c.name }}</span>
            <span class="cat-count">{{ c.articleCount }}</span>
          </div>
        </div>
      </aside>

      <!-- 文章卡片网格 -->
      <div class="list">
        <div v-if="articles.length" class="feed">
          <article v-for="a in articles" :key="a.id" class="article-card ink-card ink-card--hover" @click="goDetail(a.id)">
            <!-- 封面：有图显示图，无图渐变色块 -->
            <div class="cover" :class="{ 'cover--placeholder': !a.cover }">
              <img v-if="a.cover" :src="a.cover" :alt="a.title" loading="lazy" />
              <span v-else class="cover-fallback">{{ (a.title || 'I').slice(0, 1) }}</span>
            </div>
            <div class="card-body">
              <div class="card-top">
                <el-tag v-if="a.top" size="small" class="top-tag" effect="dark">置顶</el-tag>
                <h3 class="title">{{ a.title }}</h3>
              </div>
              <p class="summary">{{ a.summary || '作者还没写摘要，点进来看看吧～' }}</p>
              <div class="meta">
                <span class="meta-item author">
                  <span class="author-avatar">{{ (a.authorName || 'U').slice(0, 1).toUpperCase() }}</span>
                  {{ a.authorName }}
                </span>
                <span v-if="a.categoryName" class="meta-item">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.83z" />
                    <line x1="7" y1="7" x2="7.01" y2="7" />
                  </svg>
                  {{ a.categoryName }}
                </span>
                <span class="meta-item stats">
                  <span class="stat"><svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>{{ a.viewCount }}</span>
                  <span class="stat"><svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3" /></svg>{{ a.likeCount }}</span>
                  <span class="stat"><svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>{{ a.commentCount }}</span>
                </span>
              </div>
              <div v-if="a.tags?.length" class="tags">
                <span v-for="t in a.tags" :key="t" class="tag"># {{ t }}</span>
              </div>
            </div>
          </article>
        </div>

        <el-empty v-else description="暂无文章，去写第一篇吧！" :image-size="120" />

        <div v-if="total > pageSize" class="pager">
          <el-pagination background layout="prev, pager, next" :total="total"
                         :page-size="pageSize" :current-page="pageNum" @current-change="load" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getArticles, semanticSearch } from '@/api/article'
import { getCategories } from '@/api/category'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const articles = ref([])
const categories = ref([])
const keyword = ref('')
const sort = ref('latest')
const categoryId = ref(null)
const pageNum = ref(1)
const pageSize = 12
const total = ref(0)

async function load(page = 1) {
  // AI 语义搜索：有关键词时用向量召回（更懂"意思"），无关键词走原列表
  if (keyword.value && keyword.value.trim()) {
    const list = await semanticSearch(keyword.value.trim())
    articles.value = list
    total.value = list.length
    return
  }
  pageNum.value = page
  const data = await getArticles({
    pageNum: page, pageSize,
    sort: sort.value,
    categoryId: categoryId.value,
    keyword: keyword.value || undefined,
  })
  articles.value = data.records
  total.value = data.total
}
function changeSort(s) {
  sort.value = s
  load(1)
}
function selectCat(id) {
  categoryId.value = id
  load(1)
}
function goDetail(id) {
  router.push('/article/' + id)
}
function goWrite() {
  // 未登录先去登录页，登录成功后 redirect 回编辑器
  if (userStore.isLogin) router.push('/editor')
  else router.push({ path: '/login', query: { redirect: '/editor' } })
}
onMounted(async () => {
  load(1)
  categories.value = await getCategories()
})
</script>

<style scoped>
/* Hero 全宽横幅 */
.hero {
  margin: -40px 0 48px;
  padding: 96px 0 76px;
  color: var(--ink-foam);
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(1000px 460px at 50% -30%, rgba(90, 209, 230, 0.16), transparent 62%),
    radial-gradient(700px 420px at 88% 8%, rgba(29, 159, 192, 0.14), transparent 60%),
    linear-gradient(180deg, #0a1725 0%, #0d1f31 55%, #14304a 100%);
}
/* 水下光束 + 细颗粒 */
.hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), transparent 40%),
    radial-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size:
    100% 100%,
    22px 22px;
  pointer-events: none;
}
.hero-inner {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 720px;
  margin: 0 auto;
  padding: 0 24px;
}
.hero-eyebrow {
  margin-bottom: 18px;
}
.hero-title {
  font-family: var(--ink-serif);
  margin: 0 0 14px;
  font-size: 48px;
  font-weight: 600;
  letter-spacing: 0.01em;
  color: #f2f9fc;
}
.hero-sub {
  margin: 0 0 36px;
  color: var(--ink-foam-dim);
  font-size: 16px;
  letter-spacing: 0.02em;
}

.hero-search {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 999px;
  padding: 7px 7px 7px 20px;
  backdrop-filter: blur(8px);
  transition: all 0.25s ease;
}
.hero-search:focus-within {
  background: rgba(90, 209, 230, 0.1);
  border-color: rgba(90, 209, 230, 0.55);
  box-shadow: 0 8px 32px rgba(8, 18, 29, 0.35);
}
.search-icon { color: rgba(255, 255, 255, 0.7); flex-shrink: 0; }
.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: #fff;
  font-size: 15px;
  font-family: inherit;
}
.search-input::placeholder { color: rgba(255, 255, 255, 0.55); }
.search-btn { flex-shrink: 0; border-radius: 999px !important; padding: 9px 24px !important; }

.hero-sort { margin-top: 24px; display: flex; gap: 10px; justify-content: center; }
.sort-chip {
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
  border-radius: 999px;
  padding: 7px 20px;
  font-size: 13.5px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}
.sort-chip:hover { background: rgba(255, 255, 255, 0.14); }
.sort-chip.active {
  background: var(--ink-accent-glow);
  color: var(--ink-abyss);
  font-weight: 600;
  border-color: var(--ink-accent-glow);
}

/* 深海面上，主按钮要用那抹青色，否则深色按钮会看不见 */
.search-btn,
.hero-write {
  background: var(--ink-accent-glow) !important;
  color: var(--ink-abyss) !important;
  box-shadow: 0 4px 16px rgba(90, 209, 230, 0.3);
}

.hero-write {
  margin-left: 6px;
  padding: 9px 24px !important;
  border-radius: 999px !important;
}

/* 宽屏主体 */
.home-body {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 24px;
}
.body { display: flex; gap: 24px; align-items: flex-start; }

/* 分类侧栏 */
.side {
  width: 200px;
  flex-shrink: 0;
  background: var(--ink-card);
  border: 1px solid var(--ink-border-light);
  border-radius: var(--ink-radius);
  box-shadow: var(--ink-shadow-sm);
  padding: 14px 10px;
  position: sticky;
  top: 84px;
}
.side-title { font-size: 13px; font-weight: 700; color: var(--ink-faint); padding: 4px 10px 10px; letter-spacing: 1px; }
.cat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 10px;
  border-radius: var(--ink-radius-sm);
  cursor: pointer;
  font-size: 14px;
  color: var(--ink-text);
  transition: all 0.18s ease;
  margin-bottom: 2px;
}
.cat-item:hover { background: var(--ink-bg); color: var(--ink-primary); }
.cat-item.active { background: var(--ink-primary-50); color: var(--ink-primary); font-weight: 600; }
.cat-count {
  font-size: 11.5px;
  background: var(--ink-bg);
  color: var(--ink-faint);
  border-radius: 999px;
  padding: 1px 8px;
  min-width: 24px;
  text-align: center;
  transition: all 0.18s ease;
}
.cat-item.active .cat-count { background: var(--ink-primary); color: #fff; }

/* 文章卡片网格 */
.list { flex: 1; min-width: 0; }
.feed {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.article-card {
  padding: 0;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.cover {
  height: 168px;
  flex-shrink: 0;
  overflow: hidden;
  background: linear-gradient(135deg, #eef4f7, #e2edf2);
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}
.article-card:hover .cover img { transform: scale(1.05); }
.cover--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(240px 120px at 30% -30%, rgba(90, 209, 230, 0.16), transparent 65%),
    linear-gradient(180deg, #0d1f31, #14304a);
}
.cover-fallback {
  font-family: var(--ink-serif);
  font-size: 46px;
  font-weight: 600;
  color: rgba(211, 227, 238, 0.85);
}
.card-body {
  padding: 18px 20px 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.card-top { display: flex; align-items: center; gap: 10px; }
.top-tag { flex-shrink: 0; border: none !important; }
.title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--ink-ink);
  letter-spacing: -0.2px;
  transition: color 0.2s ease;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.article-card:hover .title { color: var(--ink-primary); }

.summary {
  margin: 10px 0 14px;
  color: var(--ink-muted);
  font-size: 13.5px;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.meta {
  display: flex;
  align-items: center;
  gap: 16px;
  color: var(--ink-faint);
  font-size: 13px;
  flex-wrap: wrap;
}
.meta-item { display: inline-flex; align-items: center; gap: 5px; }
.author { color: var(--ink-muted); font-weight: 500; }
.author-avatar {
  width: 22px; height: 22px;
  border-radius: 50%;
  background: var(--ink-deep);
  color: var(--ink-accent-glow);
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.stats { margin-left: auto; display: inline-flex; gap: 14px; }
.stat { display: inline-flex; align-items: center; gap: 4px; }

.tags { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
.tag {
  font-size: 12px;
  color: var(--ink-primary);
  background: var(--ink-primary-50);
  border-radius: 999px;
  padding: 2px 10px;
}

.pager { display: flex; justify-content: center; margin-top: 32px; }

@media (max-width: 1024px) {
  .hero-title { font-size: 38px; }
}
@media (max-width: 860px) {
  .body { flex-direction: column; }
  .side { width: 100%; position: static; }
  .hero { padding: 64px 0 52px; }
  .hero-title { font-size: 30px; }
  .home-body { padding: 0 16px; }
}
</style>
