<template>
  <div class="wage-adjustment">
    <!-- 统计卡片 -->
    <div class="stats-bar">
      <div class="stat-card">
        <div class="stat-icon pending">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ pendingCount }}</span>
          <span class="stat-label">待审批</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon approved">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ approvedCount }}</span>
          <span class="stat-label">已审批</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon add-amount">
          <el-icon><Top /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ totalAdd }}元</span>
          <span class="stat-label">加钱总额</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon deduct-amount">
          <el-icon><Bottom /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ totalDeduct }}元</span>
          <span class="stat-label">扣钱总额</span>
        </div>
      </div>
    </div>

    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="tab-switch">
          <div
            v-for="tab in statusTabs"
            :key="tab.value"
            class="tab-item"
            :class="{ active: activeStatus === tab.value }"
            @click="activeStatus = tab.value; loadList()"
          >
            {{ tab.label }}
          </div>
        </div>
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="workerSearch"
            placeholder="搜索工人姓名..."
            class="search-input"
            @keyup.enter="loadList"
          />
        </div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd" class="add-btn">
        添加记录
      </el-button>
    </div>

    <!-- 调整记录表格 -->
    <div class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#f8f9fc', color: '#333', fontWeight: 600 }"
      >
        <el-table-column label="工人" min-width="120">
          <template #default="{ row }">
            <div class="worker-info">
              <div class="worker-avatar">{{ row.workerName ? row.workerName.charAt(0) : '?' }}</div>
              <span>{{ row.workerName || '-' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'danger'" effect="dark" round>
              {{ row.type === 1 ? '加钱' : '扣钱' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span :class="['amount', row.type === 1 ? 'add' : 'deduct']">
              {{ row.type === 1 ? '+' : '-' }}{{ row.amount }}元
            </span>
          </template>
        </el-table-column>

        <el-table-column label="备注" prop="remark" min-width="200" show-overflow-tooltip />

        <el-table-column label="操作人" width="100" align="center">
          <template #default="{ row }">
            {{ row.operatorName || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain" round>
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="审批人" width="100" align="center">
          <template #default="{ row }">
            {{ (row.status === 1 || row.status === 3) ? (row.approverName || '-') : '-' }}
          </template>
        </el-table-column>

        <el-table-column label="时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button
                v-if="canEdit(row)"
                type="primary"
                link
                size="small"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="isBoss && row.status === 0"
                type="success"
                link
                size="small"
                @click="handleApprove(row)"
              >
                审批
              </el-button>
              <el-button
                v-if="isBoss && row.status === 0"
                type="danger"
                link
                size="small"
                @click="handleReject(row)"
              >
                拒绝
              </el-button>
              <el-button
                v-if="canDelete(row)"
                type="danger"
                link
                size="small"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="tableData.length === 0 && !loading" class="empty-tip">
        暂无调整记录
      </div>
    </div>

    <!-- 添加弹窗 -->
    <el-dialog
      v-model="addVisible"
      :title="editingId ? '编辑工资调整' : '添加工资调整'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="80px">
        <el-form-item label="工人" prop="workerId">
          <el-select
            v-model="addForm.workerId"
            placeholder="请选择工人"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="w in workerList"
              :key="w.userId"
              :label="w.realName"
              :value="w.userId"
            >
              <span>{{ w.realName }}</span>
              <span style="float: right; color: #8492a6; font-size: 12px">{{ w.role === 'supervisor' ? '主管' : '工人' }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="addForm.type">
            <el-radio :value="1">
              <span style="color: #67c23a; font-weight: 600">加钱</span>
            </el-radio>
            <el-radio :value="2">
              <span style="color: #f56c6c; font-weight: 600">扣钱</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number
            v-model="addForm.amount"
            :min="0.01"
            :precision="2"
            :step="10"
            placeholder="请输入金额"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="addForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请说明原因（必填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd" :loading="submitting">{{ editingId ? '保存' : '确定' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Clock, CircleCheck, Top, Bottom } from '@element-plus/icons-vue'
import { createAdjustment, updateAdjustment, getAdjustmentList, approveAdjustment, rejectAdjustment, deleteAdjustment } from '../api/wageAdjustment'
import { getEmployeeList } from '../api/employee'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const isBoss = computed(() => userStore.isBoss())

const loading = ref(false)
const tableData = ref([])
const activeStatus = ref(null)
const workerSearch = ref('')
const workerList = ref([])

const statusTabs = [
  { label: '全部', value: null },
  { label: '待审批', value: 0 },
  { label: '已审批', value: 1 },
  { label: '已驳回', value: 3 },
  { label: '已删除', value: 2 }
]

// 统计
const pendingCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const approvedCount = computed(() => tableData.value.filter(r => r.status === 1).length)
const totalAdd = computed(() => {
  return tableData.value
    .filter(r => r.type === 1 && r.status === 1)
    .reduce((sum, r) => sum + Number(r.amount), 0)
    .toFixed(2)
})
const totalDeduct = computed(() => {
  return tableData.value
    .filter(r => r.type === 2 && r.status === 1)
    .reduce((sum, r) => sum + Number(r.amount), 0)
    .toFixed(2)
})

// 添加/编辑弹窗
const addVisible = ref(false)
const submitting = ref(false)
const addFormRef = ref(null)
const editingId = ref(null)
const addForm = reactive({
  workerId: null,
  type: 1,
  amount: null,
  remark: ''
})
const addRules = {
  workerId: [{ required: true, message: '请选择工人', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  remark: [{ required: true, message: '请填写备注', trigger: 'blur' }]
}

function statusText(status) {
  const map = { 0: '待审批', 1: '已审批', 2: '已删除', 3: '已驳回' }
  return map[status] || '-'
}

function statusTagType(status) {
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }
  return map[status] || 'info'
}

function formatTime(time) {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

function canDelete(row) {
  if (isBoss.value) {
    // 老板可以删除待审批和已审批
    return row.status === 0 || row.status === 1
  }
  // 主管只能删除自己添加的待审批记录
  return row.status === 0 && row.operatorId === userStore.userId
}

function canEdit(row) {
  if (row.status !== 0) return false
  if (isBoss.value) return true
  // 主管只能编辑自己添加的
  return row.operatorId === userStore.userId
}

async function loadList() {
  loading.value = true
  try {
    const params = {}
    if (activeStatus.value !== null) params.status = activeStatus.value
    if (workerSearch.value.trim()) params.workerName = workerSearch.value.trim()

    const res = await getAdjustmentList(params)
    if (res.code === 200) {
      // 前端过滤工人姓名（后端不支持模糊匹配workerName，这里做前端筛选）
      let list = res.data || []
      if (workerSearch.value.trim()) {
        const keyword = workerSearch.value.trim().toLowerCase()
        list = list.filter(r => r.workerName && r.workerName.toLowerCase().includes(keyword))
      }
      tableData.value = list
    }
  } finally {
    loading.value = false
  }
}

async function loadWorkers() {
  try {
    const res = await getEmployeeList({ pageNum: 1, pageSize: 999, status: 1 })
    if (res.code === 200) {
      workerList.value = (res.data?.records || []).filter(e => e.role === 'worker' || e.role === 'supervisor')
    }
  } catch {}
}

function openAdd() {
  editingId.value = null
  addForm.workerId = null
  addForm.type = 1
  addForm.amount = null
  addForm.remark = ''
  addVisible.value = true
  loadWorkers()
}

function openEdit(row) {
  editingId.value = row.id
  addForm.workerId = row.workerId
  addForm.type = row.type
  addForm.amount = row.amount
  addForm.remark = row.remark || ''
  addVisible.value = true
  loadWorkers()
}

async function submitAdd() {
  if (!addFormRef.value) return
  await addFormRef.value.validate()
  submitting.value = true
  try {
    let res
    if (editingId.value) {
      res = await updateAdjustment(editingId.value, addForm)
    } else {
      res = await createAdjustment(addForm)
    }
    if (res.code === 200) {
      ElMessage.success(editingId.value ? '修改成功' : '添加成功')
      addVisible.value = false
      loadList()
    } else {
      ElMessage.error(res.message || (editingId.value ? '修改失败' : '添加失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(`确认审批通过该${row.type === 1 ? '加钱' : '扣钱'} ${row.amount}元 记录？`, '审批确认')
    const res = await approveAdjustment(row.id)
    if (res.code === 200) {
      ElMessage.success('审批通过')
      loadList()
    } else {
      ElMessage.error(res.message || '审批失败')
    }
  } catch {}
}

async function handleReject(row) {
  try {
    await ElMessageBox.confirm(`确认拒绝该${row.type === 1 ? '加钱' : '扣钱'} ${row.amount}元 记录？`, '拒绝确认', { type: 'warning' })
    const res = await rejectAdjustment(row.id)
    if (res.code === 200) {
      ElMessage.success('已驳回')
      loadList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {}
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该记录？删除后员工仍可看到删除痕迹。', '删除确认', { type: 'warning' })
    const res = await deleteAdjustment(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      loadList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {}
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.wage-adjustment {
  padding: 0;
}

.stats-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.stat-icon.pending { background: #fdf6ec; color: #e6a23c; }
.stat-icon.approved { background: #f0f9eb; color: #67c23a; }
.stat-icon.add-amount { background: #ecf5ff; color: #409eff; }
.stat-icon.deduct-amount { background: #fef0f0; color: #f56c6c; }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.tab-switch {
  display: flex;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 3px;
}

.tab-item {
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}

.tab-item.active {
  background: var(--bg-card);
  color: #409eff;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: #999;
}

.search-input {
  padding: 8px 12px 8px 36px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  width: 200px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #409eff;
}

.table-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.worker-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.worker-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.amount {
  font-weight: 700;
  font-size: 15px;
}

.amount.add { color: #67c23a; }
.amount.deduct { color: #f56c6c; }

.action-btns {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}
</style>
