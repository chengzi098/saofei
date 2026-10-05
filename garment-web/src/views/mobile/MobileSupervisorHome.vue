<template>
  <div class="mobile-home">
    <!-- 欢迎区 -->
    <div class="welcome-card" :class="{ 'entered': mounted }">
      <div class="welcome-text">
        <div class="greeting">{{ greeting }}，{{ userName }}</div>
        <div class="date-text">{{ todayStr }}</div>
      </div>
      <div class="weather-icon">
        <div class="avatar">{{ userName.charAt(userName.length - 1) }}</div>
      </div>
    </div>

    <!-- 快捷扫码区 -->
    <div class="scan-card" :class="{ 'entered': mounted }" @click="goScan">
      <div class="scan-bg">
        <div class="scan-pattern" v-for="n in 6" :key="n" :style="{ animationDelay: `${n * 0.3}s` }"></div>
      </div>
      <div class="scan-content">
        <div class="scan-icon-wrap">
          <el-icon :size="36" color="#fff"><Camera /></el-icon>
        </div>
        <div class="scan-text">
          <div class="scan-title">扫码报工</div>
          <div class="scan-desc">扫描扎号二维码，快速报工序</div>
        </div>
        <el-icon :size="20" color="rgba(255,255,255,0.6)"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 扫码查扎 -->
    <div class="scan-card check-card" :class="{ 'entered': mounted }" @click="goScanBundle">
      <div class="scan-bg">
        <div class="scan-pattern" v-for="n in 6" :key="n" :style="{ animationDelay: `${n * 0.3}s` }"></div>
      </div>
      <div class="scan-content">
        <div class="scan-icon-wrap">
          <el-icon :size="36" color="#fff"><Search /></el-icon>
        </div>
        <div class="scan-text">
          <div class="scan-title">扫码查扎</div>
          <div class="scan-desc">查看本扎工序进度，可退回已完成工序</div>
        </div>
        <el-icon :size="20" color="rgba(255,255,255,0.6)"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 工作看板入口 -->
    <div class="scan-card board-card" :class="{ 'entered': mounted }" @click="goBoard">
      <div class="scan-bg">
        <div class="scan-pattern" v-for="n in 6" :key="n" :style="{ animationDelay: `${n * 0.3}s` }"></div>
      </div>
      <div class="scan-content">
        <div class="scan-icon-wrap">
          <el-icon :size="36" color="#fff"><DataBoard /></el-icon>
        </div>
        <div class="scan-text">
          <div class="scan-title">工作看板</div>
          <div class="scan-desc">查看款式、床号、扎号生产进度</div>
        </div>
        <el-icon :size="20" color="rgba(255,255,255,0.6)"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 扫码出货入口 -->
    <div class="scan-card ship-card" :class="{ 'entered': mounted }" @click="goShip">
      <div class="scan-bg">
        <div class="scan-pattern" v-for="n in 6" :key="n" :style="{ animationDelay: `${n * 0.3}s` }"></div>
      </div>
      <div class="scan-content">
        <div class="scan-icon-wrap">
          <el-icon :size="36" color="#fff"><Van /></el-icon>
        </div>
        <div class="scan-text">
          <div class="scan-title">扫码出货</div>
          <div class="scan-desc">扫码确认工序完成，出货发车</div>
        </div>
        <el-icon :size="20" color="rgba(255,255,255,0.6)"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 今日统计 -->
    <div class="stats-section" :class="{ 'entered': mounted }">
      <div class="section-title">今日概况</div>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-num">{{ stats.myBundleCount || 0 }}</div>
          <div class="stat-label">我的扎数</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ stats.todayReportCount || 0 }}</div>
          <div class="stat-label">今日报工</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ stats.activeStyleCount || 0 }}</div>
          <div class="stat-label">进行中款式</div>
        </div>
        <div class="stat-card warn">
          <div class="stat-num">{{ stats.returnCount || 0 }}</div>
          <div class="stat-label">近7天退回</div>
        </div>
      </div>
    </div>

    <!-- 我的款式进度 -->
    <div class="styles-section" :class="{ 'entered': mounted }" v-if="myStyles.length > 0">
      <div class="section-header">
        <span class="section-title">我的款式进度</span>
        <span class="section-more" @click="$router.push('/mobile-supervisor/review')">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </span>
      </div>
      <div class="style-list">
        <div v-for="style in myStyles" :key="style.styleId" class="style-card">
          <div class="style-header">
            <span class="style-no">{{ style.styleNo }}</span>
            <span class="style-progress">{{ style.progressPercent }}%</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: style.progressPercent + '%' }"></div>
          </div>
          <div class="style-meta">
            <span>{{ style.myBundleCount }}/{{ style.totalBundleCount }} 扎</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 最近报工 -->
    <div class="recent-section" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">最近报工</span>
        <span class="section-more" @click="$router.push('/mobile-supervisor/review')">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </span>
      </div>
      <div class="recent-list" v-if="recentReports.length > 0">
        <div v-for="record in recentReports" :key="record.id" class="record-item">
          <div class="record-left">
            <div class="record-worker">{{ record.workerName }}</div>
            <div class="record-detail">
              <span class="record-bundle">{{ record.bundleNo }}</span>
              <span class="record-process">{{ record.processName }}</span>
            </div>
          </div>
          <div class="record-right">
            <div class="record-qty">{{ record.quantity }}件</div>
            <div class="record-amount">{{ (record.amount || 0).toFixed(2) }}元</div>
          </div>
        </div>
      </div>
      <div class="recent-empty" v-else>
        <el-icon :size="40" color="#d1d5db"><Document /></el-icon>
        <p>今日暂无报工记录</p>
        <p class="empty-tip">点击上方「扫码报工」开始工作</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Document, Search, DataBoard, Van } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { getSupervisorStats } from '../../api/dashboard'

const router = useRouter()
const userStore = useUserStore()
const mounted = ref(false)

const stats = ref({})
const myStyles = ref([])
const recentReports = ref([])

const userName = computed(() => userStore.realName || userStore.username || '主管')

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const todayStr = computed(() => {
  const d = new Date()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${d.getMonth() + 1}月${d.getDate()}日 ${weekDays[d.getDay()]}`
})

onMounted(async () => {
  requestAnimationFrame(() => { mounted.value = true })
  // 刷新角色，解决老板修改职位后不更新的问题
  const roleResult = await userStore.refreshRole()
  if (roleResult.changed && roleResult.newRole === 'worker') {
    // 主管被改为工人，跳转到工人端
    router.replace('/mobile/home')
    return
  }
  loadStats()
})

async function loadStats() {
  try {
    const res = await getSupervisorStats()
    const data = res.data || {}
    stats.value = data
    myStyles.value = data.myStyles || []
    recentReports.value = data.recentReports || []
  } catch (err) {
    console.error('加载主管统计失败:', err)
  }
}

function goScan() {
  router.push('/mobile-supervisor/scan')
}

function goScanBundle() {
  router.push('/mobile-supervisor/scan-bundle')
}

function goBoard() {
  router.push('/mobile-supervisor/board')
}

function goShip() {
  router.push('/mobile-supervisor/ship')
}
</script>

<style scoped>
.mobile-home {
  padding: 12px 16px;
  padding-bottom: 100px;
}

/* ===== 欢迎卡片 ===== */
.welcome-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  background: var(--bg-card);
  border-radius: 16px;
  margin-bottom: 12px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out;
}

.welcome-card.entered {
  opacity: 1;
  transform: translateY(0);
}

.greeting {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
  margin-bottom: 4px;
}

.date-text {
  font-size: 13px;
  color: #9ca3af;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-amber, #e8a838), var(--color-terracotta, #c75b39));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
}

/* ===== 扫码卡片 ===== */
.scan-card {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  margin-bottom: 12px;
  background: linear-gradient(135deg, var(--color-amber, #d97706), var(--color-terracotta, #b45309));
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out 0.1s;
  cursor: pointer;
}

.scan-card.entered {
  opacity: 1;
  transform: translateY(0);
}

.scan-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.scan-pattern {
  position: absolute;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  animation: float 6s ease-in-out infinite;
}

.scan-pattern:nth-child(1) { top: -40px; left: -20px; }
.scan-pattern:nth-child(2) { top: 20px; right: -30px; width: 80px; height: 80px; }
.scan-pattern:nth-child(3) { bottom: -30px; left: 30%; width: 100px; height: 100px; }
.scan-pattern:nth-child(4) { top: -20px; right: 20%; width: 60px; height: 60px; }
.scan-pattern:nth-child(5) { bottom: 10px; right: 10%; width: 90px; height: 90px; }
.scan-pattern:nth-child(6) { top: 30%; left: -10px; width: 70px; height: 70px; }

@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-10px) scale(1.05); }
}

.scan-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 20px;
}

.scan-icon-wrap {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  backdrop-filter: blur(4px);
}

.scan-text {
  flex: 1;
}

.scan-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 4px;
}

.scan-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

/* ===== 今日统计 ===== */
.stats-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out 0.2s;
}

.stats-section.entered {
  opacity: 1;
  transform: translateY(0);
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
  margin-bottom: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.stat-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 14px;
  text-align: center;
}

.stat-card.warn .stat-num {
  color: #ef4444;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
  margin-bottom: 4px;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 12px;
  color: #9ca3af;
}

/* ===== 款式进度 ===== */
.styles-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out 0.3s;
}

.styles-section.entered {
  opacity: 1;
  transform: translateY(0);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.section-more {
  font-size: 13px;
  color: var(--color-amber, #d97706);
  display: flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
}

.style-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.style-card {
  padding: 12px;
  background: #fafafa;
  border-radius: 12px;
}

.style-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.style-no {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate, #2d2a26);
}

.style-progress {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-amber, #d97706);
}

.progress-track {
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-amber, #e8a838), var(--color-terracotta, #c75b39));
  border-radius: 3px;
  transition: width 0.8s ease-out;
}

.style-meta {
  font-size: 12px;
  color: #9ca3af;
}

/* ===== 最近报工 ===== */
.recent-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 16px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out 0.4s;
}

.recent-section.entered {
  opacity: 1;
  transform: translateY(0);
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.record-item:last-child {
  border-bottom: none;
}

.record-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.record-worker {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate, #2d2a26);
}

.record-detail {
  display: flex;
  align-items: center;
  gap: 8px;
}

.record-bundle {
  font-size: 13px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 1px 8px;
  border-radius: 8px;
}

.record-process {
  font-size: 13px;
  color: #6b7280;
}

.record-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.record-qty {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
}

.record-amount {
  font-size: 13px;
  color: var(--color-amber, #d97706);
  font-weight: 500;
}

.recent-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
  gap: 8px;
}

.recent-empty p {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
}

.empty-tip {
  font-size: 12px !important;
}

/* ===== 查扎卡片 ===== */
.check-card {
  background: linear-gradient(135deg, #6366f1, #4f46e5) !important;
  transition: all 0.5s ease-out 0.15s;
}

/* ===== 工作看板卡片 ===== */
.board-card {
  background: linear-gradient(135deg, #0ea5e9, #0284c7) !important;
  transition: all 0.5s ease-out 0.2s;
}

/* ===== 扫码出货卡片 ===== */
.ship-card {
  background: linear-gradient(135deg, #10b981, #059669) !important;
  transition: all 0.5s ease-out 0.25s;
}
</style>
