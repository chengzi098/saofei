<template>
  <div class="dashboard-board">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left" v-if="!selectedStyleId">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索款号或床号..."
          clearable
          :prefix-icon="Search"
          class="search-input"
        />
        <el-select v-model="progressFilter" placeholder="进度状态" clearable class="filter-select">
          <el-option label="未开始" value="idle" />
          <el-option label="生产中" value="progress" />
          <el-option label="进展良好" value="good" />
          <el-option label="已完成" value="complete" />
        </el-select>
        <span class="filter-count">共 {{ filteredStyles.length }} 个款式</span>
      </div>
      <div class="toolbar-left" v-else>
        <span class="update-time">{{ updateTime }}</span>
      </div>
      <div class="toolbar-right">
        <span class="update-time" v-if="!selectedStyleId">{{ updateTime }}</span>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadData" class="refresh-btn">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 面包屑导航 -->
    <div class="breadcrumb" v-if="selectedStyleId || selectedBedId">
      <span class="crumb clickable" @click="backToStyles">款式列表</span>
      <el-icon class="crumb-sep" v-if="selectedStyleId"><ArrowRight /></el-icon>
      <span class="crumb clickable" v-if="selectedStyleId" @click="backToBeds">{{ selectedStyle?.styleNo || '-' }}</span>
      <el-icon class="crumb-sep" v-if="selectedBedId"><ArrowRight /></el-icon>
      <span class="crumb" v-if="selectedBedId">{{ selectedBed?.bedNo || '-' }}</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && !styles.length" class="state-loading">
      <div class="spinner"></div>
      <p>正在加载生产数据...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && !styles.length" class="empty-state">
      <div class="empty-icon">
        <el-icon><Collection /></el-icon>
      </div>
      <h3>暂无生产数据</h3>
      <p>请先在「分扎管理」中创建扎货</p>
    </div>

    <!-- ===== Step 1: 款式卡片列表 ===== -->
    <div v-if="!loading && styles.length && !selectedStyleId">
      <!-- 筛选后无结果 -->
      <div v-if="!filteredStyles.length" class="empty-state">
        <div class="empty-icon">
          <el-icon><Search /></el-icon>
        </div>
        <h3>没有匹配的款式</h3>
        <p>请调整搜索条件或筛选状态</p>
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
            <div v-if="getMatchedBedsForStyle(style).length" class="matched-beds">
              <span class="matched-beds-label">匹配床号</span>
              <button
                v-for="bed in getMatchedBedsForStyle(style)"
                :key="bed.bedId"
                type="button"
                class="matched-bed-button"
                @click.stop="selectMatchedBed(style, bed)"
              >
                <el-icon><Grid /></el-icon>
                <span>{{ bed.bedNo }}</span>
                <small v-if="bed.name">{{ bed.name }}</small>
                <el-icon class="matched-bed-arrow"><ArrowRight /></el-icon>
              </button>
            </div>
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
                v-if="isBoss && style.progressPercent === 100"
                type="warning"
                size="small"
                text
                @click.stop="handleArchiveStyle(style)"
                class="archive-btn"
              >
                归档
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

    <!-- ===== Step 2: 款式详情（床号卡片列表） ===== -->
    <div v-if="selectedStyleId && !selectedBedId && selectedStyle" class="bed-section">
      <!-- 款号信息栏 -->
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

      <!-- 床号搜索栏 -->
      <div class="bed-search-bar">
        <el-input
          v-model="bedSearchKeyword"
          placeholder="搜索床号名称..."
          clearable
          :prefix-icon="Search"
          class="bed-search-input"
        />
        <span class="bed-count-label">共 {{ filteredBeds.length }} 个床号</span>
      </div>

      <!-- 床号卡片网格 -->
      <div class="bed-cards-grid">
        <div
          v-for="(bed, index) in filteredBeds"
          :key="bed.bedId"
          class="bed-card"
          :class="{ 'my-bed': bed.myBed, 'other-bed': isSupervisor && !bed.myBed }"
          :style="{ animationDelay: `${index * 0.05}s` }"
          @click="selectBed(bed)"
        >
          <div class="bed-card-header">
            <div class="bed-title-wrap">
              <span class="bed-number">#{{ index + 1 }}</span>
              <span class="bed-name">{{ bed.bedNo }}</span>
              <span v-if="bed.name" class="bed-desc">{{ bed.name }}</span>
            </div>
            <el-tag v-if="bed.myBed" type="primary" size="small" class="bed-tag">我的</el-tag>
            <el-tag v-else-if="isSupervisor" type="info" size="small" class="bed-tag">其他</el-tag>
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
            <div class="bed-stats">
              <div class="stat-item">
                <span class="stat-label">件数</span>
                <span class="stat-value">{{ bed.quantity }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">扎数</span>
                <span class="stat-value">{{ bed.totalBundleCount }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">工序</span>
                <span class="stat-value">{{ bed.processes?.length || 0 }} 道</span>
              </div>
            </div>
            <div class="process-row">
              <span class="process-label">工序状态：</span>
              <div class="process-tags">
                <div
                  v-for="process in bed.processes.slice(0, 4)"
                  :key="process.processName"
                  class="process-tag"
                  :class="{
                    'done': process.status === 2,
                    'doing': process.status === 1,
                    'pending': process.status === 0
                  }"
                  :title="process.processName"
                >
                  <span class="dot"></span>
                  {{ process.processName }}
                </div>
                <span v-if="bed.processes?.length > 4" class="more-processes">+{{ bed.processes.length - 4 }}</span>
              </div>
            </div>
          </div>
          <div class="bed-card-footer">
            <span class="status-text" :class="getProgressStatusClass(bed.progressPercent)">
              {{ getProgressStatusText(bed.progressPercent) }}
            </span>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 空状态（搜索无结果） -->
      <div v-if="selectedStyle.beds?.length && !filteredBeds.length" class="empty-state">
        <div class="empty-icon">
          <el-icon><Search /></el-icon>
        </div>
        <h3>没有匹配的床号</h3>
        <p>请调整搜索条件</p>
      </div>

      <!-- 空状态（无床号） -->
      <div v-if="!selectedStyle.beds || !selectedStyle.beds.length" class="empty-state">
        <div class="empty-icon">
          <el-icon><Grid /></el-icon>
        </div>
        <h3>暂无床号数据</h3>
        <p>该款式下还没有配置床号</p>
      </div>
    </div>

    <!-- ===== Step 3: 床号详情（扎号列表） ===== -->
    <div v-if="selectedBedId && selectedBed" class="bundle-section">
      <!-- 床号概览卡片 -->
      <div class="bed-overview-card">
        <div class="bed-overview-header">
          <div class="bed-title">
            <span class="bed-no">{{ selectedBed.bedNo }}</span>
            <span v-if="selectedBed.name" class="bed-name">{{ selectedBed.name }}</span>
          </div>
          <span class="status-badge" :class="getProgressStatusClass(selectedBed.progressPercent)">
            {{ getProgressStatusText(selectedBed.progressPercent) }}
          </span>
        </div>
        <div class="bed-overview-body">
          <div class="overview-progress">
            <div class="progress-info">
              <span class="progress-label">总进度</span>
              <span class="progress-value" :class="getProgressClass(selectedBed.progressPercent)">
                {{ selectedBed.progressPercent }}%
              </span>
            </div>
            <div class="progress-bar">
              <div
                class="progress-fill"
                :style="{ width: selectedBed.progressPercent + '%' }"
                :class="getProgressClass(selectedBed.progressPercent)"
              ></div>
            </div>
          </div>
          <div class="overview-stats">
            <div class="stat-block">
              <span class="stat-num">{{ selectedBed.quantity }}</span>
              <span class="stat-text">总件数</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-block">
              <span class="stat-num">{{ selectedBed.totalBundleCount }}</span>
              <span class="stat-text">总扎数</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-block">
              <span class="stat-num">{{ selectedBed.processes?.length || 0 }}</span>
              <span class="stat-text">道工序</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 扎号列表 -->
      <div class="bundle-list-header">
        <span class="list-title">扎号列表</span>
        <div class="bundle-header-right">
          <el-input
            v-model="bundleSearch"
            placeholder="搜索扎号..."
            clearable
            :prefix-icon="Search"
            class="bundle-search-input"
          />
          <span class="list-count">共 {{ filteredBundles.length }} 扎</span>
        </div>
      </div>

      <div v-if="filteredBundles.length" class="bundle-grid">
        <div
          v-for="(bundle, index) in filteredBundles"
          :key="bundle.bundleId"
          class="bundle-card"
          :style="{ animationDelay: `${index * 0.03}s` }"
          @click="openBundleDrawer(bundle)"
        >
          <div class="bundle-card-header">
            <span class="bundle-no">{{ bundle.bundleNo }}</span>
            <span v-if="bundle.size" class="bundle-size">{{ bundle.size }}</span>
          </div>
          <div class="bundle-card-body">
            <div class="bundle-qty-row">
              <span class="bundle-qty">{{ bundle.quantity }} 件</span>
              <span class="progress-value" :class="getProgressClass(bundle.progressPercent)">
                {{ bundle.progressPercent }}%
              </span>
            </div>
            <div class="bundle-progress-bar">
              <div
                class="progress-fill"
                :style="{ width: bundle.progressPercent + '%' }"
                :class="getProgressClass(bundle.progressPercent)"
              ></div>
            </div>
          </div>
          <div class="bundle-card-footer">
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
                <span class="tag-name">{{ process.processName }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态（无扎号） -->
      <div v-if="!filteredBundles.length" class="empty-state">
        <div class="empty-icon">
          <el-icon><Search v-if="bundleSearch" /><Document v-else /></el-icon>
        </div>
        <h3>{{ bundleSearch ? '没有匹配的扎号' : '暂无扎号数据' }}</h3>
        <p>{{ bundleSearch ? '请调整搜索条件' : '该床号下还没有创建扎货' }}</p>
      </div>
    </div>

    <!-- 扎号工序详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerBundle ? `${drawerBundle.bundleNo} · 工序明细` : '工序明细'"
      direction="rtl"
      size="380px"
    >
      <div v-if="drawerBundle" class="drawer-content">
        <!-- 扎号概览 -->
        <div class="drawer-bundle-info">
          <div class="drawer-info-row">
            <span class="drawer-label">扎号</span>
            <span class="drawer-value mono">{{ drawerBundle.bundleNo }}</span>
          </div>
          <div class="drawer-info-row" v-if="drawerBundle.size">
            <span class="drawer-label">尺码</span>
            <span class="drawer-value">{{ drawerBundle.size }}</span>
          </div>
          <div class="drawer-info-row">
            <span class="drawer-label">件数</span>
            <span class="drawer-value">{{ drawerBundle.quantity }} 件</span>
          </div>
          <div class="drawer-info-row">
            <span class="drawer-label">进度</span>
            <span class="drawer-value" :class="getProgressClass(drawerBundle.progressPercent)">
              {{ drawerBundle.progressPercent }}%
            </span>
          </div>
        </div>

        <!-- 工序列表 -->
        <div class="drawer-process-list">
          <div class="drawer-section-title">工序详情</div>
          <div
            v-for="(process, idx) in drawerBundle.processes"
            :key="idx"
            class="drawer-process-item"
            :class="{ 'is-done': process.status === 2, 'is-doing': process.status === 1 }"
          >
            <div class="drawer-process-left">
              <div class="drawer-process-seq">{{ idx + 1 }}</div>
              <div class="drawer-process-info">
                <div class="drawer-process-name">{{ process.processName }}</div>
                <div class="drawer-process-meta" v-if="process.status === 2 && process.operatorName">
                  <el-icon><User /></el-icon>
                  <span>{{ process.operatorName }}</span>
                  <template v-if="process.completeTime">
                    <el-icon class="meta-clock"><Clock /></el-icon>
                    <span>{{ process.completeTime }}</span>
                  </template>
                </div>
                <div class="drawer-process-meta pending-meta" v-else-if="process.status === 1">
                  <span class="doing-dot"></span> 进行中
                </div>
                <div class="drawer-process-meta pending-meta" v-else>
                  待开工
                </div>
              </div>
            </div>
            <div class="drawer-process-status">
              <el-button
                v-if="process.status === 0 && isBoss"
                type="primary"
                size="small"
                @click="openAssignDialog(process)"
              >
                指派完成
              </el-button>
              <el-tag v-if="process.status === 2" type="success" size="small" effect="plain">已完成</el-tag>
              <el-tag v-else-if="process.status === 1" type="primary" size="small" effect="plain">进行中</el-tag>
              <el-tag v-else-if="!isBoss" type="info" size="small" effect="plain">未开始</el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 指派工序弹窗 -->
    <el-dialog
      v-model="assignDialogVisible"
      title="指派工序完成"
      width="420px"
      :close-on-click-modal="false"
    >
      <div v-if="assignProcess" class="assign-dialog-content">
        <div class="assign-info">
          <div class="assign-info-row">
            <span class="assign-label">扎号</span>
            <span class="assign-value mono">{{ drawerBundle?.bundleNo }}</span>
          </div>
          <div class="assign-info-row">
            <span class="assign-label">工序</span>
            <span class="assign-value">{{ assignProcess.processName }}</span>
          </div>
          <div class="assign-info-row">
            <span class="assign-label">件数</span>
            <span class="assign-value">{{ drawerBundle?.quantity }} 件</span>
          </div>
        </div>
        <div class="assign-worker-select">
          <span class="assign-select-label">选择员工</span>
          <el-select
            v-model="assignWorkerId"
            placeholder="请选择完成该工序的员工"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="emp in employeeList"
              :key="emp.userId"
              :label="emp.realName"
              :value="emp.userId"
            >
              <span>{{ emp.realName }}</span>
              <span v-if="emp.phone" style="float: right; color: #8492a6; font-size: 12px">{{ emp.phone }}</span>
            </el-option>
          </el-select>
        </div>
      </div>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssign">确认指派</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Collection, Grid, Document, ArrowRight, Search, User, Clock } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getDashboard, archiveStyle } from '../api/dashboard'
import { submitWorkRecord } from '../api/workRecord'
import { getEmployeeList } from '../api/employee'
import { filterDashboardStyles, getMatchedBeds } from '../utils/dashboardBoardSearch'

const userStore = useUserStore()
const styles = ref([])
const loading = ref(false)
const updateTime = ref('')
let lastManualRefresh = 0

// 选中状态（分级导航）
const selectedStyleId = ref(null)
const selectedStyle = ref(null)
const selectedBedId = ref(null)
const selectedBed = ref(null)

const isSupervisor = computed(() => userStore.isSupervisor())
const isBoss = computed(() => userStore.isBoss())

// 指派工序弹窗
const assignDialogVisible = ref(false)
const assignProcess = ref(null)
const assignWorkerId = ref(null)
const employeeList = ref([])
const assignLoading = ref(false)

// 扎号详情抽屉
const drawerVisible = ref(false)
const drawerBundle = ref(null)

// 床号搜索
const bedSearchKeyword = ref('')

// 筛选后的床号列表
const filteredBeds = computed(() => {
  const list = selectedStyle.value?.beds || []
  if (!bedSearchKeyword.value) return list
  const kw = bedSearchKeyword.value.toLowerCase()
  return list.filter(bed =>
    bed.bedNo?.toLowerCase().includes(kw) ||
    bed.name?.toLowerCase().includes(kw)
  )
})

// 扎号搜索
const bundleSearch = ref('')

const filteredBundles = computed(() => {
  const list = selectedBed.value?.bundles || []
  if (!bundleSearch.value) return list
  const kw = bundleSearch.value.toLowerCase()
  return list.filter(b => b.bundleNo?.toLowerCase().includes(kw))
})

// 搜索筛选分页
const searchKeyword = ref('')
const progressFilter = ref('')
const currentPage = ref(1)
const pageSize = 12

// 筛选后的款式列表
const filteredStyles = computed(() => {
  let list = filterDashboardStyles(styles.value, searchKeyword.value)
  if (progressFilter.value) {
    list = list.filter(s => getProgressClass(s.progressPercent) === progressFilter.value)
  }
  return list
})

// 当前页的款式列表
const pagedStyles = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredStyles.value.slice(start, start + pageSize)
})

// 搜索或筛选时回到第一页
watch([searchKeyword, progressFilter], () => { currentPage.value = 1 })

// ===== 进度相关 =====
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

// 计算款式总扎数
function getTotalBundles(style) {
  if (!style.beds || !style.beds.length) return 0
  return style.beds.reduce((total, bed) => total + (bed.totalBundleCount || 0), 0)
}


// ===== 分级导航 =====
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

function getMatchedBedsForStyle(style) {
  return getMatchedBeds(style, searchKeyword.value)
}

function selectMatchedBed(style, bed) {
  selectStyle(style)
  selectBed(bed)
}

function backToStyles() {
  selectedStyleId.value = null
  selectedStyle.value = null
  selectedBedId.value = null
  selectedBed.value = null
  bedSearchKeyword.value = ''
  bundleSearch.value = ''
}

function backToBeds() {
  selectedBedId.value = null
  selectedBed.value = null
  bundleSearch.value = ''
}

// ===== 扎号详情抽屉 =====
function openBundleDrawer(bundle) {
  drawerBundle.value = bundle
  drawerVisible.value = true
}

// ===== 指派工序 =====
async function loadEmployees() {
  if (employeeList.value.length > 0) return
  try {
    const res = await getEmployeeList({ pageNum: 1, pageSize: 200, status: 1 })
    if (res.code === 200) {
      employeeList.value = res.data?.records || []
    }
  } catch (e) {
    console.error('加载员工列表失败:', e)
  }
}

function openAssignDialog(process) {
  assignProcess.value = process
  assignWorkerId.value = null
  assignDialogVisible.value = true
  loadEmployees()
}

async function handleAssign() {
  if (!assignWorkerId.value) {
    ElMessage.warning('请选择员工')
    return
  }
  assignLoading.value = true
  try {
    const res = await submitWorkRecord({
      bundleId: drawerBundle.value.bundleId,
      bundleProcessId: assignProcess.value.bundleProcessId,
      workerId: assignWorkerId.value
    })
    if (res.code === 200) {
      ElMessage.success('指派成功')
      assignDialogVisible.value = false
      // 刷新看板数据
      await loadDataSilent()
      // 更新抽屉中的数据
      const updatedBundle = findBundle(drawerBundle.value.bundleId)
      if (updatedBundle) {
        drawerBundle.value = updatedBundle
      }
    } else {
      ElMessage.error(res.message || '指派失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '指派失败')
  } finally {
    assignLoading.value = false
  }
}

// ===== 归档款式 =====
async function handleArchiveStyle(style) {
  try {
    await ElMessageBox.confirm(
      `确定要将款式「${style.styleNo}」归档到历史工作中吗？归档后工作看板将不再显示该款式。`,
      '归档确认',
      { confirmButtonText: '确定归档', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await archiveStyle(style.styleId)
    if (res.code === 200) {
      ElMessage.success('归档成功')
      await loadDataSilent()
    } else {
      ElMessage.error(res.message || '归档失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '归档失败')
    }
  }
}

function findBundle(bundleId) {
  for (const style of styles.value) {
    for (const bed of (style.beds || [])) {
      for (const bundle of (bed.bundles || [])) {
        if (bundle.bundleId === bundleId) return bundle
      }
    }
  }
  return null
}

// ===== 数据加载 =====
let autoRefreshTimer = null

// 静默加载（自动刷新用，不弹提示）
async function loadDataSilent() {
  try {
    const res = await getDashboard()
    if (res.code === 200) {
      styles.value = res.data.styles || []
      const t = new Date()
      updateTime.value = `更新于 ${String(t.getHours()).padStart(2, '0')}:${String(t.getMinutes()).padStart(2, '0')}:${String(t.getSeconds()).padStart(2, '0')}`
    }
  } catch (e) {
    console.error('自动刷新看板数据失败:', e)
  }
}

// 手动刷新（点击按钮，5秒限频）
async function loadData() {
  const now = Date.now()
  if (now - lastManualRefresh < 5000) {
    ElMessage.warning('请稍后再刷新，5秒内只能刷新一次')
    return
  }
  lastManualRefresh = now
  loading.value = true
  try {
    const res = await getDashboard()
    if (res.code === 200) {
      styles.value = res.data.styles || []
      const t = new Date()
      updateTime.value = `更新于 ${String(t.getHours()).padStart(2, '0')}:${String(t.getMinutes()).padStart(2, '0')}:${String(t.getSeconds()).padStart(2, '0')}`
      ElMessage.success('数据已刷新')
    }
  } catch (e) {
    console.error('加载看板数据失败:', e)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
  // 每5秒自动刷新
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
.dashboard-board {
  max-width: 1200px;
}

/* ===== 工具栏 ===== */
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

.refresh-btn {
  font-weight: 500;
}

.search-input {
  width: 200px;
}
.filter-select {
  width: 130px;
}
.filter-count {
  font-size: 12px;
  color: var(--color-mist);
  white-space: nowrap;
}

/* ===== 分页 ===== */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* ===== 面包屑 ===== */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
  font-size: 13px;
}
.crumb { color: var(--color-mist); }
.crumb.clickable {
  color: var(--color-amber);
  cursor: pointer;
  font-weight: 500;
}
.crumb.clickable:hover { text-decoration: underline; }
.crumb-sep { font-size: 12px; color: var(--color-mist); }

/* ===== 加载状态 ===== */
.state-loading {
  text-align: center;
  padding: 80px 20px;
}
.state-loading p {
  color: #a0a6b8;
  font-size: 14px;
  margin-top: 16px;
}
.spinner {
  width: 36px;
  height: 36px;
  margin: 0 auto;
  border: 3px solid #f0f2f8;
  border-top-color: var(--color-amber);
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
  padding: 60px 0;
}
.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-cream);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--color-mist);
  margin-bottom: 16px;
}
.empty-state h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate-light);
  margin: 0 0 6px 0;
}
.empty-state p {
  font-size: 13px;
  color: var(--color-mist);
  margin: 0;
}

/* ===== 款式卡片网格 ===== */
.style-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.style-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 18px;
  box-shadow: var(--shadow-soft);
  transition: all 0.25s var(--ease-out);
  display: flex;
  flex-direction: column;
  gap: 14px;
  cursor: pointer;
  animation: slideUp 0.4s ease both;
  opacity: 0;
}
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.style-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
  border-color: var(--color-amber);
}

.matched-beds {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 10px;
  border: 1px solid rgba(232, 168, 56, 0.28);
  border-radius: 10px;
  background: rgba(232, 168, 56, 0.07);
}

.matched-beds-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--color-amber);
}

.matched-bed-button {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 100%;
  min-height: 36px;
  padding: 7px 10px;
  border: 1px solid rgba(232, 168, 56, 0.25);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--color-slate);
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s ease;
}

.matched-bed-button small {
  color: var(--color-mist);
  font-size: 11px;
  font-weight: 500;
}

.matched-bed-button:hover {
  border-color: var(--color-amber);
  color: var(--color-amber);
  transform: translateX(2px);
}

.matched-bed-arrow {
  margin-left: auto;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.style-no {
  font-size: 18px;
  font-weight: 700;
  color: #6366f1;
  font-family: 'Menlo', 'Consolas', monospace;
}

.bed-count {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-amber);
  background: rgba(232, 168, 56, 0.1);
  padding: 2px 10px;
  border-radius: 12px;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.progress-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.progress-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.progress-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-mist);
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

.progress-bar-wrap {
  width: 100%;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #f0f2f8;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}
.progress-bar.small {
  height: 6px;
}
.progress-bar::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 4px;
  box-shadow: inset 0 1px 2px rgba(0,0,0,0.04);
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}
.progress-fill::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 20px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.25));
  border-radius: 0 4px 4px 0;
}
.progress-fill.idle {
  background: #d1d5db;
}
.progress-fill.progress {
  background: linear-gradient(90deg, #60a5fa, #3b82f6);
}
.progress-fill.good {
  background: linear-gradient(90deg, #34d399, #10b981);
}
.progress-fill.complete {
  background: linear-gradient(90deg, #22c55e, #16a34a);
}

.total-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
  border-top: 1px solid #f0eeea;
}
.total-qty, .total-bundles {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-slate-light);
}

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

.archive-btn {
  font-size: 12px;
  padding: 2px 6px;
}

.status-text {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
}
.status-text.status-idle {
  background: rgba(156, 163, 175, 0.1);
  color: #9ca3af;
}
.status-text.status-progress {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}
.status-text.status-good {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}
.status-text.status-complete {
  background: rgba(22, 163, 74, 0.1);
  color: #16a34a;
}

.arrow-icon {
  font-size: 14px;
  color: var(--color-mist);
  transition: all 0.2s;
}
.style-card:hover .arrow-icon {
  color: var(--color-amber);
  transform: translateX(2px);
}

/* ===== 床号详情页 ===== */
.bed-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 款号信息栏 */
.style-info-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  box-shadow: var(--shadow-soft);
}

.style-info-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.info-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-mist);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate);
}

/* 床号搜索栏 */
.bed-search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.bed-search-input {
  width: 240px;
}

.bed-count-label {
  font-size: 12px;
  color: var(--color-mist);
  white-space: nowrap;
}

/* 床号卡片网格 */
.bed-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.bed-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-soft);
  transition: all 0.25s var(--ease-out);
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  animation: slideUp 0.4s ease both;
  opacity: 0;
  border: 2px solid transparent;
}
.bed-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
  border-color: var(--color-amber);
}
.bed-card.my-bed {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(59, 130, 246, 0.02);
}
.bed-card.my-bed:hover {
  border-color: var(--color-primary);
}
.bed-card.other-bed {
  opacity: 0.85;
}

.bed-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0eeea;
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
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  border-radius: 8px;
  flex-shrink: 0;
}

.bed-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate);
}

.bed-desc {
  font-size: 13px;
  color: var(--color-mist);
}

.bed-tag {
  font-weight: 500;
  font-size: 11px;
}

.bed-card-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bed-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 8px 0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px;
  background: var(--color-cream);
  border-radius: 8px;
}

.stat-label {
  font-size: 11px;
  color: var(--color-mist);
  font-weight: 600;
}

.stat-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-slate);
  font-variant-numeric: tabular-nums;
}

.process-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.process-label {
  font-size: 12px;
  color: var(--color-mist);
  font-weight: 500;
  flex-shrink: 0;
}

.process-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  flex: 1;
}

.process-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.operator-info {
  font-size: 9px;
  opacity: 0.8;
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
  border: 1px solid #e5e7eb;
}
.process-tag.pending .dot {
  background: #d1d5db;
}
.process-tag.doing {
  background: #eff6ff;
  color: #3b82f6;
  border: 1px solid #dbeafe;
}
.process-tag.doing .dot {
  background: #3b82f6;
  box-shadow: 0 0 4px rgba(59, 130, 246, 0.4);
  animation: pulse 2s ease infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
.process-tag.done {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}
.process-tag.done .dot {
  background: #22c55e;
}

.more-processes {
  font-size: 11px;
  color: var(--color-mist);
  font-weight: 600;
}

.bed-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f0eeea;
}

/* ===== 扎号详情页 ===== */
.bundle-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 床号概览卡片 */
.bed-overview-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-soft);
}

.bed-overview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0eeea;
}

.bed-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bed-no {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-slate);
  font-family: 'Menlo', 'Consolas', monospace;
}

.bed-name {
  font-size: 14px;
  color: var(--color-mist);
}

.status-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
}
.status-badge.status-idle {
  background: rgba(156, 163, 175, 0.1);
  color: #9ca3af;
}
.status-badge.status-progress {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}
.status-badge.status-good {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}
.status-badge.status-complete {
  background: rgba(22, 163, 74, 0.1);
  color: #16a34a;
}

.bed-overview-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview-progress {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  background: var(--color-cream);
  border-radius: var(--radius-md);
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
  font-size: 20px;
  font-weight: 700;
  color: var(--color-slate);
  font-variant-numeric: tabular-nums;
}

.stat-text {
  font-size: 11px;
  color: var(--color-mist);
  font-weight: 500;
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: #e5e7eb;
}

/* 扎号列表标题 */
.bundle-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
}

.list-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate);
}

.list-count {
  font-size: 12px;
  color: var(--color-mist);
  font-weight: 500;
}

.bundle-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bundle-search-input {
  width: 180px;
}

/* 扎号卡片网格 */
.bundle-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.bundle-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
  transition: all 0.25s var(--ease-out);
  animation: slideUp 0.3s ease both;
  opacity: 0;
  cursor: pointer;
}
.bundle-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
}

.bundle-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f8f9fb 0%, #f0f2f5 100%);
  border-bottom: 1px solid #e5e7eb;
}

.bundle-no {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-slate);
  font-family: 'Menlo', 'Consolas', monospace;
}

.bundle-size {
  font-size: 11px;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.bundle-card-body {
  padding: 12px 16px;
}

.bundle-qty-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.bundle-qty {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-slate-light);
}

.bundle-progress-bar {
  height: 6px;
  background: #f0f2f8;
  border-radius: 3px;
  overflow: hidden;
}

.bundle-card-footer {
  padding: 10px 16px;
  border-top: 1px solid #f0eeea;
}

.process-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.process-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 500;
}
.process-tag .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}
.process-tag.pending {
  background: #f3f4f6;
  color: #9ca3af;
}
.process-tag.pending .dot {
  background: #d1d5db;
}
.process-tag.doing {
  background: #eff6ff;
  color: #3b82f6;
}
.process-tag.doing .dot {
  background: #3b82f6;
  box-shadow: 0 0 4px rgba(59, 130, 246, 0.4);
  animation: pulse 2s ease infinite;
}
.process-tag.done {
  background: #f0fdf4;
  color: #16a34a;
}
.process-tag.done .dot {
  background: #22c55e;
}

.bundle-qty {
  font-size: 12px;
  color: var(--color-slate-light);
  font-weight: 600;
}

/* ===== 手机端适配 ===== */
@media (max-width: 640px) {
  .toolbar {
    flex-wrap: wrap;
  }
  .toolbar-left {
    width: 100%;
    flex-wrap: wrap;
  }
  .toolbar-right {
    width: 100%;
    justify-content: space-between;
  }
  .search-input {
    flex: 1;
    min-width: 120px;
  }
  .filter-select {
    width: 110px;
  }
  .filter-count {
    width: 100%;
  }

  .bed-search-bar {
    flex-wrap: wrap;
  }
  .bed-search-input {
    flex: 1;
    min-width: 150px;
  }

  .style-cards-grid, .bed-cards-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .overview-stats {
    gap: 0;
  }

  .stat-num {
    font-size: 18px;
  }

  .bundle-grid {
    grid-template-columns: 1fr;
  }

  .bed-overview-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

/* ===== 扎号详情抽屉 ===== */
.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.drawer-bundle-info {
  background: var(--color-cream);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.drawer-info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.drawer-label {
  font-size: 13px;
  color: var(--color-mist);
  font-weight: 500;
}

.drawer-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
}
.drawer-value.mono {
  font-family: 'Menlo', 'Consolas', monospace;
}
.drawer-value.complete { color: #16a34a; }
.drawer-value.good { color: #10b981; }
.drawer-value.progress { color: #3b82f6; }
.drawer-value.idle { color: #9ca3af; }

.drawer-section-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-slate);
  margin-bottom: 4px;
}

.drawer-process-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.drawer-process-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid #e5e7eb;
  transition: border-color 0.2s;
}
.drawer-process-item.is-done {
  border-color: #bbf7d0;
  background: #fafff9;
}
.drawer-process-item.is-doing {
  border-color: #bfdbfe;
  background: #fafffe;
}

.drawer-process-left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.drawer-process-seq {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f0f2f8;
  color: var(--color-mist);
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.drawer-process-item.is-done .drawer-process-seq {
  background: #dcfce7;
  color: #16a34a;
}
.drawer-process-item.is-doing .drawer-process-seq {
  background: #dbeafe;
  color: #3b82f6;
}

.drawer-process-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.drawer-process-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-slate);
}

.drawer-process-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--color-mist);
}
.drawer-process-meta .el-icon {
  font-size: 12px;
}
.drawer-process-meta .meta-clock {
  margin-left: 6px;
}
.drawer-process-meta.pending-meta {
  font-size: 11px;
  color: #b0b5c0;
}
.doing-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #3b82f6;
  box-shadow: 0 0 4px rgba(59, 130, 246, 0.4);
  animation: pulse 2s ease infinite;
}

.drawer-process-status {
  flex-shrink: 0;
  margin-left: 8px;
}

/* ===== 指派弹窗 ===== */
.assign-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.assign-info {
  background: var(--color-cream);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.assign-info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.assign-label {
  font-size: 13px;
  color: var(--color-mist);
  font-weight: 500;
}

.assign-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
}
.assign-value.mono {
  font-family: 'Menlo', 'Consolas', monospace;
}

.assign-worker-select {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.assign-select-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-slate);
}
</style>
