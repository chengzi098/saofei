<template>
  <div class="renew-log-page">
    <!-- 顶部工具栏 -->
    <div class="toolbar" :class="{ 'entered': mounted }">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索公司名称..."
          :prefix-icon="Search"
          clearable
          class="search-input"
          @clear="loadList"
          @keyup.enter="loadList"
        />
        <el-button type="primary" :icon="Search" @click="loadList">搜索</el-button>
      </div>
      <div class="toolbar-right">
        <el-tag type="info" size="large" effect="plain">
          共 {{ total }} 条记录
        </el-tag>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-card" :class="{ 'entered': mounted }">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#fafafa', fontWeight: 600 }"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="companyName" label="公司名称" min-width="140" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.remark === '初始租期' ? 'info' : 'warning'" size="small">
              {{ row.remark }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="续费月数" width="100" align="center">
          <template #default="{ row }">
            <span class="months-text">{{ row.months }} 个月</span>
          </template>
        </el-table-column>
        <el-table-column label="续费前到期" min-width="130" align="center">
          <template #default="{ row }">
            <span class="text-muted">{{ row.prevExpireTime ? formatDate(row.prevExpireTime) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="续费后到期" min-width="130" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isExpired(row.newExpireTime) }">
              {{ formatDate(row.newExpireTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作时间" min-width="150" align="center">
          <template #default="{ row }">
            <span class="text-muted">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getAllRenewLogs } from '../api/company'

const mounted = ref(false)
const loading = ref(false)
const searchKeyword = ref('')
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(() => {
  requestAnimationFrame(() => { mounted.value = true })
  loadList()
})

async function loadList() {
  loading.value = true
  try {
    const res = await getAllRenewLogs({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取续费记录失败:', err)
    ElMessage.error(err?.message || '获取续费记录失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function formatDate(dt) {
  if (!dt) return '-'
  return dt.replace('T', ' ').substring(0, 10)
}

function formatDateTime(dt) {
  if (!dt) return '-'
  return dt.replace('T', ' ').substring(0, 16)
}

function isExpired(expireTime) {
  if (!expireTime) return false
  return new Date(expireTime) < new Date()
}
</script>

<style scoped>
.renew-log-page {
  max-width: 1200px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.4s var(--ease-out) 0.1s;
}

.toolbar.entered {
  opacity: 1;
  transform: translateY(0);
}

.toolbar-left {
  display: flex;
  gap: 10px;
}

.search-input {
  width: 240px;
}

/* 表格卡片 */
.table-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-soft);
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.4s var(--ease-out) 0.2s;
}

.table-card.entered {
  opacity: 1;
  transform: translateY(0);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.text-muted {
  color: var(--color-mist);
  font-size: 13px;
}

.text-danger {
  color: var(--color-danger);
}

.months-text {
  font-weight: 500;
  color: var(--color-slate);
}
</style>
