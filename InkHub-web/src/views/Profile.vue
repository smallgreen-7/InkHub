<template>
  <div class="profile-page">
    <div class="profile-layout">
      <!-- 左侧：个人卡 -->
      <aside class="profile-side ink-card">
        <el-avatar
          :size="96"
          class="side-avatar"
          :src="userStore.userInfo?.avatar"
        >{{ initial }}</el-avatar>
        <h2 class="side-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</h2>
        <p class="side-username">@{{ userStore.userInfo?.username }}</p>
        <div class="side-stats">
          <div class="stat">
            <span class="num">{{ myArticles.length }}</span>
            <span class="label">文章</span>
          </div>
          <div class="stat">
            <span class="num">{{ myFavorites.length }}</span>
            <span class="label">收藏</span>
          </div>
        </div>
        <el-button type="primary" class="ink-gradient-btn side-edit" @click="openEdit">
          编辑资料
        </el-button>
      </aside>

      <!-- 右侧：内容区 -->
      <main class="profile-main">
        <el-tabs v-model="tab" class="profile-tabs">
          <el-tab-pane label="我的文章" name="mine">
            <div v-if="myArticles.length" class="ink-card table-card">
              <el-table :data="myArticles" stripe>
                <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="statusMap[row.status]?.type" effect="light" round size="small">
                      {{ statusMap[row.status]?.text }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="$router.push('/editor/' + row.id)"
                      >编辑</el-button
                    >
                    <el-button link type="danger" @click="del(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <el-empty
              v-else
              description="还没有文章，去写第一篇吧～"
              :image-size="90"
            />
          </el-tab-pane>

          <el-tab-pane label="我的收藏" name="fav">
            <div v-if="myFavorites.length" class="fav-grid">
              <div
                v-for="a in myFavorites"
                :key="a.id"
                class="fav-card ink-card ink-card--hover"
                @click="$router.push('/article/' + a.id)"
              >
                <h4 class="fav-title">{{ a.title }}</h4>
                <p class="fav-meta">
                  <span>{{ a.authorName || '作者' }}</span>
                  <span v-if="a.categoryName">· {{ a.categoryName }}</span>
                </p>
              </div>
            </div>
            <el-empty v-else description="还没有收藏任何文章" :image-size="90" />
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>

    <!-- 编辑资料弹窗：头像 + 昵称 -->
    <el-dialog v-model="editDialog.show" title="编辑资料" width="400px" align-center>
      <el-form label-width="60px">
        <el-form-item label="头像">
          <el-upload
            :show-file-list="false"
            :http-request="chooseAvatar"
            accept="image/*"
            class="avatar-upload"
          >
            <el-avatar :size="64" class="edit-avatar" :src="editDialog.avatar">
              {{ initial }}
            </el-avatar>
          </el-upload>
          <div class="avatar-tip">点击头像选择新图片（jpg/png/gif/webp，≤5MB）</div>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editDialog.nickname" maxlength="20" placeholder="请输入昵称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.show = false">取消</el-button>
        <el-button type="primary" class="ink-gradient-btn" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyArticles, deleteArticle, getMyFavorites } from '@/api/article'
import { useUserStore } from '@/stores/user'
import { uploadFile } from '@/api/upload'
import { updateProfile } from '@/api/user'

const userStore = useUserStore()
const tab = ref('mine')
const myArticles = ref([])
const myFavorites = ref([])
const statusMap = {
  0: { text: '草稿', type: 'info' },
  1: { text: '已发布', type: 'success' },
  2: { text: '已下架', type: 'danger' },
}
const editDialog = reactive({ show: false, nickname: '', avatar: null })

const initial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || 'U'
  return name.slice(0, 1).toUpperCase()
})

function openEdit() {
  // 回填当前资料
  editDialog.nickname = userStore.userInfo?.nickname || ''
  editDialog.avatar = userStore.userInfo?.avatar || null
  editDialog.show = true
}

// 弹窗里选头像：先上传拿 url，存到 editDialog，保存时一起提交
async function chooseAvatar(option) {
  try {
    const url = await uploadFile(option.file)
    editDialog.avatar = url
    ElMessage.success('头像已选择，点保存生效')
  } catch (e) {
    ElMessage.error('头像上传失败')
  }
}

async function del(row) {
  await ElMessageBox.confirm('确定删除《' + row.title + '》？', '提示', { type: 'warning' })
  await deleteArticle(row.id)
  ElMessage.success('已删除')
  load()
}
async function load() {
  const data = await getMyArticles(1, 50)
  myArticles.value = data.records
  myFavorites.value = await getMyFavorites()
}

async function saveProfile() {
  if (!editDialog.nickname.trim()) return ElMessage.warning('昵称不能为空')
  const payload = { nickname: editDialog.nickname.trim() }
  // 头像有变化才提交（没动头像就不带 avatar 字段）
  const avatarChanged =
    editDialog.avatar && editDialog.avatar !== userStore.userInfo?.avatar
  if (avatarChanged) payload.avatar = editDialog.avatar
  await updateProfile(payload)
  userStore.updateInfo(payload)
  ElMessage.success('保存成功')
  editDialog.show = false
}
onMounted(load)
</script>

<style scoped>
.profile-page {
  max-width: 1080px;
}

/* 左右分栏 */
.profile-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* 左侧个人卡 */
.profile-side {
  width: 260px;
  flex-shrink: 0;
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: sticky;
  top: 84px;
}
.side-avatar {
  background: var(--ink-deep);
  font-size: 34px;
  font-weight: 700;
  color: var(--ink-accent-glow);
  box-shadow: var(--ink-shadow);
}
.side-name {
  margin: 16px 0 0;
  font-size: 21px;
  font-weight: 800;
  color: var(--ink-ink);
}
.side-username {
  margin: 4px 0 0;
  color: var(--ink-faint);
  font-size: 13.5px;
}
.side-stats {
  display: flex;
  gap: 36px;
  margin: 22px 0;
}
.stat {
  text-align: center;
}
.num {
  display: block;
  font-size: 22px;
  font-weight: 800;
  color: var(--ink-ink);
}
.label {
  font-size: 12.5px;
  color: var(--ink-faint);
}
.side-edit {
  width: 100%;
}

/* 右侧内容区 */
.profile-main {
  flex: 1;
  min-width: 0;
}

/* Tabs */
.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--ink-border-light);
}
.profile-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
}
.profile-tabs :deep(.el-tabs__active-bar) {
  background: var(--ink-primary);
}

.table-card {
  padding: 8px 14px;
  border-radius: var(--ink-radius);
}

/* 收藏卡片流 */
.fav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.fav-card {
  padding: 20px;
  cursor: pointer;
}
.fav-title {
  margin: 0 0 10px;
  font-size: 15.5px;
  font-weight: 700;
  color: var(--ink-ink);
  line-height: 1.5;
}
.fav-meta {
  margin: 0;
  color: var(--ink-faint);
  font-size: 12.5px;
}

/* 编辑弹窗头像 */
.edit-avatar {
  background: var(--ink-deep);
  font-size: 22px;
  font-weight: 700;
  color: var(--ink-accent-glow);
  cursor: pointer;
}
.avatar-tip {
  font-size: 12px;
  color: var(--ink-faint);
  margin-top: 6px;
}

@media (max-width: 768px) {
  .profile-layout {
    flex-direction: column;
  }
  .profile-side {
    width: 100%;
    position: static;
  }
}
</style>
