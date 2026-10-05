<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? 'var(--sidebar-collapsed)' : 'var(--sidebar-width)'" class="aside">
      <!-- Logo -->
      <div class="logo-area">
        <div class="logo-icon">
          <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="32" height="32" rx="8" fill="var(--color-amber)"/>
            <path d="M8 12L16 8L24 12V20L16 24L8 20V12Z" stroke="white" stroke-width="2" fill="none"/>
            <circle cx="16" cy="16" r="3" fill="white"/>
          </svg>
        </div>
        <transition name="fade-text">
          <span v-show="!isCollapse" class="logo-text">缝迹</span>
        </transition>
      </div>

      <!-- 导航菜单 -->
      <el-menu
        :default-active="route.path"
        :default-openeds="defaultOpeneds"
        router
        :collapse="isCollapse"
        :collapse-transition="false"
        class="nav-menu"
      >
        <!-- 工作概览 -->
        <el-sub-menu index="group-overview" class="menu-group">
          <template #title>
            <el-icon class="menu-icon"><HomeFilled /></el-icon>
            <span class="menu-label">工作概览</span>
          </template>
          <el-menu-item index="/dashboard" class="menu-item">
            <el-icon class="menu-icon"><HomeFilled /></el-icon>
            <template #title><span class="menu-label">工作台</span></template>
          </el-menu-item>
          <el-menu-item v-if="userStore.isBoss() || userStore.isSupervisor() || userStore.isPenetrating()" index="/dashboard-board" class="menu-item">
            <el-icon class="menu-icon"><DataLine /></el-icon>
            <template #title><span class="menu-label">工作看板</span></template>
          </el-menu-item>
          <el-menu-item v-if="userStore.isBoss() || userStore.isSupervisor() || userStore.isPenetrating()" index="/history-board" class="menu-item">
            <el-icon class="menu-icon"><Clock /></el-icon>
            <template #title><span class="menu-label">历史工作</span></template>
          </el-menu-item>
          <el-menu-item v-if="userStore.isBoss() || userStore.isSupervisor() || userStore.isPenetrating()" index="/employee-work" class="menu-item">
            <el-icon class="menu-icon"><User /></el-icon>
            <template #title><span class="menu-label">员工工作</span></template>
          </el-menu-item>
          <el-menu-item v-if="userStore.isBoss() || userStore.isPenetrating()" index="/shipment-daily" class="menu-item">
            <el-icon class="menu-icon"><Van /></el-icon>
            <template #title><span class="menu-label">每日出货</span></template>
          </el-menu-item>
          <el-menu-item v-if="userStore.isBoss() || userStore.isPenetrating()" index="/unshipped" class="menu-item">
            <el-icon class="menu-icon"><Box /></el-icon>
            <template #title><span class="menu-label">未出货清单</span></template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 生产管理 -->
        <el-sub-menu v-if="userStore.isBoss() || userStore.isSupervisor() || userStore.isPenetrating()" index="group-production" class="menu-group">
          <template #title>
            <el-icon class="menu-icon"><Tickets /></el-icon>
            <span class="menu-label">生产管理</span>
          </template>
          <el-menu-item v-if="userStore.isBoss() || userStore.isSupervisor() || userStore.isPenetrating()" index="/style" class="menu-item">
            <el-icon class="menu-icon"><Collection /></el-icon>
            <template #title><span class="menu-label">生产管理</span></template>
          </el-menu-item>
          <el-menu-item v-if="userStore.isBoss() || userStore.isSupervisor() || userStore.isPenetrating()" index="/quick-entry" class="menu-item">
            <el-icon class="menu-icon"><DocumentAdd /></el-icon>
            <template #title><span class="menu-label">快速录入</span></template>
          </el-menu-item>
          <el-menu-item v-if="userStore.isBoss() || userStore.isPenetrating()" index="/process-template" class="menu-item">
            <el-icon class="menu-icon"><Notebook /></el-icon>
            <template #title><span class="menu-label">工序模板</span></template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 审核通知 -->
        <el-sub-menu v-if="userStore.isBoss() || userStore.isSupervisor() || userStore.isPenetrating()" index="group-review" class="menu-group">
          <template #title>
            <el-icon class="menu-icon"><CircleCheck /></el-icon>
            <span class="menu-label">审核通知</span>
          </template>
          <el-menu-item v-if="userStore.isBoss() || userStore.isSupervisor() || userStore.isPenetrating()" index="/work-review" class="menu-item">
            <el-icon class="menu-icon"><CircleCheck /></el-icon>
            <template #title><span class="menu-label">报工审核</span></template>
          </el-menu-item>
          <el-menu-item v-if="userStore.isBoss() || userStore.isPenetrating()" index="/boss-notifications" class="menu-item notify-menu-item">
            <el-icon class="menu-icon"><Bell /></el-icon>
            <template #title>
              <span class="menu-label">通知</span>
              <span v-if="bossNotifyCount > 0" class="notify-badge">{{ bossNotifyCount > 99 ? '99+' : bossNotifyCount }}</span>
            </template>
            <span v-if="bossNotifyCount > 0" class="notify-dot"></span>
            <!-- 分类小标签（展开时显示） -->
            <div v-if="!isCollapse && bossNotifyCount > 0" class="notify-type-tags">
              <span v-if="bossNotifyDetail.returned > 0" class="type-tag tag-return">{{ bossNotifyDetail.returned }}</span>
              <span v-if="bossNotifyDetail.review > 0" class="type-tag tag-review">{{ bossNotifyDetail.review }}</span>
              <span v-if="bossNotifyDetail.adjustment > 0" class="type-tag tag-adjust">{{ bossNotifyDetail.adjustment }}</span>
            </div>
          </el-menu-item>
          <el-menu-item v-if="userStore.isBoss() || userStore.isSupervisor() || userStore.isPenetrating()" index="/notification-manage" class="menu-item">
            <el-icon class="menu-icon"><Promotion /></el-icon>
            <template #title><span class="menu-label">发通知</span></template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 人事财务 -->
        <el-sub-menu v-if="userStore.isBoss() || userStore.isSupervisor() || userStore.isPenetrating()" index="group-hr" class="menu-group">
          <template #title>
            <el-icon class="menu-icon"><User /></el-icon>
            <span class="menu-label">人事财务</span>
          </template>
          <el-menu-item v-if="userStore.isBoss() || userStore.isPenetrating()" index="/employee" class="menu-item">
            <el-icon class="menu-icon"><User /></el-icon>
            <template #title><span class="menu-label">员工管理</span></template>
          </el-menu-item>
          <el-menu-item v-if="userStore.isBoss() || userStore.isSupervisor() || userStore.isPenetrating()" index="/wage-adjustment" class="menu-item">
            <el-icon class="menu-icon"><Wallet /></el-icon>
            <template #title><span class="menu-label">工资调整</span></template>
          </el-menu-item>
          <el-menu-item v-if="userStore.isBoss() || userStore.isPenetrating()" index="/wage-settlement" class="menu-item">
            <el-icon class="menu-icon"><Money /></el-icon>
            <template #title><span class="menu-label">工资结算</span></template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 数据分析 -->
        <el-sub-menu v-if="userStore.isBoss() || userStore.isPenetrating()" index="group-analysis" class="menu-group">
          <template #title>
            <el-icon class="menu-icon"><TrendCharts /></el-icon>
            <span class="menu-label">数据分析</span>
          </template>
          <el-menu-item v-if="userStore.isBoss() || userStore.isPenetrating()" index="/analysis-api" class="menu-item">
            <el-icon class="menu-icon"><TrendCharts /></el-icon>
            <template #title><span class="menu-label">数据分析</span></template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 系统管理（超级管理员） -->
        <el-sub-menu v-if="userStore.isSuperAdmin()" index="group-system" class="menu-group">
          <template #title>
            <el-icon class="menu-icon"><Setting /></el-icon>
            <span class="menu-label">系统管理</span>
          </template>
          <el-menu-item index="/renew-logs" class="menu-item">
            <el-icon class="menu-icon"><Timer /></el-icon>
            <template #title><span class="menu-label">续费记录</span></template>
          </el-menu-item>
          <el-menu-item index="/company-manage" class="menu-item">
            <el-icon class="menu-icon"><OfficeBuilding /></el-icon>
            <template #title><span class="menu-label">公司管理</span></template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <!-- 底部折叠按钮 -->
      <div class="sidebar-footer">
        <div class="collapse-trigger" @click="isCollapse = !isCollapse">
          <el-icon :size="18">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
        </div>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="main-wrapper">
      <el-header class="header">
        <div class="header-left">
          <div class="breadcrumb-area">
            <span class="page-title">{{ pageTitle }}</span>
          </div>
        </div>
        <div class="header-right">
          <div v-if="userStore.isPenetrating()" class="header-penetrate">
            <el-icon :size="14"><View /></el-icon>
            <span>正在查看：<strong>{{ userStore.currentCompanyName }}</strong></span>
            <el-button type="warning" size="small" plain @click="handleExitCompany" class="exit-btn">
              <el-icon><Back /></el-icon> 返回超管
            </el-button>
          </div>
          <div v-else-if="userStore.currentCompanyName" class="header-company">
            <el-icon :size="14"><OfficeBuilding /></el-icon>
            <span>{{ userStore.currentCompanyName }}</span>
          </div>
          <div class="header-time">{{ currentTime }}</div>
          <ThemeToggle />
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-avatar-btn">
              <div class="avatar">{{ avatarLetter }}</div>
              <span class="user-name">{{ userStore.realName || userStore.username }}</span>
              <span v-if="roleLabel" class="user-role">{{ roleLabel }}</span>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="changePassword">
                  <el-icon><Lock /></el-icon>
                  修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>

      <footer class="icp-footer">
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">粤ICP备2026085098号</a>
      </footer>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="420px"
      destroy-on-close
    >
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordRef" label-width="80px">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（至少6位）"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 公告弹窗（主管/工人可见） -->
    <el-dialog
      v-model="showAnnouncement"
      :title="currentAnnouncement.title || '新公告'"
      width="480px"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="announcement-body">
        <div class="announcement-sender" v-if="currentAnnouncement.senderName">
          发送人：{{ currentAnnouncement.senderName }}
        </div>
        <div class="announcement-content">{{ currentAnnouncement.content }}</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="handleAcknowledge" style="width: 100%;">已知晓</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import ThemeToggle from '../../components/ThemeToggle.vue'
import { changePassword } from '../../api/auth'
import { ElMessage } from 'element-plus'
import { HomeFilled, Fold, Expand, ArrowDown, SwitchButton, OfficeBuilding, Timer, User, Collection, Tickets, CircleCheck, Notebook, Money, DataLine, Lock, Wallet, Bell, Promotion, View, Back, Clock, TrendCharts, Setting, DocumentAdd, Van, Box } from '@element-plus/icons-vue'
import { getSupervisorNotifications, getBossNotifications } from '../../api/dashboard'
import { getMyNotifications, markAsRead } from '../../api/notification'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const defaultOpeneds = ref([])
const currentTime = ref('')
const notifyCount = ref(0)
const bossNotifyCount = ref(0)
const bossNotifyDetail = ref({ returned: 0, review: 0, adjustment: 0 })

// 公告弹窗状态
const showAnnouncement = ref(false)
const currentAnnouncement = ref({})
const announcementQueue = ref([])

const pageTitle = computed(() => {
  const map = { '/dashboard': '工作台', '/dashboard-board': '工作看板', '/history-board': '历史工作', '/employee': '员工管理', '/style': '生产管理', '/quick-entry': '快速录入', '/process-template': '工序模板', '/bundle': '生产管理', '/work-review': '报工审核', '/work-records': '工作记录', '/wage-settlement': '工资结算', '/wage-adjustment': '工资调整', '/renew-logs': '续费记录', '/company-manage': '公司管理', '/notifications': '通知', '/boss-notifications': '通知', '/notification-manage': '发通知', '/analysis-api': '数据分析' }
  if (route.path.startsWith('/wage-settlement/')) return '结算详情'
  return map[route.path] || '工作台'
})

const avatarLetter = computed(() => {
  const name = userStore.realName || userStore.username || '?'
  return name.charAt(0).toUpperCase()
})

const roleLabel = computed(() => {
  if (userStore.isSuperAdmin()) return '超级管理员'
  const map = { boss: '老板', supervisor: '主管' }
  return map[userStore.currentRole] || ''
})

let timer = null
function updateTime() {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const w = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()]
  currentTime.value = `周${w}  ${h}:${m}`
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 30000)
  loadNotifyCount()
  checkAnnouncements()
})

// 离开通知页面时刷新红点
const stopWatch = watch(() => route.path, (newPath, oldPath) => {
  if (oldPath === '/notifications' && newPath !== '/notifications') {
    loadNotifyCount()
  }
  if (newPath === '/notifications') {
    notifyCount.value = 0
  }
  if (oldPath === '/boss-notifications' && newPath !== '/boss-notifications') {
    loadNotifyCount()
  }
  if (newPath === '/boss-notifications') {
    bossNotifyCount.value = 0
    bossNotifyDetail.value = { returned: 0, review: 0, adjustment: 0 }
  }
})
onUnmounted(() => { clearInterval(timer); stopWatch() })

async function loadNotifyCount() {
  // 超级管理员未穿透时，不加载业务通知
  if (userStore.isSuperAdmin() && !userStore.isPenetrating()) {
    return
  }
  if (userStore.isSupervisor()) {
    try {
      const res = await getSupervisorNotifications()
      const d = res.data || {}
      notifyCount.value = (d.returnedCount || 0) + (d.reviewCount || 0) + (d.adjustmentCount || 0)
    } catch (err) {
      console.error('加载主管通知计数失败:', err)
    }
  }
  if (userStore.isBoss()) {
    try {
      const res = await getBossNotifications()
      const d = res.data || {}
      bossNotifyCount.value = (d.returnedCount || 0) + (d.reviewCount || 0) + (d.adjustmentCount || 0)
      bossNotifyDetail.value = {
        returned: d.returnedCount || 0,
        review: d.reviewCount || 0,
        adjustment: d.adjustmentCount || 0
      }
    } catch (err) {
      console.error('加载老板通知计数失败:', err)
    }
  }
}

// 检查未读公告并弹窗（仅主管/工人）
async function checkAnnouncements() {
  if (userStore.isBoss() || userStore.isSuperAdmin()) return // 老板是发送方，不弹窗
  try {
    const res = await getMyNotifications()
    const list = res.data || []
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
  announcementQueue.value.shift()
  if (announcementQueue.value.length > 0) {
    currentAnnouncement.value = announcementQueue.value[0]
  } else {
    showAnnouncement.value = false
    currentAnnouncement.value = {}
    loadNotifyCount()
  }
}

// 修改密码
const showPasswordDialog = ref(false)
const passwordLoading = ref(false)
const passwordRef = ref(null)
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleChangePassword() {
  const valid = await passwordRef.value.validate().catch(() => false)
  if (!valid) return
  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    showPasswordDialog.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    passwordLoading.value = false
  }
}

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (cmd === 'changePassword') {
    showPasswordDialog.value = true
  }
}

function handleExitCompany() {
  userStore.exitCompany()
  router.push('/dashboard').then(() => {
    window.location.reload()
  })
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: var(--bg-page);
}

/* ===== 侧边栏 ===== */
.aside {
  background: var(--color-charcoal);
  display: flex;
  flex-direction: column;
  transition: width 0.3s var(--ease-out);
  position: relative;
  z-index: 10;
}

.logo-area {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
  white-space: nowrap;
}

/* 菜单 */
.nav-menu {
  flex: 1;
  background: transparent !important;
  padding: 8px 10px;
  overflow-y: auto;
}

.nav-menu :deep(.menu-item) {
  height: 44px;
  line-height: 44px;
  margin-bottom: 2px;
  border-radius: var(--radius-sm) !important;
  color: rgba(255,255,255,0.55) !important;
  transition: all 0.2s var(--ease-out);
}

.nav-menu :deep(.menu-item:hover) {
  background: rgba(255,255,255,0.06) !important;
  color: rgba(255,255,255,0.85) !important;
}

.nav-menu :deep(.menu-item.is-active) {
  background: var(--color-amber) !important;
  color: #fff !important;
  font-weight: 500;
}

.nav-menu :deep(.menu-item.is-active .menu-icon) {
  color: #fff !important;
}

.menu-icon {
  font-size: 18px;
}

.menu-label {
  font-size: 13.5px;
}

/* 子菜单分组 */
.nav-menu :deep(.menu-group) {
  margin-bottom: 2px;
}

.nav-menu :deep(.menu-group > .el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  border-radius: var(--radius-sm) !important;
  color: rgba(255,255,255,0.55) !important;
  transition: all 0.2s var(--ease-out);
}

.nav-menu :deep(.menu-group > .el-sub-menu__title:hover) {
  background: rgba(255,255,255,0.06) !important;
  color: rgba(255,255,255,0.85) !important;
}

.nav-menu :deep(.menu-group .el-menu) {
  background: transparent !important;
  padding: 2px 0;
}

.nav-menu :deep(.menu-group .el-menu .menu-item) {
  padding-left: 52px !important;
  height: 40px;
  line-height: 40px;
}

.nav-menu :deep(.el-sub-menu .el-sub-menu__icon-arrow) {
  color: rgba(255,255,255,0.35);
  font-size: 12px;
}

/* 通知红点 */
.notify-menu-item {
  position: relative;
}

.notify-dot {
  position: absolute;
  top: 10px;
  left: 28px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  z-index: 1;
}

.nav-menu :deep(.is-active) .notify-dot {
  background: var(--bg-card);
  top: 10px;
  left: 28px;
}

.notify-badge {
  display: inline-block;
  font-size: 11px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  border-radius: 9px;
  background: #ef4444;
  color: #fff;
  padding: 0 4px;
  margin-left: 6px;
  font-weight: 600;
}

.nav-menu :deep(.is-active) .notify-badge {
  background: rgba(255, 255, 255, 0.3);
}

/* 通知分类小标签 */
.notify-type-tags {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  gap: 3px;
  z-index: 1;
}

.type-tag {
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  text-align: center;
  border-radius: 8px;
  padding: 0 4px;
  font-weight: 600;
}

.tag-return {
  background: #fef2f2;
  color: #ef4444;
}

.tag-review {
  background: #eff6ff;
  color: #3b82f6;
}

.tag-adjust {
  background: #fef3c7;
  color: #d97706;
}

.nav-menu :deep(.is-active) .type-tag {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

/* 侧边栏底部 */
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.collapse-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  border-radius: var(--radius-sm);
  color: rgba(255,255,255,0.4);
  cursor: pointer;
  transition: all 0.2s;
}

.collapse-trigger:hover {
  background: rgba(255,255,255,0.08);
  color: rgba(255,255,255,0.7);
}

/* ===== 顶栏 ===== */
.header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: var(--bg-header);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.page-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-company {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: rgba(232, 168, 56, 0.1);
  border-radius: 20px;
  font-size: 13px;
  color: var(--color-amber);
  font-weight: 500;
}

.header-penetrate {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 6px 4px 14px;
  background: linear-gradient(135deg, rgba(234, 88, 12, 0.08), rgba(234, 88, 12, 0.15));
  border: 1px solid rgba(234, 88, 12, 0.2);
  border-radius: 20px;
  font-size: 13px;
  color: #ea580c;
  font-weight: 500;
}

.header-penetrate strong {
  color: #c2410c;
  font-weight: 700;
}

.header-penetrate .exit-btn {
  margin-left: 4px;
  border-radius: 14px;
  font-size: 12px;
  padding: 4px 12px;
}

.header-time {
  font-size: 13px;
  color: var(--color-mist);
  font-variant-numeric: tabular-nums;
}

.user-avatar-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px 4px 4px;
  border-radius: var(--radius-xl);
  transition: background 0.2s;
}

.user-avatar-btn:hover {
  background: var(--color-cream-dark);
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-amber), var(--color-terracotta));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-name {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text-primary);
}

.user-role {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 10px;
  background: rgba(232, 168, 56, 0.12);
  color: var(--color-amber);
  font-weight: 500;
  white-space: nowrap;
}

.arrow-icon {
  font-size: 12px;
  color: var(--color-mist);
}

/* ===== 内容区 ===== */
.main-wrapper {
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 24px 28px;
  background: var(--bg-page);
  overflow-y: auto;
}

/* ===== ICP 备案号 ===== */
.icp-footer {
  flex-shrink: 0;
  padding: 8px 0;
  text-align: center;
  font-size: 12px;
  background: var(--bg-page);
  border-top: 1px solid var(--border-color, rgba(0, 0, 0, 0.06));
}

.icp-footer a {
  color: var(--color-mist, #909399);
  text-decoration: none;
}

.icp-footer a:hover {
  color: var(--color-accent, #e8a838);
}

/* ===== 过渡动画 ===== */
.fade-text-enter-active { transition: opacity 0.2s; }
.fade-text-leave-active { transition: opacity 0.1s; }
.fade-text-enter-from, .fade-text-leave-to { opacity: 0; }

.page-fade-enter-active { transition: all 0.25s var(--ease-out); }
.page-fade-leave-active { transition: all 0.15s ease-in; }
.page-fade-enter-from { opacity: 0; transform: translateY(8px); }
.page-fade-leave-to { opacity: 0; }

/* 公告弹窗样式 */
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
</style>
