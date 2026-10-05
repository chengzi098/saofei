<template>
  <div class="mobile-profile">
    <!-- 用户信息卡片 -->
    <div class="profile-card" :class="{ 'entered': mounted }">
      <div class="profile-header">
        <div class="avatar">
          {{ userName.charAt(userName.length - 1) }}
        </div>
        <div class="profile-info">
          <div class="profile-name">{{ userName }}</div>
          <div class="profile-meta">
            <span class="role-tag" :class="userStore.currentRole">
              {{ roleText }}
            </span>
            <span class="company-tag" v-if="userStore.currentCompanyName">
              {{ userStore.currentCompanyName }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 本月数据 -->
    <div class="stats-card" :class="{ 'entered': mounted }">
      <div class="stats-grid">
        <div class="stat-box">
          <div class="stat-icon" style="background: rgba(232, 168, 56, 0.1)">
            <el-icon size="20" color="var(--color-amber)"><Wallet /></el-icon>
          </div>
          <div class="stat-data">
            <div class="stat-value">{{ monthEarning }}元</div>
            <div class="stat-label">本月收入</div>
          </div>
        </div>
        <div class="stat-box">
          <div class="stat-icon" style="background: rgba(74, 222, 128, 0.1)">
            <el-icon size="20" color="#16a34a"><Box /></el-icon>
          </div>
          <div class="stat-data">
            <div class="stat-value">{{ monthPieces }}</div>
            <div class="stat-label">完成件数</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 功能菜单 -->
    <div class="menu-card" :class="{ 'entered': mounted }">
      <div class="menu-group">
        <div class="menu-item" @click="goRecords">
          <div class="menu-icon" style="background: rgba(232, 168, 56, 0.1)">
            <el-icon size="20" color="var(--color-amber)"><Document /></el-icon>
          </div>
          <span class="menu-text">报工记录</span>
          <el-icon class="menu-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="menu-item" @click="goScan">
          <div class="menu-icon" style="background: rgba(59, 130, 246, 0.1)">
            <el-icon size="20" color="#3b82f6"><Camera /></el-icon>
          </div>
          <span class="menu-text">扫码报工</span>
          <el-icon class="menu-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="menu-item" @click="goWageRecords">
          <div class="menu-icon" style="background: rgba(245, 158, 11, 0.1)">
            <el-icon size="20" color="#f59e0b"><Wallet /></el-icon>
          </div>
          <span class="menu-text">工资调整</span>
          <el-icon class="menu-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="menu-item" @click="goWageSign">
          <div class="menu-icon" style="background: rgba(16, 185, 129, 0.1)">
            <el-icon size="20" color="#10b981"><Money /></el-icon>
          </div>
          <span class="menu-text">工资签名</span>
          <el-icon class="menu-arrow"><ArrowRight /></el-icon>
        </div>
      </div>

      <div class="menu-divider"></div>

      <div class="menu-group">
        <div class="menu-item" @click="showPasswordDialog = true">
          <div class="menu-icon" style="background: rgba(139, 92, 246, 0.1)">
            <el-icon size="20" color="#8b5cf6"><Lock /></el-icon>
          </div>
          <span class="menu-text">修改密码</span>
          <el-icon class="menu-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="menu-item" @click="showAbout = true">
          <div class="menu-icon" style="background: rgba(107, 114, 128, 0.1)">
            <el-icon size="20" color="#6b7280"><InfoFilled /></el-icon>
          </div>
          <span class="menu-text">关于系统</span>
          <el-icon class="menu-arrow"><ArrowRight /></el-icon>
        </div>
      </div>

      <div class="menu-divider"></div>

      <div class="menu-group">
        <div class="menu-item danger" @click="handleLogout">
          <div class="menu-icon" style="background: rgba(239, 68, 68, 0.1)">
            <el-icon size="20" color="#ef4444"><SwitchButton /></el-icon>
          </div>
          <span class="menu-text">退出登录</span>
          <el-icon class="menu-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <!-- 版本号 -->
    <div class="version-text">缝迹 v1.0</div>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="90%"
      class="mobile-dialog"
      destroy-on-close
    >
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordRef" label-width="0">
        <el-form-item prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="当前密码"
            show-password
            size="large"
          />
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="新密码"
            show-password
            size="large"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="确认新密码"
            show-password
            size="large"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false" size="large" style="flex: 1">取消</el-button>
        <el-button type="primary" @click="handleChangePassword" size="large" style="flex: 1">确认</el-button>
      </template>
    </el-dialog>

    <!-- 关于弹窗 -->
    <el-dialog
      v-model="showAbout"
      title="关于缝迹"
      width="90%"
      class="mobile-dialog"
      align-center
    >
      <div class="about-content">
        <div class="about-logo">
          <svg viewBox="0 0 32 32" width="56" height="56">
            <rect width="32" height="32" rx="8" fill="var(--color-amber)"/>
            <path d="M8 12L16 8L24 12V20L16 24L8 20V12Z" stroke="white" stroke-width="2" fill="none"/>
            <circle cx="16" cy="16" r="3" fill="white"/>
          </svg>
        </div>
        <div class="about-name">缝迹</div>
        <div class="about-desc">制衣厂综合管理系统</div>
        <div class="about-version">版本 1.0.0</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Wallet, Box, Document, Camera,
  Lock, InfoFilled, SwitchButton, ArrowRight, Money
} from '@element-plus/icons-vue'
import { getWorkRecordList } from '../../api/workRecord'
import { getWorkerMonthlyIncome } from '../../api/dashboard'
import { changePassword } from '../../api/auth'

const router = useRouter()
const userStore = useUserStore()
const mounted = ref(false)

const monthEarning = ref('0.00')
const monthPieces = ref(0)
const showPasswordDialog = ref(false)
const showAbout = ref(false)
const passwordRef = ref(null)

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const userName = computed(() => userStore.realName || userStore.username || '工友')

const roleText = computed(() => {
  const map = { boss: '老板', supervisor: '主管', worker: '工人' }
  return map[userStore.currentRole] || '员工'
})

onMounted(() => {
  requestAnimationFrame(() => { mounted.value = true })
  loadMonthStats()
})

async function loadMonthStats() {
  try {
    // 本月收入（报工工资 + 工资调整）
    const incomeRes = await getWorkerMonthlyIncome()
    const incomeData = incomeRes.data || {}
    monthEarning.value = (incomeData.totalAmount || 0).toFixed(2)

    // 本月完成件数（仍通过报工记录统计）
    const now = new Date()
    const startOfMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
    const res = await getWorkRecordList({
      pageNum: 1,
      pageSize: 100,
      workerId: userStore.userId,
      startDate: startOfMonth,
      status: 1
    })
    const records = res.data?.records || []
    monthPieces.value = records.reduce((sum, r) => sum + (r.quantity || 0), 0)
  } catch (err) {
    console.error('加载统计失败:', err)
  }
}

function goRecords() {
  router.push('/mobile/records')
}

function goScan() {
  router.push('/mobile/scan')
}

function goWageSign() {
  router.push('/mobile/wage-sign')
}

function goWageRecords() {
  router.push('/mobile/wage-records')
}

async function handleChangePassword() {
  const valid = await passwordRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    showPasswordDialog.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e) {
    // 错误已由拦截器处理
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.logout()
    router.push('/login')
  } catch (e) {
    // cancel
  }
}
</script>

<style scoped>
.mobile-profile {
  padding: 12px 16px;
  padding-bottom: 100px;
}

/* ===== 用户卡片 ===== */
.profile-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 12px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out;
}

.profile-card.entered {
  opacity: 1;
  transform: translateY(0);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-amber, #e8a838), var(--color-terracotta, #c75b39));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
  margin-bottom: 8px;
}

.profile-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.role-tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.role-tag.boss {
  background: rgba(232, 168, 56, 0.12);
  color: var(--color-amber, #d97706);
}
.role-tag.supervisor {
  background: rgba(251, 191, 36, 0.12);
  color: #d97706;
}
.role-tag.worker {
  background: rgba(74, 222, 128, 0.12);
  color: #16a34a;
}

.company-tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  background: #f3f4f6;
  color: #6b7280;
}

/* ===== 统计卡片 ===== */
.stats-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out 0.1s;
}

.stats-card.entered {
  opacity: 1;
  transform: translateY(0);
}

.stats-grid {
  display: flex;
  gap: 12px;
}

.stat-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #fafafa;
  border-radius: 12px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-data {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 12px;
  color: #9ca3af;
}

/* ===== 菜单卡片 ===== */
.menu-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 8px 0;
  margin-bottom: 16px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s ease-out 0.2s;
}

.menu-card.entered {
  opacity: 1;
  transform: translateY(0);
}

.menu-group {
  padding: 4px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.menu-item:active {
  background: #f9fafb;
}

.menu-item.danger .menu-text {
  color: #ef4444;
}

.menu-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-text {
  flex: 1;
  font-size: 15px;
  color: var(--color-slate, #2d2a26);
  font-weight: 500;
}

.menu-arrow {
  font-size: 16px;
  color: #d1d5db;
}

.menu-divider {
  height: 1px;
  background: #f3f4f6;
  margin: 4px 16px;
}

/* ===== 版本号 ===== */
.version-text {
  text-align: center;
  font-size: 12px;
  color: #d1d5db;
  margin-top: 8px;
}

/* ===== 弹窗 ===== */
:deep(.mobile-dialog) {
  max-width: 360px;
}

:deep(.mobile-dialog .el-dialog__header) {
  padding: 16px;
  margin: 0;
  border-bottom: 1px solid #f3f4f6;
}

:deep(.mobile-dialog .el-dialog__body) {
  padding: 16px;
}

:deep(.mobile-dialog .el-dialog__footer) {
  padding: 12px 16px;
  display: flex;
  gap: 8px;
}

.about-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
  gap: 8px;
}

.about-logo {
  margin-bottom: 4px;
}

.about-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
}

.about-desc {
  font-size: 14px;
  color: #9ca3af;
}

.about-version {
  font-size: 12px;
  color: #d1d5db;
  margin-top: 8px;
}
</style>
