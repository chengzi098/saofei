<template>
  <div class="work-records">
    <!-- 顶部筛选栏 -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-radio-group v-model="activeDateTab" size="default" @change="onDateTabChange">
          <el-radio-button value="today">今日</el-radio-button>
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
          <el-radio-button value="custom">自定义</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-if="activeDateTab === 'custom'"
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="margin-left: 12px"
          @change="loadRecords"
        />
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-icon" style="background: rgba(59,130,246,0.1); color: #3b82f6">
          <el-icon :size="22"><Document /></el-icon>
        </div>
        <div class="summary-info">
          <div class="summary-num">{{ statTotalCount }}</div>
          <div class="summary-label">报工次数</div>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-icon" style="background: rgba(16,185,129,0.1); color: #10b981">
          <el-icon :size="22"><Box /></el-icon>
        </div>
        <div class="summary-info">
          <div class="summary-num">{{ statTotalPieces }}</div>
          <div class="summary-label">完成件数</div>
        </div>
      </div>
      <div class="summary-card highlight">
        <div class="summary-icon" style="background: rgba(217,119,6,0.1); color: #d97706">
          <el-icon :size="22"><Money /></el-icon>
        </div>
        <div class="summary-info">
          <div class="summary-num">{{ statTotalAmount }}元</div>
          <div class="summary-label">预计收入</div>
        </div>
      </div>
    </div>

    <!-- 记录表格 -->
    <div class="records-panel">
      <div class="panel-header">
        <span class="panel-title">报工明细</span>
        <span class="panel-subtitle">共 {{ records.length }} 条记录</span>
      </div>

      <el-table
        v-loading="loading"
        :data="records"
        stripe
        style="width: 100%"
        empty-text="暂无报工记录"
        :header-cell-style="{ background: '#fafafa', color: '#374151', fontWeight: 600 }"
      >
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="styleNo" label="款号" min-width="120">
          <template #default="{ row }">
            {{ row.styleNo || row.styleName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="bundleNo" label="扎号" width="100" />
        <el-table-column prop="processName" label="工序" width="120" />
        <el-table-column prop="quantity" label="件数" width="80" align="center">
          <template #default="{ row }">
            <span class="qty-badge">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="单价" width="80" align="right">
          <template #default="{ row }">
            {{ (row.price || 0).toFixed(2) }}元
          </template>
        </el-table-column>
        <el-table-column label="收入" width="100" align="right">
          <template #default="{ row }">
            <span class="amount-text">{{ ((row.quantity || 0) * (row.price || 0)).toFixed(2) }}元</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" round>
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Document, Box, Money } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getWorkRecordList } from '../api/workRecord'

const userStore = useUserStore()
const loading = ref(false)
const records = ref([])
const activeDateTab = ref('today')
const dateRange = ref([])

const statTotalCount = computed(() => records.value.length)
const statTotalPieces = computed(() =>
  records.value.reduce((sum, r) => sum + (r.quantity || 0), 0)
)
const statTotalAmount = computed(() =>
  records.value
    .filter(r => r.status === 1)
    .reduce((sum, r) => sum + ((r.quantity || 0) * (r.price || 0)), 0)
    .toFixed(2)
)

onMounted(() => {
  loadRecords()
})

function onDateTabChange(tab) {
  if (tab !== 'custom') {
    loadRecords()
  }
}

async function loadRecords() {
  loading.value = true
  try {
    const params = {
      pageNum: 1,
      pageSize: 200,
      workerId: userStore.userId || undefined
    }

    const now = new Date()
    const today = now.toISOString().split('T')[0]

    switch (activeDateTab.value) {
      case 'today':
        params.startDate = today
        params.endDate = today
        break
      case 'week': {
        const dayOfWeek = now.getDay() || 7
        const startOfWeek = new Date(now)
        startOfWeek.setDate(now.getDate() - dayOfWeek + 1)
        params.startDate = startOfWeek.toISOString().split('T')[0]
        params.endDate = today
        break
      }
      case 'month':
        params.startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
        params.endDate = today
        break
      case 'custom':
        if (dateRange.value && dateRange.value.length === 2) {
          params.startDate = dateRange.value[0]
          params.endDate = dateRange.value[1]
        }
        break
    }

    const res = await getWorkRecordList(params)
    records.value = res.data?.records || []
  } catch (err) {
    console.error('加载记录失败:', err)
    records.value = []
  } finally {
    loading.value = false
  }
}

function formatDateTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${h}:${m}`
}

function getStatusText(status) {
  const map = { 1: '已通过', 2: '已驳回', 3: '已退回' }
  return map[status] || '未知'
}

function getStatusType(status) {
  const map = { 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}
</script>

<style scoped>
.work-records {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 筛选栏 */
.filter-bar {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
}

.filter-left {
  display: flex;
  align-items: center;
}

/* 统计卡片 */
.summary-cards {
  display: flex;
  gap: 16px;
}

.summary-card {
  flex: 1;
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.summary-info {
  display: flex;
  flex-direction: column;
}

.summary-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.summary-card.highlight .summary-num {
  color: var(--color-amber, #d97706);
}

.summary-label {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 2px;
}

/* 记录面板 */
.records-panel {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
}

.panel-subtitle {
  font-size: 13px;
  color: #9ca3af;
}

.qty-badge {
  font-weight: 600;
  color: var(--color-slate, #2d2a26);
}

.amount-text {
  font-weight: 700;
  color: var(--color-amber, #d97706);
  font-variant-numeric: tabular-nums;
}
</style>
