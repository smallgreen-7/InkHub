<template>
  <div class="settings-page page-container">
    <h1 class="page-title">账号设置</h1>
    <p class="page-sub">管理你的资料、密码、通知偏好与界面外观</p>

    <div class="settings-layout">
      <!-- 左侧：用户概览 -->
      <aside class="settings-side ink-card">
        <el-avatar :size="88" class="side-avatar" :src="profile.avatar">{{ initial }}</el-avatar>
        <h2 class="side-name">{{ profile.nickname || profile.username }}</h2>
        <p class="side-username">@{{ profile.username }}</p>
        <el-tag :type="profile.role === 2 ? 'warning' : 'info'" effect="light" round size="small">
          {{ profile.role === 2 ? '管理员' : '普通用户' }}
        </el-tag>
        <div class="side-links">
          <el-button link type="primary" @click="$router.push('/profile')">我的文章与收藏</el-button>
        </div>
      </aside>

      <!-- 右侧：设置面板 -->
      <main class="settings-main ink-card">
        <el-tabs v-model="tab">
          <!-- ================= 基本资料 ================= -->
          <el-tab-pane label="基本资料" name="basic">
            <el-form label-width="88px" class="settings-form">
              <el-form-item label="头像">
                <el-upload
                  :show-file-list="false"
                  :http-request="chooseAvatar"
                  accept="image/*"
                  class="avatar-upload"
                >
                  <el-avatar :size="64" class="edit-avatar" :src="profile.avatar">
                    {{ initial }}
                  </el-avatar>
                </el-upload>
                <div class="hint">点击头像上传新图片（jpg/png/gif/webp，≤5MB）</div>
              </el-form-item>

              <el-form-item label="昵称">
                <el-input v-model="profile.nickname" maxlength="20" placeholder="最多 20 字" />
              </el-form-item>

              <el-form-item label="邮箱">
                <el-input v-model="profile.email" placeholder="用于找回密码，留空表示解绑" />
              </el-form-item>

              <el-form-item label="用户名">
                <div class="inline-row">
                  <el-input :model-value="profile.username" disabled />
                  <el-button @click="openUsername">修改</el-button>
                </div>
                <div class="hint">用户名是登录名，修改后下次登录请使用新用户名</div>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" class="ink-gradient-btn" :loading="saving" @click="saveProfile">
                  保存资料
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- ================= 密码与安全 ================= -->
          <el-tab-pane label="密码与安全" name="security">
            <el-form
              ref="pwdFormRef"
              :model="pwdForm"
              :rules="pwdRules"
              label-width="88px"
              class="settings-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-20 位" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" class="ink-gradient-btn" :loading="pwdSaving" @click="savePassword">
                  修改密码
                </el-button>
                <span class="hint inline">修改成功后需要重新登录</span>
              </el-form-item>
            </el-form>

            <el-divider />

            <div class="danger-zone">
              <div>
                <h4 class="danger-title">注销账号</h4>
                <p class="danger-desc">
                  注销后账号将被禁用（软删除），无法登录；已发布的文章与评论仍会保留。
                </p>
              </div>
              <el-button type="danger" plain @click="doDeactivate">注销账号</el-button>
            </div>
          </el-tab-pane>

          <!-- ================= 通知偏好 ================= -->
          <el-tab-pane label="通知偏好" name="notify">
            <div class="notify-list">
              <div class="notify-item">
                <div>
                  <h4>评论 / 回复</h4>
                  <p class="hint">有人回复你的评论时通知我</p>
                </div>
                <el-switch v-model="notify.notifyComment" :active-value="1" :inactive-value="0" @change="saveNotify" />
              </div>
              <div class="notify-item">
                <div>
                  <h4>点赞</h4>
                  <p class="hint">有人点赞你的文章时通知我</p>
                </div>
                <el-switch v-model="notify.notifyLike" :active-value="1" :inactive-value="0" @change="saveNotify" />
              </div>
              <div class="notify-item">
                <div>
                  <h4>收藏</h4>
                  <p class="hint">有人收藏你的文章时通知我</p>
                </div>
                <el-switch
                  v-model="notify.notifyFavorite"
                  :active-value="1"
                  :inactive-value="0"
                  @change="saveNotify"
                />
              </div>
            </div>
            <p class="hint">关闭后只是不再收到新的站内通知，已有通知不会被删除。</p>
          </el-tab-pane>

          <!-- ================= 外观 ================= -->
          <el-tab-pane label="外观" name="theme">
            <div class="notify-item">
              <div>
                <h4>深色模式</h4>
                <p class="hint">夜间阅读更省眼，设置保存在本机浏览器</p>
              </div>
              <el-switch :model-value="isDark" @change="onThemeChange" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>

    <!-- 修改用户名弹窗 -->
    <el-dialog v-model="usernameDialog.show" title="修改用户名" width="380px" align-center>
      <el-form label-width="76px">
        <el-form-item label="新用户名">
          <el-input v-model="usernameDialog.username" maxlength="20" placeholder="3-20 位字母/数字/下划线" />
        </el-form-item>
        <el-form-item label="当前密码">
          <el-input v-model="usernameDialog.password" type="password" show-password placeholder="确认身份" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="usernameDialog.show = false">取消</el-button>
        <el-button type="primary" class="ink-gradient-btn" :loading="usernameDialog.loading" @click="saveUsername">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getMe, updateProfile, updateUsername, changePassword, deactivateAccount, getNotifyPref, updateNotifyPref } from '@/api/user'
import { uploadFile } from '@/api/upload'
import { getTheme, toggleTheme } from '@/utils/theme'

const router = useRouter()
const userStore = useUserStore()

const tab = ref('basic')
const saving = ref(false)
const pwdSaving = ref(false)
const pwdFormRef = ref()

const profile = reactive({ username: '', nickname: '', avatar: '', email: '', role: 1 })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const notify = reactive({ notifyComment: 1, notifyLike: 1, notifyFavorite: 1 })
const usernameDialog = reactive({ show: false, username: '', password: '', loading: false })
const isDark = ref(getTheme() === 'dark')

const initial = computed(() => {
  const name = profile.nickname || profile.username || 'U'
  return name.slice(0, 1).toUpperCase()
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度 6-20 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

onMounted(async () => {
  const me = await getMe()
  Object.assign(profile, {
    username: me.username,
    nickname: me.nickname || '',
    avatar: me.avatar || '',
    email: me.email || '',
    role: me.role,
  })
  const pref = await getNotifyPref()
  Object.assign(notify, pref)
})

// ---------- 基本资料 ----------
async function chooseAvatar(option) {
  try {
    profile.avatar = await uploadFile(option.file)
    ElMessage.success('头像已上传，别忘了点保存')
  } catch {
    ElMessage.error('头像上传失败')
  }
}

async function saveProfile() {
  if (!profile.nickname.trim()) return ElMessage.warning('昵称不能为空')
  saving.value = true
  try {
    const payload = { nickname: profile.nickname.trim(), avatar: profile.avatar, email: profile.email.trim() }
    await updateProfile(payload)
    userStore.updateInfo(payload)
    ElMessage.success('资料已保存')
  } finally {
    saving.value = false
  }
}

function openUsername() {
  usernameDialog.username = ''
  usernameDialog.password = ''
  usernameDialog.show = true
}

async function saveUsername() {
  const name = usernameDialog.username.trim()
  if (name.length < 3 || name.length > 20) return ElMessage.warning('用户名长度 3-20 位')
  if (!/^[A-Za-z0-9_]+$/.test(name)) return ElMessage.warning('只能包含字母、数字、下划线')
  if (!usernameDialog.password) return ElMessage.warning('请输入当前密码')
  usernameDialog.loading = true
  try {
    await updateUsername({ username: name, password: usernameDialog.password })
    profile.username = name
    userStore.updateInfo({ username: name })
    ElMessage.success('用户名已修改，下次登录请用新用户名')
    usernameDialog.show = false
  } finally {
    usernameDialog.loading = false
  }
}

// ---------- 密码 ----------
async function savePassword() {
  await pwdFormRef.value.validate()
  pwdSaving.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    userStore.logout()
    router.push('/login')
  } finally {
    pwdSaving.value = false
  }
}

// ---------- 注销 ----------
async function doDeactivate() {
  const { value } = await ElMessageBox.prompt(
    '注销后该账号将无法登录，此操作不可自助恢复。请输入当前密码确认：',
    '注销账号',
    { confirmButtonText: '确认注销', cancelButtonText: '取消', inputType: 'password' },
  )
  await deactivateAccount({ password: value })
  ElMessage.success('账号已注销')
  userStore.logout()
  router.push('/')
}

// ---------- 通知偏好 ----------
async function saveNotify() {
  await updateNotifyPref({
    notifyComment: notify.notifyComment,
    notifyLike: notify.notifyLike,
    notifyFavorite: notify.notifyFavorite,
  })
  ElMessage.success('通知偏好已保存')
}

// ---------- 外观 ----------
function onThemeChange() {
  isDark.value = toggleTheme() === 'dark'
}
</script>

<style scoped>
.settings-page {
  max-width: 1040px;
}
.page-title {
  margin: 0 0 4px;
  font-size: 26px;
}
.page-sub {
  margin: 0 0 20px;
  color: var(--ink-muted);
  font-size: 14px;
}
.settings-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.settings-side {
  width: 240px;
  flex-shrink: 0;
  padding: 28px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
  position: sticky;
  top: 84px;
}
.side-avatar {
  background: var(--ink-deep);
  color: var(--ink-accent-glow);
  font-size: 30px;
  font-weight: 700;
}
.side-name {
  margin: 6px 0 0;
  font-size: 19px;
}
.side-username {
  margin: 0;
  font-size: 13px;
  color: var(--ink-muted);
}
.side-links {
  margin-top: 8px;
}
.settings-main {
  flex: 1;
  min-width: 0;
  padding: 8px 24px 24px;
}
.settings-form {
  max-width: 560px;
  padding-top: 8px;
}
.inline-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.avatar-upload :deep(.el-upload) {
  border-radius: 50%;
}
.edit-avatar {
  background: var(--ink-deep);
  color: var(--ink-accent-glow);
  font-weight: 700;
  cursor: pointer;
}
.hint {
  font-size: 12px;
  color: var(--ink-faint);
  line-height: 1.6;
  margin-top: 4px;
}
.hint.inline {
  margin-left: 12px;
  margin-top: 0;
}
.danger-zone {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 18px;
  border: 1px solid var(--ink-border);
  border-radius: var(--ink-radius-sm);
  max-width: 640px;
}
.danger-title {
  margin: 0 0 4px;
  font-size: 15px;
  color: var(--ink-danger);
}
.danger-desc {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-muted);
  line-height: 1.6;
}
.notify-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 640px;
}
.notify-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--ink-border-light);
}
.notify-item h4 {
  margin: 0 0 2px;
  font-size: 15px;
}
.notify-item p {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-muted);
}
</style>
