/**
 * 主题（浅色 / 深色）管理
 * - 存 localStorage，刷新后保持
 * - html[data-theme] 给我们自己的 --ink-* 变量切换用
 * - html.dark 给 Element Plus 官方的 dark css-vars 用
 */
const THEME_KEY = 'inkhub:theme'

export function getTheme() {
  return localStorage.getItem(THEME_KEY) === 'dark' ? 'dark' : 'light'
}

export function applyTheme(theme) {
  const dark = theme === 'dark'
  document.documentElement.dataset.theme = dark ? 'dark' : 'light'
  document.documentElement.classList.toggle('dark', dark)
  localStorage.setItem(THEME_KEY, dark ? 'dark' : 'light')
}

export function initTheme() {
  applyTheme(getTheme())
}

export function toggleTheme() {
  const next = getTheme() === 'dark' ? 'light' : 'dark'
  applyTheme(next)
  return next
}
