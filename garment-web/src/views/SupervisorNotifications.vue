<template>
  <div class="supervisor-notifications">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div
        v-for="tab in filterTabs"
        :key="tab.value"
        class="filter-tab"
        :class="{ active: activeFilter === tab.value }"
        @click="setFilter(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.count > 0" class="filter-count">{{ tab.count }}</span>
      </div>
      <el-date-picker
        v-model="filterDateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始"
        end-placeholder="结束"
        size="small"
        style="width: 240px; margin-left: auto"
        @change="applyFilter"
      />
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="filteredNotifications.length === 0" class="empty-state">
      <el-icon :size="56" color="#d1d5db"><Bell /></el-icon>
      <p class="empty-title">暂无通知</p>
      <p class="empty-tip">公告和待审批的工资调整会在这里显示</p>
    </div>

    <!-- 通知列表 -->
    <div v-else class="notify-list">
      <div
        v-for="item in paginatedNotifications"
        :key="item.key"
        class="notify-card"
        :class="getCardClass(item.notifyType)"
        @click="goTo(item.notifyType, item)"
      >
        <div class="notify-icon" :class="getIconClass(item.notifyType)">
          <el-icon size="18"><component :is="getIcon(item.notifyType)" /></el-icon>
        </div>

        <div class="notify-content">
          <div class="notify-header">
            <span class="notify-type">{{ getTypeLabel(item.notifyType) }}</span>
            <span class="notify-time">{{ formatTime(item.time) }}</span>
          </div>

          <template v-if="item.notifyType === 'announcement'">
            <div class="notify-title">{{ item.title }}</div>
            <div class="notify-desc">{{ item.senderName }}发送</div>
          </template>

          <template v-if="item.notifyType === 'adjust'">
            <div class="notify-title">
              <span :class="item.type === 1 ? 'type-add' : 'type-deduct'">
                {{ item.type === 1 ? '加钱' : '扣钱' }}
              </span>
              <span class="adjust-amount" :class="item.type === 1 ? 'amount-add' : 'amount-deduct'">
                {{ item.type === 1 ? '+' : '-' }}{{ item.amount }}元
              </span>
            </div>
            <div class="notify-desc">{{ item.workerName }}</div>
            <span class="status-tag status-pending">待审批</span>
          </template>
        </div>

        <el-icon class="notify-arrow" size="14" color="#d1d5db"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="filteredNotifications.length > pageSize" class="pagination-bar">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredNotifications.length"
        layout="prev, pager, next"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Wallet, ArrowRight, Loading } from '@element-plus/icons-vue'
import { getAdjustmentList } from '../api/wageAdjustment'
import { markSupervisorNotificationsViewed } from '../api/dashboard'
import { getMyNotifications, markAsRead } from '../api/notification'

const router = useRouter()
const loading = ref(true)
const activeFilter = ref('all')
const filterDateRange = ref(null)
const currentPage = ref(1)
const pageSize = 10

const announcements = ref([])
const adjustments = ref([])

const filterTabs = computed(() => [
  { label: '全部', value: 'all', count: allNotifications.value.length },
  { label: '公告', value: 'announcement', count: announcements.value.length },
  { label: '待审批', value: 'adjust', count: adjustments.value.length }
])

const allNotifications = computed(() => {
  const list = []

  for (const a of announcements.value) {
    list.push({
      ...a,
      key: 'announcement-' + a.id,
      notifyType: 'announcement',
      time: a.createTime
    })
  }

  for (const a of adjustments.value) {
    list.push({
      ...a,
      key: 'adjust-' + a.id,
      notifyType: 'adjust',
      time: a.createTime
    })
  }

  list.sort((a, b) => new Date(b.time) - new Date(a.time))
  return list
})

const filteredNotifications = computed(() => {
  let list = allNotifications.value

  if (activeFilter.value !== 'all') {
    list = list.filter(n => n.notifyType === activeFilter.value)
  }

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

  return list
})

const paginatedNotifications = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredNotifications.value.slice(start, start + pageSize)
})

function setFilter(value) {
  activeFilter.value = value
  currentPage.value = 1
}

function applyFilter() {
  currentPage.value = 1
}

onMounted(() => {
  loadAll()
})

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadAnnouncements(), loadAdjustments()])
    await markSupervisorNotificationsViewed('all')
  } catch (err) {
    console.error('加载通知失败:', err)
  } finally {
    loading.value = false
  }
}

async function loadAnnouncements() {
  try {
    const res = await getMyNotifications()
    announcements.value = res.data || []
  } catch (err) {
    console.error('加载公告失败:', err)
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

function getTypeLabel(type) {
  const map = { announcement: '公告', adjust: '工资调整' }
  return map[type] || ''
}

function getIcon(type) {
  const map = { announcement: Bell, adjust: Wallet }
  return map[type] || Bell
}

function getCardClass(type) {
  const map = { announcement: 'card-announcement', adjust: 'card-adjust' }
  return map[type] || ''
}

function getIconClass(type) {
  const map = { announcement: 'icon-announcement', adjust: 'icon-adjust' }
  return map[type] || ''
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
  if (isToday) return `今天 ${hour}:${minute}`
  return `${month}/${day} ${hour}:${minute}`
}

function goTo(type, item) {
  if (type === 'announcement' && item && !item.read) {
    markAsRead(item.id).then(() => { item.read = true }).catch(() => {})
  }
  if (type === 'adjust') {
    router.push('/wage-adjustment')
  }
}
</script>

<style scoped>
.supervisor-notifications {
  max-width: 800px;
}

/* ===== 筛选栏 ===== */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--bg-card);
  border-radius: 10px;
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e5e7eb;
}

.filter-tab:hover {
  border-color: var(--color-amber, #d97706);
  color: var(--color-amber, #d97706);
}

.filter-tab.active {
  color: #fff;
  background: var(--color-amber, #d97706);
  border-color: var(--color-amber, #d97706);
}

.filter-count {
  font-size: 11px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  text-align: center;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.1);
  padding: 0 4px;
}

.filter-tab.active .filter-count {
  background: rgba(255, 255, 255, 0.3);
}

/* ===== 加载中 ===== */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  gap: 12px;
  color: #9ca3af;
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  gap: 8px;
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

/* ===== 通知列表 ===== */
.notify-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notify-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: var(--bg-card);
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s;
  border-left: 3px solid transparent;
}

.notify-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.card-announcement {
  border-left-color: #8b5cf6;
}

.card-adjust {
  border-left-color: #f59e0b;
}

/* ===== 图标 ===== */
.notify-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-announcement {
  background: #f3e8ff;
  color: #8b5cf6;
}

.icon-adjust {
  background: #fef3c7;
  color: #f59e0b;
}

/* ===== 内容 ===== */
.notify-content {
  flex: 1;
  min-width: 0;
}

.notify-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.notify-type {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 500;
}

.notify-time {
  font-size: 11px;
  color: #9ca3af;
}

.notify-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate, #2d2a26);
  display: flex;
  align-items: center;
  gap: 8px;
}

.notify-desc {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

.notify-arrow {
  flex-shrink: 0;
  margin-top: 12px;
}

/* ===== 工资调整样式 ===== */
.type-add {
  color: #16a34a;
  font-size: 12px;
  background: #ecfdf5;
  padding: 1px 6px;
  border-radius: 4px;
}

.type-deduct {
  color: #ef4444;
  font-size: 12px;
  background: #fef2f2;
  padding: 1px 6px;
  border-radius: 4px;
}

.adjust-amount {
  font-size: 16px;
  font-weight: 700;
}

.amount-add {
  color: #16a34a;
}

.amount-deduct {
  color: #ef4444;
}

/* ===== 状态标签 ===== */
.status-tag {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
  margin-top: 6px;
}

.status-pending {
  background: #fef3c7;
  color: #d97706;
}

/* ===== 分页 ===== */
.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
