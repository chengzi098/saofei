<template>
  <div class="notification-manage">
    <!-- 顶部标题区 -->
    <div class="hero">
      <div class="hero-left">
        <div class="hero-badge">
          <el-icon size="16"><Bell /></el-icon>
        </div>
        <div>
          <h1 class="hero-title">通知中心</h1>
          <p class="hero-desc">向员工推送通知，追踪阅读回执</p>
        </div>
      </div>
      <button class="compose-btn" @click="showSendDialog = true">
        <span class="compose-icon">
          <el-icon size="18"><Promotion /></el-icon>
        </span>
        <span>发通知</span>
      </button>
    </div>

    <!-- 统计条 -->
    <div class="stats-strip">
      <div class="stat-item" v-for="s in statsData" :key="s.label">
        <span class="stat-value" :style="{ color: s.color }">{{ s.value }}</span>
        <span class="stat-label">{{ s.label }}</span>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-row">
      <div class="filter-group">
        <el-select v-model="filterTarget" placeholder="接收范围" clearable size="default" @change="applyFilter">
          <el-option label="全部" :value="null" />
          <el-option label="全部员工" :value="0" />
          <el-option label="仅主管" :value="1" />
          <el-option label="仅工人" :value="2" />
        </el-select>
        <el-select v-model="filterReadStatus" placeholder="已读状态" clearable size="default" @change="applyFilter">
          <el-option label="全部" :value="null" />
          <el-option label="全部已读" :value="1" />
          <el-option label="有未读" :value="0" />
        </el-select>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="applyFilter"
        />
      </div>
      <transition name="fade">
        <button v-if="hasFilter" class="reset-btn" @click="resetFilter">
          <el-icon size="14"><Close /></el-icon>
          清除筛选
        </button>
      </transition>
    </div>

    <!-- 加载态 -->
    <div v-if="loading" class="state-box">
      <div class="spinner" />
      <span>正在加载通知...</span>
    </div>

    <!-- 空态 -->
    <div v-else-if="filteredNotifications.length === 0" class="state-box state-empty">
      <div class="empty-graphic">
        <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
          <rect x="8" y="12" width="48" height="40" rx="6" stroke="#d4d0c8" stroke-width="2" fill="#faf8f5"/>
          <path d="M8 22L32 38L56 22" stroke="#d4d0c8" stroke-width="2" fill="none"/>
          <circle cx="50" cy="18" r="8" fill="#e5a00d" opacity="0.15"/>
          <path d="M50 14v8M46 18h8" stroke="#e5a00d" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </div>
      <p class="empty-main">{{ hasFilter ? '没有匹配的通知' : '还没有发过通知' }}</p>
      <p class="empty-sub">{{ hasFilter ? '试试调整筛选条件' : '点击右上角「发通知」开始' }}</p>
    </div>

    <!-- 通知列表 -->
    <div v-else class="notify-list">
      <div
        v-for="(item, idx) in paginatedNotifications"
        :key="item.id"
        class="notify-card"
        :class="{ 'is-unread': !item.read }"
        :style="{ animationDelay: idx * 40 + 'ms' }"
        @click="showReadDetail(item)"
      >
        <!-- 左侧色条 -->
        <div class="card-bar" :class="'bar-' + getTargetClass(item.targetType)" />

        <div class="card-main">
          <!-- 头部：标题 + 标签 + 时间 -->
          <div class="card-top">
            <h3 class="card-title">{{ item.title }}</h3>
            <div class="card-meta">
              <span class="meta-tag" :class="'mtag-' + getTargetClass(item.targetType)">
                {{ getTargetLabel(item.targetType) }}
              </span>
              <span class="meta-dot" />
              <span class="meta-time">{{ formatTime(item.createTime) }}</span>
            </div>
          </div>

          <!-- 内容 -->
          <p class="card-content">{{ item.content }}</p>

          <!-- 底部：状态 + 操作 -->
          <div class="card-bottom">
            <span class="read-badge" :class="item.read ? 'badge-ok' : 'badge-pending'">
              <span class="badge-dot" />
              {{ item.read ? '全部已读' : '有未读' }}
            </span>
            <button class="card-delete" @click.stop="handleDelete(item.id)" title="删除">
              <el-icon size="14"><Delete /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="filteredNotifications.length > pageSize" class="pager">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredNotifications.length"
        layout="prev, pager, next"
        background
      />
    </div>

    <!-- ===== 发送通知弹窗 ===== -->
    <el-dialog
      v-model="showSendDialog"
      width="580px"
      destroy-on-close
      :show-close="false"
      class="compose-dialog"
    >
      <template #header>
        <div class="compose-head">
          <div class="compose-head-icon">
            <el-icon size="28"><Promotion /></el-icon>
          </div>
          <div class="compose-head-text">
            <h2>发送新通知</h2>
            <p>通知将即时推送给所选范围的员工</p>
          </div>
          <button class="compose-close" @click="showSendDialog = false">
            <el-icon size="18"><Close /></el-icon>
          </button>
        </div>
      </template>

      <el-form :model="sendForm" :rules="sendRules" ref="sendFormRef" label-width="0" class="compose-form">
        <div class="field-group">
          <label class="field-label">通知标题</label>
          <el-form-item prop="title">
            <el-input
              v-model="sendForm.title"
              placeholder="例如：明天加班通知"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>
        </div>

        <div class="field-group">
          <label class="field-label">通知内容</label>
          <el-form-item prop="content">
            <el-input
              v-model="sendForm.content"
              type="textarea"
              :rows="5"
              placeholder="详细描述通知内容..."
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>
        </div>

        <div class="field-group">
          <label class="field-label">发送对象</label>
          <el-form-item prop="targetType">
            <div class="audience-cards">
              <div
                v-for="opt in targetOptions"
                :key="opt.value"
                class="aud-card"
                :class="{ 'aud-active': sendForm.targetType === opt.value }"
                @click="sendForm.targetType = opt.value"
              >
                <div class="aud-icon" :class="'aud-' + opt.cls">
                  <el-icon size="22"><component :is="opt.icon" /></el-icon>
                </div>
                <div class="aud-text">
                  <span class="aud-name">{{ opt.label }}</span>
                  <span class="aud-hint">{{ opt.hint }}</span>
                </div>
                <div class="aud-check" v-if="sendForm.targetType === opt.value">
                  <el-icon size="14"><CircleCheck /></el-icon>
                </div>
              </div>
            </div>
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="compose-foot">
          <button class="foot-cancel" @click="showSendDialog = false">取消</button>
          <button class="foot-send" :class="{ 'is-loading': sendLoading }" :disabled="sendLoading" @click="handleSend">
            <span v-if="!sendLoading" class="foot-send-icon">
              <el-icon size="16"><Promotion /></el-icon>
            </span>
            <span v-if="sendLoading" class="foot-spinner" />
            <span>{{ sendLoading ? '发送中...' : '立即发送' }}</span>
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 已读详情弹窗 ===== -->
    <el-dialog
      v-model="showReadDetailDialog"
      width="580px"
      destroy-on-close
      :show-close="false"
      class="detail-dialog"
    >
      <template #header>
        <div class="detail-head">
          <div class="detail-head-text">
            <h2>{{ currentNotification.title || '通知详情' }}</h2>
            <p v-if="currentNotification.createTime">{{ formatTime(currentNotification.createTime) }}</p>
          </div>
          <button class="compose-close" @click="showReadDetailDialog = false">
            <el-icon size="18"><Close /></el-icon>
          </button>
        </div>
      </template>

      <div class="detail-body">
        <div class="detail-content-box">{{ currentNotification.content }}</div>

        <div class="detail-tabs">
          <button
            class="tab-btn"
            :class="{ 'tab-active': readDetailTab === 'read' }"
            @click="readDetailTab = 'read'"
          >
            已读
            <span v-if="readDetailData.readList?.length" class="tab-num tab-num-ok">
              {{ readDetailData.readList.length }}
            </span>
          </button>
          <button
            class="tab-btn"
            :class="{ 'tab-active': readDetailTab === 'unread' }"
            @click="readDetailTab = 'unread'"
          >
            未读
            <span v-if="readDetailData.unreadList?.length" class="tab-num tab-num-pending">
              {{ readDetailData.unreadList.length }}
            </span>
          </button>
        </div>

        <div class="detail-list" v-if="readDetailTab === 'read'">
          <div v-if="readDetailData.readList?.length === 0" class="detail-empty">暂无人阅读</div>
          <div v-for="p in readDetailData.readList" :key="p.userId" class="person-row">
            <div class="person-avatar" :class="p.role === 'supervisor' ? 'pav-boss' : 'pav-worker'">
              {{ p.userName?.charAt(0) }}
            </div>
            <div class="person-info">
              <span class="person-name">{{ p.userName }}</span>
              <span v-if="p.role === 'supervisor'" class="person-role">主管</span>
            </div>
            <span class="person-time">{{ p.readTime }}</span>
          </div>
        </div>

        <div class="detail-list" v-else>
          <div v-if="readDetailData.unreadList?.length === 0" class="detail-empty">全部已读 🎉</div>
          <div v-for="p in readDetailData.unreadList" :key="p.userId" class="person-row">
            <div class="person-avatar" :class="p.role === 'supervisor' ? 'pav-boss' : 'pav-worker'">
              {{ p.userName?.charAt(0) }}
            </div>
            <div class="person-info">
              <span class="person-name">{{ p.userName }}</span>
              <span v-if="p.role === 'supervisor'" class="person-role">主管</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Bell, Loading, CircleCheck, Delete, WarningFilled, Clock, Promotion, User, UserFilled, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sendNotification, getSentNotifications, deleteNotification, getReadDetail } from '../api/notification'

const loading = ref(true)
const notifications = ref([])
const showSendDialog = ref(false)
const sendLoading = ref(false)
const sendFormRef = ref(null)

// 筛选
const filterTarget = ref(null)
const filterReadStatus = ref(null)
const filterDateRange = ref(null)

// 分页
const currentPage = ref(1)
const pageSize = 10

// 已读详情
const showReadDetailDialog = ref(false)
const readDetailTab = ref('read')
const currentNotification = ref({})
const readDetailData = ref({ readList: [], unreadList: [] })

const sendForm = ref({
  title: '',
  content: '',
  targetType: 0
})

const sendRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

// 统计
const totalCount = computed(() => notifications.value.length)
const allReadCount = computed(() => notifications.value.filter(n => n.read).length)
const partialReadCount = computed(() => notifications.value.filter(n => !n.read).length)

const statsData = computed(() => [
  { label: '全部通知', value: totalCount.value, color: '#2d2a26' },
  { label: '已读完毕', value: allReadCount.value, color: '#16a34a' },
  { label: '尚有未读', value: partialReadCount.value, color: '#e5a00d' },
])

const hasFilter = computed(() => {
  return filterTarget.value !== null || filterReadStatus.value !== null || filterDateRange.value !== null
})

const targetOptions = [
  { value: 0, label: '全部员工', icon: 'UserFilled', cls: 'all', hint: '主管和工人都会收到' },
  { value: 1, label: '仅主管', icon: 'User', cls: 'sv', hint: '只发给主管' },
  { value: 2, label: '仅工人', icon: 'User', cls: 'wk', hint: '只发给工人' }
]

// 筛选后的通知
const filteredNotifications = computed(() => {
  let list = [...notifications.value]

  if (filterTarget.value !== null) {
    list = list.filter(n => n.targetType === filterTarget.value)
  }

  if (filterReadStatus.value !== null) {
    const isRead = filterReadStatus.value === 1
    list = list.filter(n => n.read === isRead)
  }

  if (filterDateRange.value && filterDateRange.value.length === 2) {
    const start = new Date(filterDateRange.value[0])
    start.setHours(0, 0, 0, 0)
    const end = new Date(filterDateRange.value[1])
    end.setHours(23, 59, 59, 999)
    list = list.filter(n => {
      const t = new Date(n.createTime)
      return t >= start && t <= end
    })
  }

  return list
})

// 分页后的通知
const paginatedNotifications = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredNotifications.value.slice(start, start + pageSize)
})

onMounted(() => {
  loadNotifications()
})

async function loadNotifications() {
  loading.value = true
  try {
    const res = await getSentNotifications()
    notifications.value = res.data || []
  } catch (err) {
    console.error('加载通知失败:', err)
  } finally {
    loading.value = false
  }
}

function applyFilter() {
  currentPage.value = 1
}

function resetFilter() {
  filterTarget.value = null
  filterReadStatus.value = null
  filterDateRange.value = null
  currentPage.value = 1
}

async function handleSend() {
  const valid = await sendFormRef.value.validate().catch(() => false)
  if (!valid) return

  sendLoading.value = true
  try {
    await sendNotification(sendForm.value)
    ElMessage.success('通知已发送')
    showSendDialog.value = false
    sendForm.value = { title: '', content: '', targetType: 0 }
    loadNotifications()
  } catch (err) {
    // 错误已由拦截器处理
  } finally {
    sendLoading.value = false
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除这条通知？删除后不可恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteNotification(id)
    ElMessage.success('已删除')
    loadNotifications()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除失败:', err)
    }
  }
}

async function showReadDetail(item) {
  currentNotification.value = item
  readDetailTab.value = 'read'
  showReadDetailDialog.value = true

  try {
    const res = await getReadDetail(item.id)
    readDetailData.value = res.data || { readList: [], unreadList: [] }
  } catch (err) {
    console.error('加载已读详情失败:', err)
    readDetailData.value = { readList: [], unreadList: [] }
  }
}

function getTargetLabel(type) {
  const map = { 0: '全部员工', 1: '仅主管', 2: '仅工人' }
  return map[type] || '全部'
}

function getTargetClass(type) {
  const map = { 0: 'all', 1: 'sv', 2: 'wk' }
  return map[type] || 'all'
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
/* ============================================================
   DESIGN TOKENS
   ============================================================ */
.notification-manage {
  --c-ink: #2d2a26;
  --c-ink-light: #5c574e;
  --c-ink-muted: #9a9488;
  --c-ink-faint: #c8c3b8;
  --c-surface: #ffffff;
  --c-surface-warm: #faf8f5;
  --c-surface-dim: #f3f1ec;
  --c-border: #e8e4dc;
  --c-amber: #e5a00d;
  --c-amber-soft: #fdf4e0;
  --c-green: #16a34a;
  --c-green-soft: #ecfdf5;
  --c-blue: #3b82f6;
  --c-blue-soft: #eff6ff;
  --c-red: #dc2626;
  width: 100%;
  animation: pageIn 0.3s ease-out;
}

@keyframes pageIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ============================================================
   HERO HEADER
   ============================================================ */
.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-badge {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: var(--c-ink);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--c-ink);
  margin: 0;
  letter-spacing: -0.03em;
  line-height: 1.2;
}

.hero-desc {
  font-size: 13px;
  color: var(--c-ink-muted);
  margin: 3px 0 0;
}

/* Compose button */
.compose-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  border: none;
  border-radius: 10px;
  background: var(--c-ink);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(45, 42, 38, 0.18);
}

.compose-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(45, 42, 38, 0.25);
  background: #3d3a34;
}

.compose-btn:active {
  transform: translateY(0);
}

.compose-icon {
  display: flex;
  align-items: center;
}

/* ============================================================
   STATS STRIP
   ============================================================ */
.stats-strip {
  display: flex;
  gap: 0;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 12px;
  margin-bottom: 20px;
  overflow: hidden;
}

.stat-item {
  flex: 1;
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 18px 24px;
  border-right: 1px solid var(--c-border);
}

.stat-item:last-child {
  border-right: none;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: var(--c-ink-muted);
  font-weight: 500;
}

/* ============================================================
   FILTER ROW
   ============================================================ */
.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-group :deep(.el-select) {
  width: 130px;
}

.filter-group :deep(.el-date-editor) {
  width: 260px;
}

.filter-group :deep(.el-input__wrapper) {
  border-radius: 8px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: none;
}

.filter-group :deep(.el-input__wrapper:hover) {
  border-color: var(--c-ink-faint);
}

.filter-group :deep(.el-input__wrapper.is-focus) {
  border-color: var(--c-ink);
  box-shadow: 0 0 0 2px rgba(45, 42, 38, 0.08);
}

.reset-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  background: var(--c-surface);
  color: var(--c-ink-light);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.reset-btn:hover {
  background: var(--c-surface-dim);
  border-color: var(--c-ink-faint);
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* ============================================================
   STATE BOXES (loading / empty)
   ============================================================ */
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 14px;
  color: var(--c-ink-muted);
  font-size: 14px;
}

.spinner {
  width: 28px;
  height: 28px;
  border: 3px solid var(--c-border);
  border-top-color: var(--c-ink);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.state-empty {
  gap: 10px;
}

.empty-graphic {
  margin-bottom: 8px;
  opacity: 0.7;
}

.empty-main {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-ink-light);
  margin: 0;
}

.empty-sub {
  font-size: 13px;
  color: var(--c-ink-faint);
  margin: 0;
}

/* ============================================================
   NOTIFICATION LIST
   ============================================================ */
.notify-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Card entry animation */
@keyframes cardSlide {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.notify-card {
  display: flex;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  animation: cardSlide 0.35s ease-out both;
  position: relative;
}

.notify-card:hover {
  border-color: var(--c-ink-faint);
  box-shadow: 0 2px 12px rgba(45, 42, 38, 0.06);
  transform: translateY(-1px);
}

.notify-card.is-unread {
  border-color: var(--c-ink-faint);
  background: var(--c-surface-warm);
}

/* 左侧色条 */
.card-bar {
  width: 4px;
  flex-shrink: 0;
}

.bar-all {
  background: var(--c-blue);
}

.bar-sv {
  background: var(--c-amber);
}

.bar-wk {
  background: var(--c-ink);
}

/* Card body */
.card-main {
  flex: 1;
  padding: 16px 18px;
  min-width: 0;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 6px;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--c-ink);
  margin: 0;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.meta-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
  white-space: nowrap;
}

.mtag-all {
  background: var(--c-blue-soft);
  color: var(--c-blue);
}

.mtag-sv {
  background: var(--c-amber-soft);
  color: var(--c-amber);
}

.mtag-wk {
  background: var(--c-surface-dim);
  color: var(--c-ink-light);
}

.meta-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--c-ink-faint);
}

.meta-time {
  font-size: 12px;
  color: var(--c-ink-muted);
  white-space: nowrap;
}

.card-content {
  font-size: 14px;
  color: var(--c-ink-light);
  line-height: 1.6;
  margin: 0 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* Read badge */
.read-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.badge-ok {
  background: var(--c-green-soft);
  color: var(--c-green);
}

.badge-ok .badge-dot {
  background: var(--c-green);
}

.badge-pending {
  background: var(--c-amber-soft);
  color: var(--c-amber);
}

.badge-pending .badge-dot {
  background: var(--c-amber);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* Delete button */
.card-delete {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--c-ink-faint);
  cursor: pointer;
  opacity: 0;
  transition: all 0.15s;
}

.notify-card:hover .card-delete {
  opacity: 1;
}

.card-delete:hover {
  background: #fef2f2;
  color: var(--c-red);
}

/* ============================================================
   PAGER
   ============================================================ */
.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.pager :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: var(--c-ink);
}

/* ============================================================
   COMPOSE DIALOG
   ============================================================ */
.compose-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.compose-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
}

.compose-dialog :deep(.el-dialog__body) {
  padding: 24px 28px 0;
}

.compose-dialog :deep(.el-dialog__footer) {
  padding: 0 28px 24px;
}

.compose-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px 28px 20px;
  background: var(--c-surface-warm);
  border-bottom: 1px solid var(--c-border);
  position: relative;
}

.compose-head-icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: var(--c-ink);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.compose-head-text h2 {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-ink);
  margin: 0;
}

.compose-head-text p {
  font-size: 13px;
  color: var(--c-ink-muted);
  margin: 3px 0 0;
}

.compose-close {
  position: absolute;
  top: 20px;
  right: 24px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--c-ink-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.compose-close:hover {
  background: var(--c-surface-dim);
  color: var(--c-ink);
}

/* Form fields */
.compose-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-ink-light);
}

.compose-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.compose-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px var(--c-border);
}

.compose-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--c-ink-faint);
}

.compose-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(45, 42, 38, 0.12);
}

.compose-form :deep(.el-textarea__inner) {
  border-radius: 10px;
  border: 1px solid var(--c-border);
  box-shadow: none;
}

.compose-form :deep(.el-textarea__inner:hover) {
  border-color: var(--c-ink-faint);
}

.compose-form :deep(.el-textarea__inner:focus) {
  border-color: var(--c-ink);
  box-shadow: 0 0 0 2px rgba(45, 42, 38, 0.08);
}

/* Audience cards */
.audience-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.aud-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 2px solid var(--c-border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
  background: var(--c-surface);
  position: relative;
}

.aud-card:hover {
  border-color: var(--c-ink-faint);
  background: var(--c-surface-warm);
}

.aud-card.aud-active {
  border-color: var(--c-ink);
  background: var(--c-surface-warm);
  box-shadow: 0 0 0 1px var(--c-ink);
}

.aud-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.aud-all {
  background: var(--c-blue-soft);
  color: var(--c-blue);
}

.aud-sv {
  background: var(--c-amber-soft);
  color: var(--c-amber);
}

.aud-wk {
  background: var(--c-surface-dim);
  color: var(--c-ink-light);
}

.aud-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.aud-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-ink);
}

.aud-hint {
  font-size: 12px;
  color: var(--c-ink-muted);
}

.aud-check {
  color: var(--c-ink);
  display: flex;
  align-items: center;
  animation: checkIn 0.2s ease-out;
}

@keyframes checkIn {
  from { transform: scale(0.5); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

/* Footer */
.compose-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 4px;
}

.foot-cancel {
  padding: 10px 20px;
  border: 1px solid var(--c-border);
  border-radius: 10px;
  background: var(--c-surface);
  color: var(--c-ink-light);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.foot-cancel:hover {
  background: var(--c-surface-dim);
  border-color: var(--c-ink-faint);
}

.foot-send {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  border: none;
  border-radius: 10px;
  background: var(--c-ink);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(45, 42, 38, 0.18);
}

.foot-send:hover:not(:disabled) {
  background: #3d3a34;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(45, 42, 38, 0.25);
}

.foot-send:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.foot-send-icon {
  display: flex;
  align-items: center;
}

.foot-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* ============================================================
   DETAIL DIALOG
   ============================================================ */
.detail-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.detail-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
}

.detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 24px 28px 20px;
  background: var(--c-surface-warm);
  border-bottom: 1px solid var(--c-border);
  position: relative;
}

.detail-head-text h2 {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-ink);
  margin: 0;
  padding-right: 40px;
}

.detail-head-text p {
  font-size: 13px;
  color: var(--c-ink-muted);
  margin: 4px 0 0;
}

.detail-body {
  padding: 24px 28px;
}

.detail-content-box {
  font-size: 14px;
  color: var(--c-ink-light);
  line-height: 1.7;
  padding: 16px;
  background: var(--c-surface-warm);
  border: 1px solid var(--c-border);
  border-radius: 10px;
  white-space: pre-wrap;
  margin-bottom: 20px;
}

/* Custom tabs */
.detail-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--c-border);
  padding-bottom: 0;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px 12px;
  border: none;
  background: transparent;
  color: var(--c-ink-muted);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
  transition: color 0.15s;
}

.tab-btn:hover {
  color: var(--c-ink-light);
}

.tab-btn.tab-active {
  color: var(--c-ink);
}

.tab-btn.tab-active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--c-ink);
  border-radius: 2px 2px 0 0;
}

.tab-num {
  font-size: 11px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.tab-num-ok {
  background: var(--c-green-soft);
  color: var(--c-green);
}

.tab-num-pending {
  background: var(--c-amber-soft);
  color: var(--c-amber);
}

/* Person list */
.detail-list {
  max-height: 320px;
  overflow-y: auto;
}

.detail-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--c-ink-faint);
  font-size: 14px;
}

.person-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--c-surface-dim);
}

.person-row:last-child {
  border-bottom: none;
}

.person-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.pav-boss {
  background: var(--c-amber);
}

.pav-worker {
  background: var(--c-ink);
}

.person-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
}

.person-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-ink);
}

.person-role {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--c-amber-soft);
  color: var(--c-amber);
}

.person-time {
  font-size: 12px;
  color: var(--c-ink-muted);
}
</style>
