<template>
  <div class="mobile-layout">
    <!-- 顶部状态栏占位 -->
    <div class="status-bar-placeholder"></div>

    <!-- 顶部标题栏 -->
    <div class="mobile-header" v-if="showHeader">
      <div class="header-left">
        <span class="header-title">{{ pageTitle }}</span>
      </div>
      <div class="header-right">
        <span class="company-name" v-if="userStore.currentCompanyName">
          {{ userStore.currentCompanyName }}
        </span>
        <ThemeToggle size="small" />
      </div>
    </div>

    <!-- 主内容区 -->
    <main class="mobile-main" :class="{ 'has-header': showHeader }">
      <router-view v-slot="{ Component }">
        <transition name="page-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 底部导航 -->
    <nav class="mobile-tabbar">
      <router-link
        v-for="tab in tabs"
        :key="tab.path"
        :to="tab.path"
        class="tab-item"
        :class="{ active: isActive(tab.path) }"
      >
        <div class="tab-icon">
          <component :is="tab.icon" />
          <span v-if="tab.badge" class="tab-badge"></span>
        </div>
        <span class="tab-label">{{ tab.label }}</span>
      </router-link>
    </nav>

    <!-- 底部安全区 -->
    <div class="safe-area-bottom"></div>

    <!-- 公告弹窗 -->
    <el-dialog
      v-model="showAnnouncement"
      :title="currentAnnouncement.title || '新公告'"
      width="90%"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      class="announcement-dialog"
    >
      <div class="announcement-body">
        <div class="announcement-sender" v-if="currentAnnouncement.senderName">
          发送人：{{ currentAnnouncement.senderName }}
        </div>
        <div class="announcement-content">{{ currentAnnouncement.content }}</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="handleAcknowledge" class="ack-btn">已知晓</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, provide } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import ThemeToggle from '../../components/ThemeToggle.vue'
import { HomeFilled, Camera, Document, Bell, User } from '@element-plus/icons-vue'
import { getWorkerNotifications } from '../../api/dashboard'
import { getUnreadCount, getMyNotifications, markAsRead, markAllAsRead } from '../../api/notification'

const route = useRoute()
const userStore = useUserStore()

// 通知红点状态
const hasNotification = ref(false)

// 公告弹窗状态
const showAnnouncement = ref(false)
const currentAnnouncement = ref({})
const announcementQueue = ref([])

// 提供给子组件刷新通知的方法
provide('notifications', {
  hasNotification,
  loadNotifications
})

const tabs = computed(() => [
  { path: '/mobile/home', label: '工作台', icon: HomeFilled },
  { path: '/mobile/scan', label: '扫码', icon: Camera },
  { path: '/mobile/records', label: '记录', icon: Document },
  { path: '/mobile/notifications', label: '通知', icon: Bell, badge: hasNotification.value },
  { path: '/mobile/profile', label: '我的', icon: User }
])

const isActive = (path) => route.path === path

// 加载通知计数
async function loadNotifications() {
  if (!userStore.currentCompanyId) return
  try {
    const [workerRes, unreadRes] = await Promise.all([
      getWorkerNotifications(),
      getUnreadCount()
    ])
    const data = workerRes.data || {}
    const unreadData = unreadRes.data || {}
    hasNotification.value = (data.returnedCount || 0) > 0
      || (data.adjustmentCount || 0) > 0
      || (data.settlementCount || 0) > 0
      || (unreadData.count || 0) > 0
  } catch (err) {
    console.error('加载通知失败:', err)
  }
}

onMounted(() => {
  loadNotifications()
  checkAnnouncements()
})

// 切换公司时重新加载
watch(() => userStore.currentCompanyId, () => {
  loadNotifications()
  checkAnnouncements()
})

// 检查未读公告并弹窗
async function checkAnnouncements() {
  if (!userStore.currentCompanyId) return
  try {
    const res = await getMyNotifications()
    const list = res.data || []
    // 筛选未读公告
    const unread = list.filter(n => !n.read)
    if (unread.length > 0) {
      announcementQueue.value = unread
      currentAnnouncement.value = unread[0]
      showAnnouncement.value = true
    }
  } catch (err) {
    console.error('检查公告失败:', err)
  }
}

// 点击已知晓
async function handleAcknowledge() {
  const current = currentAnnouncement.value
  if (current && current.id) {
    try {
      await markAsRead(current.id)
    } catch (err) {
      console.error('标记已读失败:', err)
    }
  }
  // 移除当前公告，显示下一条
  announcementQueue.value.shift()
  if (announcementQueue.value.length > 0) {
    currentAnnouncement.value = announcementQueue.value[0]
  } else {
    showAnnouncement.value = false
    currentAnnouncement.value = {}
    // 刷新红点
    loadNotifications()
  }
}

// 进入通知页面时由通知页面主动调用 loadNotifications 刷新红点

const showHeader = computed(() => {
  return route.path !== '/mobile/scan'
})

const pageTitle = computed(() => {
  const map = {
    '/mobile/home': '工作台',
    '/mobile/records': '报工记录',
    '/mobile/notifications': '通知',
    '/mobile/profile': '个人中心'
  }
  return map[route.path] || '缝迹'
})
</script>

<style scoped>
.mobile-layout {
  min-height: 100vh;
  background: var(--bg-page);
  display: flex;
  flex-direction: column;
  max-width: 430px;
  margin: 0 auto;
  position: relative;
}

/* 顶部状态栏占位（iPhone刘海屏） */
.status-bar-placeholder {
  height: env(safe-area-inset-top, 0px);
  background: var(--bg-card);
  flex-shrink: 0;
}

/* 顶部标题栏 */
.mobile-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.company-name {
  font-size: 12px;
  color: var(--color-amber, #d97706);
  background: rgba(232, 168, 56, 0.1);
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 主内容区 */
.mobile-main {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.mobile-main::-webkit-scrollbar {
  display: none;
}

.mobile-main.has-header {
  padding-bottom: 80px;
}

/* 底部导航 */
.mobile-tabbar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 430px;
  height: calc(56px + env(safe-area-inset-bottom, 0px));
  display: flex;
  align-items: flex-start;
  justify-content: space-around;
  padding-top: 6px;
  background: var(--bg-tabbar);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid var(--border-color);
  z-index: 1000;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 4px 16px;
  text-decoration: none;
  color: #9ca3af;
  transition: all 0.2s ease;
  border-radius: 12px;
}

.tab-item.active {
  color: var(--color-amber, #d97706);
}

.tab-icon {
  font-size: 22px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease;
  position: relative;
}

.tab-item.active .tab-icon {
  transform: scale(1.1);
}

.tab-label {
  font-size: 11px;
  font-weight: 500;
}

.tab-badge {
  position: absolute;
  top: -2px;
  right: -4px;
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
  border: 1.5px solid #fff;
}

/* 安全区 */
.safe-area-bottom {
  height: calc(56px + env(safe-area-inset-bottom, 0px));
  flex-shrink: 0;
}

/* 页面过渡动画 */
.page-slide-enter-active {
  transition: all 0.25s ease-out;
}
.page-slide-leave-active {
  transition: all 0.15s ease-in;
}
.page-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}
.page-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 公告弹窗样式 */
.announcement-dialog :deep(.el-dialog) {
  border-radius: 16px;
  margin-top: 30vh;
}

.announcement-dialog :deep(.el-dialog__header) {
  padding: 20px 20px 10px;
  margin: 0;
}

.announcement-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
}

.announcement-dialog :deep(.el-dialog__body) {
  padding: 10px 20px;
}

.announcement-dialog :deep(.el-dialog__footer) {
  padding: 10px 20px 20px;
}

.announcement-body {
  min-height: 60px;
}

.announcement-sender {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 12px;
}

.announcement-content {
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.7;
  white-space: pre-wrap;
}

.ack-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
}
</style>
