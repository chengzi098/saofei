<template>
  <button
    class="theme-toggle"
    :class="[`theme-toggle--${size}`, { 'is-dark': isDark }]"
    :title="isDark ? '切换到明亮模式' : '切换到暗黑模式'"
    @click="themeStore.toggle()"
  >
    <el-icon class="theme-toggle__icon">
      <Sunny v-if="isDark" />
      <Moon v-else />
    </el-icon>
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { useThemeStore } from '../stores/theme'

const props = defineProps({
  size: {
    type: String,
    default: 'default' // 'default' | 'small'
  }
})

const themeStore = useThemeStore()
const isDark = computed(() => themeStore.mode === 'dark')
</script>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-primary);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.25s var(--ease-out);
  flex-shrink: 0;
}

.theme-toggle:hover {
  border-color: var(--color-amber);
  color: var(--color-amber);
  transform: rotate(15deg);
}

.theme-toggle--default {
  width: 38px;
  height: 38px;
  font-size: 18px;
}

.theme-toggle--small {
  width: 32px;
  height: 32px;
  font-size: 16px;
}

.theme-toggle__icon {
  display: inline-flex;
}
</style>
