<template>
  <div class="mobile-wage-records">
    <!-- 统计卡片 -->
    <div class="stats-card">
      <div class="stat-item add">
        <span class="stat-label">本月加钱</span>
        <span class="stat-value">+{{ monthAdd }}元</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item deduct">
        <span class="stat-label">本月扣钱</span>
        <span class="stat-value">-{{ monthDeduct }}元</span>
      </div>
    </div>

    <!-- 状态筛选 -->
    <div class="filter-bar">
      <div
        v-for="tab in statusTabs"
        :key="tab.value"
        class="filter-item"
        :class="{ active: activeStatus === tab.value }"
        @click="activeStatus = tab.value"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- 记录列表 -->
    <div class="record-list" v-loading="loading">
      <div
        v-for="record in filteredList"
        :key="record.id"
        class="record-card"
        :class="[record.type === 1 ? 'add' : 'deduct', `status-${record.status}`]"
      >
        <div class="record-header">
          <div class="record-type">
            <span class="type-icon">{{ record.type === 1 ? '↑' : '↓' }}</span>
            <span class="type-text">{{ record.type === 1 ? '加钱' : '扣钱' }}</span>
          </div>
          <span class="record-amount" :class="record.type === 1 ? 'add' : 'deduct'">
            {{ record.type === 1 ? '+' : '-' }}{{ record.amount }}元
          </span>
        </div>
        <div class="record-remark">{{ record.remark }}</div>
        <div class="record-footer">
          <span class="record-status" :class="`status-${record.status}`">
            {{ statusText(record.status) }}
          </span>
          <span class="record-time">{{ formatTime(record.createTime) }}</span>
        </div>
        <div v-if="record.status === 2" class="deleted-overlay">
          <span>记录已删除</span>
        </div>
      </div>

      <div v-if="filteredList.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-text">暂无调整记录</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyAdjustments } from '../../api/wageAdjustment'

const loading = ref(false)
const recordList = ref([])
const activeStatus = ref(null)

const statusTabs = [
  { label: '全部', value: null },
  { label: '待审批', value: 0 },
  { label: '已生效', value: 1 },
  { label: '已删除', value: 2 }
]

const filteredList = computed(() => {
  if (activeStatus.value === null) return recordList.value
  return recordList.value.filter(r => r.status === activeStatus.value)
})

// 本月统计（已审批的）
const monthAdd = computed(() => {
  const now = new Date()
  const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  return recordList.value
    .filter(r => r.type === 1 && r.status === 1 && r.createTime && r.createTime.startsWith(month))
    .reduce((sum, r) => sum + Number(r.amount), 0)
    .toFixed(2)
})

const monthDeduct = computed(() => {
  const now = new Date()
  const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  return recordList.value
    .filter(r => r.type === 2 && r.status === 1 && r.createTime && r.createTime.startsWith(month))
    .reduce((sum, r) => sum + Number(r.amount), 0)
    .toFixed(2)
})

function statusText(status) {
  const map = { 0: '待审批', 1: '已生效', 2: '已删除' }
  return map[status] || '-'
}

function formatTime(time) {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

async function loadList() {
  loading.value = true
  try {
    const res = await getMyAdjustments()
    if (res.code === 200) {
      recordList.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.mobile-wage-records {
  min-height: 100vh;
  background: var(--bg-page);
  padding: 16px;
  padding-bottom: 80px;
}

.stats-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-item.add .stat-value { color: #67c23a; }
.stat-item.deduct .stat-value { color: #f56c6c; }

.stat-label {
  font-size: 13px;
  color: #999;
  display: block;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: #eee;
}

.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.filter-item {
  padding: 8px 16px;
  border-radius: 20px;
  background: var(--bg-card);
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-item.active {
  background: #409eff;
  color: #fff;
}

.record-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.record-card.status-2 {
  opacity: 0.6;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.record-type {
  display: flex;
  align-items: center;
  gap: 6px;
}

.type-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.record-card.add .type-icon {
  background: #f0f9eb;
  color: #67c23a;
}

.record-card.deduct .type-icon {
  background: #fef0f0;
  color: #f56c6c;
}

.type-text {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.record-amount {
  font-size: 20px;
  font-weight: 700;
}

.record-amount.add { color: #67c23a; }
.record-amount.deduct { color: #f56c6c; }

.record-remark {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
  line-height: 1.5;
}

.record-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.record-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.record-status.status-0 { background: #fdf6ec; color: #e6a23c; }
.record-status.status-1 { background: #f0f9eb; color: #67c23a; }
.record-status.status-2 { background: #f4f4f5; color: #999; }

.record-time {
  font-size: 12px;
  color: #999;
}

.deleted-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(-15deg);
  font-size: 18px;
  font-weight: 700;
  color: #ccc;
  pointer-events: none;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
  color: #999;
}
</style>
