<template>
  <div class="employee-work">
    <!-- 筛选区域 -->
    <div class="filter-bar">
      <div class="filter-row">
        <el-select
          v-model="filterWorkerId"
          placeholder="选择员工"
          clearable
          filterable
          style="width: 160px"
          @change="handleFilter"
        >
          <el-option
            v-for="w in workerList"
            :key="w.userId"
            :label="w.realName"
            :value="w.userId"
          />
        </el-select>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 220px"
          @change="handleFilter"
        />
        <el-select v-model="filterStyleNo" placeholder="款号" clearable filterable style="width: 110px" @change="onStyleChange">
          <el-option v-for="v in styleNoOptions" :key="v" :label="v" :value="v" />
        </el-select>
        <el-select v-model="filterBedNo" placeholder="床号" clearable filterable style="width: 100px" @change="onBedChange">
          <el-option v-for="v in bedNoOptions" :key="v" :label="v" :value="v" />
        </el-select>
        <el-select v-model="filterBundleNo" placeholder="扎号" clearable filterable style="width: 110px" @change="onBundleChange">
          <el-option v-for="v in bundleNoOptions" :key="v" :label="v" :value="v" />
        </el-select>
        <el-select v-model="filterProcessName" placeholder="工序" clearable filterable style="width: 120px" @change="handleFilter">
          <el-option v-for="v in processNameOptions" :key="v" :label="v" :value="v" />
        </el-select>
        <el-button @click="resetFilter">重置</el-button>
      </div>
    </div>

    <!-- 汇总卡片 -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-label">总件数</div>
        <div class="summary-value">{{ summary.totalQuantity }}<span class="unit">件</span></div>
      </div>
      <div class="summary-card amount">
        <div class="summary-label">总金额</div>
        <div class="summary-value">{{ formatMoney(summary.totalAmount) }}<span class="unit">元</span></div>
      </div>
      <div class="summary-card">
        <div class="summary-label">工作天数</div>
        <div class="summary-value">{{ summary.workDays }}<span class="unit">天</span></div>
      </div>
      <div class="summary-card">
        <div class="summary-label">记录条数</div>
        <div class="summary-value">{{ summary.recordCount }}<span class="unit">条</span></div>
      </div>
    </div>

    <!-- 作业速度图表 -->
    <div class="chart-wrap">
      <div class="chart-title">
        作业速度（每扎真实作业耗时）
        <span class="chart-subtitle">按同员工·同床号·同工序的相邻两次报工时间差计算，第一扎不计</span>
      </div>
      <div v-if="!filterWorkerId" class="chart-placeholder">请先选择一名员工，查看其作业速度</div>
      <div v-else-if="speedList.length === 0" class="chart-placeholder">该员工在此期间没有可统计的作业速度数据（需同工序连续报工 ≥2 次）</div>
      <div v-else ref="speedChartRef" class="chart-canvas"></div>
    </div>

    <!-- 明细表格 -->
    <div class="table-wrap">
      <el-table
        :data="recordList"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="workerName" label="员工" width="90" />
        <el-table-column prop="styleNo" label="款号" width="110">
          <template #default="{ row }">
            <span class="style-tag">{{ row.styleNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bedNo" label="床号" width="80">
          <template #default="{ row }">{{ row.bedNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="bundleNo" label="扎号" width="90">
          <template #default="{ row }">
            <span class="bundle-tag">{{ row.bundleNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="processName" label="工序" min-width="100" />
        <el-table-column label="数量" width="80">
          <template #default="{ row }">
            <span>{{ row.quantity }}件</span>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="80">
          <template #default="{ row }">
            <span>{{ formatMoney(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="90">
          <template #default="{ row }">
            <span class="amount-val">{{ formatMoney(row.amount) }}元</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="140">
          <template #default="{ row }">
            <span class="time-text">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @current-change="loadRecords"
        @size-change="handleSizeChange"
        small
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { useThemeStore } from '../stores/theme'
import { getEchartsThemeOpts } from '../utils/echartsTheme'
import { getWorkRecordList } from '../api/workRecord'
import { getEmployeeList } from '../api/employee'
import { getEmployeeWorkSummary, getWorkerSpeed, getFilterOptions } from '../api/analysis'

// 筛选条件
const filterWorkerId = ref(null)
const filterDateRange = ref(null)
const filterStyleNo = ref('')
const filterBedNo = ref('')
const filterBundleNo = ref('')
const filterProcessName = ref('')
const workerList = ref([])

// 筛选下拉候选项
const styleNoOptions = ref([])
const bedNoOptions = ref([])
const bundleNoOptions = ref([])
const processNameOptions = ref([])

// 汇总
const summary = reactive({
  totalQuantity: 0,
  totalAmount: 0,
  workDays: 0,
  recordCount: 0
})

// 明细
const recordList = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

onMounted(() => {
  initDefaultDateRange()
  loadWorkers()
  loadFilterOptions()
  loadAll()
  window.addEventListener('resize', handleResize)
})

// 主题切换时重渲染图表，让坐标轴/文字色跟随主题
const themeStore = useThemeStore()
watch(() => themeStore.mode, () => {
  nextTick(() => {
    if (speedList.value.length) renderSpeedChart()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (speedChartInst) {
    speedChartInst.dispose()
    speedChartInst = null
  }
})

function handleResize() {
  if (speedChartInst) speedChartInst.resize()
}

function formatDuration(min) {
  const m = Number(min) || 0
  const h = Math.floor(m / 60)
  const mm = m % 60
  return h > 0 ? `${h}小时${mm}分` : `${mm}分钟`
}

// ===== 作业速度图（相邻报工时间差法）=====
const speedChartRef = ref(null)
const speedList = ref([])
let speedChartInst = null

async function loadSpeed() {
  if (!filterWorkerId.value) {
    speedList.value = []
    renderSpeedChart()
    return
  }
  if (!filterDateRange.value || filterDateRange.value.length !== 2) return
  try {
    const res = await getWorkerSpeed({
      workerId: filterWorkerId.value,
      startDate: filterDateRange.value[0],
      endDate: filterDateRange.value[1]
    })
    speedList.value = res.data || []
  } catch (e) {
    speedList.value = []
  }
  renderSpeedChart()
}

function renderSpeedChart() {
  nextTick(() => {
    if (!speedChartRef.value) return
    if (!speedChartInst) speedChartInst = echarts.init(speedChartRef.value)
    // 接口按时间倒序，反转成正序（左早右晚）
    const data = [...speedList.value].reverse()
    speedChartInst.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (p) => {
          const d = data[p.dataIndex]
          return `床号：${d.bedNo || '-'}　扎号：${d.bundleNo}<br/>工序：${d.processName || '-'}<br/>耗时：${formatDuration(d.durationMinutes)}<br/>件数：${d.quantity}件<br/>速度：${formatSpeed(d.quantity, d.durationMinutes)}`
        }
      },
      grid: { left: 50, right: 20, top: 24, bottom: 56 },
      xAxis: {
        type: 'category',
        data: data.map(d => d.bundleNo),
        axisLabel: { rotate: 35, fontSize: 11, color: getEchartsThemeOpts().xAxis.axisLabel.color }
      },
      yAxis: { type: 'value', name: '分钟', nameTextStyle: { color: getEchartsThemeOpts().yAxis.axisLabel.color } },
      series: [{
        type: 'bar',
        data: data.map(d => d.durationMinutes),
        itemStyle: { color: '#10b981', borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 40
      }]
    })
  })
}

function formatSpeed(quantity, minutes) {
  const m = Number(minutes) || 0
  if (m <= 0) return '-'
  const perHour = (Number(quantity) || 0) * 60 / m
  return `${perHour.toFixed(1)} 件/小时`
}

// 默认本月
function initDefaultDateRange() {
  const now = new Date()
  const firstDay = new Date(now.getFullYear(), now.getMonth(), 1)
  filterDateRange.value = [formatDateISO(firstDay), formatDateISO(now)]
}

function formatDateISO(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function loadWorkers() {
  try {
    const res = await getEmployeeList({ pageNum: 1, pageSize: 999, status: 1 })
    workerList.value = res.data?.records || []
  } catch (e) {
    console.error('加载员工列表失败:', e)
  }
}

async function loadFilterOptions() {
  const params = {}
  if (filterStyleNo.value) params.styleNo = filterStyleNo.value
  if (filterBedNo.value) params.bedNo = filterBedNo.value
  if (filterBundleNo.value) params.bundleNo = filterBundleNo.value
  try {
    const res = await getFilterOptions(params)
    const d = res.data || {}
    styleNoOptions.value = d.styleNos || []
    bedNoOptions.value = d.bedNos || []
    bundleNoOptions.value = d.bundleNos || []
    processNameOptions.value = d.processNames || []
  } catch (e) {
    console.error('加载筛选候选项失败:', e)
  }
}

// 级联：上层变化时清空下层选项并重新拉取
function onStyleChange() {
  filterBedNo.value = ''
  filterBundleNo.value = ''
  filterProcessName.value = ''
  loadFilterOptions()
  handleFilter()
}

function onBedChange() {
  filterBundleNo.value = ''
  filterProcessName.value = ''
  loadFilterOptions()
  handleFilter()
}

function onBundleChange() {
  filterProcessName.value = ''
  loadFilterOptions()
  handleFilter()
}

// 筛选变化
function handleFilter() {
  pageNum.value = 1
  loadAll()
}

// 重置
function resetFilter() {
  filterWorkerId.value = null
  filterStyleNo.value = ''
  filterBedNo.value = ''
  filterBundleNo.value = ''
  filterProcessName.value = ''
  initDefaultDateRange()
  pageNum.value = 1
  loadFilterOptions()
  loadAll()
}

function handleSizeChange() {
  pageNum.value = 1
  loadRecords()
}

// 同时刷新汇总、明细、图表
async function loadAll() {
  loadSummary()
  loadRecords()
  loadSpeed()
}

async function loadSummary() {
  if (!filterDateRange.value || filterDateRange.value.length !== 2) {
    Object.assign(summary, { totalQuantity: 0, totalAmount: 0, workDays: 0, recordCount: 0 })
    return
  }
  try {
    const params = {
      startDate: filterDateRange.value[0],
      endDate: filterDateRange.value[1]
    }
    if (filterWorkerId.value) {
      params.workerId = filterWorkerId.value
    }
    const res = await getEmployeeWorkSummary(params)
    const list = res.data || []
    // 接口按员工 GROUP BY 返回多行，前端聚合为合计
    const agg = list.reduce((acc, r) => {
      acc.totalQuantity += Number(r.totalQuantity) || 0
      acc.totalAmount += Number(r.totalAmount) || 0
      acc.workDays += Number(r.workDays) || 0
      acc.recordCount += Number(r.recordCount) || 0
      return acc
    }, { totalQuantity: 0, totalAmount: 0, workDays: 0, recordCount: 0 })
    Object.assign(summary, agg)
  } catch (e) {
    console.error('加载汇总失败:', e)
  }
}

async function loadRecords() {
  if (!filterDateRange.value || filterDateRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: 1,
      startDate: filterDateRange.value[0],
      endDate: filterDateRange.value[1]
    }
    if (filterWorkerId.value) {
      params.workerId = filterWorkerId.value
    }
    if (filterStyleNo.value) params.styleNo = filterStyleNo.value
    if (filterBedNo.value) params.bedNo = filterBedNo.value
    if (filterBundleNo.value) params.bundleNo = filterBundleNo.value
    if (filterProcessName.value) params.processName = filterProcessName.value
    const res = await getWorkRecordList(params)
    recordList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function formatMoney(val) {
  const n = Number(val) || 0
  return n.toFixed(2)
}

function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}`
}
</script>

<style scoped>
.employee-work {
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

/* 汇总卡片 */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.summary-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.summary-card.amount {
  background: linear-gradient(135deg, #ff7875, #f56c6c);
  color: #fff;
}

.summary-card.amount .summary-label {
  color: rgba(255,255,255,0.85);
}

.summary-label {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.summary-card.amount .summary-value {
  color: #fff;
}

.summary-value .unit {
  font-size: 14px;
  font-weight: 500;
  margin-left: 4px;
}

/* 图表区域 */
.chart-wrap {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.chart-subtitle {
  font-size: 12px;
  font-weight: 400;
  color: #999;
  margin-left: 8px;
}

.chart-canvas {
  width: 100%;
  height: 320px;
}

.chart-placeholder {
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
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

.time-text {
  font-size: 12px;
  color: #999;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .summary-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
