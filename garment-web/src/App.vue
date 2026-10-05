<template>
  <router-view />
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Noto+Sans+SC:wght@300;400;500;700&display=swap');
@import './styles/theme.css';

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: 'Outfit', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif;
  color: var(--text-primary);
  background: var(--bg-page);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* Element Plus overrides */
.el-menu { border-right: none !important; }
.el-card { border: none !important; border-radius: var(--radius-md) !important; }
.el-button--primary {
  --el-button-bg-color: var(--color-amber) !important;
  --el-button-border-color: var(--color-amber) !important;
  --el-button-hover-bg-color: var(--color-amber-dark) !important;
  --el-button-hover-border-color: var(--color-amber-dark) !important;
  --el-button-active-bg-color: #c08a28 !important;
  --el-button-active-border-color: #c08a28 !important;
}
.el-input__wrapper {
  border-radius: var(--radius-sm) !important;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
  transition: box-shadow 0.2s var(--ease-out) !important;
}
.el-input__wrapper:hover {
  box-shadow: 0 0 0 1px var(--border-color-hover) inset !important;
}
.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px var(--color-amber) inset !important;
}
.el-dropdown-menu {
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-elevated) !important;
  border: none !important;
}
.el-tag {
  border-radius: 20px !important;
  border: none !important;
}

/* Scrollbar */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--scrollbar-thumb); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--scrollbar-thumb-hover); }

/* Animations */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes shimmer {
  0% { background-position: -200% center; }
  100% { background-position: 200% center; }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}
</style>
