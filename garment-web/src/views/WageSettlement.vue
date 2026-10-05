<template>
  <div class="wage-settlement">
    <!-- ===== 主页面：左右分栏 ===== -->
    <template v-if="!showPreview && !selectedSettlement">
      <div class="main-layout" :class="{ entered: mounted }">
        <!-- 左侧：工资结算操作区 -->
        <div class="left-panel">
          <div class="panel-card settle-card">
            <div class="settle-header">
              <div class="settle-icon">💰</div>
              <h2 class="settle-title">工资结算</h2>
            </div>

            <div class="settle-body">
              <!-- 日期范围选择 -->
              <div class="date-section">
                <label class="section-label">结算时间段</label>
                <el-date-picker
                  v-model="dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="点击选择开始日期"
                  end-placeholder="点击选择结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  size="large"
                  :prefix-icon="Calendar"
                  :disabled-date="disabledDate"
                  class="date-picker-boost"
                />
                <div class="date-hint" v-if="minDate">
                  <span class="hint-icon">📅</span>
                  <span>可选范围：{{ minDate }} 至 {{ maxDate }}</span>
                </div>
              </div>

              <!-- 预览按钮 -->
              <el-button
                type="primary"
                size="large"
                :icon="Search"
                :loading="previewing"
                @click="handlePreview"
                class="preview-btn"
              >
                预览工资明细
              </el-button>
            </div>
          </div>
        </div>

        <!-- 右侧：历史记录 -->
        <div class="right-panel">
          <div class="panel-card history-card">
            <div class="history-header">
              <div class="header-left">
                <span class="history-icon">📋</span>
                <h3 class="history-title">历史记录</h3>
              </div>
              <span class="record-count">{{ settlements.length }} 条</span>
            </div>

            <div class="history-body">
              <div v-if="settlements.length > 0" class="history-list">
                <div
                  v-for="item in settlements"
                  :key="item.id"
                  class="history-item"
                  @click="selectSettlement(item)"
                >
                  <div class="item-left">
                    <div class="item-period">
                      <span class="period-icon">📅</span>
                      <span class="period-text">{{ item.startDate }} ~ {{ item.endDate }}</span>
                    </div>
                    <div class="item-meta">
                      <span class="meta-amount">{{ (item.totalAmount || 0).toFixed(2) }}元</span>
                      <span class="meta-workers">{{ item.workerCount }} 名工人</span>
                    </div>
                  </div>
                  <div class="item-right">
                    <el-tag
                      :type="item.status === 2 ? 'success' : item.status === 1 ? 'warning' : 'info'"
                      size="small"
                      round
                    >
                      {{ item.status === 2 ? '已签名' : item.status === 1 ? '待签名' : '已结算' }}
                    </el-tag>
                    <span class="item-arrow">→</span>
                  </div>
                </div>
              </div>

              <div v-else class="history-empty">
                <div class="empty-icon">📭</div>
                <p class="empty-text">暂无结算记录</p>
                <p class="empty-desc">选择时间段后点击「预览工资明细」开始</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 预览页面 ===== -->
    <template v-if="showPreview && !selectedSettlement">
      <div class="preview-page" :class="{ entered: mounted }">
        <!-- 顶部信息栏 -->
        <div class="preview-header">
          <div class="header-info">
            <div class="info-block">
              <span class="info-label">结算时间段</span>
              <span class="info-value period">{{ previewData.startDate }} ~ {{ previewData.endDate }}</span>
            </div>
            <div class="info-divider"></div>
            <div class="info-block">
              <span class="info-label">工人数</span>
              <span class="info-value">{{ previewData.workerCount }}</span>
            </div>
            <div class="info-divider"></div>
            <div class="info-block">
              <span class="info-label">工资总额</span>
              <span class="info-value amount">{{ (previewData.totalAmount || 0).toFixed(2) }}元</span>
            </div>
          </div>
          <div class="header-actions">
            <el-button @click="showPreview = false" size="large">返回</el-button>
            <el-button
              type="primary"
              size="large"
              :loading="confirming"
              :disabled="previewData.hasPendingPrice"
              @click="handleBatchConfirm"
              class="confirm-btn"
            >
              确认并发起签名
            </el-button>
          </div>
        </div>

        <!-- 待定价警告 -->
        <div v-if="previewData.hasPendingPrice" class="pending-price-warning">
          <el-alert
            type="warning"
            show-icon
            :closable="false"
          >
            <template #title>
              存在 {{ previewData.pendingPriceCount }} 条「待定价」记录，请先完善工序单价后再结算
            </template>
            <template #default>
              <div class="warning-actions">
                <el-button type="warning" size="small" @click="goToStyleManagement">去完善单价</el-button>
              </div>
            </template>
          </el-alert>
        </div>

        <!-- 员工列表 -->
        <div class="worker-section">
          <div class="section-header">
            <div class="section-title-wrap">
              <span class="section-icon">👥</span>
              <h4 class="section-title">员工列表</h4>
              <span class="count-badge">{{ previewData.workers?.length || 0 }} 人</span>
            </div>
            <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
          </div>

          <el-table
            ref="workerTableRef"
            :data="previewData.workers || []"
            stripe
            style="width: 100%"
            @selection-change="handleSelectionChange"
            :header-cell-style="{ background: '#f8f6f3', fontWeight: 600, color: '#4a4846' }"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="workerName" label="工人姓名" width="120" />
            <el-table-column label="职位" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.workerRole === 'supervisor' ? 'warning' : 'info'" size="small" round>
                  {{ roleLabel(row.workerRole) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="手机号" width="150">
              <template #header>
                <span>手机号</span>
                <el-icon
                  class="phone-toggle"
                  @click="showPhone = !showPhone"
                  :style="{ cursor: 'pointer', marginLeft: '6px', verticalAlign: 'middle', color: showPhone ? '#d4a04a' : '#c0c0c0' }"
                >
                  <component :is="showPhone ? View : Hide" />
                </el-icon>
              </template>
              <template #default="{ row }">{{ showPhone ? (row.workerPhone || '-') : maskPhone(row.workerPhone) }}</template>
            </el-table-column>
            <el-table-column prop="totalQuantity" label="完成件数" width="100" align="center">
              <template #default="{ row }"><span class="qty-badge">{{ row.totalQuantity }}</span></template>
            </el-table-column>
            <el-table-column label="计件工资" width="130" align="right">
              <template #default="{ row }"><span class="amount-text">{{ (row.baseAmount || 0).toFixed(2) }}元</span></template>
            </el-table-column>
            <el-table-column label="调整工资" width="130" align="right">
              <template #default="{ row }">
                <span :class="{ 'adjust-add': row.supplementAmount > 0, 'adjust-sub': row.supplementAmount < 0 }">
                  {{ row.supplementAmount > 0 ? '+' : '' }}{{ (row.supplementAmount || 0).toFixed(2) }}元
                </span>
              </template>
            </el-table-column>
            <el-table-column label="应发工资" width="150" align="right">
              <template #default="{ row }"><span class="amount-text highlight">{{ (row.finalAmount || 0).toFixed(2) }}元</span></template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openWorkerPreview(row)">
                  查看明细
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </template>

    <!-- ===== 结算详情 ===== -->
    <template v-if="selectedSettlement">
      <div class="detail-page" :class="{ entered: mounted }">
        <!-- 面包屑 -->
        <div class="breadcrumb">
          <span class="crumb clickable" @click="backToList">结算列表</span>
          <el-icon class="crumb-sep"><ArrowRight /></el-icon>
          <span class="crumb">{{ selectedSettlement.startDate }} ~ {{ selectedSettlement.endDate }}</span>
        </div>

        <!-- 详情信息栏 -->
        <div class="detail-header">
          <div class="header-info">
            <div class="info-block">
              <span class="info-label">结算时间段</span>
              <span class="info-value period">{{ selectedSettlement.startDate }} ~ {{ selectedSettlement.endDate }}</span>
            </div>
            <div class="info-divider"></div>
            <div class="info-block">
              <span class="info-label">工资总额</span>
              <span class="info-value amount">{{ (selectedSettlement.totalAmount || 0).toFixed(2) }}元</span>
            </div>
            <div class="info-divider"></div>
            <div class="info-block">
              <span class="info-label">确认进度</span>
              <span class="info-value">{{ details.filter(d => d.employeeConfirmed).length }}/{{ details.length }}</span>
            </div>
            <div class="info-divider"></div>
            <div class="info-block">
              <span class="info-label">状态</span>
              <el-tag
                :type="selectedSettlement.status === 2 ? 'success' : selectedSettlement.status === 1 ? 'warning' : 'info'"
                size="small"
                round
              >
                {{ selectedSettlement.status === 2 ? '已签名' : selectedSettlement.status === 1 ? '待签名' : '已结算' }}
              </el-tag>
            </div>
          </div>
          <div class="header-actions">
            <el-button type="success" plain @click="handlePrint" size="large">
              <el-icon><Printer /></el-icon>
              打印工资条
            </el-button>
          </div>
        </div>

        <!-- 工人卡片网格 -->
        <div class="worker-section">
          <div class="section-header">
            <div class="section-title-wrap">
              <span class="section-icon">👥</span>
              <h4 class="section-title">工人列表</h4>
              <span class="count-badge">{{ details.length }} 人</span>
            </div>
          </div>

          <div class="worker-cards-grid">
            <div
              v-for="(d, idx) in details"
              :key="d.id || idx"
              class="worker-card"
              :class="{ confirmed: d.status >= 1 }"
              @click="openWorkerDetail(d)"
            >
              <div class="worker-card-header">
                <div class="worker-title-wrap">
                  <span class="worker-index">#{{ idx + 1 }}</span>
                  <span class="worker-name">{{ d.workerName || '未知' }}</span>
                </div>
                <el-tag :type="d.status >= 1 ? 'success' : 'info'" size="small" round>
                  {{ d.status >= 1 ? '已确认' : '待确认' }}
                </el-tag>
              </div>

              <div class="worker-card-body">
                <div class="worker-meta-item">
                  <span class="meta-label">完成件数</span>
                  <span class="meta-value">{{ d.totalQuantity }}</span>
                </div>
                <div class="worker-meta-item">
                  <span class="meta-label">计件工资</span>
                  <span class="meta-value">{{ (d.baseAmount || 0).toFixed(2) }}元</span>
                </div>
                <div class="worker-meta-item" v-if="d.supplementAmount && d.supplementAmount !== 0">
                  <span class="meta-label">调整工资</span>
                  <span class="meta-value" :class="{ 'adjust-add': d.supplementAmount > 0, 'adjust-sub': d.supplementAmount < 0 }">
                    {{ d.supplementAmount > 0 ? '+' : '' }}{{ (d.supplementAmount || 0).toFixed(2) }}元
                  </span>
                </div>
                <div class="worker-meta-item">
                  <span class="meta-label">应发工资</span>
                  <span class="meta-value highlight">{{ (d.finalAmount || 0).toFixed(2) }}元</span>
                </div>
              </div>

              <div class="worker-card-footer">
                <div class="visibility-toggle" @click.stop>
                  <el-switch
                    v-model="d.visibleToEmployee"
                    :active-value="1"
                    :inactive-value="0"
                    size="small"
                    @change="(val) => handleVisibilityChange(d, val)"
                  />
                  <span class="visibility-text">{{ d.visibleToEmployee ? '员工可见' : '员工不可见' }}</span>
                </div>
                <span v-if="d.employeeConfirmed" class="confirmed-text">✓ 员工已确认</span>
              </div>
            </div>
          </div>

          <div v-if="details.length === 0" class="empty-state">
            <div class="empty-icon">👥</div>
            <p class="empty-title">暂无工人数据</p>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 工人报工明细弹窗 ===== -->
    <el-dialog
      v-model="showWorkerDialog"
      :title="currentWorker ? currentWorker.workerName + ' — 工资明细' : ''"
      width="720px"
      destroy-on-close
      class="worker-dialog"
    >
      <div v-if="currentWorker" class="dialog-body">
        <div class="dialog-summary">
          <div class="ds-item">
            <span class="ds-label">完成件数</span>
            <span class="ds-value">{{ currentWorker.totalQuantity }}</span>
          </div>
          <div class="ds-item">
            <span class="ds-label">计件工资</span>
            <span class="ds-value amount">{{ (currentWorker.baseAmount || 0).toFixed(2) }}元</span>
          </div>
          <div class="ds-item" v-if="currentWorker.supplementAmount && currentWorker.supplementAmount !== 0">
            <span class="ds-label">调整工资</span>
            <span class="ds-value" :class="{ 'adjust-add': currentWorker.supplementAmount > 0, 'adjust-sub': currentWorker.supplementAmount < 0 }">
              {{ currentWorker.supplementAmount > 0 ? '+' : '' }}{{ (currentWorker.supplementAmount || 0).toFixed(2) }}元
            </span>
          </div>
          <div class="ds-item">
            <span class="ds-label">应发工资</span>
            <span class="ds-value highlight">{{ (currentWorker.finalAmount || 0).toFixed(2) }}元</span>
          </div>
        </div>

        <div class="work-records-section">
          <div class="wr-header">
            <span class="wr-title">报工明细</span>
            <span class="wr-count">{{ workRecords.length }} 条</span>
          </div>
          <el-table
            :data="workRecords"
            stripe
            size="default"
            style="width: 100%"
            empty-text="暂无报工记录"
            :header-cell-style="{ background: '#f8f6f3', fontWeight: 600, color: '#4a4846' }"
          >
            <el-table-column prop="createTime" label="时间" width="150">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column prop="styleNo" label="款号" width="120">
              <template #default="{ row }"><span class="style-no-text">{{ row.styleNo || '-' }}</span></template>
            </el-table-column>
            <el-table-column prop="bedNo" label="床号" width="100">
              <template #default="{ row }">{{ row.bedNo || '-' }}</template>
            </el-table-column>
            <el-table-column prop="bundleNo" label="扎号" width="90" />
            <el-table-column prop="processName" label="工序" width="120">
              <template #default="{ row }">{{ row.processName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="件数" width="80" align="center">
              <template #default="{ row }"><span class="seq-num">{{ row.quantity }}</span></template>
            </el-table-column>
            <el-table-column label="单价" width="90" align="right">
              <template #default="{ row }">{{ (row.price || 0).toFixed(2) }}元</template>
            </el-table-column>
            <el-table-column label="小计" width="110" align="right">
              <template #default="{ row }"><span class="amount-text">{{ (row.amount || 0).toFixed(2) }}元</span></template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="showWorkerDialog = false" class="footer-btn">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowRight, User, Box, Money, Printer, Calendar, View, Hide } from '@element-plus/icons-vue'
import { getLastSettlement, previewSettlement, confirmEmployees, getSettlementList, getSettlementDetail, getWorkerDetail, setEmployeeVisibility, getWageStatistics } from '../api/wageSettlement'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const router = useRouter()

const mounted = ref(false)
const loading = ref(false)
const previewing = ref(false)
const confirming = ref(false)
const settlements = ref([])

// 新建结算
const dateRange = ref([])
const showPreview = ref(false)
const previewData = ref({})
const minDate = ref('')
const maxDate = ref('')

// 选中状态
const selectedSettlement = ref(null)
const details = ref([])

// 选择员工
const selectAll = ref(false)
const selectedWorkers = ref([])
const workerTableRef = ref(null)
const showPhone = ref(false)

// 弹窗
const showWorkerDialog = ref(false)
const currentWorker = ref(null)
const workRecords = ref([])

onMounted(() => {
  requestAnimationFrame(() => { mounted.value = true })
  loadLastSettlement()
  loadSettlements()
})

async function loadLastSettlement() {
  try {
    const res = await getLastSettlement()
    if (res.data) {
      minDate.value = res.data.nextStartDate || res.data.companyCreateDate
      // 最大可选日期为昨天（今天还没结束，不能结算）
      const yesterday = new Date()
      yesterday.setDate(yesterday.getDate() - 1)
      const maxDateStr = yesterday.toISOString().split('T')[0]
      maxDate.value = maxDateStr
      if (res.data.nextStartDate && res.data.nextStartDate <= maxDateStr) {
        dateRange.value = [res.data.nextStartDate, maxDateStr]
      }
    }
  } catch (err) {
    console.error('加载上次结算信息失败:', err)
  }
}

function disabledDate(date) {
  if (!minDate.value) return false
  const dateStr = date.toISOString().split('T')[0]
  if (dateStr < minDate.value) return true
  if (dateStr > maxDate.value) return true
  return false
}

async function loadSettlements() {
  loading.value = true
  try {
    const res = await getSettlementList()
    settlements.value = res.data || []
  } catch (err) {
    console.error('加载结算单失败:', err)
  } finally {
    loading.value = false
  }
}

async function handlePreview() {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择结算时间段')
    return
  }
  previewing.value = true
  try {
    const res = await previewSettlement({
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    })
    previewData.value = res.data || {}
    showPreview.value = true
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '预览失败')
  } finally {
    previewing.value = false
  }
}

function handleSelectionChange(selection) {
  selectedWorkers.value = selection
  selectAll.value = selection.length === (previewData.value.workers?.length || 0)
}

function roleLabel(role) {
  const map = { boss: '老板', supervisor: '主管', worker: '工人' }
  return map[role] || '工人'
}

function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone || '-'
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
}

function handleSelectAll(val) {
  if (!workerTableRef.value) return
  if (val) {
    workerTableRef.value.toggleAllSelection()
  } else {
    workerTableRef.value.clearSelection()
  }
}

function goToStyleManagement() {
  router.push('/style')
}

async function handleBatchConfirm() {
  if (selectedWorkers.value.length === 0) {
    ElMessage.warning('请先选择要确认的员工')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要对选中的 ${selectedWorkers.value.length} 名员工进行结算吗？结算后将发起工资签名通知。`,
      '确认结算',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch { return }

  confirming.value = true
  try {
    const employeeIds = selectedWorkers.value.map(w => w.workerId)
    const res = await confirmEmployees({
      startDate: previewData.value.startDate,
      endDate: previewData.value.endDate,
      employeeIds
    })
    ElMessage.success(`成功结算 ${res.data?.confirmedCount || 0} 名员工，已发起工资签名通知`)
    showPreview.value = false
    loadSettlements()
    loadLastSettlement()
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '结算失败')
  } finally {
    confirming.value = false
  }
}

function backToList() {
  selectedSettlement.value = null
  details.value = []
  loadSettlements()
}

async function selectSettlement(item) {
  selectedSettlement.value = { ...item }
  try {
    const res = await getSettlementDetail(item.id)
    selectedSettlement.value = res.data?.settlement || item
    details.value = res.data?.details || []
  } catch (err) {
    ElMessage.error('加载详情失败')
    details.value = []
  }
}

async function openWorkerPreview(worker) {
  currentWorker.value = worker
  workRecords.value = worker.workRecords || []
  showWorkerDialog.value = true
}

async function openWorkerDetail(d) {
  try {
    const res = await getWorkerDetail(selectedSettlement.value.id, d.workerId)
    currentWorker.value = res.data?.detail || d
    workRecords.value = res.data?.workRecords || []
    showWorkerDialog.value = true
  } catch (err) {
    ElMessage.error('加载工人详情失败')
  }
}

async function handleVisibilityChange(d, val) {
  try {
    await setEmployeeVisibility(selectedSettlement.value.id, d.workerId, val)
    ElMessage.success(val ? '已设为员工可见' : '已设为员工不可见')
  } catch (err) {
    ElMessage.error('设置失败')
    d.visibleToEmployee = val ? 0 : 1
  }
}

function handlePrint() {
  router.push(`/wage-print/${selectedSettlement.value.id}`)
}

function formatDateTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  return `${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
</script>

<style scoped>
.wage-settlement {
  max-width: 1200px;
}

/* ===== 主页面左右分栏布局 ===== */
.main-layout {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 24px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.1s;
}
.main-layout.entered { opacity: 1; transform: translateY(0); }

/* ===== 面板卡片通用样式 ===== */
.panel-card {
  background: var(--bg-card);
  border-radius: 16px;
  border: 1px solid #ede9e3;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* ===== 左侧：工资结算卡片 ===== */
.settle-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px;
  border-bottom: 1px solid #f0ece6;
  background: linear-gradient(135deg, #faf8f5 0%, #f5f2ed 100%);
}

.settle-icon {
  font-size: 24px;
  line-height: 1;
}

.settle-title {
  font-size: 20px;
  font-weight: 700;
  color: #3d3935;
  margin: 0;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.settle-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  flex: 1;
}

.date-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-label {
  font-size: 13px;
  font-weight: 600;
  color: #8a8580;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.date-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #a09a94;
  padding: 10px 14px;
  background: #faf8f5;
  border-radius: 8px;
  border: 1px solid #ede9e3;
}

.hint-icon {
  font-size: 14px;
  line-height: 1;
}

.preview-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #d4a04a 0%, #c08b3a 100%);
  border: none;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.preview-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(212, 160, 74, 0.25);
}

/* 日期选择器醒目样式 */
:deep(.date-picker-boost .el-input__wrapper) {
  background: #faf8f5;
  border: 2px solid #ede9e3;
  border-radius: 10px;
  box-shadow: none;
  transition: all 0.3s ease;
}

:deep(.date-picker-boost .el-input__wrapper:hover) {
  border-color: #d4a04a;
}

:deep(.date-picker-boost .el-input__wrapper.is-focus) {
  border-color: #d4a04a;
  box-shadow: 0 0 0 3px rgba(212, 160, 74, 0.15);
}

:deep(.date-picker-boost .el-input__icon) {
  color: #d4a04a;
  font-size: 16px;
}

:deep(.date-picker-boost .el-range-input::placeholder) {
  color: #b8b2ac;
  font-size: 13px;
}

/* 弹出日历面板醒目样式 */
:deep(.el-date-range-picker) {
  border: 2px solid #d4a04a;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

:deep(.el-date-range-picker__header) {
  background: #faf8f5;
  border-bottom: 1px solid #ede9e3;
}

:deep(.el-date-range-picker__header .el-picker-panel__icon-btn) {
  color: #d4a04a;
  font-size: 16px;
}

:deep(.el-date-range-picker__header .el-picker-panel__header-title) {
  font-weight: 700;
  color: #3d3935;
}

:deep(.el-date-table th) {
  color: #8a8580;
  font-weight: 600;
}

:deep(.el-date-table td.today .el-date-table-cell__text) {
  color: #d4a04a;
  font-weight: 700;
}

:deep(.el-date-table td.in-range .el-date-table-cell) {
  background: rgba(212, 160, 74, 0.15);
  border-radius: 0;
}

:deep(.el-date-table td.in-range .el-date-table-cell__text) {
  color: #b07830;
  font-weight: 600;
}

:deep(.el-date-table td.start-date .el-date-table-cell),
:deep(.el-date-table td.end-date .el-date-table-cell) {
  background: rgba(212, 160, 74, 0.15);
}

:deep(.el-date-table td.start-date .el-date-table-cell__text),
:deep(.el-date-table td.end-date .el-date-table-cell__text) {
  background: linear-gradient(135deg, #d4a04a 0%, #c08b3a 100%);
  color: #fff;
  font-weight: 700;
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(212, 160, 74, 0.4);
  transform: scale(1.1);
  transition: all 0.2s ease;
}

:deep(.el-date-table td.available:hover .el-date-table-cell__text) {
  color: #d4a04a;
  font-weight: 700;
  transform: scale(1.15);
  transition: all 0.15s ease;
}

:deep(.el-date-table td.available:hover .el-date-table-cell) {
  background: rgba(212, 160, 74, 0.08);
}

:deep(.el-picker-panel__sidebar) {
  border-right: 1px solid #ede9e3;
}

:deep(.el-picker-panel__shortcut) {
  color: #5a5550;
  font-weight: 500;
}

:deep(.el-picker-panel__shortcut:hover) {
  color: #d4a04a;
  background: rgba(212, 160, 74, 0.08);
}

/* ===== 右侧：历史记录卡片 ===== */
.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f0ece6;
  background: linear-gradient(135deg, #faf8f5 0%, #f5f2ed 100%);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.history-icon {
  font-size: 20px;
  line-height: 1;
}

.history-title {
  font-size: 18px;
  font-weight: 700;
  color: #3d3935;
  margin: 0;
}

.record-count {
  font-size: 12px;
  font-weight: 600;
  color: #8a8580;
  background: #f0ece6;
  padding: 4px 12px;
  border-radius: 12px;
}

.history-body {
  flex: 1;
  overflow: hidden;
}

.history-list {
  height: 100%;
  overflow-y: auto;
  max-height: 500px;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #f5f2ed;
  cursor: pointer;
  transition: all 0.2s ease;
}

.history-item:hover {
  background: #faf8f5;
}

.item-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-period {
  display: flex;
  align-items: center;
  gap: 8px;
}

.period-icon {
  font-size: 14px;
  line-height: 1;
}

.period-text {
  font-size: 13px;
  font-weight: 600;
  color: #5a5550;
  font-family: 'Menlo', 'Consolas', monospace;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.meta-amount {
  font-size: 16px;
  font-weight: 700;
  color: #d4a04a;
}

.meta-workers {
  font-size: 12px;
  color: #a09a94;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-arrow {
  font-size: 14px;
  color: #c0bab4;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.history-item:hover .item-arrow {
  opacity: 1;
}

.history-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  height: 100%;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-text {
  font-size: 15px;
  font-weight: 600;
  color: #5a5550;
  margin: 0 0 6px 0;
}

.empty-desc {
  font-size: 13px;
  color: #a09a94;
  margin: 0;
}

/* ===== 预览页面 ===== */
.preview-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.1s;
}
.preview-page.entered { opacity: 1; transform: translateY(0); }

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #ede9e3;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  flex-wrap: wrap;
  gap: 16px;
}

.pending-price-warning {
  margin-top: 16px;
}

.warning-actions {
  margin-top: 8px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.info-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-label {
  font-size: 11px;
  font-weight: 600;
  color: #a09a94;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: #3d3935;
}

.info-value.period {
  font-family: 'Menlo', 'Consolas', monospace;
  font-size: 15px;
  color: #5a5550;
}

.info-value.amount {
  color: #d4a04a;
  font-size: 22px;
  font-weight: 700;
}

.info-divider {
  width: 1px;
  height: 40px;
  background: #ede9e3;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.confirm-btn {
  background: linear-gradient(135deg, #d4a04a 0%, #c08b3a 100%);
  border: none;
  font-weight: 600;
}

.confirm-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(212, 160, 74, 0.25);
}

/* ===== 员工列表区域 ===== */
.worker-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #ede9e3;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-icon {
  font-size: 20px;
  line-height: 1;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #3d3935;
  margin: 0;
}

.count-badge {
  font-size: 12px;
  font-weight: 600;
  color: #8a8580;
  background: #f0ece6;
  padding: 3px 10px;
  border-radius: 10px;
}

/* ===== 数字徽章 ===== */
.qty-badge {
  font-weight: 600;
  color: #5a5550;
}

.amount-text {
  font-weight: 700;
  color: #d4a04a;
  font-variant-numeric: tabular-nums;
}

.amount-text.highlight {
  font-size: 16px;
}

.seq-num {
  font-weight: 700;
  color: #d4a04a;
}

.phone-toggle {
  cursor: pointer;
  transition: color 0.2s;
}
.phone-toggle:hover {
  color: #d4a04a !important;
}

.style-no-text {
  font-weight: 600;
  color: #6366f1;
  font-family: 'Menlo', 'Consolas', monospace;
}

/* ===== 调整金额颜色 ===== */
.adjust-add {
  color: #10b981;
  font-weight: 600;
}

.adjust-sub {
  color: #ef4444;
  font-weight: 600;
}

/* ===== 详情页面 ===== */
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.1s;
}
.detail-page.entered { opacity: 1; transform: translateY(0); }

/* ===== 面包屑 ===== */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.crumb {
  color: #a09a94;
}

.crumb.clickable {
  color: #d4a04a;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s ease;
}

.crumb.clickable:hover {
  color: #c08b3a;
}

.crumb-sep {
  font-size: 12px;
  color: #c0bab4;
}

/* ===== 详情头部 ===== */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #ede9e3;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  flex-wrap: wrap;
  gap: 16px;
}

/* ===== 工人卡片网格 ===== */
.worker-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.worker-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 18px;
  border: 1px solid #ede9e3;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
  gap: 14px;
  cursor: pointer;
}

.worker-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  border-color: #d4a04a;
}

.worker-card.confirmed {
  border-color: #10b981;
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
}

.worker-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0ece6;
}

.worker-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.worker-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #d4a04a, #c08b3a);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  border-radius: 8px;
  flex-shrink: 0;
}

.worker-name {
  font-size: 15px;
  font-weight: 700;
  color: #3d3935;
}

.worker-card-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.worker-meta-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.meta-label {
  font-size: 12px;
  color: #a09a94;
}

.meta-value {
  font-size: 14px;
  font-weight: 600;
  color: #3d3935;
}

.meta-value.highlight {
  color: #d4a04a;
  font-size: 18px;
  font-weight: 700;
}

.worker-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #f0ece6;
}

.visibility-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
}

.visibility-text {
  font-size: 12px;
  color: #a09a94;
}

.confirmed-text {
  font-size: 12px;
  font-weight: 600;
  color: #10b981;
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
}

/* ===== 弹窗样式 ===== */
:deep(.worker-dialog .el-dialog) {
  background: var(--bg-card);
  border: 1px solid #ede9e3;
  border-radius: 16px;
}

:deep(.worker-dialog .el-dialog__header) {
  padding: 20px 28px;
  margin: 0;
  border-bottom: 1px solid #f0ece6;
  background: linear-gradient(135deg, #faf8f5 0%, #f5f2ed 100%);
}

:deep(.worker-dialog .el-dialog__title) {
  font-size: 17px;
  font-weight: 700;
  color: #3d3935;
}

:deep(.worker-dialog .el-dialog__body) {
  padding: 24px 28px;
}

:deep(.worker-dialog .el-dialog__footer) {
  padding: 16px 28px;
  border-top: 1px solid #f0ece6;
}

.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dialog-summary {
  display: flex;
  gap: 32px;
  padding: 18px 22px;
  background: #faf8f5;
  border-radius: 12px;
  border: 1px solid #ede9e3;
}

.ds-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ds-label {
  font-size: 11px;
  font-weight: 600;
  color: #a09a94;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.ds-value {
  font-size: 16px;
  font-weight: 700;
  color: #3d3935;
}

.ds-value.amount {
  color: #d4a04a;
  font-size: 22px;
}

.ds-value.highlight {
  color: #d4a04a;
  font-size: 22px;
}

.work-records-section {}

.wr-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.wr-title {
  font-size: 15px;
  font-weight: 700;
  color: #3d3935;
}

.wr-count {
  font-size: 12px;
  font-weight: 600;
  color: #8a8580;
  background: #f0ece6;
  padding: 3px 10px;
  border-radius: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.footer-btn {
  font-size: 15px;
  padding: 0 28px;
  height: 44px;
  font-weight: 500;
}

/* ===== 响应式：手机端 ===== */
@media (max-width: 768px) {
  .main-layout {
    grid-template-columns: 1fr;
  }

  .preview-header,
  .detail-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-info {
    flex-direction: column;
    gap: 12px;
  }

  .info-divider {
    width: 100%;
    height: 1px;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
  }

  .worker-cards-grid {
    grid-template-columns: 1fr;
  }

  .dialog-summary {
    flex-wrap: wrap;
    gap: 16px;
  }
}
</style>
