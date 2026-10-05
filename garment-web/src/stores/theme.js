import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'theme_mode'

/** 读取初始主题：localStorage 优先，否则跟随系统偏好 */
function detectInitialMode() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark'
  }
  return 'light'
}

/** 给 <html> 增删 dark 类（Element Plus 暗黑模式依赖此 class） */
function applyMode(mode) {
  document.documentElement.classList.toggle('dark', mode === 'dark')
}

/**
 * 在应用挂载前调用：根据持久化/系统偏好给 <html> 打上 dark 类，避免首屏闪烁。
 * 与 store 解耦，main.js 直接调用即可。
 */
export function initTheme() {
  applyMode(detectInitialMode())
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref(detectInitialMode())

  function setMode(next) {
    mode.value = next
    localStorage.setItem(STORAGE_KEY, next)
    applyMode(next)
  }

  function toggle() {
    setMode(mode.value === 'dark' ? 'light' : 'dark')
  }

  function isDark() {
    return mode.value === 'dark'
  }

  return { mode, setMode, toggle, isDark }
})
