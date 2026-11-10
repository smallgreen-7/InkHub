<template>
  <div class="forgot-page">
    <div class="bg-decor">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
    </div>

    <div class="forgot-card ink-card">
      <div class="brand">
        <span class="logo-mark">
          <svg
            viewBox="0 0 24 24"
            width="26"
            height="26"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          </svg>
        </span>
        <h2 class="logo">Ink<em>Hub</em></h2>
        <p class="slogan">通过注册邮箱重置密码</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="注册邮箱" size="large" />
        </el-form-item>

        <el-form-item prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="6 位验证码" size="large" maxlength="6" />
            <el-button size="large" :disabled="countdown > 0" :loading="sending" @click="sendCode">
              {{ countdown > 0 ? countdown + 's 后重发' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="新密码（6-20 位）" size="large" />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="确认新密码"
            size="large"
            @keyup.enter="doReset"
          />
        </el-form-item>

        <el-button
          type="primary"
          class="submit ink-gradient-btn"
          size="large"
          :loading="loading"
          @click="doReset"
        >
          重置密码
        </el-button>
      </el-form>

      <div class="foot">
        <el-button link type="primary" @click="$router.push('/login')">返回登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendResetCode, resetPassword } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)

const form = reactive({ email: '', code: '', newPassword: '', confirmPassword: '' })

const rules = {
  email: [
    { required: true, message: '请输入注册邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为 6 位数字', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度 6-20 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

function startCountdown() {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function sendCode() {
  if (!form.email) return ElMessage.warning('请先填写邮箱')
  sending.value = true
  try {
    await sendResetCode({ email: form.email })
    ElMessage.success('如果该邮箱已注册，验证码已发送（5 分钟内有效）')
    startCountdown()
  } finally {
    sending.value = false
  }
}

async function doReset() {
  await formRef.value.validate()
  loading.value = true
  try {
    await resetPassword({ email: form.email, code: form.code, newPassword: form.newPassword })
    ElMessage.success('密码已重置，请用新密码登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forgot-page {
  min-height: calc(100vh - 200px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
  overflow: hidden;
}
.bg-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.35;
}
.blob-1 {
  width: 320px;
  height: 320px;
  background: var(--ink-accent-glow);
  top: -60px;
  left: 8%;
}
.blob-2 {
  width: 280px;
  height: 280px;
  background: var(--ink-navy);
  bottom: -40px;
  right: 10%;
}
.forgot-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  padding: 34px 30px 26px;
}
.brand {
  text-align: center;
  margin-bottom: 22px;
}
.logo-mark {
  display: inline-flex;
  width: 46px;
  height: 46px;
  align-items: center;
  justify-content: center;
  background: var(--ink-accent-glow);
  color: var(--ink-abyss);
  border-radius: 13px;
  box-shadow: 0 0 22px rgba(90, 209, 230, 0.35);
}
.logo {
  margin: 10px 0 4px;
  font-size: 24px;
  font-family: var(--ink-serif);
}
.logo em {
  font-style: normal;
  color: var(--ink-accent);
}
.slogan {
  margin: 0;
  font-size: 13px;
  color: var(--ink-muted);
}
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.submit {
  width: 100%;
  margin-top: 4px;
}
.foot {
  margin-top: 14px;
  text-align: center;
}
</style>
