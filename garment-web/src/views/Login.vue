<template>
  <div class="login-container">
    <!-- 装饰背景 -->
    <div class="bg-pattern"></div>

    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-content">
        <div class="brand-icon">
          <svg viewBox="0 0 48 48" fill="none">
            <rect width="48" height="48" rx="12" fill="rgba(255,255,255,0.15)"/>
            <path d="M12 18L24 12L36 18V30L24 36L12 30V18Z" stroke="white" stroke-width="2.5" fill="none"/>
            <circle cx="24" cy="24" r="4.5" fill="white" opacity="0.9"/>
            <path d="M24 12V24" stroke="white" stroke-width="1.5" opacity="0.3"/>
            <path d="M12 18L24 24L36 18" stroke="white" stroke-width="1.5" opacity="0.3"/>
          </svg>
        </div>
        <h1 class="brand-name">缝迹</h1>
        <p class="brand-slogan">针线之间，数据可见</p>
        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>订单全流程追踪</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>计件工资自动核算</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>多工厂数据协同</span>
          </div>
        </div>
      </div>
      <div class="brand-footer">
        <span>Garment Management System</span>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-panel">
      <div class="form-wrapper" :class="{ 'form-enter': mounted }">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">请登录您的账号以继续</p>
        </div>

        <el-form
          :model="form"
          :rules="rules"
          ref="formRef"
          @keyup.enter="handleLogin"
          class="login-form"
          size="large"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span>注册账号联系电话：18576526757（程先生）</span>
        </div>
      </div>
    </div>

    <!-- 强制修改密码对话框 -->
    <el-dialog
      v-model="showChangePwd"
      title="修改密码"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      width="400px"
      align-center
    >
      <div style="margin-bottom: 16px; color: #e6a23c; font-size: 14px;">
        ⚠️ 您使用的是初始密码，为了账号安全，请先修改密码后再使用系统。
      </div>
      <el-form
        :model="pwdForm"
        :rules="pwdRules"
        ref="pwdFormRef"
        label-width="80px"
      >
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            placeholder="请输入新密码（至少6位）"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button
          type="primary"
          :loading="pwdLoading"
          @click="submitChangePwd"
          style="width: 100%;"
        >
          确认修改
        </el-button>
      </template>
    </el-dialog>

    <!-- ICP 备案号 -->
    <div class="icp-footer">
      <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">粤ICP备2026085098号</a>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { changePassword } from '../api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const mounted = ref(false)

// 登录成功后要返回的目标路径（如 /mobile/scan?code=xxx），仅接受站内相对路径
const redirectTarget = (() => {
  const raw = route.query.redirect
  if (typeof raw !== 'string' || !raw) return null
  // 防止开放重定向：只接受以 / 开头的站内路径
  return raw.startsWith('/') ? raw : null
})()

onMounted(() => {
  requestAnimationFrame(() => { mounted.value = true })
})

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// ===== 强制改密相关 =====
const showChangePwd = ref(false)
const pwdFormRef = ref()
const pwdLoading = ref(false)
const pwdForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

// 登录成功后的跳转逻辑（延迟执行，可能需要先改密）
let pendingNavigate = null

function getDefaultPath(userType) {
  return userType === 0 ? '/' : '/company-select'
}

function navigateAfterLogin(userType) {
  if (redirectTarget) {
    if (userType === 0) {
      // 超级管理员直接返回原页面
      router.push(redirectTarget)
    } else {
      // 普通用户需要先选择公司，把返回地址带过去
      router.push({ path: '/company-select', query: { redirect: redirectTarget } })
    }
  } else {
    router.push(getDefaultPath(userType))
  }
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')

    // 检查是否需要强制改密
    if (data.needChangePassword) {
      showChangePwd.value = true
      // 保存跳转逻辑，改密成功后执行
      pendingNavigate = () => navigateAfterLogin(data.userType)
    } else {
      // 不需要改密，直接跳转
      navigateAfterLogin(data.userType)
    }
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

async function submitChangePwd() {
  await pwdFormRef.value.validate()
  pwdLoading.value = true
  try {
    await changePassword({
      oldPassword: form.password,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功')
    userStore.clearChangePasswordFlag()
    showChangePwd.value = false
    // 执行延迟的跳转
    if (pendingNavigate) {
      pendingNavigate()
      pendingNavigate = null
    }
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

/* 背景纹理 */
.bg-pattern {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(circle at 20% 80%, rgba(232, 168, 56, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(196, 122, 90, 0.06) 0%, transparent 50%),
    var(--color-cream);
  z-index: 0;
}

/* ===== 左侧品牌面板 ===== */
.brand-panel {
  width: 46%;
  min-width: 380px;
  background: linear-gradient(160deg, #2a3038 0%, var(--color-charcoal) 40%, #1a1e23 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px;
  position: relative;
  z-index: 1;
  overflow: hidden;
}

.brand-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      45deg,
      transparent,
      transparent 40px,
      rgba(255,255,255,0.01) 40px,
      rgba(255,255,255,0.01) 41px
    );
  pointer-events: none;
}

.brand-panel::after {
  content: '';
  position: absolute;
  top: -30%;
  right: -20%;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(232,168,56,0.08) 0%, transparent 70%);
  pointer-events: none;
}

.brand-content {
  text-align: center;
  animation: fadeInUp 0.7s var(--ease-out) 0.2s both;
}

.brand-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 24px;
  animation: float 4s ease-in-out infinite;
}

.brand-icon svg {
  width: 100%;
  height: 100%;
}

.brand-name {
  font-size: 42px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 12px;
  margin-bottom: 12px;
}

.brand-slogan {
  font-size: 14px;
  color: rgba(255,255,255,0.4);
  letter-spacing: 3px;
  margin-bottom: 48px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-start;
  margin: 0 auto;
  width: fit-content;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255,255,255,0.55);
  font-size: 14px;
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-amber);
  flex-shrink: 0;
}

.brand-footer {
  position: absolute;
  bottom: 32px;
  color: rgba(255,255,255,0.2);
  font-size: 12px;
  letter-spacing: 2px;
}

/* ===== 右侧表单面板 ===== */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  z-index: 1;
}

.form-wrapper {
  width: 100%;
  max-width: 380px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s var(--ease-out) 0.4s;
}

.form-wrapper.form-enter {
  opacity: 1;
  transform: translateY(0);
}

.form-header {
  margin-bottom: 36px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-slate);
  margin-bottom: 8px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--color-mist);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 8px 14px;
  height: 48px;
  background: var(--bg-card);
  box-shadow: 0 0 0 1px var(--color-cream-dark) inset !important;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c8c4be inset !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-amber) inset !important;
}

.login-form :deep(.el-input__inner) {
  font-size: 14px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  border: none;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-amber), var(--color-amber-dark));
  color: #fff;
  cursor: pointer;
  transition: all 0.3s var(--ease-out);
  box-shadow: var(--shadow-amber);
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(232, 168, 56, 0.35);
}

.login-btn:active {
  transform: translateY(0);
}

.form-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 12.5px;
  color: var(--color-mist);
}

.icp-footer {
  position: absolute;
  bottom: 14px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 12px;
  z-index: 10;
}

.icp-footer a {
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  transition: color 0.2s;
}

.icp-footer a:hover {
  color: #ffffff;
}

/* ===== 响应式 ===== */
@media (max-width: 860px) {
  .brand-panel {
    display: none;
  }
  .form-panel {
    padding: 24px;
  }
}
</style>
