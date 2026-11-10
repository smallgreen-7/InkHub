import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'   // 官方深色变量（配合 html.dark 使用）
import zhCn from 'element-plus/es/locale/lang/zh-cn'   // 中文
import 'highlight.js/styles/github-dark.css'            // 代码高亮样式（深色，配阅读页深色代码块）
import '@/styles/main.css'                              // InkHub 设计系统
import App from './App.vue'
import router from './router'
import { initTheme } from '@/utils/theme'

// 启动前先应用主题，避免刷新时闪一下白
initTheme()

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
