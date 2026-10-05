<template>
  <div class="unshipped-list">
    <!-- 筛选区 -->
    <div class="filter-bar">
      <div class="filter-row">
        <el-select
          v-model="filterStyleId"
          placeholder="所有款式"
          clearable
          filterable
          style="width: 200px"
          @change="loadList"
        >
          <el-option
            v-for="s in styleList"
            :key="s.id"
            :label="s.styleNo + (s.name ? ' · ' + s.name : '')"
            :value="s.id"
          />
        </el-select>
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="keyword"
            placeholder="搜索床号"
            class="search-input"
          />
        </div>
        <el-button @click="resetFilter">重置</el-button>
      </div>
    </div>

    <!-- 汇总卡片 -->
    <div class="summary-cards">
      <div class="summary-card accent">
        <div class="summary-label">未出货</div>
        <div class="summary-value">{{ summary.total }}<span class="unit">扎</span></div>
      </div>
      <div class="summary-card">
        <div class="summary-label">未出件数</div>
        <div class="summary-value">{{ summary.quantity }}<span class="unit">件</span></div>
      </div>
      <div class="summary-card warn">
        <div class="summary-label">待出货（已完成）</div>
        <div class="summary-value">{{ summary.ready }}<span class="unit">扎</span></div>
      </div>
      <div class="summary-card">
        <div class="summary-label">生产中</div>
        <div class="summary-value">{{ summary.ongoing }}<span class="unit">扎</span></div>
      </div>
    </div>

    <!-- 列表 -->
    <div class="table-wrap">
      <el-table
        :data="filteredList"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        :row-class-name="rowClass"
      >
        <el-table-column label="款号" width="130">
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
        <el-table-column label="扎号" width="110">
          <template #default="{ row }">
            <span class="bundle-tag">{{ row.bundleNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="尺码" width="80">
          <template #default="{ row }">{{ row.size || '-' }}</template>
        </el-table-column>
        <el-table-column label="件数" width="80">
          <template #default="{ row }">{{ row.quantity }}件</template>
        </el-table-column>
        <el-table-column label="生产进度" min-width="200">
          <template #default="{ row }">
            <div class="progress-cell">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: progressOf(row) + '%' }"></div>
              </div>
              <span class="progress-text">{{ row.completedProcesses }}/{{ row.totalProcesses }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span v-if="row.status === 2" class="status-tag ready">待出货</span>
            <span v-else-if="row.status === 1" class="status-tag doing">生产中</span>
            <span v-else class="status-tag pending">未开始</span>
          </template>
        </el-table-column>
        <el-table-column label="建扎时间" width="150">
          <template #default="{ row }">
            <span class="time-text">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && filteredList.length === 0" class="empty-state">
        <div class="empty-icon"><el-icon><CircleCheck /></el-icon></div>
        <p class="empty-title">没有未出货的扎</p>
        <p class="empty-desc">当前筛选条件下，所有扎都已出货</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, CircleCheck } from '@element-plus/icons-vue'
import { getUnshippedList } from '../api/shipment'
import { getStyleList } from '../api/style'

const loading = ref(false)
const list = ref([])
const styleList = ref([])
const filterStyleId = ref(null)
const keyword = ref('')

const summary = reactive({
  total: 0,
  quantity: 0,
  ready: 0,
  ongoing: 0
})

const filteredList = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(b =>
    (b.bedNo || '').toLowerCase().includes(kw)
  )
})

function progressOf(row) {
  if (!row.totalProcesses) return 0
  return Math.round((row.completedProcesses / row.totalProcesses) * 100)
}

function rowClass({ row }) {
  return row.status === 2 ? 'row-ready' : ''
}

onMounted(() => {
  loadStyles()
  loadList()
})

async function loadStyles() {
  try {
    const res = await getStyleList({ pageNum: 1, pageSize: 999 })
    styleList.value = res.data?.records || []
  } catch (e) {
    styleList.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = {}
    if (filterStyleId.value) params.styleId = filterStyleId.value
    const res = await getUnshippedList(params)
    list.value = res.data || []
    computeSummary()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '加载失败')
    list.value = []
  } finally {
    loading.value = false
  }
}

function computeSummary() {
  let qty = 0, ready = 0, ongoing = 0
  for (const b of list.value) {
    qty += Number(b.quantity) || 0
    if (b.status === 2) ready++
    else ongoing++
  }
  summary.total = list.value.length
  summary.quantity = qty
  summary.ready = ready
  summary.ongoing = ongoing
}

function resetFilter() {
  filterStyleId.value = null
  keyword.value = ''
  loadList()
}

function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return String(dateStr).replace('T', ' ').slice(0, 16)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
</script>

<style scoped>
.unshipped-list {
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
.search-box {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f5f6fa;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 0 10px;
  height: 32px;
  width: 220px;
}
.search-icon { font-size: 14px; color: #9ca3af; }
.search-input {
  border: none; outline: none; background: transparent;
  font-size: 13px; color: #333; width: 100%;
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
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
}
.summary-card.warn {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
}
.summary-card.accent .summary-label,
.summary-card.warn .summary-label {
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
.summary-card.accent .summary-value,
.summary-card.warn .summary-value {
  color: #fff;
}
.summary-value .unit {
  font-size: 14px;
  font-weight: 500;
  margin-left: 4px;
}

/* 表格 */
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
.time-text {
  font-size: 12px;
  color: #999;
}

/* 进度单元格 */
.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.progress-bar {
  flex: 1;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
  min-width: 60px;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #4f46e5);
  border-radius: 3px;
}
.progress-text {
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* 状态标签 */
.status-tag {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
}
.status-tag.ready {
  color: #d97706;
  background: rgba(217, 119, 6, 0.12);
}
.status-tag.doing {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.1);
}
.status-tag.pending {
  color: #9ca3af;
  background: #f3f4f6;
}

/* 待出货行高亮 */
:deep(.el-table .row-ready) {
  background: #fffbeb !important;
}
:deep(.el-table .row-ready:hover > td) {
  background: rgba(217, 119, 6, 0.08) !important;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
}
.empty-icon {
  width: 64px; height: 64px; border-radius: 50%;
  background: rgba(16, 185, 129, 0.1);
  display: flex; align-items: center; justify-content: center;
  font-size: 30px; color: #10b981; margin-bottom: 14px;
}
.empty-title { font-size: 15px; font-weight: 600; color: #4b5563; margin: 0 0 4px; }
.empty-desc { font-size: 13px; color: #9ca3af; margin: 0; }

@media (max-width: 768px) {
  .summary-cards { grid-template-columns: repeat(2, 1fr); }
  .search-box { width: 100%; }
}
</style>
