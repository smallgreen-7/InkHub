<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <span class="brand-mark">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          </svg>
        </span>
        <span class="brand-text">Ink<em>Hub</em> 管理台</span>
      </div>

      <el-menu :default-active="$route.path" router class="menu">
        <div class="menu-label">内容管理</div>
        <el-menu-item index="/admin/articles">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" /></svg>
          <span>文章管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/comments">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
          <span>评论管理</span>
        </el-menu-item>
        <div class="menu-label">系统设置</div>
        <el-menu-item index="/admin/categories">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.83z" /><line x1="7" y1="7" x2="7.01" y2="7" /></svg>
          <span>分类标签</span>
        </el-menu-item>
        <el-menu-item index="/admin/stats">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10" /><line x1="12" y1="20" x2="12" y2="4" /><line x1="6" y1="20" x2="6" y2="14" /></svg>
          <span>数据统计</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-foot">
        <router-link to="/" class="back-link">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5" /><polyline points="12 19 5 12 12 5" /></svg>
          返回前台
        </router-link>
      </div>
    </el-aside>

    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
</script>

<style scoped>
.admin-layout { min-height: calc(100vh - 60px); }

/* 深墨色侧边栏 */
.aside {
  background: linear-gradient(180deg, var(--ink-deep) 0%, var(--ink-abyss) 100%);
  border-right: none;
  display: flex;
  flex-direction: column;
  color: var(--ink-foam);
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 22px 20px 18px;
}
.brand-mark {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  background: var(--ink-accent-glow);
  border-radius: 10px;
  color: var(--ink-abyss);
  box-shadow: 0 0 18px rgba(90, 209, 230, 0.3);
}
.brand-text { font-family: var(--ink-serif); font-size: 16px; font-weight: 600; color: var(--ink-foam); letter-spacing: 0.02em; }
.brand-text em { font-style: normal; color: var(--ink-accent-glow); }

.menu { background: transparent; border-right: none; flex: 1; --el-menu-text-color: #a5a9c6; --el-menu-hover-text-color: #fff; --el-menu-active-color: #fff; --el-menu-item-height: 46px; }
.menu-label {
  padding: 18px 22px 6px;
  font-size: 11px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 1.5px;
}
.menu :deep(.el-menu-item) {
  margin: 2px 12px;
  border-radius: 9px;
  font-size: 14px;
  gap: 10px;
}
.menu :deep(.el-menu-item:hover) { background: rgba(255, 255, 255, 0.07); }
.menu :deep(.el-menu-item.is-active) {
  background: rgba(90, 209, 230, 0.14);
  color: var(--ink-accent-glow) !important;
  box-shadow: none;
}

.aside-foot { padding: 16px 20px 22px; border-top: 1px solid rgba(255, 255, 255, 0.08); }
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
  transition: color 0.2s ease;
}
.back-link:hover { color: #fff; }

.main {
  background: var(--ink-bg);
  padding: 24px 28px;
}
</style>
