<template>
  <div class="admin-comment">
    <div class="page-head">
      <h2 class="page-title">评论管理</h2>
      <span class="page-sub">共 {{ list.length }} 条评论</span>
    </div>

    <div class="ink-card table-card">
      <el-table :data="list" stripe>
        <el-table-column prop="articleTitle" label="所属文章" min-width="220" show-overflow-tooltip />
        <el-table-column prop="nickname" label="评论人" width="130">
          <template #default="{ row }">
            <span class="nick-cell">
              <span class="mini-avatar">{{ (row.nickname || 'U').slice(0, 1).toUpperCase() }}</span>
              {{ row.nickname }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!list.length" description="暂无评论" :image-size="90" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminComments, deleteCommentByAdmin } from '@/api/admin'

const list = ref([])

async function load() {
  list.value = await getAdminComments()
  list.value.forEach((c) => {
    c.createTime = (c.createTime || '').replace('T', ' ').slice(0, 16)
  })
}
async function del(row) {
  await ElMessageBox.confirm('确定删除这条评论？', '提示', { type: 'warning' })
  await deleteCommentByAdmin(row.id)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 18px;
}
.page-title { margin: 0; font-size: 20px; font-weight: 800; color: var(--ink-ink); }
.page-sub { color: var(--ink-faint); font-size: 13px; }
.table-card { padding: 8px 14px; border-radius: var(--ink-radius); }
.nick-cell { display: inline-flex; align-items: center; gap: 7px; font-weight: 500; }
.mini-avatar {
  width: 24px; height: 24px;
  border-radius: 50%;
  background: var(--ink-deep);
  color: var(--ink-accent-glow);
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>
