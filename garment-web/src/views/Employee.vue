<template>
  <div class="employee-manage">
    <!-- 统计卡片 -->
    <div class="stats-bar" :class="{ 'entered': mounted }">
      <div class="stat-card">
        <div class="stat-icon total">
          <el-icon><UserFilled /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ total }}</span>
          <span class="stat-label">员工总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon supervisor">
          <el-icon><StarFilled /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ supervisorCount }}</span>
          <span class="stat-label">主管</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon worker">
          <el-icon><Tools /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ workerCount }}</span>
          <span class="stat-label">工人</span>
        </div>
      </div>
    </div>

    <!-- 顶部操作栏 -->
    <div class="toolbar" :class="{ 'entered': mounted }">
      <div class="toolbar-left">
        <div class="tab-switch">
          <div
            class="tab-item"
            :class="{ active: activeTab === 1 }"
            @click="switchTab(1)"
          >
            在职员工
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 0 }"
            @click="switchTab(0)"
          >
            已离职
          </div>
        </div>
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="searchKeyword"
            placeholder="搜索姓名或手机号..."
            class="search-input"
            @keyup.enter="loadList"
          />
          <el-icon v-if="searchKeyword" class="clear-icon" @click="resetSearch"><CircleClose /></el-icon>
        </div>
      </div>
      <el-button v-if="activeTab === 1" type="primary" :icon="Plus" @click="openAdd" class="add-btn">
        添加员工
      </el-button>
    </div>

    <!-- 员工列表 -->
    <div class="table-card" :class="{ 'entered': mounted }">
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="headerStyle"
        row-class-name="employee-row"
      >
        <!-- 姓名 -->
        <el-table-column label="员工信息" min-width="180">
          <template #default="{ row }">
            <div class="employee-info">
              <div class="avatar" :class="row.role">
                {{ row.realName ? row.realName.charAt(0) : '?' }}
              </div>
              <div class="info-text">
                <span class="name">{{ row.realName || '-' }}</span>
                <span class="phone">
                  {{ row.showPhone ? row.phone : maskPhone(row.phone) }}
                  <el-icon class="eye-icon" @click.stop="row.showPhone = !row.showPhone">
                    <View v-if="row.showPhone" />
                    <Hide v-else />
                  </el-icon>
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 性别 -->
        <el-table-column label="性别" width="80" align="center">
          <template #default="{ row }">
            <span class="gender-icon" :class="row.gender === 1 ? 'male' : row.gender === 0 ? 'female' : ''">
              <el-icon v-if="row.gender === 1"><Male /></el-icon>
              <el-icon v-else-if="row.gender === 0"><Female /></el-icon>
              <span v-else class="text-muted">-</span>
            </span>
          </template>
        </el-table-column>

        <!-- 角色 -->
        <el-table-column label="职位" width="140" align="center">
          <template #default="{ row }">
            <span class="role-badge" :class="row.role">
              <span class="role-dot"></span>
              {{ roleText(row.role) }}
              <span v-if="row.role === 'supervisor' && row.bundlePrefix" class="prefix-tag">{{ row.bundlePrefix }}</span>
            </span>
          </template>
        </el-table-column>

        <!-- 入职时间 -->
        <el-table-column label="入职时间" width="120" align="center">
          <template #default="{ row }">
            <span class="date-text">{{ row.hireDate || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-dot" :class="row.status === 1 ? 'active' : 'inactive'">
              {{ row.status === 1 ? '在职' : '离职' }}
            </span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <div v-if="activeTab === 1">
              <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
              <el-popconfirm
                title="确定将该员工设为离职吗？"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button link type="danger" size="small">离职</el-button>
                </template>
              </el-popconfirm>
            </div>
            <div v-else>
              <el-popconfirm
                title="确定恢复该员工吗？恢复后员工可重新登录。"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="handleRestore(row)"
              >
                <template #reference>
                  <el-button link type="success" size="small">恢复入职</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <div v-if="!loading && tableData.length === 0" class="empty-state">
        <div class="empty-icon">
          <el-icon><User /></el-icon>
        </div>
        <p class="empty-title">暂无员工</p>
        <p class="empty-desc">点击右上角「添加员工」开始录入</p>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-wrap">
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

    <!-- 添加/编辑员工对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑员工' : '添加员工'"
      width="480px"
      destroy-on-close
      @close="resetForm"
      class="employee-dialog"
    >
      <!-- 手机号已注册提示 -->
      <el-alert
        v-if="existingUserInfo.exists && !isEdit"
        :title="`该手机号已注册（姓名：${existingUserInfo.realName || '-'}，已有公司：${existingUserInfo.companiesText}），确认添加到本公司？`"
        type="warning"
        :closable="false"
        show-icon
        class="existing-alert"
      />

      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <!-- 手机号 -->
        <el-form-item label="手机号" prop="phone" v-if="!isEdit">
          <el-input
            v-model="form.phone"
            placeholder="请输入11位手机号"
            maxlength="11"
            :disabled="existingUserInfo.exists"
            @blur="onPhoneBlur"
          />
        </el-form-item>

        <!-- 姓名 -->
        <el-form-item label="姓名" prop="realName">
          <el-input
            v-model="form.realName"
            placeholder="请输入姓名"
            :disabled="existingUserInfo.exists && !isEdit"
          />
        </el-form-item>

        <!-- 性别 -->
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender" :disabled="existingUserInfo.exists && !isEdit">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="0">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 密码（仅新增时显示） -->
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="留空则自动生成随机密码"
            show-password
          />
        </el-form-item>

        <!-- 身份证号 -->
        <el-form-item label="身份证号" prop="idCard">
          <el-input
            v-model="form.idCard"
            placeholder="请输入身份证号（选填）"
            :disabled="existingUserInfo.exists && !isEdit"
          />
        </el-form-item>

        <!-- 银行卡号 -->
        <el-form-item label="银行卡号" prop="bankCard">
          <el-input
            v-model="form.bankCard"
            placeholder="请输入银行卡号（选填）"
            :disabled="existingUserInfo.exists && !isEdit"
          />
        </el-form-item>

        <!-- 职位 -->
        <el-form-item label="职位" prop="role">
          <el-select v-model="form.role" placeholder="请选择职位" style="width: 100%">
            <el-option label="主管" value="supervisor" />
            <el-option label="工人" value="worker" />
          </el-select>
        </el-form-item>

        <!-- 入职时间 -->
        <el-form-item label="入职时间" prop="hireDate">
          <el-date-picker
            v-model="form.hireDate"
            type="date"
            placeholder="选择入职时间"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '确认添加' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 密码显示弹窗 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="操作成功"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="password-content">
        <el-alert
          :title="passwordInfo.isCustom ? '添加成功！' : '请妥善保存以下信息，密码仅显示一次！'"
          :type="passwordInfo.isCustom ? 'success' : 'warning'"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        />
        <div class="password-info">
          <div class="info-item">
            <span class="label">员工姓名：</span>
            <span class="value">{{ passwordInfo.realName }}</span>
          </div>
          <div class="info-item">
            <span class="label">{{ passwordInfo.isCustom ? '登录密码：' : '临时密码：' }}</span>
            <span class="value password">{{ passwordInfo.password }}</span>
          </div>
          <div v-if="!passwordInfo.isCustom" class="info-item">
            <span class="label">提示：</span>
            <span class="value" style="font-size: 12px; color: #999;">首次登录需修改密码</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="passwordDialogVisible = false">我已记住</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, View, Hide, UserFilled, StarFilled, Tools, Male, Female, CircleClose, User } from '@element-plus/icons-vue'
import { getEmployeeList, addEmployee, updateEmployee, deleteEmployee, checkPhone, resetEmployeePassword, restoreEmployee } from '../api/employee'

const mounted = ref(false)
const loading = ref(false)
const submitting = ref(false)
const searchKeyword = ref('')
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const activeTab = ref(1) // 1=在职, 0=离职

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const passwordDialogVisible = ref(false)
const passwordInfo = ref({ realName: '', password: '' })

const existingUserInfo = reactive({
  exists: false,
  userId: null,
  realName: '',
  companiesText: ''
})

const form = reactive({
  phone: '',
  realName: '',
  gender: 1,
  idCard: '',
  bankCard: '',
  role: 'worker',
  hireDate: '',
  existingUserId: null,
  password: ''
})

const supervisorCount = computed(() => tableData.value.filter(r => r.role === 'supervisor').length)
const workerCount = computed(() => tableData.value.filter(r => r.role === 'worker').length)

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  role: [{ required: true, message: '请选择职位', trigger: 'change' }],
  hireDate: [{ required: true, message: '请选择入职时间', trigger: 'change' }],
  idCard: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback()
          return
        }
        const idCardReg = /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/
        if (!idCardReg.test(value)) {
          callback(new Error('身份证号格式不正确'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const headerStyle = () => ({
  background: '#f8f6f3',
  fontWeight: 600,
  color: '#4a4846',
  fontSize: '13px',
  padding: '14px 12px'
})

onMounted(() => {
  requestAnimationFrame(() => { mounted.value = true })
  loadList()
})

async function loadList() {
  loading.value = true
  try {
    const res = await getEmployeeList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined,
      status: activeTab.value
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取员工列表失败:', err)
    ElMessage.error(err?.message || '获取员工列表失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function switchTab(tab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  pageNum.value = 1
  searchKeyword.value = ''
  loadList()
}

function resetSearch() {
  searchKeyword.value = ''
  pageNum.value = 1
  loadList()
}

function maskPhone(phone) {
  if (!phone) return '-'
  if (phone.length !== 11) return phone
  return phone.substring(0, 3) + '****' + phone.substring(7)
}

function roleText(role) {
  const map = { supervisor: '主管', worker: '工人', boss: '老板' }
  return map[role] || role
}

// 手机号失去焦点时检查
async function onPhoneBlur() {
  if (!form.phone || !/^1\d{10}$/.test(form.phone)) return
  if (isEdit.value) return

  try {
    const res = await checkPhone(form.phone)
    const data = res.data
    if (data.exists) {
      existingUserInfo.exists = true
      existingUserInfo.userId = data.userId
      existingUserInfo.realName = data.realName
      existingUserInfo.companiesText = (data.existingCompanies || []).join('、') || '未知'
      form.realName = data.realName || ''
      form.existingUserId = data.userId
    } else {
      resetExistingInfo()
    }
  } catch (e) {
    // handled
  }
}

function resetExistingInfo() {
  existingUserInfo.exists = false
  existingUserInfo.userId = null
  existingUserInfo.realName = ''
  existingUserInfo.companiesText = ''
  form.existingUserId = null
}

function resetForm() {
  resetExistingInfo()
  form.phone = ''
  form.realName = ''
  form.gender = 1
  form.idCard = ''
  form.bankCard = ''
  form.role = 'worker'
  form.hireDate = ''
  form.existingUserId = null
  form.password = ''
}

function openAdd() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  resetExistingInfo()
  form.phone = row.phone || ''
  form.realName = row.realName || ''
  form.gender = row.gender ?? 1
  form.idCard = row.idCard || ''
  form.bankCard = row.bankCard || ''
  form.role = row.role || 'worker'
  form.hireDate = row.hireDate || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      realName: form.realName,
      gender: form.gender,
      idCard: form.idCard || undefined,
      bankCard: form.bankCard || undefined,
      role: form.role,
      hireDate: form.hireDate || undefined
    }

    if (isEdit.value) {
      await updateEmployee(editId.value, payload)
      ElMessage.success('更新成功')
      dialogVisible.value = false
      loadList()
    } else {
      payload.phone = form.phone
      if (form.password) {
        payload.password = form.password
      }
      if (existingUserInfo.exists) {
        payload.existingUserId = existingUserInfo.userId
      }
      const res = await addEmployee(payload)
      const tempPassword = res.data
      if (tempPassword) {
        passwordInfo.value = {
          realName: form.realName,
          password: tempPassword,
          isCustom: !!form.password
        }
        passwordDialogVisible.value = true
      } else {
        ElMessage.success(existingUserInfo.exists ? '已成功添加到本公司' : '添加成功')
      }
      dialogVisible.value = false
      loadList()
    }
  } catch (e) {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await deleteEmployee(row.id)
    ElMessage.success('已设为离职')
    loadList()
  } catch (e) {
    // handled by interceptor
  }
}

async function handleRestore(row) {
  try {
    await restoreEmployee(row.id)
    ElMessage.success('已恢复入职')
    loadList()
  } catch (e) {
    // handled by interceptor
  }
}

async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(
      `确定重置「${row.realName}」的密码吗？系统将生成新的随机密码`,
      '确认重置密码',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await resetEmployeePassword(row.id)
    const tempPassword = res.data
    if (tempPassword) {
      passwordInfo.value = {
        realName: row.realName,
        password: tempPassword
      }
      passwordDialogVisible.value = true
    } else {
      ElMessage.success('密码重置成功')
    }
  } catch (e) {
    if (e !== 'cancel') {
      // handled
    }
  }
}
</script>

<style scoped>
.employee-manage {
  max-width: 1100px;
}

/* ===== 统计卡片 ===== */
.stats-bar {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.5s var(--ease-out);
}

.stats-bar.entered {
  opacity: 1;
  transform: translateY(0);
}

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-soft);
  transition: transform 0.2s var(--ease-out);
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-icon.total {
  background: linear-gradient(135deg, rgba(232, 168, 56, 0.12), rgba(232, 168, 56, 0.05));
  color: var(--color-amber);
}

.stat-icon.supervisor {
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.12), rgba(251, 191, 36, 0.05));
  color: #d97706;
}

.stat-icon.worker {
  background: linear-gradient(135deg, rgba(74, 222, 128, 0.12), rgba(74, 222, 128, 0.05));
  color: #16a34a;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-slate);
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: var(--color-mist);
}

/* ===== 工具栏 ===== */
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
  align-items: center;
  gap: 16px;
}

/* ===== Tab 切换 ===== */
.tab-switch {
  display: flex;
  background: var(--color-cream-dark);
  border-radius: var(--radius-md);
  padding: 3px;
  gap: 3px;
}

.tab-item {
  padding: 6px 16px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-slate-light);
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
  user-select: none;
}

.tab-item:hover {
  color: var(--color-slate);
}

.tab-item.active {
  background: var(--bg-card);
  color: var(--color-slate);
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-card);
  border: 1.5px solid var(--color-cream-dark);
  border-radius: var(--radius-md);
  padding: 0 12px;
  height: 36px;
  width: 260px;
  transition: border-color 0.2s;
}

.search-box:focus-within {
  border-color: var(--color-amber);
  box-shadow: 0 0 0 3px rgba(232, 168, 56, 0.08);
}

.search-icon {
  font-size: 15px;
  color: var(--color-mist);
  flex-shrink: 0;
}

.search-input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13.5px;
  color: var(--color-slate);
  width: 100%;
}

.search-input::placeholder {
  color: #bbb;
}

.clear-icon {
  font-size: 14px;
  color: var(--color-mist);
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.2s;
}

.clear-icon:hover {
  color: var(--color-slate-light);
}

.add-btn {
  font-weight: 500;
}

/* ===== 表格卡片 ===== */
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

/* 表格行样式 */
:deep(.employee-row) {
  transition: background 0.2s;
}

:deep(.employee-row:hover) {
  background: #fdfcfa !important;
}

:deep(.employee-row td) {
  padding: 10px 12px !important;
  border-bottom: 1px solid #f0eeea !important;
  vertical-align: middle !important;
}

/* ===== 员工信息列 ===== */
.employee-info {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-amber), var(--color-terracotta));
}

.avatar.supervisor {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.avatar.worker {
  background: linear-gradient(135deg, #4ade80, #16a34a);
}

.info-text {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
  white-space: nowrap;
}

.phone {
  font-size: 12.5px;
  color: var(--color-slate-light);
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.eye-icon {
  font-size: 13px;
  color: var(--color-mist);
  cursor: pointer;
  transition: color 0.2s;
}

.eye-icon:hover {
  color: var(--color-slate-light);
}

/* ===== 性别图标 ===== */
.gender-icon {
  font-size: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.gender-icon.male {
  color: #3b82f6;
}

.gender-icon.female {
  color: #ec4899;
}

/* ===== 角色标签 ===== */
.role-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12.5px;
  font-weight: 500;
}

.role-badge.supervisor {
  background: rgba(251, 191, 36, 0.1);
  color: #b45309;
}

.role-badge.worker {
  background: rgba(74, 222, 128, 0.1);
  color: #15803d;
}

.prefix-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.08);
  font-size: 11px;
  font-weight: 700;
  color: #b45309;
  margin-left: 4px;
}

.role-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* ===== 日期文字 ===== */
.date-text {
  font-size: 13px;
  color: var(--color-slate-light);
}

/* ===== 状态标签 ===== */
.status-dot {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  font-weight: 500;
  white-space: nowrap;
  flex-wrap: nowrap;
}

.status-dot::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-dot.active {
  color: #16a34a;
}

.status-dot.inactive {
  color: #9ca3af;
}

/* ===== 操作按钮组 ===== */
.action-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.action-btn {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  border: none;
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  transition: all 0.2s var(--ease-out);
  color: var(--color-slate-light);
  flex-shrink: 0;
}

.action-btn:hover {
  background: var(--color-cream);
}

.action-btn.edit:hover {
  color: var(--color-amber);
  background: rgba(232, 168, 56, 0.08);
}

.action-btn.reset:hover {
  color: #d97706;
  background: rgba(251, 191, 36, 0.08);
}

.action-btn.delete:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.08);
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
}

.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-cream);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--color-mist);
  margin-bottom: 16px;
}

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate-light);
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 13px;
  color: var(--color-mist);
}

/* ===== 分页 ===== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-cream-dark);
}

.text-muted {
  color: var(--color-mist);
  font-size: 13px;
}

/* ===== 对话框 ===== */
:deep(.employee-dialog .el-dialog__header) {
  border-bottom: 1px solid var(--color-cream-dark);
  padding: 16px 24px;
  margin: 0;
}

:deep(.employee-dialog .el-dialog__body) {
  padding: 20px 24px;
}

:deep(.employee-dialog .el-dialog__footer) {
  border-top: 1px solid var(--color-cream-dark);
  padding: 12px 24px;
}

.existing-alert {
  margin-bottom: 16px;
}

/* 密码弹窗 */
.password-content {
  padding: 8px 0;
}

.password-info {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-item .label {
  color: #666;
  font-size: 14px;
  width: 80px;
}

.info-item .value {
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.info-item .value.password {
  color: #e6a23c;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
}
</style>
