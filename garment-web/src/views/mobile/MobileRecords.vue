<template>
  <div class="mobile-records">
    <!-- 日期筛选 -->
    <div class="filter-bar">
      <div class="date-tabs">
        <div
          v-for="tab in dateTabs"
          :key="tab.value"
          class="date-tab"
          :class="{ active: activeDateTab === tab.value }"
          @click="switchDateTab(tab.value)"
        >
          {{ tab.label }}
        </div>
      </div>
      <div class="custom-date" v-if="activeDateTab === 'custom'">
        <MobileDateRangePicker
          v-model="dateRange"
          @change="loadRecords"
        />
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-num">{{ statTotalCount }}</div>
        <div class="summary-label">报工次数</div>
      </div>
      <div class="summary-card">
        <div class="summary-num">{{ statTotalPieces }}</div>
        <div class="summary-label">完成件数</div>
      </div>
      <div class="summary-card highlight">
        <div class="summary-num">{{ statTotalAmount }}元</div>
        <div class="summary-label">预计收入</div>
      </div>
    </div>

    <!-- 记录列表 -->
    <div class="records-section">
      <div class="section-title">报工明细</div>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="groupedRecords.length === 0" class="empty-state">
        <el-icon :size="48" color="#d1d5db"><Document /></el-icon>
        <p>暂无报工记录</p>
        <p class="empty-tip">该时间段内没有记录</p>
      </div>

      <div v-else class="record-groups">
        <div v-for="group in groupedRecords" :key="group.date" class="record-group">
          <div class="group-header">
            <span class="group-date">{{ group.date }}</span>
            <span class="group-summary">{{ group.records.length }}条 · {{ group.totalAmount }}元</span>
          </div>
          <div class="group-list">
            <div v-for="record in group.records" :key="record.id" class="record-card">
              <div class="record-main">
                <div class="record-left">
                  <div class="record-style">{{ record.styleNo || record.styleName || '未知款式' }}</div>
                  <div class="record-detail">
                    <span class="record-bundle">{{ record.bundleNo }}</span>
                    <span class="record-process">{{ record.processName }}</span>
                  </div>
                </div>
                <div class="record-right">
                  <div class="record-earn">
                    <template v-if="record.price > 0">+{{ ((record.quantity || 0) * (record.price || 0)).toFixed(2) }}元</template>
                    <template v-else><span class="pricing-pending">定价中</span></template>
                  </div>
                  <div class="record-qty">{{ record.quantity }}件</div>
                </div>
              </div>
              <div class="record-footer">
                <span class="record-time">{{ formatTime(record.createTime) }}</span>
                <span class="record-status" :class="getStatusClass(record.status)">
                  {{ getStatusText(record.status) }}
                </span>
              </div>
              <div v-if="record.status === 3 && record.returnReason" class="record-return-reason">
                <el-icon size="14"><WarningFilled /></el-icon>
                <span>退回原因：{{ record.returnReason }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Loading, Document, WarningFilled } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { getWorkRecordList } from '../../api/workRecord'
import { buildMobileRecordParams } from '../../utils/mobileRecordFilters'
import MobileDateRangePicker from '../../components/mobile/MobileDateRangePicker.vue'

const userStore = useUserStore()
const loading = ref(false)
const records = ref([])
const activeDateTab = ref('today')
const dateRange = ref([])

const dateTabs = [
  { label: '今日', value: 'today' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '自定义', value: 'custom' }
]

const statTotalCount = computed(() => records.value.filter(r => r.status === 1).length)
const statTotalPieces = computed(() =>
  records.value.filter(r => r.status === 1).reduce((sum, r) => sum + (r.quantity || 0), 0)
)
const statTotalAmount = computed(() =>
  records.value
    .filter(r => r.status === 1)
    .reduce((sum, r) => sum + ((r.quantity || 0) * (r.price || 0)), 0)
    .toFixed(2)
)

// 按日期分组
const groupedRecords = computed(() => {
  const groups = {}
  for (const record of records.value) {
    const date = record.createTime ? record.createTime.split('T')[0] : '未知日期'
    if (!groups[date]) {
      groups[date] = { date, records: [], totalAmount: 0 }
    }
    groups[date].records.push(record)
    groups[date].totalAmount += (record.quantity || 0) * (record.price || 0)
  }
  return Object.values(groups)
    .map(g => ({
      ...g,
      totalAmount: g.totalAmount.toFixed(2)
    }))
    .sort((a, b) => b.date.localeCompare(a.date))
})

onMounted(() => {
  loadRecords()
})

function switchDateTab(tab) {
  activeDateTab.value = tab
  if (tab !== 'custom') {
    loadRecords()
  }
}

async function loadRecords() {
  loading.value = true
  try {
    const params = buildMobileRecordParams({
      role: userStore.currentRole,
      userId: userStore.userId,
      activeDateTab: activeDateTab.value,
      dateRange: dateRange.value
    })

    const res = await getWorkRecordList(params)
    records.value = res.data?.records || []
  } catch (err) {
    console.error('加载记录失败:', err)
    records.value = []
  } finally {
    loading.value = false
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function getStatusText(status) {
  const map = { 1: '已通过', 2: '已驳回', 3: '已退回' }
  return map[status] || '未知'
}

function getStatusClass(status) {
  const map = { 1: 'approved', 2: 'rejected', 3: 'returned' }
  return map[status] || ''
}
</script>

<style scoped>
.mobile-records {
  padding: 12px 16px;
  padding-bottom: 100px;
}

/* ===== 日期筛选 ===== */
.filter-bar {
  margin-bottom: 12px;
}

.date-tabs {
  display: flex;
  gap: 8px;
  background: var(--bg-card);
  border-radius: 12px;
  padding: 4px;
}

.date-tab {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.date-tab.active {
  background: var(--color-amber, #d97706);
  color: #fff;
}

.custom-date {
  margin-top: 8px;
  background: var(--bg-card);
  border-radius: 12px;
  padding: 12px;
}

/* ===== 统计卡片 ===== */
.summary-cards {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.summary-card {
  flex: 1;
  background: var(--bg-card);
  border-radius: 14px;
  padding: 14px 10px;
  text-align: center;
}

.summary-card.highlight .summary-num {
  color: var(--color-amber, #d97706);
}

.summary-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
  margin-bottom: 4px;
  font-variant-numeric: tabular-nums;
}

.summary-label {
  font-size: 12px;
  color: #9ca3af;
}

/* ===== 记录列表 ===== */
.records-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
  margin-bottom: 14px;
}

.record-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding: 0 4px;
}

.group-date {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-slate, #2d2a26);
}

.group-summary {
  font-size: 12px;
  color: #9ca3af;
}

.group-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 12px 14px;
  border: 1px solid #f3f4f6;
}

.record-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.record-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.record-style {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate, #2d2a26);
}

.record-detail {
  display: flex;
  align-items: center;
  gap: 8px;
}

.record-bundle {
  font-size: 13px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 1px 8px;
  border-radius: 8px;
}

.record-process {
  font-size: 13px;
  color: #6b7280;
}

.record-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.record-earn {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-amber, #d97706);
  font-variant-numeric: tabular-nums;
}

.pricing-pending {
  color: #f59e0b;
  font-weight: 500;
  font-size: 14px;
}

.record-qty {
  font-size: 12px;
  color: #9ca3af;
}

.record-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.record-time {
  font-size: 12px;
  color: #d1d5db;
}

.record-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.record-status.pending {
  background: #fef3c7;
  color: #d97706;
}
.record-status.approved {
  background: #ecfdf5;
  color: #059669;
}
.record-status.rejected {
  background: #fef2f2;
  color: #ef4444;
}
.record-status.returned {
  background: #f4f4f5;
  color: #909399;
}

.record-return-reason {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  margin-top: 8px;
  background: #fef2f2;
  border-radius: 8px;
  font-size: 12px;
  color: #ef4444;
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 50px 0;
  gap: 8px;
}

.empty-state p {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
}

.empty-tip {
  font-size: 12px !important;
}

/* ===== 加载 ===== */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 50px 0;
  gap: 8px;
  color: #9ca3af;
}
</style>
