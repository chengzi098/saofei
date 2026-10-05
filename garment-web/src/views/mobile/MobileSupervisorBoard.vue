<template>
  <div class="mobile-board">
    <!-- 搜索筛选 -->
    <div class="filter-bar" :class="{ 'entered': mounted }">
      <el-input
        v-model="activeSearchKeyword"
        :placeholder="searchPlaceholder"
        clearable
        :prefix-icon="Search"
        size="default"
        class="search-input"
      />
      <el-select v-model="progressFilter" placeholder="进度" clearable size="default" class="filter-select">
        <el-option label="未开始" value="idle" />
        <el-option label="生产中" value="progress" />
        <el-option label="进展良好" value="good" />
        <el-option label="已完成" value="complete" />
      </el-select>
    </div>

    <!-- 更新时间 -->
    <div class="update-row" :class="{ 'entered': mounted }">
      <span class="update-time">{{ updateTime }}</span>
      <span class="filter-count">{{ filterCountText }}</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && !styles.length" class="state-loading">
      <div class="spinner"></div>
      <p>正在加载生产数据...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && !styles.length" class="empty-state">
      <el-icon :size="48" color="#d1d5db"><Collection /></el-icon>
      <h3>暂无生产数据</h3>
      <p>请先在「分扎管理」中创建扎货</p>
    </div>

    <!-- 款式卡片列表 -->
    <div v-else-if="!selectedStyle" class="style-list" :class="{ 'entered': mounted }">
      <div v-if="!filteredStyles.length" class="empty-state">
        <el-icon :size="48" color="#d1d5db"><Search /></el-icon>
        <h3>没有匹配的款式</h3>
      </div>

      <div
        v-for="(style, index) in filteredStyles"
        :key="style.styleId"
        class="style-card"
        :style="{ animationDelay: `${index * 0.05}s` }"
        @click="selectStyle(style)"
      >
        <div class="style-card-header">
          <span class="style-no">{{ style.styleNo }}</span>
          <span class="bed-count">{{ style.beds?.length || 0 }} 个床号</span>
        </div>
        <div class="style-card-body">
          <div v-if="getMatchedBedsForStyle(style).length" class="matched-beds">
            <span class="matched-beds-label">匹配床号</span>
            <button
              v-for="bed in getMatchedBedsForStyle(style)"
              :key="bed.bedId"
              type="button"
              class="matched-bed-button"
              @click.stop="openMatchedBed(style, bed)"
            >
              <span>{{ bed.bedNo }}</span>
              <small v-if="bed.name">{{ bed.name }}</small>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
          <div class="progress-row">
            <span class="progress-label">总进度</span>
            <span class="progress-value" :class="getProgressClass(style.progressPercent)">
              {{ style.progressPercent }}%
            </span>
          </div>
          <div class="progress-bar">
            <div
              class="progress-fill"
              :style="{ width: style.progressPercent + '%' }"
              :class="getProgressClass(style.progressPercent)"
            ></div>
          </div>
          <div class="style-stats">
            <span>{{ style.totalQuantity }} 件</span>
            <span>共 {{ getTotalBundles(style) }} 扎</span>
          </div>
        </div>
        <div class="style-card-footer">
          <span class="status-text" :class="getProgressStatusClass(style.progressPercent)">
            {{ getProgressStatusText(style.progressPercent) }}
          </span>
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <!-- 款式详情（床号 + 扎号） -->
    <div v-else class="detail-view">
      <!-- 顶部返回栏 -->
      <div class="detail-header" @click="backToStyles">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回款式列表</span>
      </div>

      <!-- 款号概览 -->
      <div class="style-overview">
        <div class="overview-title">
          <span class="style-no-big">{{ selectedStyle.styleNo }}</span>
          <span class="status-badge" :class="getProgressStatusClass(selectedStyle.progressPercent)">
            {{ getProgressStatusText(selectedStyle.progressPercent) }}
          </span>
        </div>
        <div class="overview-progress">
          <div class="progress-bar">
            <div
              class="progress-fill"
              :style="{ width: selectedStyle.progressPercent + '%' }"
              :class="getProgressClass(selectedStyle.progressPercent)"
            ></div>
          </div>
          <div class="overview-stats">
            <div class="stat-block">
              <span class="stat-num">{{ selectedStyle.totalQuantity }}</span>
              <span class="stat-text">总件数</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-block">
              <span class="stat-num">{{ getTotalBundles(selectedStyle) }}</span>
              <span class="stat-text">总扎数</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-block">
              <span class="stat-num">{{ selectedStyle.progressPercent }}%</span>
              <span class="stat-text">总进度</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 床号列表 -->
      <div class="bed-section">
        <div class="section-title">床号列表</div>
        <div
          v-for="(bed, index) in filteredBeds"
          :key="bed.bedId"
          class="bed-card"
          :style="{ animationDelay: `${index * 0.05}s` }"
          @click="openBedSheet(bed)"
        >
          <div class="bed-card-header">
            <div class="bed-title-wrap">
              <span class="bed-number">#{{ index + 1 }}</span>
              <span class="bed-name">{{ bed.bedNo }}</span>
              <span v-if="bed.name" class="bed-desc">{{ bed.name }}</span>
            </div>
            <span class="progress-value" :class="getProgressClass(bed.progressPercent)">
              {{ bed.progressPercent }}%
            </span>
          </div>
          <div class="bed-card-body">
            <div class="progress-bar small">
              <div
                class="progress-fill"
                :style="{ width: bed.progressPercent + '%' }"
                :class="getProgressClass(bed.progressPercent)"
              ></div>
            </div>
            <div class="bed-stats">
              <span>{{ bed.quantity }} 件</span>
              <span>{{ bed.totalBundleCount }} 扎</span>
              <span>{{ bed.processes?.length || 0 }} 道工序</span>
            </div>
            <div class="process-tags">
              <div
                v-for="process in (bed.processes || []).slice(0, 5)"
                :key="process.processName"
                class="process-tag"
                :class="{
                  'done': process.status === 2,
                  'doing': process.status === 1,
                  'pending': process.status === 0
                }"
              >
                <span class="dot"></span>
                {{ process.processName }}
              </div>
              <span v-if="(bed.processes || []).length > 5" class="more-processes">
                +{{ bed.processes.length - 5 }}
              </span>
            </div>
          </div>
          <div class="bed-card-footer">
            <span class="status-text" :class="getProgressStatusClass(bed.progressPercent)">
              {{ getProgressStatusText(bed.progressPercent) }}
            </span>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </div>

        <div
          v-if="selectedStyle.beds?.length && !filteredBeds.length"
          class="empty-state small"
        >
          <p>没有匹配的床号</p>
        </div>

        <div v-else-if="!selectedStyle.beds?.length" class="empty-state small">
          <p>暂无床号数据</p>
        </div>
      </div>
    </div>

    <!-- 床号详情底部弹出 -->
    <el-drawer
      v-model="bedSheetVisible"
      :title="selectedBed ? `${selectedBed.bedNo} · 扎号列表` : '扎号列表'"
      direction="btt"
      size="75%"
      class="bed-sheet"
    >
      <div v-if="selectedBed" class="bed-sheet-content">
        <!-- 床号概览 -->
        <div class="bed-overview-mini">
          <div class="overview-row">
            <span class="overview-label">件数</span>
            <span class="overview-val">{{ selectedBed.quantity }}</span>
            <span class="overview-label">扎数</span>
            <span class="overview-val">{{ selectedBed.totalBundleCount }}</span>
            <span class="overview-label">进度</span>
            <span class="overview-val" :class="getProgressClass(selectedBed.progressPercent)">
              {{ selectedBed.progressPercent }}%
            </span>
          </div>
        </div>

        <!-- 扎号列表 -->
        <div class="bundle-list">
          <div
            v-for="bundle in selectedBed.bundles"
            :key="bundle.bundleId"
            class="bundle-item"
            @click="openBundleDetail(bundle)"
          >
            <div class="bundle-item-header">
              <span class="bundle-no">{{ bundle.bundleNo }}</span>
              <span v-if="bundle.size" class="bundle-size">{{ bundle.size }}</span>
              <span class="bundle-qty">{{ bundle.quantity }} 件</span>
              <span class="progress-value" :class="getProgressClass(bundle.progressPercent)">
                {{ bundle.progressPercent }}%
              </span>
            </div>
            <div class="progress-bar mini">
              <div
                class="progress-fill"
                :style="{ width: bundle.progressPercent + '%' }"
                :class="getProgressClass(bundle.progressPercent)"
              ></div>
            </div>
            <div class="process-tags">
              <div
                v-for="process in bundle.processes"
                :key="process.processName"
                class="process-tag"
                :class="{
                  'done': process.status === 2,
                  'doing': process.status === 1,
                  'pending': process.status === 0
                }"
              >
                <span class="dot"></span>
                {{ process.processName }}
              </div>
            </div>
          </div>

          <div v-if="!selectedBed.bundles?.length" class="empty-state small">
            <p>暂无扎号数据</p>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 扎号工序详情弹窗 -->
    <el-drawer
      v-model="bundleDetailVisible"
      :title="bundleDetail ? `${bundleDetail.bundleNo} · 工序明细` : '工序明细'"
      direction="btt"
      size="65%"
      class="bundle-sheet"
    >
      <div v-if="bundleDetail" class="bundle-detail-content">
        <div class="bundle-info-grid">
          <div class="info-item">
            <span class="info-label">扎号</span>
            <span class="info-value mono">{{ bundleDetail.bundleNo }}</span>
          </div>
          <div class="info-item" v-if="bundleDetail.size">
            <span class="info-label">尺码</span>
            <span class="info-value">{{ bundleDetail.size }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">件数</span>
            <span class="info-value">{{ bundleDetail.quantity }} 件</span>
          </div>
          <div class="info-item">
            <span class="info-label">进度</span>
            <span class="info-value" :class="getProgressClass(bundleDetail.progressPercent)">
              {{ bundleDetail.progressPercent }}%
            </span>
          </div>
        </div>

        <div class="process-list">
          <div class="process-list-title">工序详情</div>
          <div
            v-for="(process, idx) in bundleDetail.processes"
            :key="idx"
            class="process-item"
            :class="{ 'is-done': process.status === 2, 'is-doing': process.status === 1 }"
          >
            <div class="process-seq">{{ idx + 1 }}</div>
            <div class="process-info">
              <div class="process-name">{{ process.processName }}</div>
              <div class="process-meta" v-if="process.status === 2 && process.operatorName">
                {{ process.operatorName }}
                <template v-if="process.completeTime"> · {{ process.completeTime }}</template>
              </div>
              <div class="process-meta doing-text" v-else-if="process.status === 1">进行中</div>
              <div class="process-meta pending-text" v-else>待开工</div>
            </div>
            <div class="process-status">
              <el-tag v-if="process.status === 2" type="success" size="small" effect="plain">已完成</el-tag>
              <el-tag v-else-if="process.status === 1" type="primary" size="small" effect="plain">进行中</el-tag>
              <el-tag v-else type="info" size="small" effect="plain">未开始</el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Collection, Search, ArrowRight, ArrowLeft } from '@element-plus/icons-vue'
import { getDashboard } from '../../api/dashboard'
import {
  filterDashboardBeds,
  filterDashboardStyles,
  getMatchedBeds
} from '../../utils/dashboardBoardSearch'

const styles = ref([])
const loading = ref(false)
const updateTime = ref('')
const mounted = ref(false)
let lastManualRefresh = 0

// 搜索筛选
const searchKeyword = ref('')
const progressFilter = ref('')

// 选中状态
const selectedStyle = ref(null)
const bedSearchKeyword = ref('')
const selectedBed = ref(null)
const bedSheetVisible = ref(false)
const bundleDetail = ref(null)
const bundleDetailVisible = ref(false)

const activeSearchKeyword = computed({
  get: () => selectedStyle.value ? bedSearchKeyword.value : searchKeyword.value,
  set: (value) => {
    if (selectedStyle.value) {
      bedSearchKeyword.value = value
    } else {
      searchKeyword.value = value
    }
  }
})

const searchPlaceholder = computed(() =>
  selectedStyle.value ? '搜索床号...' : '搜索款号或床号...'
)

const filteredBeds = computed(() =>
  filterDashboardBeds(selectedStyle.value?.beds, bedSearchKeyword.value)
)

// 筛选后的款式列表
const filteredStyles = computed(() => {
  let list = filterDashboardStyles(styles.value, searchKeyword.value)
  if (progressFilter.value) {
    list = list.filter(s => getProgressClass(s.progressPercent) === progressFilter.value)
  }
  return list
})

const filterCountText = computed(() =>
  selectedStyle.value
    ? `共 ${filteredBeds.value.length} 个床号`
    : `共 ${filteredStyles.value.length} 个款式`
)

// 进度相关
function getProgressClass(percent) {
  if (percent === 100) return 'complete'
  if (percent >= 60) return 'good'
  if (percent > 0) return 'progress'
  return 'idle'
}

function getProgressStatusClass(percent) {
  if (percent === 100) return 'status-complete'
  if (percent >= 60) return 'status-good'
  if (percent > 0) return 'status-progress'
  return 'status-idle'
}

function getProgressStatusText(percent) {
  if (percent === 100) return '已完成'
  if (percent >= 60) return '进展良好'
  if (percent > 0) return '生产中'
  return '未开始'
}

function getTotalBundles(style) {
  if (!style.beds || !style.beds.length) return 0
  return style.beds.reduce((total, bed) => total + (bed.totalBundleCount || 0), 0)
}

// 导航
function selectStyle(style) {
  bedSearchKeyword.value = ''
  selectedStyle.value = { ...style }
}

function getMatchedBedsForStyle(style) {
  return getMatchedBeds(style, searchKeyword.value)
}

function openMatchedBed(style, bed) {
  selectStyle(style)
  openBedSheet(bed)
}

function backToStyles() {
  selectedStyle.value = null
  selectedBed.value = null
  bedSheetVisible.value = false
  bundleDetail.value = null
  bundleDetailVisible.value = false
}

function openBedSheet(bed) {
  selectedBed.value = bed
  bedSheetVisible.value = true
}

function openBundleDetail(bundle) {
  bundleDetail.value = bundle
  bundleDetailVisible.value = true
}

// 数据加载
let autoRefreshTimer = null

async function loadDataSilent() {
  try {
    const res = await getDashboard()
    if (res.code === 200) {
      styles.value = res.data.styles || []
      const t = new Date()
      updateTime.value = `${String(t.getHours()).padStart(2, '0')}:${String(t.getMinutes()).padStart(2, '0')}`
    }
  } catch (e) {
    console.error('刷新看板数据失败:', e)
  }
}

async function loadData() {
  const now = Date.now()
  if (now - lastManualRefresh < 5000) {
    ElMessage.warning('请稍后再刷新')
    return
  }
  lastManualRefresh = now
  loading.value = true
  try {
    const res = await getDashboard()
    if (res.code === 200) {
      styles.value = res.data.styles || []
      const t = new Date()
      updateTime.value = `${String(t.getHours()).padStart(2, '0')}:${String(t.getMinutes()).padStart(2, '0')}`
    }
  } catch (e) {
    console.error('加载看板数据失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  requestAnimationFrame(() => { mounted.value = true })
  loadData()
  autoRefreshTimer = setInterval(loadDataSilent, 5000)
})

onUnmounted(() => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
})
</script>

<style scoped>
.mobile-board {
  padding: 12px 16px;
  padding-bottom: 100px;
}

/* ===== 筛选栏 ===== */
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out;
}
.filter-bar.entered {
  opacity: 1;
  transform: translateY(0);
}

.search-input {
  flex: 1;
}

.filter-select {
  width: 100px;
}

/* ===== 更新时间行 ===== */
.update-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out 0.05s;
}
.update-row.entered {
  opacity: 1;
  transform: translateY(0);
}

.update-time {
  font-size: 12px;
  color: #9ca3af;
}

.filter-count {
  font-size: 12px;
  color: #9ca3af;
}

.matched-beds {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-bottom: 12px;
  padding: 10px;
  border: 1px solid rgba(217, 119, 6, 0.25);
  border-radius: 10px;
  background: rgba(217, 119, 6, 0.07);
}

.matched-beds-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--color-amber, #d97706);
}

.matched-bed-button {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 100%;
  min-height: 44px;
  padding: 8px 10px;
  border: 1px solid rgba(217, 119, 6, 0.25);
  border-radius: 9px;
  background: var(--bg-card);
  color: #374151;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  text-align: left;
}

.matched-bed-button small {
  color: #9ca3af;
  font-size: 11px;
  font-weight: 500;
}

.matched-bed-button .el-icon {
  margin-left: auto;
  color: var(--color-amber, #d97706);
}

/* ===== 加载状态 ===== */
.state-loading {
  text-align: center;
  padding: 60px 20px;
}
.state-loading p {
  color: #9ca3af;
  font-size: 14px;
  margin-top: 12px;
}
.spinner {
  width: 32px;
  height: 32px;
  margin: 0 auto;
  border: 3px solid #f0f2f8;
  border-top-color: var(--color-amber, #d97706);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
  gap: 8px;
}
.empty-state.small {
  padding: 20px 0;
}
.empty-state h3 {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin: 0;
}
.empty-state p {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

/* ===== 款式列表 ===== */
.style-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out 0.1s;
}
.style-list.entered {
  opacity: 1;
  transform: translateY(0);
}

.style-card {
  background: var(--bg-card);
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  animation: slideUp 0.4s ease both;
  opacity: 0;
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.style-card:active {
  transform: scale(0.98);
}

.style-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.style-no {
  font-size: 17px;
  font-weight: 700;
  color: #6366f1;
  font-family: 'Menlo', 'Consolas', monospace;
}

.bed-count {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-amber, #d97706);
  background: rgba(232, 168, 56, 0.1);
  padding: 2px 10px;
  border-radius: 12px;
}

.style-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.progress-label {
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
}

.progress-value {
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.progress-value.idle { color: #9ca3af; }
.progress-value.progress { color: #3b82f6; }
.progress-value.good { color: #10b981; }
.progress-value.complete { color: #16a34a; }

.progress-bar {
  width: 100%;
  height: 8px;
  background: #f0f2f8;
  border-radius: 4px;
  overflow: hidden;
}
.progress-bar.small {
  height: 6px;
}
.progress-bar.mini {
  height: 4px;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}
.progress-fill.idle { background: #d1d5db; }
.progress-fill.progress { background: linear-gradient(90deg, #60a5fa, #3b82f6); }
.progress-fill.good { background: linear-gradient(90deg, #34d399, #10b981); }
.progress-fill.complete { background: linear-gradient(90deg, #22c55e, #16a34a); }

.style-stats {
  display: flex;
  justify-content: space-between;
  padding-top: 8px;
  border-top: 1px solid #f0eeea;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.style-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f0eeea;
}

.status-text {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
}
.status-text.status-idle { background: rgba(156, 163, 175, 0.1); color: #9ca3af; }
.status-text.status-progress { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.status-text.status-good { background: rgba(16, 185, 129, 0.1); color: #10b981; }
.status-text.status-complete { background: rgba(22, 163, 74, 0.1); color: #16a34a; }

.arrow-icon {
  font-size: 14px;
  color: #9ca3af;
}

/* ===== 详情视图 ===== */
.detail-view {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-amber, #d97706);
  cursor: pointer;
  padding: 4px 0;
}

.style-overview {
  background: var(--bg-card);
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.overview-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0eeea;
}

.style-no-big {
  font-size: 20px;
  font-weight: 700;
  color: #6366f1;
  font-family: 'Menlo', 'Consolas', monospace;
}

.status-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
}
.status-badge.status-idle { background: rgba(156, 163, 175, 0.1); color: #9ca3af; }
.status-badge.status-progress { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.status-badge.status-good { background: rgba(16, 185, 129, 0.1); color: #10b981; }
.status-badge.status-complete { background: rgba(22, 163, 74, 0.1); color: #16a34a; }

.overview-progress {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.overview-stats {
  display: flex;
  align-items: center;
  background: #fafafa;
  border-radius: 12px;
  padding: 12px 0;
}

.stat-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.stat-text {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 500;
}

.stat-divider {
  width: 1px;
  height: 24px;
  background: #e5e7eb;
}

/* ===== 床号卡片 ===== */
.bed-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.bed-card {
  background: var(--bg-card);
  border-radius: 14px;
  padding: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
  animation: slideUp 0.4s ease both;
  opacity: 0;
  transition: all 0.2s ease;
}
.bed-card:active {
  transform: scale(0.98);
}

.bed-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bed-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bed-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  border-radius: 7px;
  flex-shrink: 0;
}

.bed-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.bed-desc {
  font-size: 12px;
  color: #9ca3af;
}

.bed-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bed-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.process-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.process-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
  font-weight: 500;
  white-space: nowrap;
}
.process-tag .dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  display: inline-block;
}
.process-tag.pending {
  background: #f3f4f6;
  color: #9ca3af;
}
.process-tag.pending .dot { background: #d1d5db; }
.process-tag.doing {
  background: #eff6ff;
  color: #3b82f6;
}
.process-tag.doing .dot {
  background: #3b82f6;
  box-shadow: 0 0 4px rgba(59, 130, 246, 0.4);
}
.process-tag.done {
  background: #f0fdf4;
  color: #16a34a;
}
.process-tag.done .dot { background: #22c55e; }

.more-processes {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 600;
}

.bed-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f0eeea;
}

/* ===== 床号底部弹出 ===== */
.bed-sheet-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bed-overview-mini {
  background: #fafafa;
  border-radius: 12px;
  padding: 12px;
}

.overview-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.overview-label {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
}

.overview-val {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin-right: 8px;
}

.bundle-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bundle-item {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.bundle-item:active {
  transform: scale(0.98);
  border-color: var(--color-amber, #d97706);
}

.bundle-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bundle-no {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: 'Menlo', 'Consolas', monospace;
}

.bundle-size {
  font-size: 11px;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  padding: 1px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.bundle-qty {
  font-size: 13px;
  color: #6b7280;
  margin-left: auto;
}

/* ===== 扎号工序详情 ===== */
.bundle-detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bundle-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  background: #fafafa;
  border-radius: 12px;
  padding: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.info-value.mono {
  font-family: 'Menlo', 'Consolas', monospace;
}

.process-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.process-list-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.process-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: var(--bg-card);
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}
.process-item.is-done {
  border-color: #bbf7d0;
  background: #fafff9;
}
.process-item.is-doing {
  border-color: #bfdbfe;
  background: #fafffe;
}

.process-seq {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f0f2f8;
  color: #9ca3af;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.process-item.is-done .process-seq {
  background: #dcfce7;
  color: #16a34a;
}
.process-item.is-doing .process-seq {
  background: #dbeafe;
  color: #3b82f6;
}

.process-info {
  flex: 1;
  min-width: 0;
}

.process-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.process-meta {
  font-size: 11px;
  color: #9ca3af;
}
.process-meta.doing-text { color: #3b82f6; }
.process-meta.pending-text { color: #b0b5c0; }

.process-status {
  flex-shrink: 0;
}
</style>
