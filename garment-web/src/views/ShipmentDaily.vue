<template>
  <div class="shipment-daily">
    <!-- 筛选区域 -->
    <div class="filter-bar">
      <div class="filter-header">
        <div class="filter-title-wrap">
          <span class="filter-icon-box"><el-icon><Calendar /></el-icon></span>
          <div>
            <div class="filter-title">出货筛选</div>
            <div class="filter-sub">选择日期，查看当天的出货明细</div>
          </div>
        </div>
        <el-button text class="reset-btn" @click="resetFilter">
          <el-icon><RefreshRight /></el-icon>&nbsp;重置
        </el-button>
      </div>
      <div class="filter-row">
        <el-radio-group v-model="quickRange" class="quick-group" @change="handleQuick">
          <el-radio-button value="today">今天</el-radio-button>
          <el-radio-button value="yesterday">昨天</el-radio-button>
          <el-radio-button value="dayBefore">前天</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="date-picker"
          style="width: 280px"
          @change="handleDateChange"
        />
      </div>
    </div>

    <!-- 汇总卡片 -->
    <div class="summary-cards">
      <div class="summary-card accent">
        <div class="summary-label">出货扎数</div>
        <div class="summary-value">{{ summary.totalBundles }}<span class="unit">扎</span></div>
      </div>
      <div class="summary-card">
        <div class="summary-label">出货总件数</div>
        <div class="summary-value">{{ summary.totalQuantity }}<span class="unit">件</span></div>
      </div>
      <div class="summary-card">
        <div class="summary-label">涉及款号</div>
        <div class="summary-value">{{ summary.styleCount }}<span class="unit">款</span></div>
      </div>
      <div class="summary-card">
        <div class="summary-label">出货人</div>
        <div class="summary-value">{{ summary.shipperCount }}<span class="unit">人</span></div>
      </div>
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
        <el-table-column label="出货时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatDateTime(row.shippedTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="styleNo" label="款号" width="120">
          <template #default="{ row }">
            <span class="style-tag">{{ row.styleNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="styleName" label="款式" min-width="120">
          <template #default="{ row }">{{ row.styleName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="bedNo" label="床号" width="80">
          <template #default="{ row }">{{ row.bedNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="bundleNo" label="扎号" width="100">
          <template #default="{ row }">
            <span class="bundle-tag">{{ row.bundleNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="尺码" width="80">
          <template #default="{ row }">{{ row.size || '-' }}</template>
        </el-table-column>
        <el-table-column label="件数" width="90">
          <template #default="{ row }">
            <span class="qty-val">{{ row.quantity }}件</span>
          </template>
        </el-table-column>
        <el-table-column prop="shipperName" label="出货人" width="100">
          <template #default="{ row }">{{ row.shipperName || '-' }}</template>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, RefreshRight } from '@element-plus/icons-vue'
import { getShipmentDaily, getShipmentSummary } from '../api/shipment'

// 筛选条件
const filterDateRange = ref(null)
const quickRange = ref('today')

// 汇总
const summary = reactive({
  totalBundles: 0,
  totalQuantity: 0,
  styleCount: 0,
  shipperCount: 0
})

// 明细
const recordList = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

onMounted(() => {
  initDefaultDateRange()
  loadAll()
})

// 默认今天
function initDefaultDateRange() {
  const now = new Date()
  const today = formatDateISO(now)
  filterDateRange.value = [today, today]
}

function formatDateISO(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function handleFilter() {
  pageNum.value = 1
  loadAll()
}

// 快捷按钮：今天 / 昨天 / 前天（单天范围）
function handleQuick(val) {
  const offset = val === 'today' ? 0 : val === 'yesterday' ? -1 : -2
  const day = dateOffset(offset)
  filterDateRange.value = [day, day]
  pageNum.value = 1
  loadAll()
}

// 手动改日期范围时，取消快捷按钮的选中
function handleDateChange() {
  quickRange.value = null
  handleFilter()
}

function dateOffset(days) {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return formatDateISO(d)
}

function resetFilter() {
  quickRange.value = 'today'
  initDefaultDateRange()
  pageNum.value = 1
  loadAll()
}

function handleSizeChange() {
  pageNum.value = 1
  loadRecords()
}

async function loadAll() {
  loadSummary()
  loadRecords()
}

async function loadSummary() {
  if (!filterDateRange.value || filterDateRange.value.length !== 2) {
    Object.assign(summary, { totalBundles: 0, totalQuantity: 0, styleCount: 0, shipperCount: 0 })
    return
  }
  try {
    const res = await getShipmentSummary({
      startDate: filterDateRange.value[0],
      endDate: filterDateRange.value[1]
    })
    Object.assign(summary, res.data || {})
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
    const res = await getShipmentDaily({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      startDate: filterDateRange.value[0],
      endDate: filterDateRange.value[1]
    })
    recordList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return String(dateStr).replace('T', ' ').slice(0, 16)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}`
}
</script>

<style scoped>
.shipment-daily {
  padding: 20px;
  background: var(--bg-page);
  min-height: 100vh;
}

/* 筛选栏（暖色商务） */
.filter-bar {
  background: linear-gradient(135deg, #fffdf7 0%, #fff8ea 100%);
  border: 1px solid #f3e7cb;
  padding: 18px 22px;
  border-radius: 16px;
  margin-bottom: 16px;
  box-shadow: 0 4px 18px rgba(217, 119, 6, 0.07);
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.filter-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  background: linear-gradient(135deg, #d97706, #b45309);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  box-shadow: 0 4px 12px rgba(217, 119, 6, 0.28);
}

.filter-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
}

.filter-sub {
  font-size: 12px;
  color: #a78b5f;
  margin-top: 2px;
}

.reset-btn {
  color: #b45309 !important;
  font-weight: 600;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

/* 快捷按钮 */
.quick-group :deep(.el-radio-button__inner) {
  border-color: #ecdcb6;
  background: var(--bg-card);
  color: #8a6d3b;
  font-weight: 600;
  padding: 9px 20px;
  transition: all 0.2s ease;
}
.quick-group :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 10px 0 0 10px;
}
.quick-group :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 0 10px 10px 0;
}
.quick-group :deep(.el-radio-button__inner:hover) {
  color: #d97706;
  background: #fff8ea;
}
.quick-group :deep(.is-active .el-radio-button__inner) {
  background: linear-gradient(135deg, #d97706, #b45309) !important;
  border-color: #d97706 !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(217, 119, 6, 0.32);
}

/* 日期选择器 */
.date-picker :deep(.el-range-editor.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #ecdcb6 inset;
  transition: all 0.2s ease;
}
.date-picker :deep(.el-range-editor.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #d97706 inset;
}
.date-picker :deep(.el-range-editor.is-active),
.date-picker :deep(.el-range-editor.is-focus) {
  box-shadow: 0 0 0 1px #d97706 inset, 0 0 0 3px rgba(217, 119, 6, 0.14) !important;
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

.summary-card.accent {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
}

.summary-card.accent .summary-label {
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

.summary-card.accent .summary-value {
  color: #fff;
}

.summary-value .unit {
  font-size: 14px;
  font-weight: 500;
  margin-left: 4px;
}

/* 表格区域 */
.table-wrap {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.style-tag {
  color: #10b981;
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

.bundle-tag {
  color: #3b82f6;
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

.qty-val {
  font-weight: 600;
  color: #333;
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
