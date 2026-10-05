<template>
  <div class="work-review">
    <!-- 筛选区域 -->
    <div class="filter-bar">
      <div class="filter-row">
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 100px" @change="handleFilter">
          <el-option label="全部" value="" />
          <el-option label="已通过" :value="1" />
          <el-option label="已退回" :value="3" />
        </el-select>
        <el-select
          v-model="filterWorkerName"
          placeholder="工人姓名"
          clearable
          filterable
          style="width: 140px"
          @change="handleFilter"
        >
          <el-option
            v-for="w in workerList"
            :key="w.id"
            :label="w.realName"
            :value="w.realName"
          />
        </el-select>
        <el-input
          v-model="filterBedNo"
          placeholder="床号"
          clearable
          style="width: 100px"
          @input="handleFilterDebounce"
        />
        <el-input
          v-model="filterBundleNo"
          placeholder="扎号"
          clearable
          style="width: 100px"
          @input="handleFilterDebounce"
        />
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
          @change="handleFilter"
        />
        <el-button @click="resetFilter">重置</el-button>
      </div>
      <div class="filter-row">
        <div class="scan-area">
          <el-input
            v-model="manualCode"
            placeholder="输入二维码内容查询"
            clearable
            @keyup.enter="handleManualQuery"
            style="width: 200px"
          />
          <el-button type="primary" @click="handleManualQuery" :loading="scanLoading">
            查询
          </el-button>
          <el-button @click="handleScanClick">
            <el-icon><Iphone /></el-icon>
            <span>扫码</span>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 报工记录表格 -->
    <div class="table-wrap">
      <el-table
        :data="recordList"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="workerName" label="工人" width="80" />
        <el-table-column prop="styleNo" label="款式" width="100">
          <template #default="{ row }">
            <span class="style-tag">{{ row.styleNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bundleNo" label="扎号" width="80">
          <template #default="{ row }">
            <span class="bundle-tag">{{ row.bundleNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="processName" label="工序" width="100" />
        <el-table-column label="数量" width="100">
          <template #default="{ row }">
            <span>{{ row.quantity }}/{{ row.bundleQuantity || '?' }}件</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="80">
          <template #default="{ row }">
            <span class="amount-val">{{ row.amount }}元</span>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="120">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                :percentage="getProgressPercent(row)"
                :stroke-width="6"
                :show-text="false"
                style="flex: 1"
              />
              <span class="progress-text">{{ row.completedProcesses || 0 }}/{{ row.totalProcesses || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="100">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="100">
          <template #default="{ row }">
            <span v-if="row.workerRemark" class="remark-text">{{ row.workerRemark }}</span>
            <span v-else-if="row.status === 3" class="return-reason">
              <span class="return-label">退回：</span>{{ row.returnReason }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              size="small"
              type="danger"
              text
              @click="handleReturn(row)"
            >
              退回
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
        small
        background
      />
    </div>

    <!-- 退回弹窗 -->
    <el-dialog
      v-model="returnDialogVisible"
      title="退回报工"
      width="420px"
      :close-on-click-modal="false"
    >
      <div class="return-dialog">
        <div class="return-info" v-if="returnTarget">
          <span class="return-label">工人：</span>
          <span>{{ returnTarget.workerName }}</span>
          <span class="return-process">「{{ returnTarget.processName }}」</span>
        </div>

        <div class="return-section">
          <div class="return-section-title">退回原因（必填）</div>
          <div class="return-default-reasons">
            <el-tag
              v-for="r in defaultReturnReasons"
              :key="r"
              :type="returnReason === r ? '' : 'info'"
              @click="returnReason = r"
              style="cursor: pointer"
            >{{ r }}</el-tag>
          </div>
          <el-input
            v-model="returnReason"
            type="textarea"
            :rows="3"
            placeholder="请输入退回原因"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="danger" :disabled="!returnReason.trim()" @click="confirmReturn">
          确认退回
        </el-button>
      </template>
    </el-dialog>

    <!-- 扫码结果弹窗 -->
    <el-dialog
      v-model="scanDialogVisible"
      title="扎货工序详情"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="scan-result" v-if="scanBundleInfo">
        <div class="scan-bundle-header">
          <div class="scan-bundle-row">
            <span class="scan-bundle-label">款式</span>
            <span class="scan-bundle-value">{{ scanBundleInfo.styleNo || '-' }}</span>
          </div>
          <div class="scan-bundle-row">
            <span class="scan-bundle-label">扎号</span>
            <span class="scan-bundle-value highlight">{{ scanBundleInfo.bundleNo }}</span>
          </div>
          <div class="scan-bundle-row">
            <span class="scan-bundle-label">件数</span>
            <span class="scan-bundle-value">{{ scanBundleInfo.quantity }}件</span>
          </div>
        </div>

        <div class="scan-process-title">工序完成情况</div>
        <div class="scan-process-list">
          <div
            v-for="p in scanBundleInfo.processes"
            :key="p.id"
            class="scan-process-item"
            :class="{ 'is-done': p.status === 2 }"
          >
            <div class="scan-process-left">
              <span class="scan-process-seq">{{ p.seq }}</span>
              <span class="scan-process-name">{{ p.processName }}</span>
            </div>
            <div class="scan-process-right">
              <template v-if="p.status === 2">
                <span class="scan-process-status done">已完成</span>
                <span class="scan-process-worker">{{ p.workerName }}</span>
                <el-button size="small" type="danger" text @click="handleScanReturn(p)">退回</el-button>
              </template>
              <span v-else-if="p.status === 1" class="scan-process-status doing">进行中</span>
              <span v-else class="scan-process-status pending">未开始</span>
            </div>
          </div>
        </div>

        <div class="scan-progress">
          <span>完成进度：</span>
          <el-progress
            :percentage="getScanProgressPercent()"
            :stroke-width="8"
            style="flex: 1; margin: 0 10px"
          />
          <span class="scan-progress-text">{{ getScanCompletedCount() }}/{{ scanBundleInfo.processes.length }}</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="scanDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone } from '@element-plus/icons-vue'
import { getWorkRecordList, returnWorkRecord } from '../api/workRecord'
import { scanBundle } from '../api/bundle'

function extractQrCode(raw) {
  try {
    const url = new URL(raw)
    const code = url.searchParams.get('code')
    if (code) return code
  } catch {}
  return raw
}
import { getEmployeeList } from '../api/employee'

const router = useRouter()

// 筛选条件
const filterStatus = ref('')
const filterWorkerName = ref('')
const workerList = ref([])
const filterBedNo = ref('')
const filterBundleNo = ref('')
const filterDateRange = ref(null)

// 防抖定时器
let filterTimer = null

const recordList = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

// 手动输入查询
const manualCode = ref('')
const scanLoading = ref(false)
const scanDialogVisible = ref(false)
const scanBundleInfo = ref(null)

// 退回弹窗
const returnDialogVisible = ref(false)
const returnTarget = ref(null)
const returnReason = ref('')
const defaultReturnReasons = ['扫错码', '质量不合格', '尺寸不对', '做错了需要返工', '线头太多', '工序漏做']

onMounted(() => {
  loadRecords()
  loadWorkers()
})

async function loadWorkers() {
  try {
    const res = await getEmployeeList({ pageNum: 1, pageSize: 999, status: 1 })
    workerList.value = res.data?.records || []
  } catch (e) {
    console.error('加载工人列表失败:', e)
  }
}

async function loadRecords() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    // 筛选条件
    if (filterStatus.value !== '') {
      params.status = filterStatus.value
    }
    if (filterWorkerName.value) {
      params.workerName = filterWorkerName.value.trim()
    }
    if (filterBedNo.value) {
      params.bedNo = filterBedNo.value.trim()
    }
    if (filterBundleNo.value) {
      params.bundleNo = filterBundleNo.value.trim()
    }
    if (filterDateRange.value && filterDateRange.value.length === 2) {
      params.startDate = filterDateRange.value[0]
      params.endDate = filterDateRange.value[1]
    }
    const res = await getWorkRecordList(params)
    recordList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 筛选变化
function handleFilter() {
  pageNum.value = 1
  loadRecords()
}

// 防抖筛选
function handleFilterDebounce() {
  if (filterTimer) {
    clearTimeout(filterTimer)
  }
  filterTimer = setTimeout(() => {
    pageNum.value = 1
    loadRecords()
  }, 300)
}

// 重置筛选
function resetFilter() {
  filterStatus.value = ''
  filterWorkerName.value = ''
  filterBedNo.value = ''
  filterBundleNo.value = ''
  filterDateRange.value = null
  pageNum.value = 1
  loadRecords()
}

function handlePageChange(p) {
  pageNum.value = p
  loadRecords()
}

function handleSizeChange(size) {
  pageSize.value = size
  pageNum.value = 1
  loadRecords()
}

function getStatusType(status) {
  return { 1: 'success', 2: 'danger', 3: 'info' }[status] || 'info'
}

function getStatusText(status) {
  return { 1: '已通过', 2: '已驳回', 3: '已退回' }[status] || '未知'
}

function getProgressPercent(r) {
  if (!r.totalProcesses) return 0
  return Math.round((r.completedProcesses / r.totalProcesses) * 100)
}

function handleReturn(r) {
  returnTarget.value = r
  returnReason.value = ''
  returnDialogVisible.value = true
}

async function confirmReturn() {
  if (!returnReason.value.trim()) return

  try {
    await returnWorkRecord(returnTarget.value.bundleProcessId, { reason: returnReason.value.trim() })
    ElMessage.success('已退回')
    returnDialogVisible.value = false
    loadRecords()
    // 如果扫码弹窗打开着，刷新扫码数据
    if (scanDialogVisible.value && scanBundleInfo.value) {
      handleManualQuery()
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '退回失败')
  }
}

// 手动输入查询
async function handleManualQuery() {
  const code = manualCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入二维码内容')
    return
  }

  scanLoading.value = true
  try {
    const res = await scanBundle(extractQrCode(code))
    scanBundleInfo.value = res.data
    scanDialogVisible.value = true
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '查询失败')
  } finally {
    scanLoading.value = false
  }
}

// 扫码按钮点击
function handleScanClick() {
  const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
  if (isMobile) {
    router.push('/mobile/scan')
  } else {
    ElMessage.warning('请使用手机端扫码查工序')
  }
}

// 扫码弹窗中的退回
function handleScanReturn(p) {
  returnTarget.value = {
    bundleProcessId: p.id,
    processName: p.processName,
    workerName: p.workerName
  }
  returnReason.value = ''
  returnDialogVisible.value = true
}

// 扫码进度计算
function getScanProgressPercent() {
  if (!scanBundleInfo.value || !scanBundleInfo.value.processes) return 0
  const total = scanBundleInfo.value.processes.length
  const completed = scanBundleInfo.value.processes.filter(p => p.status === 2).length
  return total > 0 ? Math.round((completed / total) * 100) : 0
}

function getScanCompletedCount() {
  if (!scanBundleInfo.value || !scanBundleInfo.value.processes) return 0
  return scanBundleInfo.value.processes.filter(p => p.status === 2).length
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${min}`
}
</script>

<style scoped>
.work-review {
  padding: 20px;
  background: var(--bg-page);
  min-height: 100vh;
}

/* 筛选栏 */
.filter-bar {
  background: var(--bg-card);
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.filter-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-row + .filter-row {
  margin-top: 12px;
}

.scan-area {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-left: auto;
}

/* 表格区域 */
.table-wrap {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.style-tag {
  color: #6366f1;
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

.bundle-tag {
  color: #3b82f6;
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

.amount-val {
  color: #f56c6c;
  font-weight: 600;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-text {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}

.time-text {
  font-size: 12px;
  color: #999;
}

.remark-text {
  font-size: 12px;
  color: #666;
}

.return-reason {
  font-size: 12px;
  color: #999;
}

.return-label {
  color: #f56c6c;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 退回弹窗样式 */
.return-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.return-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  font-size: 14px;
}

.return-label {
  color: #999;
}

.return-process {
  color: #409eff;
  font-weight: 500;
}

.return-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.return-default-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.return-section-title {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

/* 扫码结果弹窗 */
.scan-bundle-header {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

.scan-bundle-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}

.scan-bundle-row + .scan-bundle-row {
  border-top: 1px solid #eee;
}

.scan-bundle-label {
  font-size: 13px;
  color: #999;
}

.scan-bundle-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.scan-bundle-value.highlight {
  color: #409eff;
  font-weight: 700;
  font-family: 'Courier New', monospace;
}

.scan-process-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.scan-process-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.scan-process-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.scan-process-item.is-done {
  background: #f0f9eb;
}

.scan-process-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.scan-process-seq {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #e0e0e0;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.scan-process-item.is-done .scan-process-seq {
  background: #67c23a;
  color: #fff;
}

.scan-process-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.scan-process-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.scan-process-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.scan-process-status.done {
  background: #67c23a;
  color: #fff;
}

.scan-process-status.doing {
  background: #e6a23c;
  color: #fff;
}

.scan-process-status.pending {
  background: #e0e0e0;
  color: #666;
}

.scan-process-worker {
  font-size: 12px;
  color: #059669;
}

.scan-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #666;
}

.scan-progress-text {
  font-weight: 600;
  color: #333;
  white-space: nowrap;
}

/* 响应式 */
@media (max-width: 768px) {
  .top-bar {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .scan-area {
    width: 100%;
  }

  .scan-area .el-input {
    flex: 1;
  }
}
</style>
