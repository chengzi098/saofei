<template>
  <div class="company-select-container">
    <div class="bg-pattern"></div>

    <div class="content-wrapper" :class="{ 'content-enter': !loading }">
      <!-- 头部 -->
      <div class="header-area">
        <div class="brand-mini">
          <svg viewBox="0 0 32 32" fill="none" width="28" height="28">
            <rect width="32" height="32" rx="8" fill="var(--color-amber)"/>
            <path d="M8 12L16 8L24 12V20L16 24L8 20V12Z" stroke="white" stroke-width="2" fill="none"/>
            <circle cx="16" cy="16" r="3" fill="white"/>
          </svg>
        </div>
        <h1 class="page-title">选择工作空间</h1>
        <p class="page-desc">您关联了多个公司，请选择要进入的工作空间</p>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-area">
        <div class="loading-spinner"></div>
        <span>正在获取您的公司信息...</span>
      </div>

      <!-- 公司列表 -->
      <div v-else class="company-grid">
        <div
          v-for="(item, index) in companies"
          :key="item.companyId"
          class="company-card"
          :style="{ animationDelay: `${0.1 + index * 0.08}s` }"
          @click="selectCompany(item)"
        >
          <div class="card-accent" :class="roleAccent(item.role)"></div>
          <div class="card-body">
            <div class="card-icon">
              <el-icon :size="22"><OfficeBuilding /></el-icon>
            </div>
            <div class="card-info">
              <span class="card-name">{{ item.companyName || ('公司 #' + item.companyId) }}</span>
              <div class="card-meta">
                <span class="role-badge" :class="item.role">
                  {{ roleLabel(item.role) }}
                </span>
              </div>
            </div>
            <el-icon class="card-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && companies.length === 0" class="empty-area">
        <el-icon :size="48" color="var(--color-mist)"><OfficeBuilding /></el-icon>
        <p>暂无关联公司</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getCompanies } from '../api/auth'
import { OfficeBuilding, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const companies = ref([])
const loading = ref(true)

// 登录前被拦截的原始路径（如 /mobile/scan?code=xxx），仅接受站内相对路径
const redirectTarget = (() => {
  const raw = route.query.redirect
  if (typeof raw !== 'string' || !raw) return null
  return raw.startsWith('/') ? raw : null
})()

onMounted(async () => {
  try {
    const res = await getCompanies()
    companies.value = res.data || []
    if (companies.value.length === 1) {
      selectCompany(companies.value[0])
    }
  } finally {
    loading.value = false
  }
})

function selectCompany(item) {
  userStore.setCurrentCompanyId(item.companyId)
  userStore.setCurrentCompanyName(item.companyName || '')
  userStore.setCurrentRole(item.role)
  // 如果有登录前保存的返回地址（如扫码页），优先跳转回去
  if (redirectTarget) {
    router.push(redirectTarget)
  } else {
    router.push('/')
  }
}

function roleLabel(role) {
  const map = { boss: '老板', supervisor: '主管', worker: '工人' }
  return map[role] || role
}

function roleAccent(role) {
  return role || ''
}
</script>

<style scoped>
.company-select-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
}

.bg-pattern {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(circle at 30% 70%, rgba(232, 168, 56, 0.07) 0%, transparent 50%),
    radial-gradient(circle at 70% 30%, rgba(196, 122, 90, 0.05) 0%, transparent 50%),
    var(--color-cream);
  z-index: 0;
}

.content-wrapper {
  width: 100%;
  max-width: 520px;
  position: relative;
  z-index: 1;
  opacity: 0;
  transform: translateY(16px);
  transition: all 0.5s var(--ease-out);
}

.content-wrapper.content-enter {
  opacity: 1;
  transform: translateY(0);
}

/* 头部 */
.header-area {
  text-align: center;
  margin-bottom: 36px;
}

.brand-mini {
  margin-bottom: 16px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-slate);
  margin-bottom: 8px;
}

.page-desc {
  font-size: 14px;
  color: var(--color-mist);
}

/* 加载 */
.loading-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 48px 0;
  color: var(--color-slate-light);
  font-size: 14px;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid var(--color-cream-dark);
  border-top-color: var(--color-amber);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 公司卡片 */
.company-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.company-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-soft);
  cursor: pointer;
  overflow: hidden;
  transition: all 0.25s var(--ease-out);
  animation: fadeInUp 0.5s var(--ease-out) both;
}

.company-card:hover {
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}

.company-card:active {
  transform: translateY(0);
}

.card-accent {
  height: 3px;
  background: var(--color-amber);
}

.card-accent.boss { background: linear-gradient(90deg, #f87171, #ef4444); }
.card-accent.supervisor { background: linear-gradient(90deg, #fbbf24, #f59e0b); }
.card-accent.worker { background: linear-gradient(90deg, #4ade80, #22c55e); }

.card-body {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.card-icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-sm);
  background: var(--color-cream);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-slate-light);
  flex-shrink: 0;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-name {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-badge {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 10px;
  border-radius: 20px;
}

.role-badge.boss {
  background: rgba(248, 113, 113, 0.1);
  color: #ef4444;
}

.role-badge.supervisor {
  background: rgba(251, 191, 36, 0.12);
  color: #d97706;
}

.role-badge.worker {
  background: rgba(74, 222, 128, 0.1);
  color: #16a34a;
}

.card-arrow {
  color: var(--color-mist);
  transition: transform 0.2s var(--ease-out);
  flex-shrink: 0;
}

.company-card:hover .card-arrow {
  color: var(--color-amber);
  transform: translateX(3px);
}

/* 空状态 */
.empty-area {
  text-align: center;
  padding: 48px 0;
  color: var(--color-mist);
  font-size: 14px;
}

.empty-area p {
  margin-top: 12px;
}
</style>
