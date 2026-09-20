<template>
  <div class="login-page">
    <div class="bg-decor">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
    </div>

    <div class="login-card ink-card">
      <div class="brand">
        <span class="logo-mark">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          </svg>
        </span>
        <h2 class="logo">Ink<em>Hub</em></h2>
        <p class="slogan">欢迎回来，继续书写你的思考</p>
      </div>

      <el-tabs v-model="tab" class="auth-tabs" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" size="large">
                <template #prefix>
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" /></svg>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password @keyup.enter="doLogin">
                <template #prefix>
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></svg>
                </template>
              </el-input>
            </el-form-item>
            <el-button type="primary" class="submit ink-gradient-btn" size="large" :loading="loading" @click="doLogin">登 录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form ref="regFormRef" :model="regForm" :rules="regRules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="regForm.username" placeholder="用户名（3-20 位）" size="large" />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="regForm.nickname" placeholder="昵称（选填）" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="regForm.password" type="password" placeholder="密码（6-20 位）" size="large" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="regForm.confirmPassword" type="password" placeholder="确认密码" size="large" show-password />
            </el-form-item>
            <el-button type="primary" class="submit ink-gradient-btn" size="large" :loading="loading" @click="doRegister">注 册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const tab = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const regFormRef = ref()

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', nickname: '', password: '', confirmPassword: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const regRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度 3-20 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度 6-20 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== regForm.password) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function doLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    const data = await login(loginForm)
    userStore.setLogin(data.token, data)
    ElMessage.success('登录成功')
    // 管理员进后台，普通用户回首页（或回跳转前页面）
    if (data.role === 2) router.push('/admin')
    else router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  await regFormRef.value.validate()
  loading.value = true
  try {
    await register({ username: regForm.username, nickname: regForm.nickname, password: regForm.password })
    ElMessage.success('注册成功，自动登录')
    // 注册成功后直接调登录接口
    const data = await login({ username: regForm.username, password: regForm.password })
    userStore.setLogin(data.token, data)
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 40px 16px;
  background:
    radial-gradient(700px 400px at 80% -10%, rgba(90, 209, 230, 0.14), transparent 60%),
    radial-gradient(600px 360px at 10% 110%, rgba(20, 48, 74, 0.1), transparent 55%),
    var(--ink-bg);
}

/* 漂浮光斑 */
.bg-decor { position: absolute; inset: 0; pointer-events: none; }
.blob { position: absolute; border-radius: 50%; filter: blur(70px); opacity: 0.45; }
.blob-1 { width: 340px; height: 340px; background: #7fc7d8; top: -80px; right: -60px; }
.blob-2 { width: 300px; height: 300px; background: #14304a; bottom: -100px; left: -40px; opacity: 0.28; }
.blob-3 { width: 200px; height: 200px; background: #a9dbe8; top: 40%; left: 60%; }

.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  max-width: 100%;
  padding: 38px 36px 30px;
  border-radius: 18px;
  box-shadow: var(--ink-shadow-lg);
}

.brand { text-align: center; margin-bottom: 20px; }
.logo-mark {
  display: inline-flex;
  width: 52px; height: 52px;
  align-items: center; justify-content: center;
  background: var(--ink-accent-glow);
  color: var(--ink-abyss);
  border-radius: 14px;
  box-shadow: 0 0 22px rgba(90, 209, 230, 0.35);
  margin-bottom: 12px;
}
.logo { margin: 0; font-family: var(--ink-serif); font-size: 27px; font-weight: 600; color: var(--ink-ink); letter-spacing: 0.01em; }
.logo em {
  font-style: normal;
  color: var(--ink-accent);
}
.slogan { margin: 8px 0 0; color: var(--ink-faint); font-size: 13.5px; }

.auth-tabs :deep(.el-tabs__nav-wrap::after) { height: 1px; background: var(--ink-border-light); }
.auth-tabs :deep(.el-tabs__item) { font-size: 15px; }
.auth-tabs :deep(.el-tabs__active-bar) { background: var(--ink-primary); }

.submit {
  width: 100%;
  margin-top: 6px;
  border-radius: 10px !important;
  font-size: 15.5px;
  letter-spacing: 4px;
}
</style>
