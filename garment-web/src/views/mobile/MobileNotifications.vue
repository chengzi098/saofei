<template>
  <div class="mobile-notifications">
    <!-- 顶部操作 -->
    <div class="top-bar" v-if="allNotifications.length > 0">
      <span class="total-count">{{ allNotifications.length }} 条通知</span>
      <span class="read-all-btn" @click="handleReadAll">全部已读</span>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="allNotifications.length === 0" class="empty-state">
      <el-icon :size="56" color="#d1d5db"><Bell /></el-icon>
      <p class="empty-title">暂无通知</p>
      <p class="empty-tip">公告和退回的消息会在这里显示</p>
    </div>

    <!-- 通知列表 -->
    <div v-else class="notify-list">
      <div
        v-for="item in allNotifications"
        :key="item.key"
        class="notify-card"
        :class="getCardClass(item.notifyType)"
        @click="handleClick(item)"
      >
        <!-- 未读标记 -->
        <div v-if="item.notifyType === 'announcement' && !item.read" class="unread-dot"></div>

        <div class="notify-content">
          <div class="notify-header">
            <span class="notify-type">{{ getTypeLabel(item.notifyType) }}</span>
            <span class="notify-time">{{ formatTime(item.time) }}</span>
          </div>

          <!-- 公告 -->
          <template v-if="item.notifyType === 'announcement'">
            <div class="notify-title">{{ item.title }}</div>
            <div class="notify-desc" :class="{ collapsed: !item.expanded }">{{ item.content }}</div>
            <div v-if="item.content && item.content.length > 60" class="expand-btn" @click.stop="item.expanded = !item.expanded">
              {{ item.expanded ? '收起' : '展开全文' }}
            </div>
          </template>

          <!-- 退回 -->
          <template v-if="item.notifyType === 'return'">
            <div class="notify-title">{{ item.bundleNo }} · {{ item.processName }}</div>
            <div class="notify-desc">{{ item.quantity }}件</div>
            <div v-if="item.returnReason" class="notify-reason">{{ item.returnReason }}</div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { Bell, WarningFilled, Loading } from '@element-plus/icons-vue'
import { getWorkRecordList } from '../../api/workRecord'
import { markNotificationsViewed } from '../../api/dashboard'
import { getMyNotifications, markAsRead, markAllAsRead } from '../../api/notification'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)

const notificationsCtx = inject('notifications', null)
const parentLoadNotifications = notificationsCtx ? notificationsCtx.loadNotifications : null

const returnedRecords = ref([])
const announcements = ref([])

// 合并通知
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

  for (const r of returnedRecords.value) {
    list.push({
      ...r,
      key: 'return-' + r.id,
      notifyType: 'return',
      time: r.returnTime || r.createTime
    })
  }

  list.sort((a, b) => new Date(b.time) - new Date(a.time))
  return list
})

onMounted(() => {
  loadAll()
})

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadAnnouncements(), loadReturned()])
    await markNotificationsViewed('all')
    if (parentLoadNotifications) parentLoadNotifications()
  } catch (err) {
    console.error('加载通知失败:', err)
  } finally {
    loading.value = false
  }
}

async function loadAnnouncements() {
  try {
    const res = await getMyNotifications()
    announcements.value = (res.data || []).map(n => ({ ...n, expanded: false }))
  } catch (err) {
    console.error('加载公告失败:', err)
  }
}

async function loadReturned() {
  try {
    const res = await getWorkRecordList({
      pageNum: 1,
      pageSize: 50,
      workerId: userStore.userId,
      status: 3
    })
    returnedRecords.value = res.data?.records || []
  } catch (err) {
    console.error('加载退回记录失败:', err)
  }
}

function handleClick(item) {
  if (item.notifyType === 'announcement') {
    if (!item.read) {
      markAsRead(item.id).then(() => { item.read = true }).catch(() => {})
    }
  } else if (item.notifyType === 'return') {
    router.push('/mobile/records')
  }
}

async function handleReadAll() {
  try {
    await markAllAsRead()
    announcements.value.forEach(n => { n.read = true })
    if (parentLoadNotifications) parentLoadNotifications()
  } catch (err) {
    console.error('全部已读失败:', err)
  }
}

function getTypeLabel(type) {
  const map = { announcement: '公告', return: '退回' }
  return map[type] || ''
}

function getCardClass(type) {
  const map = { announcement: 'card-announcement', return: 'card-return' }
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
</script>

<style scoped>
.mobile-notifications {
  padding: 12px 16px;
  padding-bottom: 100px;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.total-count {
  font-size: 13px;
  color: #6b7280;
}

.read-all-btn {
  font-size: 13px;
  color: var(--color-amber, #d97706);
  font-weight: 500;
  cursor: pointer;
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
  background: var(--bg-card);
  border-radius: 12px;
  padding: 14px;
  position: relative;
  border-left: 3px solid transparent;
}

.card-announcement {
  border-left-color: #8b5cf6;
}

.card-return {
  border-left-color: #ef4444;
}

.unread-dot {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
}

.notify-content {
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
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate, #2d2a26);
  margin-bottom: 4px;
}

.notify-desc {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
  white-space: pre-wrap;
}

.notify-desc.collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.expand-btn {
  font-size: 12px;
  color: var(--color-amber, #d97706);
  margin-top: 4px;
  cursor: pointer;
  font-weight: 500;
}

.notify-reason {
  margin-top: 6px;
  padding: 5px 8px;
  background: #fef2f2;
  border-radius: 6px;
  font-size: 12px;
  color: #ef4444;
}
</style>
