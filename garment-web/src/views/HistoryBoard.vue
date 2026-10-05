<template>
  <div class="history-board">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left" v-if="!selectedStyleId">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索款号..."
          clearable
          :prefix-icon="Search"
          class="search-input"
        />
        <span class="filter-count">共 {{ filteredStyles.length }} 个历史款式</span>
      </div>
      <div class="toolbar-left" v-else>
        <span class="update-time">历史工作</span>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadData" class="refresh-btn">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 面包屑导航 -->
    <div class="breadcrumb" v-if="selectedStyleId || selectedBedId">
      <span class="crumb clickable" @click="backToStyles">历史列表</span>
      <el-icon class="crumb-sep" v-if="selectedStyleId"><ArrowRight /></el-icon>
      <span class="crumb clickable" v-if="selectedStyleId" @click="backToBeds">{{ selectedStyle?.styleNo || '-' }}</span>
      <el-icon class="crumb-sep" v-if="selectedBedId"><ArrowRight /></el-icon>
      <span class="crumb" v-if="selectedBedId">{{ selectedBed?.bedNo || '-' }}</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && !styles.length" class="state-loading">
      <div class="spinner"></div>
      <p>正在加载历史数据...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && !styles.length" class="empty-state">
      <div class="empty-icon">
        <el-icon><Clock /></el-icon>
      </div>
      <h3>暂无历史工作</h3>
      <p>在工作看板中归档已完成的款式后，将显示在这里</p>
    </div>

    <!-- ===== 款式卡片列表 ===== -->
    <div v-if="!loading && styles.length && !selectedStyleId">
      <div v-if="!filteredStyles.length" class="empty-state">
        <div class="empty-icon">
          <el-icon><Search /></el-icon>
        </div>
        <h3>没有匹配的款式</h3>
        <p>请调整搜索条件</p>
      </div>

      <div v-else class="style-cards-grid">
        <div
          v-for="(style, index) in pagedStyles"
          :key="style.styleId"
          class="style-card"
          :style="{ animationDelay: `${index * 0.05}s` }"
          @click="selectStyle(style)"
        >
          <div class="card-header">
            <span class="style-no">{{ style.styleNo }}</span>
            <span class="bed-count">{{ style.beds?.length || 0 }} 个床号</span>
          </div>
          <div class="card-body">
            <div class="progress-row">
              <div class="progress-info">
                <span class="progress-label">总进度</span>
                <span class="progress-value" :class="getProgressClass(style.progressPercent)">
                  {{ style.progressPercent }}%
                </span>
              </div>
              <div class="progress-bar-wrap">
                <div class="progress-bar">
                  <div
                    class="progress-fill"
                    :style="{ width: style.progressPercent + '%' }"
                    :class="getProgressClass(style.progressPercent)"
                  ></div>
                </div>
              </div>
            </div>
            <div class="total-info">
              <span class="total-qty">{{ style.totalQuantity }} 件</span>
              <span class="total-bundles">共 {{ getTotalBundles(style) }} 扎</span>
            </div>
          </div>
          <div class="card-footer">
            <span class="status-text" :class="getProgressStatusClass(style.progressPercent)">
              {{ getProgressStatusText(style.progressPercent) }}
            </span>
            <div class="card-footer-right">
              <el-button
                v-if="isBoss"
                type="primary"
                size="small"
                text
                @click.stop="handleUnarchiveStyle(style)"
                class="unarchive-btn"
              >
                取消归档
              </el-button>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="filteredStyles.length > pageSize" class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="filteredStyles.length"
          layout="prev, pager, next"
          hide-on-single-page
        />
      </div>
    </div>

    <!-- ===== 床号详情 ===== -->
    <div v-if="selectedStyleId && !selectedBedId && selectedStyle" class="bed-section">
      <div class="style-info-bar">
        <div class="style-info-left">
          <div class="info-block">
            <span class="info-label">款号</span>
            <span class="info-value">{{ selectedStyle.styleNo }}</span>
          </div>
          <div class="info-block">
            <span class="info-label">总件数</span>
            <span class="info-value">{{ selectedStyle.totalQuantity }} 件</span>
          </div>
          <div class="info-block">
            <span class="info-label">总进度</span>
            <span class="info-value" :class="getProgressClass(selectedStyle.progressPercent)">
              {{ selectedStyle.progressPercent }}%
            </span>
          </div>
        </div>
      </div>

      <div class="bed-cards-grid">
        <div
          v-for="(bed, index) in selectedStyle.beds"
          :key="bed.bedId"
          class="bed-card"
          :style="{ animationDelay: `${index * 0.05}s` }"
          @click="selectBed(bed)"
        >
          <div class="bed-card-header">
            <div class="bed-title-wrap">
              <span class="bed-number">#{{ index + 1 }}</span>
              <span class="bed-name">{{ bed.bedNo }}</span>
              <span v-if="bed.name" class="bed-desc">{{ bed.name }}</span>
            </div>
          </div>
          <div class="bed-card-body">
            <div class="progress-row">
              <div class="progress-info">
                <span class="progress-label">床号进度</span>
                <span class="progress-value" :class="getProgressClass(bed.progressPercent)">
                  {{ bed.progressPercent }}%
                </span>
              </div>
              <div class="progress-bar-wrap">
                <div class="progress-bar small">
                  <div
                    class="progress-fill"
                    :style="{ width: bed.progressPercent + '%' }"
                    :class="getProgressClass(bed.progressPercent)"
                  ></div>
                </div>
              </div>
            </div>
            <div class="bed-info-row">
              <span>{{ bed.quantity }} 件</span>
              <span>共 {{ bed.totalBundleCount || 0 }} 扎</span>
            </div>
          </div>
          <div class="card-footer">
            <div class="process-tags">
              <template v-for="p in (bed.processes || []).slice(0, 4)" :key="p.bundleProcessId">
                <span class="process-tag" :class="p.status === 2 ? 'tag-done' : p.status === 1 ? 'tag-ing' : 'tag-todo'">
                  {{ p.processName }}
                </span>
              </template>
              <span v-if="(bed.processes || []).length > 4" class="process-tag tag-more">
                +{{ bed.processes.length - 4 }}
              </span>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 扎号列表 ===== -->
    <div v-if="selectedBedId && selectedBed" class="bundle-section">
      <div class="style-info-bar">
        <div class="style-info-left">
          <div class="info-block">
            <span class="info-label">床号</span>
            <span class="info-value">{{ selectedBed.bedNo }}</span>
          </div>
          <div class="info-block">
            <span class="info-label">总件数</span>
            <span class="info-value">{{ selectedBed.quantity }} 件</span>
          </div>
          <div class="info-block">
            <span class="info-label">床号进度</span>
            <span class="info-value" :class="getProgressClass(selectedBed.progressPercent)">
              {{ selectedBed.progressPercent }}%
            </span>
          </div>
        </div>
      </div>

      <div class="bundle-cards-grid">
        <div
          v-for="(bundle, index) in filteredBundles"
          :key="bundle.bundleId"
          class="bundle-card"
          :style="{ animationDelay: `${index * 0.03}s` }"
          @click="openBundleDrawer(bundle)"
        >
          <div class="bundle-card-header">
            <span class="bundle-no">{{ bundle.bundleNo }}</span>
            <span class="bundle-size">{{ bundle.size || '-' }}</span>
          </div>
          <div class="bundle-card-body">
            <div class="progress-row compact">
              <div class="progress-bar-wrap">
                <div class="progress-bar small">
                  <div
                    class="progress-fill"
                    :style="{ width: bundle.progressPercent + '%' }"
                    :class="getProgressClass(bundle.progressPercent)"
                  ></div>
                </div>
              </div>
              <span class="progress-value small" :class="getProgressClass(bundle.progressPercent)">
                {{ bundle.progressPercent }}%
              </span>
            </div>
            <div class="bundle-info">
              <span>{{ bundle.quantity }} 件</span>
            </div>
            <div class="process-tags">
              <template v-for="p in (bundle.processes || []).slice(0, 3)" :key="p.bundleProcessId">
                <span class="process-tag" :class="p.status === 2 ? 'tag-done' : p.status === 1 ? 'tag-ing' : 'tag-todo'">
                  {{ p.processName }}
                </span>
              </template>
              <span v-if="(bundle.processes || []).length > 3" class="process-tag tag-more">
                +{{ bundle.processes.length - 3 }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 扎号工序详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerBundle?.bundleNo || '扎号详情'"
      direction="rtl"
      size="360px"
    >
      <div v-if="drawerBundle" class="drawer-content">
        <div class="drawer-info">
          <span>尺码: {{ drawerBundle.size || '-' }}</span>
          <span>{{ drawerBundle.quantity }} 件</span>
        </div>
        <div class="drawer-processes">
          <div
            v-for="p in drawerBundle.processes || []"
            :key="p.bundleProcessId"
            class="process-item"
          >
            <div class="process-left">
              <span class="process-dot" :class="p.status === 2 ? 'dot-done' : p.status === 1 ? 'dot-ing' : 'dot-todo'"></span>
              <span class="process-name">{{ p.processName }}</span>
            </div>
            <div class="process-right">
              <span v-if="p.operatorName" class="operator">{{ p.operatorName }}</span>
              <span v-if="p.completeTime" class="time">{{ p.completeTime }}</span>
              <span class="status-badge" :class="p.status === 2 ? 'badge-done' : p.status === 1 ? 'badge-ing' : 'badge-todo'">
                {{ p.statusText }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, ArrowRight, Search, Clock } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getHistoryStyles, unarchiveStyle } from '../api/dashboard'

const userStore = useUserStore()
const styles = ref([])
const loading = ref(false)

const selectedStyleId = ref(null)
const selectedStyle = ref(null)
const selectedBedId = ref(null)
const selectedBed = ref(null)

const isBoss = computed(() => userStore.isBoss())

const drawerVisible = ref(false)
const drawerBundle = ref(null)

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = 12

const filteredStyles = computed(() => {
  let list = styles.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(s => s.styleNo?.toLowerCase().includes(kw))
  }
  return list
})

const pagedStyles = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredStyles.value.slice(start, start + pageSize)
})

const filteredBundles = computed(() => {
  return selectedBed.value?.bundles || []
})

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

function selectStyle(style) {
  selectedStyleId.value = style.styleId
  selectedStyle.value = { ...style }
  selectedBedId.value = null
  selectedBed.value = null
}

function selectBed(bed) {
  selectedBedId.value = bed.bedId
  selectedBed.value = { ...bed }
}

function backToStyles() {
  selectedStyleId.value = null
  selectedStyle.value = null
  selectedBedId.value = null
  selectedBed.value = null
}

function backToBeds() {
  selectedBedId.value = null
  selectedBed.value = null
}

function openBundleDrawer(bundle) {
  drawerBundle.value = bundle
  drawerVisible.value = true
}

async function handleUnarchiveStyle(style) {
  try {
    await ElMessageBox.confirm(
      `确定要将款式「${style.styleNo}」取消归档吗？取消后将重新显示在工作看板中。`,
      '取消归档确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    const res = await unarchiveStyle(style.styleId)
    if (res.code === 200) {
      ElMessage.success('已取消归档')
      await loadData()
      // 如果当前在详情页，回到列表
      if (selectedStyleId.value === style.styleId) {
        backToStyles()
      }
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getHistoryStyles()
    if (res.code === 200) {
      styles.value = res.data || []
    }
  } catch (e) {
    console.error('加载历史数据失败:', e)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.history-board {
  max-width: 1200px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.update-time {
  font-size: 12px;
  color: var(--color-mist);
  white-space: nowrap;
}

.search-input {
  width: 200px;
}

.filter-count {
  font-size: 12px;
  color: var(--color-mist);
  white-space: nowrap;
}

/* ===== 面包屑 ===== */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
}
.crumb { color: var(--color-mist); }
.crumb.clickable { color: var(--color-amber); cursor: pointer; }
.crumb.clickable:hover { text-decoration: underline; }
.crumb-sep { font-size: 12px; color: var(--color-mist); }

/* ===== 状态页 ===== */
.state-loading, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--color-mist);
}
.empty-icon { font-size: 48px; margin-bottom: 16px; opacity: 0.4; }
.empty-state h3 { font-size: 16px; color: var(--color-charcoal); margin-bottom: 8px; }
.empty-state p { font-size: 13px; }

/* ===== 款式卡片网格 ===== */
.style-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.style-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  animation: fadeInUp 0.3s ease forwards;
  opacity: 0;
}
.style-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.style-no { font-size: 15px; font-weight: 600; color: var(--color-charcoal); }
.bed-count { font-size: 12px; color: var(--color-mist); }

.card-body { flex: 1; }

.progress-row { margin-bottom: 10px; }
.progress-info { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.progress-label { font-size: 12px; color: var(--color-mist); }
.progress-value { font-size: 13px; font-weight: 600; }
.progress-value.idle { color: #9ca3af; }
.progress-value.progress { color: #3b82f6; }
.progress-value.good { color: #10b981; }
.progress-value.complete { color: #16a34a; }
.progress-value.small { font-size: 12px; }

.progress-bar-wrap { flex: 1; }
.progress-bar { height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; }
.progress-bar.small { height: 4px; }
.progress-fill { height: 100%; border-radius: 3px; transition: width 0.5s ease; }
.progress-fill.idle { background: #d1d5db; }
.progress-fill.progress { background: #3b82f6; }
.progress-fill.good { background: #10b981; }
.progress-fill.complete { background: #16a34a; }

.total-info { display: flex; gap: 16px; font-size: 12px; color: var(--color-mist); }

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f0eeea;
  margin-top: auto;
}

.card-footer-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.unarchive-btn {
  font-size: 12px;
  padding: 2px 6px;
}

.status-text {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
}
.status-text.status-idle { background: rgba(156,163,175,0.1); color: #9ca3af; }
.status-text.status-progress { background: rgba(59,130,246,0.1); color: #3b82f6; }
.status-text.status-good { background: rgba(16,185,129,0.1); color: #10b981; }
.status-text.status-complete { background: rgba(22,163,74,0.1); color: #16a34a; }

.arrow-icon { font-size: 14px; color: var(--color-mist); }

/* ===== 分页 ===== */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* ===== 床号卡片 ===== */
.style-info-bar {
  background: white;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.style-info-left { display: flex; gap: 24px; }
.info-block { display: flex; flex-direction: column; gap: 2px; }
.info-label { font-size: 11px; color: var(--color-mist); }
.info-value { font-size: 14px; font-weight: 600; color: var(--color-charcoal); }

.bed-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.bed-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  animation: fadeInUp 0.3s ease forwards;
  opacity: 0;
}
.bed-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

.bed-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.bed-title-wrap { display: flex; align-items: center; gap: 8px; }
.bed-number { font-size: 12px; color: var(--color-mist); font-weight: 500; }
.bed-name { font-size: 14px; font-weight: 600; color: var(--color-charcoal); }
.bed-desc { font-size: 12px; color: var(--color-mist); }

.bed-card-body { flex: 1; }
.bed-info-row { display: flex; gap: 16px; font-size: 12px; color: var(--color-mist); margin-top: 8px; }

.process-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.process-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}
.tag-done { background: rgba(22,163,74,0.1); color: #16a34a; }
.tag-ing { background: rgba(59,130,246,0.1); color: #3b82f6; animation: blink 1.5s infinite; }
.tag-todo { background: rgba(156,163,175,0.1); color: #9ca3af; }
.tag-more { background: #f5f5f5; color: #999; }

/* ===== 扎号卡片 ===== */
.bundle-section { }

.bundle-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.bundle-card {
  background: white;
  border-radius: 10px;
  padding: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: all 0.2s;
  animation: fadeInUp 0.3s ease forwards;
  opacity: 0;
}
.bundle-card:hover { transform: translateY(-1px); box-shadow: 0 3px 8px rgba(0,0,0,0.1); }

.bundle-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.bundle-no { font-size: 14px; font-weight: 600; color: var(--color-charcoal); }
.bundle-size { font-size: 12px; color: var(--color-mist); }

.bundle-card-body { }
.progress-row.compact { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.progress-row.compact .progress-bar-wrap { flex: 1; }
.bundle-info { font-size: 12px; color: var(--color-mist); margin-bottom: 6px; }

/* ===== 抽屉 ===== */
.drawer-content { padding: 0 4px; }
.drawer-info { display: flex; gap: 16px; font-size: 13px; color: var(--color-mist); margin-bottom: 16px; }
.drawer-processes { display: flex; flex-direction: column; gap: 0; }

.process-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.process-item:last-child { border-bottom: none; }

.process-left { display: flex; align-items: center; gap: 8px; }
.process-dot { width: 8px; height: 8px; border-radius: 50%; }
.dot-done { background: #16a34a; }
.dot-ing { background: #3b82f6; animation: blink 1.5s infinite; }
.dot-todo { background: #d1d5db; }

.process-name { font-size: 13px; font-weight: 500; color: var(--color-charcoal); }

.process-right { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.operator { color: var(--color-charcoal); }
.time { color: var(--color-mist); }

.status-badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}
.badge-done { background: rgba(22,163,74,0.1); color: #16a34a; }
.badge-ing { background: rgba(59,130,246,0.1); color: #3b82f6; }
.badge-todo { background: rgba(156,163,175,0.1); color: #9ca3af; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
