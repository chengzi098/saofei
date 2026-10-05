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

    <!-- 今日统计 -->
    <div class="stats-section" :class="{ 'entered': mounted }">
      <div class="section-title">今日报工</div>
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-num">{{ todayCount }}</div>
          <div class="stat-label">已报工序</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-num">{{ todayPieces }}</div>
          <div class="stat-label">完成件数</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-num highlight">{{ todayEarning }}元</div>
          <div class="stat-label">预计收入</div>
        </div>
      </div>
    </div>

    <!-- 本月工资预估 -->
    <div class="salary-card" :class="{ 'entered': mounted }">
      <div class="salary-header">
        <span class="salary-title">本月工资预估</span>
        <span class="salary-month">{{ currentMonth }}</span>
      </div>
      <div class="salary-amount">
        <span class="currency">元</span>
        <span class="amount">{{ monthEarning }}</span>
      </div>
      <div class="salary-progress">
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <div class="progress-text">
          <span>报工工资 {{ approvedAmount }}元</span>
          <span v-if="parseFloat(adjustmentAmount) !== 0">
            工资调整 {{ parseFloat(adjustmentAmount) >= 0 ? '+' : '' }}{{ adjustmentAmount }}元
          </span>
          <span v-if="parseFloat(pendingAmount) > 0">已退回 {{ pendingAmount }}元</span>
        </div>
      </div>
    </div>

    <!-- 最近记录 -->
    <div class="recent-section" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">最近报工</span>
        <span class="section-more" @click="$router.push('/mobile/records')">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </span>
      </div>
      <div class="recent-list" v-if="recentRecords.length > 0">
        <div v-for="record in recentRecords" :key="record.id" class="record-item">
          <div class="record-left">
            <div class="record-bundle">{{ record.bundleNo }}</div>
            <div class="record-process">{{ record.processName }}</div>
          </div>
          <div class="record-right">
            <div class="record-qty">{{ record.quantity }}件</div>
            <div class="record-status" :class="getRecordStatusClass(record.status)">
              {{ getRecordStatusText(record.status) }}
            </div>
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
import { useUserStore } from '../../stores/user'
import { Camera, ArrowRight, Document } from '@element-plus/icons-vue'
import { getWorkRecordList } from '../../api/workRecord'
import { getWorkerMonthlyIncome } from '../../api/dashboard'
import { formatLocalDate } from '../../utils/mobileRecordFilters'

const router = useRouter()
const userStore = useUserStore()
const mounted = ref(false)

// 统计数据
const todayCount = ref(0)
const todayPieces = ref(0)
const todayEarning = ref('0.00')
const monthEarning = ref('0.00')
const approvedAmount = ref('0.00')
const adjustmentAmount = ref('0.00')
const pendingAmount = ref('0.00')
const recentRecords = ref([])

const userName = computed(() => userStore.realName || userStore.username || '工友')

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

const currentMonth = computed(() => {
  return `${new Date().getMonth() + 1}月`
})

const progressPercent = computed(() => {
  const total = parseFloat(monthEarning.value)
  if (total <= 0) return 0
  return Math.min(100, Math.round((parseFloat(approvedAmount.value) / total) * 100))
})

onMounted(async () => {
  requestAnimationFrame(() => { mounted.value = true })
  // 刷新角色，解决老板修改职位后不更新的问题
  const roleResult = await userStore.refreshRole()
  if (roleResult.changed && roleResult.newRole === 'supervisor') {
    // 工人被改为主管，跳转到主管端
    router.replace('/mobile-supervisor/home')
    return
  }
  loadTodayStats()
})

async function loadTodayStats() {
  try {
    // 获取今日记录
    const today = formatLocalDate(new Date())
    const res = await getWorkRecordList({
      pageNum: 1,
      pageSize: 10,
      workerId: userStore.userId,
      startDate: today,
      endDate: today
    })
    const records = res.data?.records || []
    recentRecords.value = records.slice(0, 5)

    // 只统计已通过的记录（status=1）
    const passedRecords = records.filter(r => r.status === 1)
    todayCount.value = passedRecords.length
    todayPieces.value = passedRecords.reduce((sum, r) => sum + (r.quantity || 0), 0)
    todayEarning.value = passedRecords
      .reduce((sum, r) => sum + ((r.quantity || 0) * (r.price || 0)), 0)
      .toFixed(2)

    // 获取本月记录（简化处理，实际应调API）
    loadMonthStats()
  } catch (err) {
    console.error('加载今日统计失败:', err)
  }
}

async function loadMonthStats() {
  try {
    const res = await getWorkerMonthlyIncome()
    const data = res.data || {}
    approvedAmount.value = (data.workAmount || 0).toFixed(2)
    adjustmentAmount.value = (data.adjustmentAmount || 0).toFixed(2)
    pendingAmount.value = (data.returnedAmount || 0).toFixed(2)
    monthEarning.value = (data.totalAmount || 0).toFixed(2)
  } catch (err) {
    console.error('加载月度统计失败:', err)
  }
}

function goScan() {
  router.push('/mobile/scan')
}

function getRecordStatusText(status) {
  const map = { 1: '已通过', 2: '已驳回', 3: '已退回' }
  return map[status] || '未知'
}

function getRecordStatusClass(status) {
  const map = { 1: 'approved', 2: 'rejected', 3: 'returned' }
  return map[status] || ''
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
  padding: 16px 20px;
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

.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
  font-variant-numeric: tabular-nums;
}

.stat-num.highlight {
  color: var(--color-amber, #d97706);
}

.stat-label {
  font-size: 12px;
  color: #9ca3af;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: #f3f4f6;
}

/* ===== 工资卡片 ===== */
.salary-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 16px 20px;
  margin-bottom: 12px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out 0.3s;
}

.salary-card.entered {
  opacity: 1;
  transform: translateY(0);
}

.salary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.salary-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
}

.salary-month {
  font-size: 12px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 10px;
  border-radius: 10px;
}

.salary-amount {
  margin-bottom: 14px;
}

.currency {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-amber, #d97706);
}

.amount {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-amber, #d97706);
  font-variant-numeric: tabular-nums;
}

.progress-track {
  height: 6px;
  background: #f3f4f6;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-amber, #e8a838), var(--color-terracotta, #c75b39));
  border-radius: 3px;
  transition: width 0.8s ease-out;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #9ca3af;
}

/* ===== 最近记录 ===== */
.recent-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 16px 20px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out 0.4s;
}

.recent-section.entered {
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

.record-bundle {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate, #2d2a26);
}

.record-process {
  font-size: 13px;
  color: #9ca3af;
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

.record-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.record-status.approved {
  background: #ecfdf5;
  color: #059669;
}

.record-status.pending {
  background: #fef3c7;
  color: #d97706;
}

.record-status.rejected {
  background: #fef2f2;
  color: #ef4444;
}

.record-status.returned {
  background: #f4f4f5;
  color: #909399;
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
</style>
