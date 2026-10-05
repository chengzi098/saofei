<template>
  <div class="company-manage">
    <!-- 顶部操作栏 -->
    <div class="toolbar" :class="{ 'entered': mounted }">
      <div class="toolbar-left">
        <el-input
          v-model="searchName"
          placeholder="企业名"
          :prefix-icon="Search"
          clearable
          class="search-input"
          @clear="loadList"
          @keyup.enter="loadList"
        />
        <el-input
          v-model="searchPhone"
          placeholder="手机号码"
          clearable
          class="search-input"
          @clear="loadList"
          @keyup.enter="loadList"
        />
        <el-input
          v-model="searchBossName"
          placeholder="老板名字"
          clearable
          class="search-input"
          @clear="loadList"
          @keyup.enter="loadList"
        />
        <el-select v-model="searchStatus" placeholder="状态" clearable class="status-select" @change="loadList">
          <el-option label="全部" value="" />
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="loadList">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增公司</el-button>
    </div>

    <!-- 公司列表 -->
    <div class="table-card" :class="{ 'entered': mounted }">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#fafafa', fontWeight: 600 }"
        :row-class-name="rowClassName"
      >
        <el-table-column prop="name" label="公司名称" min-width="140" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
        <el-table-column label="老板账号" width="140">
          <template #default="{ row }">
            <span v-if="row.bossUsername" class="boss-info">
              <el-tag size="small" type="warning" effect="plain">{{ row.bossUsername }}</el-tag>
            </span>
            <span v-else class="text-muted">未生成</span>
          </template>
        </el-table-column>
        <el-table-column label="老板姓名" width="100">
          <template #default="{ row }">
            <span v-if="row.bossRealName">{{ row.bossRealName }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="到期时间" width="110" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isExpired(row.expireTime), 'text-warning': isNearExpire(row.expireTime) }">
              {{ row.expireTime ? formatDate(row.expireTime) : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small" effect="light">正常</el-tag>
            <el-tag v-else type="danger" size="small" effect="dark">
              <el-icon><Lock /></el-icon> 已禁用
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" link type="success" size="small" @click="enterCompany(row)">
              <el-icon><View /></el-icon> 进入查看
            </el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" size="small" @click="openBatchAdd(row)">批量添加员工</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="warning" size="small" @click="openRenew(row)">续费</el-button>
            <el-popconfirm
              title="确定重置该老板密码吗？系统将生成新的随机密码"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleResetPassword(row)"
            >
              <template #reference>
                <el-button link type="warning" size="small">重置密码</el-button>
              </template>
            </el-popconfirm>
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

    <!-- 续费对话框 -->
    <el-dialog
      v-model="renewDialogVisible"
      title="续费"
      width="440px"
      destroy-on-close
      class="renew-dialog"
    >
      <div class="renew-content">
        <p class="renew-hint">请选择续费时长</p>
        <div class="renew-options">
          <div
            v-for="opt in renewOptions"
            :key="opt.value"
            class="renew-option"
            :class="{ active: renewMonths === opt.value }"
            @click="renewMonths = opt.value"
          >
            <span class="renew-option-value">{{ opt.label }}</span>
            <span class="renew-option-tag">{{ opt.tag }}</span>
          </div>
        </div>
        <div v-if="renewMonths" class="renew-preview">
          <el-icon><Info-Filled /></el-icon>
          <span>{{ renewPreviewText }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="renewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRenewSubmit">确认续费</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑公司' : '新增公司'"
      width="500px"
      destroy-on-close
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="公司名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入地址（选填）" />
        </el-form-item>
        <el-form-item label="租期时长" prop="expireTime" v-if="!isEdit">
          <div class="duration-select">
            <div
              v-for="opt in renewOptions"
              :key="opt.value"
              class="duration-item"
              :class="{ active: form.months === opt.value }"
              @click="selectCreateMonths(opt.value)"
            >
              <span class="duration-num">{{ opt.label }}</span>
              <span class="duration-unit">{{ opt.tag }}</span>
            </div>
          </div>
          <div class="expire-preview" v-if="form.expireTime">
            <el-icon><Calendar /></el-icon>
            <span>到期时间 {{ formatDateTime(form.expireTime) }}</span>
          </div>
        </el-form-item>
        <el-form-item label="到期时间" prop="expireTime" v-else>
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="选择到期时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="!isEdit">
          <el-alert
            title="系统将自动创建老板账号：用联系电话作为用户名，密码随机生成"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 密码显示弹窗 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="账号创建成功"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="password-content">
        <el-alert
          title="请妥善保存以下信息，密码仅显示一次！"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        />
        <div class="password-info">
          <div class="info-item">
            <span class="label">用户名：</span>
            <span class="value">{{ passwordInfo.username }}</span>
          </div>
          <div class="info-item">
            <span class="label">临时密码：</span>
            <span class="value password">{{ passwordInfo.password }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="passwordDialogVisible = false">我已记住</el-button>
      </template>
    </el-dialog>

    <!-- 批量添加员工对话框 -->
    <el-dialog
      v-model="batchDialogVisible"
      :title="`批量添加员工 - ${batchCompanyName}`"
      width="600px"
      destroy-on-close
      @close="resetBatchForm"
    >
      <el-form :model="batchForm" :rules="batchRules" ref="batchFormRef" label-width="100px">
        <!-- 初始密码 -->
        <el-form-item label="初始密码" prop="password">
          <el-input
            v-model="batchForm.password"
            placeholder="请设置统一初始密码"
            show-password
          />
        </el-form-item>

        <!-- 默认职位 -->
        <el-form-item label="默认职位" prop="role">
          <el-select v-model="batchForm.role" style="width: 100%">
            <el-option label="工人" value="worker" />
            <el-option label="主管" value="supervisor" />
          </el-select>
        </el-form-item>

        <!-- 员工列表 -->
        <el-form-item label="员工列表" prop="employeesText">
          <el-input
            v-model="batchForm.employeesText"
            type="textarea"
            :rows="10"
            placeholder="每行一个员工，格式：手机号 姓名&#10;例如：&#10;13800138001 张三&#10;13800138002 李四&#10;13800138003 王五"
          />
          <div class="form-tip">
            支持空格、逗号、制表符分隔手机号和姓名
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="handleBatchSubmit">
          确认添加
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量添加结果弹窗 -->
    <el-dialog
      v-model="batchResultVisible"
      title="批量添加结果"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="batch-result">
        <el-alert
          v-if="batchResult.successCount > 0"
          :title="`成功添加 ${batchResult.successCount} 名员工`"
          type="success"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        />
        <el-alert
          v-if="batchResult.failures && batchResult.failures.length > 0"
          :title="`${batchResult.failures.length} 名员工添加失败`"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        />
        <div v-if="batchResult.failures && batchResult.failures.length > 0" class="fail-list">
          <div v-for="(fail, index) in batchResult.failures" :key="index" class="fail-item">
            <span class="fail-phone">{{ fail.phone || '未知' }}</span>
            <span class="fail-name">{{ fail.realName || '-' }}</span>
            <span class="fail-reason">{{ fail.reason }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="batchResultVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Plus, InfoFilled, Calendar, Lock, View } from '@element-plus/icons-vue'
import {
  getCompanyList, createCompany, updateCompany,
  toggleCompanyStatus, resetBossPassword, renewCompany
} from '../api/company'
import { batchAddEmployees } from '../api/employee'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const mounted = ref(false)
const loading = ref(false)
const submitting = ref(false)
const searchName = ref('')
const searchPhone = ref('')
const searchBossName = ref('')
const searchStatus = ref('')
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const renewDialogVisible = ref(false)
const renewCompanyId = ref(null)
const renewMonths = ref(null)
const renewBaseExpire = ref(null)

const passwordDialogVisible = ref(false)
const passwordInfo = ref({ username: '', password: '' })

// 批量添加相关
const batchDialogVisible = ref(false)
const batchSubmitting = ref(false)
const batchFormRef = ref(null)
const batchCompanyId = ref(null)
const batchCompanyName = ref('')
const batchResultVisible = ref(false)
const batchResult = ref({ successCount: 0, failures: [] })
const batchForm = reactive({
  password: '',
  role: 'worker',
  employeesText: ''
})

const renewOptions = [
  { value: 1, label: '1', tag: '个月' },
  { value: 3, label: '3', tag: '个月' },
  { value: 6, label: '6', tag: '个月' },
  { value: 12, label: '1', tag: '年' },
  { value: 24, label: '2', tag: '年' }
]

const renewPreviewText = computed(() => {
  const opt = renewOptions.find(o => o.value === renewMonths.value)
  if (!opt) return ''
  return `已选择续费 ${opt.label}${opt.tag}，到期时间将自动延长`
})

const form = reactive({
  name: '',
  contactPerson: '',
  contactPhone: '',
  address: '',
  expireTime: '',
  months: 12
})

const rules = {
  name: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const batchRules = {
  password: [
    { required: true, message: '请设置初始密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择默认职位', trigger: 'change' }],
  employeesText: [
    { required: true, message: '请输入员工列表', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!value || !value.trim()) {
          callback(new Error('请输入员工列表'))
          return
        }
        const lines = value.trim().split('\n').filter(line => line.trim())
        if (lines.length === 0) {
          callback(new Error('至少输入一个员工'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  requestAnimationFrame(() => { mounted.value = true })
  loadList()
})

async function loadList() {
  loading.value = true
  try {
    const res = await getCompanyList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      nameKeyword: searchName.value || undefined,
      phoneKeyword: searchPhone.value || undefined,
      bossNameKeyword: searchBossName.value || undefined,
      status: searchStatus.value !== '' ? searchStatus.value : undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取公司列表失败:', err)
    ElMessage.error(err?.message || '获取公司列表失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchName.value = ''
  searchPhone.value = ''
  searchBossName.value = ''
  searchStatus.value = ''
  pageNum.value = 1
  loadList()
}

function rowClassName({ row }) {
  return row.status === 0 ? 'row-disabled' : ''
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.contactPerson = ''
  form.contactPhone = ''
  form.address = ''
  form.months = 12
  updateCreateExpireTime()
  dialogVisible.value = true
}

function selectCreateMonths(months) {
  form.months = months
  updateCreateExpireTime()
}

function updateCreateExpireTime() {
  const d = new Date()
  d.setMonth(d.getMonth() + form.months)
  form.expireTime = d.toISOString().slice(0, 19)
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.contactPerson = row.contactPerson
  form.contactPhone = row.contactPhone
  form.address = row.address || ''
  form.expireTime = row.expireTime ? row.expireTime.replace(' ', 'T').substring(0, 19) : ''
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCompany(editId.value, form)
      ElMessage.success('更新成功')
      dialogVisible.value = false
      loadList()
    } else {
      const res = await createCompany(form)
      const tempPassword = res.data
      if (tempPassword) {
        passwordInfo.value = {
          username: form.contactPhone,
          password: tempPassword
        }
        passwordDialogVisible.value = true
      } else {
        ElMessage.success('创建成功，老板账号已自动生成')
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

async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  try {
    await toggleCompanyStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadList()
  } catch (e) {
    // handled
  }
}

async function handleResetPassword(row) {
  try {
    const res = await resetBossPassword(row.id)
    const tempPassword = res.data
    if (tempPassword) {
      passwordInfo.value = {
        username: row.bossUsername || row.contactPhone,
        password: tempPassword
      }
      passwordDialogVisible.value = true
    } else {
      ElMessage.success('密码重置成功')
    }
  } catch (e) {
    // handled
  }
}

function enterCompany(row) {
  userStore.enterCompany(row.id, row.name)
  router.push('/dashboard')
  ElMessage.success(`已进入「${row.name}」，可查看该公司全部数据`)
}

// 续费
function openRenew(row) {
  renewCompanyId.value = row.id
  renewBaseExpire.value = row.expireTime || null
  renewMonths.value = null
  renewDialogVisible.value = true
}

async function handleRenewSubmit() {
  if (!renewMonths.value) {
    ElMessage.warning('请先选择续费时长')
    return
  }
  try {
    await renewCompany(renewCompanyId.value, renewMonths.value)
    ElMessage.success('续费成功')
    renewDialogVisible.value = false
    loadList()
  } catch (e) {
    // handled by interceptor
  }
}

// 工具函数
function formatDate(dt) {
  if (!dt) return '-'
  return dt.replace('T', ' ').substring(0, 10)
}

// 批量添加员工
function openBatchAdd(row) {
  batchCompanyId.value = row.id
  batchCompanyName.value = row.name
  resetBatchForm()
  batchDialogVisible.value = true
}

function resetBatchForm() {
  batchForm.password = ''
  batchForm.role = 'worker'
  batchForm.employeesText = ''
}

function parseEmployeesText(text) {
  const lines = text.trim().split('\n').filter(line => line.trim())
  const employees = []

  for (const line of lines) {
    // 支持空格、逗号、制表符分隔
    const parts = line.trim().split(/[\s,，\t]+/)
    if (parts.length >= 2) {
      employees.push({
        phone: parts[0],
        realName: parts[1]
      })
    } else if (parts.length === 1 && parts[0]) {
      // 只有手机号，姓名留空让后端报错
      employees.push({
        phone: parts[0],
        realName: ''
      })
    }
  }

  return employees
}

async function handleBatchSubmit() {
  const valid = await batchFormRef.value.validate().catch(() => false)
  if (!valid) return

  const employees = parseEmployeesText(batchForm.employeesText)
  if (employees.length === 0) {
    ElMessage.error('请输入至少一个员工')
    return
  }

  batchSubmitting.value = true
  try {
    const res = await batchAddEmployees({
      companyId: batchCompanyId.value,
      password: batchForm.password,
      role: batchForm.role,
      employees: employees
    })

    batchResult.value = res.data
    batchResultVisible.value = true
    batchDialogVisible.value = false
  } catch (e) {
    // handled by interceptor
  } finally {
    batchSubmitting.value = false
  }
}

function formatDateTime(dt) {
  if (!dt) return '-'
  return dt.replace('T', ' ').substring(0, 16)
}

function isExpired(expireTime) {
  if (!expireTime) return false
  return new Date(expireTime) < new Date()
}

function isNearExpire(expireTime) {
  if (!expireTime) return false
  const d = new Date(expireTime)
  const now = new Date()
  const diff = d - now
  return diff > 0 && diff < 30 * 24 * 60 * 60 * 1000 // 30天内
}
</script>

<style scoped>
.company-manage {
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
  width: 160px;
}

.status-select {
  width: 100px;
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

.boss-info {
  display: inline-block;
}

.text-danger {
  color: var(--color-danger);
}
.text-warning {
  color: var(--color-amber);
}

/* ===== 续费对话框 ===== */
.renew-content {
  padding: 8px 4px;
}

.renew-hint {
  font-size: 14px;
  color: var(--color-slate-light);
  margin-bottom: 16px;
  font-weight: 500;
}

.renew-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}

.renew-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 14px 8px;
  border-radius: var(--radius-md);
  border: 2px solid var(--color-cream);
  background: var(--bg-card);
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
  user-select: none;
}

.renew-option:hover {
  border-color: var(--color-amber);
  background: rgba(232, 168, 56, 0.04);
}

.renew-option.active {
  border-color: var(--color-amber);
  background: rgba(232, 168, 56, 0.08);
  box-shadow: 0 0 0 3px rgba(232, 168, 56, 0.12);
}

.renew-option-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-slate);
  line-height: 1.2;
}

.renew-option-tag {
  font-size: 12px;
  color: var(--color-mist);
  margin-top: 2px;
}

.renew-option.active .renew-option-value {
  color: var(--color-amber);
}

.renew-option.active .renew-option-tag {
  color: var(--color-amber);
  opacity: 0.7;
}

.renew-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  background: var(--color-cream);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-slate-light);
}

.renew-preview .el-icon {
  color: var(--color-amber);
  flex-shrink: 0;
}

/* 新增公司租期选择 */
.duration-select {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.duration-item {
  display: flex;
  align-items: baseline;
  gap: 2px;
  padding: 6px 14px;
  border-radius: var(--radius-md);
  border: 1.5px solid var(--color-cream);
  background: var(--bg-card);
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
  user-select: none;
}

.duration-item:hover {
  border-color: var(--color-amber);
}

.duration-item.active {
  border-color: var(--color-amber);
  background: rgba(232, 168, 56, 0.08);
}

.duration-num {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-slate);
}

.duration-unit {
  font-size: 12px;
  color: var(--color-mist);
}

.duration-item.active .duration-num,
.duration-item.active .duration-unit {
  color: var(--color-amber);
}

.expire-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--color-cream);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-slate-light);
}

.expire-preview .el-icon {
  color: var(--color-amber);
  flex-shrink: 0;
}

/* 禁用行样式 */
:deep(.row-disabled) {
  background-color: #f5f5f5 !important;
}
:deep(.row-disabled td) {
  color: #999 !important;
}
:deep(.row-disabled .el-tag--danger) {
  opacity: 0.85;
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

/* ===== 批量添加相关样式 ===== */
.form-tip {
  font-size: 12px;
  color: var(--color-mist);
  margin-top: 4px;
}

.batch-result {
  padding: 8px 0;
}

.fail-list {
  background: #fef9f5;
  border-radius: 8px;
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.fail-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0eeea;
  font-size: 13px;
}

.fail-item:last-child {
  border-bottom: none;
}

.fail-phone {
  font-weight: 600;
  color: var(--color-slate);
  min-width: 100px;
}

.fail-name {
  color: var(--color-slate-light);
  min-width: 60px;
}

.fail-reason {
  color: #ef4444;
  flex: 1;
}
</style>
