<template>
  <div class="boss-notifications">
    <!-- 顶部统计卡片 -->
    <div class="stats-grid">
      <div
        class="stat-card stat-return"
        :class="{ active: activeFilter === 'return' }"
        @click="setFilter('return')"
      >
        <div class="stat-icon-wrap">
          <el-icon size="22"><WarningFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ returns.length }}</div>
          <div class="stat-label">退回</div>
        </div>
        <div class="stat-sub">需要处理</div>
      </div>

      <div
        class="stat-card stat-review"
        :class="{ active: activeFilter === 'review' }"
        @click="setFilter('review')"
      >
        <div class="stat-icon-wrap">
          <el-icon size="22"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ reviews.length }}</div>
          <div class="stat-label">新报工</div>
        </div>
        <div class="stat-sub">等待审核</div>
      </div>

      <div
        class="stat-card stat-adjust"
        :class="{ active: activeFilter === 'adjust' }"
        @click="setFilter('adjust')"
      >
        <div class="stat-icon-wrap">
          <el-icon size="22"><Wallet /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ adjustments.length }}</div>
          <div class="stat-label">待审批</div>
        </div>
        <div class="stat-sub">等待确认</div>
      </div>
    </div>

    <!-- 日期筛选 -->
    <div class="filter-row">
      <div class="filter-tabs">
        <button
          class="tab-btn"
          :class="{ active: activeFilter === 'all' }"
          @click="setFilter('all')"
        >
          全部
          <span class="tab-count">{{ formatCount(allNotifications.length) }}</span>
        </button>
        <button
          class="tab-btn tab-return"
          :class="{ active: activeFilter === 'return' }"
          @click="setFilter('return')"
        >
          退回
        </button>
        <button
          class="tab-btn tab-review"
          :class="{ active: activeFilter === 'review' }"
          @click="setFilter('review')"
        >
          新报工
        </button>
        <button
          class="tab-btn tab-adjust"
          :class="{ active: activeFilter === 'adjust' }"
          @click="setFilter('adjust')"
        >
          待审批
        </button>
      </div>
      <el-date-picker
        v-model="filterDateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始"
        end-placeholder="结束"
        size="small"
        style="width: 240px"
        @change="applyFilter"
      />
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="filteredNotifications.length === 0" class="empty-state">
      <div class="empty-icon">
        <el-icon :size="40" color="#d1d5db"><Bell /></el-icon>
      </div>
      <p class="empty-title">暂无通知</p>
      <p class="empty-tip">退回、新报工和待审批的工资调整会在这里显示</p>
    </div>

    <!-- 分组列表 -->
    <template v-else>
      <!-- 退回分组 -->
      <section v-if="(activeFilter === 'all' || activeFilter === 'return') && visibleGroups.return.length > 0" class="notify-section">
        <div class="section-header">
          <div class="section-title">
            <span class="section-dot dot-return"></span>
            退回
            <span class="section-count">{{ visibleGroups.return.length }}</span>
          </div>
          <button
            v-if="filteredGroups.return.length > groupLimit"
            class="section-more"
            @click="toggleGroup('return')"
          >
            {{ expandedGroups.return ? '收起' : `查看全部 ${formatCount(filteredGroups.return.length)}` }}
          </button>
        </div>
        <div class="section-list">
          <div
            v-for="item in visibleGroups.return"
            :key="item.key"
            class="notify-row row-return"
            @click="goTo('return', item)"
          >
            <div class="row-icon">
              <el-icon size="16"><WarningFilled /></el-icon>
            </div>
            <div class="row-body">
              <div class="row-main">
                <span class="row-info">{{ buildInfo(item) }}</span>
                <span class="row-meta">{{ item.workerName }} · {{ item.quantity }}件</span>
              </div>
              <div v-if="item.returnReason" class="row-reason">{{ item.returnReason }}</div>
            </div>
            <div class="row-time">{{ formatTime(item.time) }}</div>
            <el-icon class="row-arrow" size="12"><ArrowRight /></el-icon>
          </div>
        </div>
      </section>

      <!-- 新报工分组 -->
      <section v-if="(activeFilter === 'all' || activeFilter === 'review') && visibleGroups.review.length > 0" class="notify-section">
        <div class="section-header">
          <div class="section-title">
            <span class="section-dot dot-review"></span>
            新报工
            <span class="section-count">{{ visibleGroups.review.length }}</span>
          </div>
          <button
            v-if="filteredGroups.review.length > groupLimit"
            class="section-more"
            @click="toggleGroup('review')"
          >
            {{ expandedGroups.review ? '收起' : `查看全部 ${formatCount(filteredGroups.review.length)}` }}
          </button>
        </div>
        <div class="section-list">
          <div
            v-for="item in visibleGroups.review"
            :key="item.key"
            class="notify-row row-review"
            @click="goTo('review', item)"
          >
            <div class="row-icon">
              <el-icon size="16"><Document /></el-icon>
            </div>
            <div class="row-body">
              <div class="row-main">
                <span class="row-info">{{ buildInfo(item) }}</span>
                <span class="row-meta">{{ item.workerName }} · {{ item.quantity }}件</span>
              </div>
            </div>
            <span v-if="item.amount" class="row-amount amount-review">{{ item.amount }}元</span>
            <div class="row-time">{{ formatTime(item.time) }}</div>
            <el-icon class="row-arrow" size="12"><ArrowRight /></el-icon>
          </div>
        </div>
      </section>

      <!-- 待审批分组 -->
      <section v-if="(activeFilter === 'all' || activeFilter === 'adjust') && visibleGroups.adjust.length > 0" class="notify-section">
        <div class="section-header">
          <div class="section-title">
            <span class="section-dot dot-adjust"></span>
            待审批
            <span class="section-count">{{ visibleGroups.adjust.length }}</span>
          </div>
          <button
            v-if="filteredGroups.adjust.length > groupLimit"
            class="section-more"
            @click="toggleGroup('adjust')"
          >
            {{ expandedGroups.adjust ? '收起' : `查看全部 ${formatCount(filteredGroups.adjust.length)}` }}
          </button>
        </div>
        <div class="section-list">
          <div
            v-for="item in visibleGroups.adjust"
            :key="item.key"
            class="notify-row row-adjust"
            @click="goTo('adjust', item)"
          >
            <div class="row-icon">
              <el-icon size="16"><Wallet /></el-icon>
            </div>
            <div class="row-body">
              <div class="row-main">
                <span :class="item.type === 1 ? 'type-add' : 'type-deduct'">
                  {{ item.type === 1 ? '加钱' : '扣钱' }}
                </span>
                <span class="row-meta">{{ item.workerName }} · {{ item.operatorName }}提交</span>
              </div>
            </div>
            <span class="row-amount" :class="item.type === 1 ? 'amount-add' : 'amount-deduct'">
              {{ item.type === 1 ? '+' : '-' }}{{ item.amount }}元
            </span>
            <div class="row-time">{{ formatTime(item.time) }}</div>
            <el-icon class="row-arrow" size="12"><ArrowRight /></el-icon>
          </div>
        </div>
      </section>
    </template>

    <!-- 底部分页（仅全部模式且数据多时显示） -->
    <div v-if="activeFilter === 'all' && allNotifications.length > pageTotal" class="pagination-bar">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageTotal"
        :total="allNotifications.length"
        layout="prev, pager, next"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, ArrowRight, WarningFilled, Document, Wallet } from '@element-plus/icons-vue'
import { getAdjustmentList } from '../api/wageAdjustment'
import { markBossNotificationsViewed } from '../api/dashboard'
import { getWorkRecordList } from '../api/workRecord'

const router = useRouter()
const loading = ref(true)
const activeFilter = ref('all')
const filterDateRange = ref(null)
const currentPage = ref(1)
const pageTotal = 20
const groupLimit = 5

const expandedGroups = reactive({ return: false, review: false, adjust: false })

const returns = ref([])
const reviews = ref([])
const adjustments = ref([])

// 合并全部通知
const allNotifications = computed(() => {
  const list = []
  for (const r of returns.value) {
    list.push({ ...r, key: 'return-' + r.id, notifyType: 'return', time: r.returnTime || r.createTime })
  }
  for (const r of reviews.value) {
    list.push({ ...r, key: 'review-' + r.id, notifyType: 'review', time: r.createTime })
  }
  for (const a of adjustments.value) {
    list.push({ ...a, key: 'adjust-' + a.id, notifyType: 'adjust', time: a.createTime })
  }
  list.sort((a, b) => new Date(b.time) - new Date(a.time))
  return list
})

// 按日期筛选后的分组
const filteredGroups = computed(() => {
  let list = allNotifications.value
  if (filterDateRange.value && filterDateRange.value.length === 2) {
    const start = new Date(filterDateRange.value[0])
    start.setHours(0, 0, 0, 0)
    const end = new Date(filterDateRange.value[1])
    end.setHours(23, 59, 59, 999)
    list = list.filter(n => {
      const t = new Date(n.time)
      return t >= start && t <= end
    })
  }
  return {
    return: list.filter(n => n.notifyType === 'return'),
    review: list.filter(n => n.notifyType === 'review'),
    adjust: list.filter(n => n.notifyType === 'adjust')
  }
})

// 筛选后的全部列表（用于 tab 切换）
const filteredNotifications = computed(() => {
  if (activeFilter.value === 'all') {
    const groups = filteredGroups.value
    const all = [...groups.return, ...groups.review, ...groups.adjust]
    all.sort((a, b) => new Date(b.time) - new Date(a.time))
    return all
  }
  return filteredGroups.value[activeFilter.value] || []
})

// 可见的分组数据（带展开逻辑）
const visibleGroups = computed(() => {
  const groups = filteredGroups.value
  return {
    return: expandedGroups.return ? groups.return : groups.return.slice(0, groupLimit),
    review: expandedGroups.review ? groups.review : groups.review.slice(0, groupLimit),
    adjust: expandedGroups.adjust ? groups.adjust : groups.adjust.slice(0, groupLimit)
  }
})

function setFilter(value) {
  activeFilter.value = value
  currentPage.value = 1
}

function applyFilter() {
  currentPage.value = 1
}

function toggleGroup(type) {
  expandedGroups[type] = !expandedGroups[type]
}

onMounted(() => {
  loadAll()
})

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadReturns(), loadReviews(), loadAdjustments()])
    await markBossNotificationsViewed('all')
  } catch (err) {
    console.error('加载通知失败:', err)
  } finally {
    loading.value = false
  }
}

async function loadReturns() {
  try {
    const res = await getWorkRecordList({ status: 3, pageNum: 1, pageSize: 100 })
    returns.value = res.data?.records || []
  } catch (err) {
    console.error('加载退回记录失败:', err)
  }
}

async function loadReviews() {
  try {
    const res = await getWorkRecordList({ status: 1, pageNum: 1, pageSize: 100 })
    reviews.value = res.data?.records || []
  } catch (err) {
    console.error('加载报工记录失败:', err)
  }
}

async function loadAdjustments() {
  try {
    const res = await getAdjustmentList({ status: 0 })
    adjustments.value = res.data || []
  } catch (err) {
    console.error('加载工资调整失败:', err)
  }
}

function formatCount(n) {
  return n > 99 ? '99+' : n
}

function buildInfo(item) {
  const parts = []
  if (item.styleNo) parts.push(item.styleNo)
  if (item.bedNo) parts.push(item.bedNo)
  if (item.bundleNo) parts.push(item.bundleNo)
  if (item.processName) parts.push(item.processName)
  return parts.join(' - ') || '新报工'
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  if (isToday) return `${hour}:${minute}`
  return `${month}/${day} ${hour}:${minute}`
}

function goTo(type, item) {
  if (type === 'adjust') {
    router.push('/wage-adjustment')
  } else if (type === 'return' || type === 'review') {
    router.push('/work-review')
  }
}
</script>

<style scoped>
.boss-notifications {
  width: 100%;
}

/* ===== 统计卡片 ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 18px 20px;
  cursor: pointer;
  user-select: none;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
  border: 2px solid transparent;
}

.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  opacity: 0.04;
  transform: translate(20px, -20px);
  transition: opacity 0.25s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.stat-card.active {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.stat-card.active::after {
  opacity: 0.08;
}

/* 退回卡片 */
.stat-return .stat-icon-wrap {
  background: #fef2f2;
  color: #ef4444;
}
.stat-return::after { background: #ef4444; }
.stat-return.active { border-color: #ef4444; }

/* 报工卡片 */
.stat-review .stat-icon-wrap {
  background: #eff6ff;
  color: #3b82f6;
}
.stat-review::after { background: #3b82f6; }
.stat-review.active { border-color: #3b82f6; }

/* 待审批卡片 */
.stat-adjust .stat-icon-wrap {
  background: #fef3c7;
  color: #d97706;
}
.stat-adjust::after { background: #d97706; }
.stat-adjust.active { border-color: #d97706; }

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.stat-info {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-slate, #2d3748);
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
}

.stat-sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

/* ===== 筛选栏 ===== */
.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 10px 16px;
  background: var(--bg-card);
  border-radius: 10px;
}

.filter-tabs {
  display: flex;
  gap: 4px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: #f3f4f6;
}

.tab-btn.active {
  color: #fff;
  background: var(--color-charcoal);
}

.tab-count {
  font-size: 11px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  border-radius: 9px;
  background: rgba(0, 0, 0, 0.08);
  padding: 0 5px;
}

.tab-btn.active .tab-count {
  background: rgba(255, 255, 255, 0.25);
}

/* ===== 加载中 ===== */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  gap: 12px;
  color: #9ca3af;
  font-size: 14px;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #e5e7eb;
  border-top-color: var(--color-amber, #e8a838);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  gap: 8px;
}

.empty-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #9ca3af;
  margin: 0;
}

.empty-tip {
  font-size: 13px;
  color: #d1d5db;
  margin: 0;
}

/* ===== 通知分组 ===== */
.notify-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px 10px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate, #2d3748);
}

.section-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-return { background: #ef4444; }
.dot-review { background: #3b82f6; }
.dot-adjust { background: #d97706; }

.section-count {
  font-size: 12px;
  font-weight: 500;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 1px 8px;
  border-radius: 10px;
}

.section-more {
  font-size: 13px;
  color: var(--color-amber, #e8a838);
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s;
}

.section-more:hover {
  color: var(--color-amber-dark, #d4952e);
}

.section-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* ===== 通知行 ===== */
.notify-row {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-card);
  border-radius: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.notify-row:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transform: translateX(2px);
}

.row-return { border-left-color: #ef4444; }
.row-review { border-left-color: #3b82f6; }
.row-adjust { border-left-color: #d97706; }

.row-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.row-return .row-icon {
  background: #fef2f2;
  color: #ef4444;
}

.row-review .row-icon {
  background: #eff6ff;
  color: #3b82f6;
}

.row-adjust .row-icon {
  background: #fef3c7;
  color: #d97706;
}

.row-body {
  flex: 1;
  min-width: 0;
}

.row-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.row-process {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate, #2d3748);
}

.row-info {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate, #2d3748);
}

.row-meta {
  font-size: 13px;
  color: #6b7280;
}

.row-reason {
  font-size: 12px;
  color: #ef4444;
  margin-top: 4px;
  padding: 3px 8px;
  background: #fef2f2;
  border-radius: 6px;
  display: inline-block;
}

.row-amount {
  font-size: 15px;
  font-weight: 700;
  flex-shrink: 0;
}

.amount-review { color: #3b82f6; }
.amount-add { color: #16a34a; }
.amount-deduct { color: #ef4444; }

.type-add {
  font-size: 12px;
  color: #16a34a;
  background: #ecfdf5;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}

.type-deduct {
  font-size: 12px;
  color: #ef4444;
  background: #fef2f2;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}

.row-time {
  font-size: 12px;
  color: #9ca3af;
  flex-shrink: 0;
  min-width: 48px;
  text-align: right;
}

.row-arrow {
  flex-shrink: 0;
  color: #d1d5db;
  transition: transform 0.2s;
}

.notify-row:hover .row-arrow {
  transform: translateX(2px);
  color: #9ca3af;
}

/* ===== 分页 ===== */
.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
