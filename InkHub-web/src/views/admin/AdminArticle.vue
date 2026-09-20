<template>
  <div class="admin-article">
    <div class="page-head">
      <h2 class="page-title">文章管理</h2>
      <el-radio-group v-model="status" size="default" @change="load">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">草稿</el-radio-button>
        <el-radio-button :value="1">已发布</el-radio-button>
        <el-radio-button :value="2">已下架</el-radio-button>
      </el-radio-group>
    </div>

    <div class="ink-card table-card">
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type" effect="light" round size="small">
              {{ statusMap[row.status]?.text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.top === 1" type="danger" effect="dark" size="small">置顶</el-tag>
            <span v-else class="dim">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" align="center" />
        <el-table-column label="操作" width="270" align="right">
          <template #default="{ row }">
            <el-button link type="warning" @click="toggleTop(row)">{{ row.top === 1 ? '取消置顶' : '置顶' }}</el-button>
            <el-button v-if="row.status === 1" link type="danger" @click="off(row)">下架</el-button>
            <el-button v-else-if="row.status === 2" link type="success" @click="on(row)">恢复</el-button>
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!list.length" description="暂无文章" :image-size="90" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminArticles, offArticle, onArticle, topArticle, deleteArticleByAdmin } from '@/api/admin'

const list = ref([])
const status = ref(null)
const statusMap = {
  0: { text: '草稿', type: 'info' },
  1: { text: '已发布', type: 'success' },
  2: { text: '已下架', type: 'danger' },
}

async function load() {
  list.value = await getAdminArticles(status.value)
}
async function toggleTop(row) {
  await topArticle(row.id, row.top === 1 ? false : true)
  ElMessage.success('操作成功')
  load()
}
async function off(row) {
  await ElMessageBox.confirm(`下架《${row.title}》？`, '提示', { type: 'warning' })
  await offArticle(row.id)
  ElMessage.success('已下架')
  load()
}
async function on(row) {
  await onArticle(row.id)
  ElMessage.success('已恢复发布')
  load()
}
async function del(row) {
  await ElMessageBox.confirm(`确定删除《${row.title}》？删除后不可恢复`, '警告', { type: 'error' })
  await deleteArticleByAdmin(row.id)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.page-title { margin: 0; font-size: 20px; font-weight: 800; color: var(--ink-ink); }
.table-card { padding: 8px 14px; border-radius: var(--ink-radius); }
.dim { color: var(--ink-faint); }
</style>
