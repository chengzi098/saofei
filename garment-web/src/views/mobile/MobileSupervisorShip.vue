<template>
  <div class="mobile-ship">
    <!-- 扫码区域 -->
    <div class="scan-viewport" v-if="!bundleInfo">
      <div class="camera-view">
        <video
          v-show="cameraStatus === 'ready'"
          ref="videoRef"
          class="camera-video"
          autoplay
          playsinline
          muted
        ></video>
        <canvas ref="canvasRef" class="hidden-canvas"></canvas>

        <div v-if="cameraStatus === 'loading'" class="camera-placeholder">
          <el-icon class="is-loading" :size="48" color="rgba(255,255,255,0.7)"><Loading /></el-icon>
          <p class="camera-hint">正在打开摄像头...</p>
        </div>

        <div v-if="cameraStatus === 'unsupported' || cameraStatus === 'error'" class="camera-placeholder">
          <div class="photo-scan-icon">
            <el-icon :size="64" color="rgba(255,255,255,0.8)"><Camera /></el-icon>
          </div>
          <p class="camera-hint">点击下方按钮拍照识别二维码</p>
          <button class="photo-scan-btn" @click="triggerFileInput">
            <el-icon size="20"><Camera /></el-icon>
            <span>拍照扫描</span>
          </button>
          <p class="camera-hint-small">对准扎号上的二维码拍照即可</p>
        </div>

        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          capture="environment"
          class="hidden-input"
          @change="handlePhotoScan"
        />

        <div v-if="cameraStatus === 'ready'" class="scan-frame">
          <div class="scan-corner top-left"></div>
          <div class="scan-corner top-right"></div>
          <div class="scan-corner bottom-left"></div>
          <div class="scan-corner bottom-right"></div>
          <div class="scan-line"></div>
        </div>

        <div class="scan-hint-bar" v-if="cameraStatus === 'ready'">
          <span>扫描扎号二维码，确认出货</span>
        </div>
      </div>

      <div class="scan-bottom">
        <p class="scan-tip">扫描扎货二维码，查看进度并出货</p>
      </div>
    </div>

    <!-- 扎货详情面板 -->
    <div class="bundle-panel" v-if="bundleInfo">
      <!-- 扎货信息卡片 -->
      <div class="bundle-card" :class="{ 'shipped': isShipped }">
        <div class="bundle-header">
          <div class="bundle-tag">{{ bundleInfo.styleNo || bundleInfo.styleName || '未知款式' }}</div>
          <el-button link type="info" size="small" @click="resetScan">
            <el-icon><Close /></el-icon> 重新扫码
          </el-button>
        </div>
        <div class="bundle-body">
          <div class="bundle-no">{{ bundleInfo.bundleNo }}</div>
          <div class="bundle-meta">
            <span class="meta-item">
              <el-icon><Box /></el-icon>
              {{ bundleInfo.quantity }}件
            </span>
            <span class="meta-item" v-if="bundleInfo.size">尺码: {{ bundleInfo.size }}</span>
            <span class="meta-item" v-if="bundleInfo.bedNo">床: {{ bundleInfo.bedNo }}</span>
          </div>
        </div>
        <div v-if="isShipped" class="shipped-banner">
          <el-icon><CircleCheckFilled /></el-icon>
          <span>已出货</span>
          <span class="ship-time" v-if="bundleInfo.shippedTime">{{ formatTime(bundleInfo.shippedTime) }}</span>
          <span class="ship-by" v-if="bundleInfo.shipperName">· {{ bundleInfo.shipperName }}</span>
        </div>
      </div>

      <!-- 工序进度 -->
      <div class="process-section">
        <div class="section-title">
          工序进度
          <span class="progress-count">{{ completedCount }}/{{ totalCount }}</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>

        <div class="process-list">
          <div v-for="p in bundleInfo.processes" :key="p.id" class="process-item" :class="{ done: p.status === 2 }">
            <div class="process-check">
              <el-icon v-if="p.status === 2" color="#10b981" size="22"><CircleCheckFilled /></el-icon>
              <div v-else class="check-empty"></div>
            </div>
            <div class="process-info">
              <div class="process-name">{{ p.processName }}</div>
              <div class="process-worker" v-if="p.status === 2 && p.workerName">
                已完成 · {{ p.workerName }}
              </div>
              <div class="process-worker pending" v-else>
                未完成
              </div>
            </div>
            <button
              v-if="p.status !== 2"
              class="assign-btn"
              :disabled="isShipped || assigning"
              @click="openAssignDialog(p)"
            >
              <el-icon size="14"><User /></el-icon>
              指派完成
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="submit-bar" v-if="bundleInfo">
      <!-- 已出货：撤销 -->
      <template v-if="isShipped">
        <button class="submit-btn unship-btn" :disabled="submitting" @click="handleUnship">
          <span v-if="!submitting">撤销出货</span>
          <span v-else>处理中...</span>
        </button>
      </template>
      <!-- 未出货 -->
      <template v-else>
        <button class="submit-btn" :disabled="!allDone || submitting" @click="handleShip">
          <span v-if="submitting">处理中...</span>
          <span v-else-if="allDone">确认出货</span>
          <span v-else>还有 {{ totalCount - completedCount }} 道工序未完成</span>
        </button>
      </template>
    </div>

    <!-- 指派员工弹窗 -->
    <el-dialog v-model="assignDialogVisible" title="指派员工完成工序" width="90%" class="assign-dialog">
      <div class="assign-process-name" v-if="assignTarget">
        工序：{{ assignTarget.processName }}
      </div>
      <div v-loading="employeeLoading" class="employee-list">
        <div
          v-for="emp in employeeList"
          :key="emp.userId"
          class="employee-item"
          @click="handleAssign(emp)"
        >
          <div class="emp-avatar">{{ (emp.realName || '?').charAt(0) }}</div>
          <div class="emp-info">
            <div class="emp-name">{{ emp.realName }}</div>
            <div class="emp-phone" v-if="emp.phone">{{ emp.phone }}</div>
          </div>
          <el-icon color="#10b981"><ArrowRight /></el-icon>
        </div>
        <div v-if="!employeeLoading && employeeList.length === 0" class="employee-empty">暂无员工</div>
      </div>
    </el-dialog>

    <!-- 加载状态 -->
    <div v-if="loading && !bundleInfo" class="loading-overlay">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>查询中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Camera, Close, Loading, Box, User, ArrowRight, CircleCheckFilled
} from '@element-plus/icons-vue'
import jsQR from 'jsqr'
import { scanBundle } from '../../api/bundle'
import { submitWorkRecord } from '../../api/workRecord'
import { getEmployeeList } from '../../api/employee'
import { shipBundle, unshipBundle } from '../../api/shipment'

const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const assigning = ref(false)
const bundleInfo = ref(null)

// 摄像头相关
const videoRef = ref(null)
const canvasRef = ref(null)
const fileInputRef = ref(null)
const cameraStatus = ref('loading')
let stream = null
let scanTimer = null
let lastScannedCode = ''
let lastScanTime = 0

// 指派弹窗
const assignDialogVisible = ref(false)
const assignTarget = ref(null)
const employeeLoading = ref(false)
const employeeList = ref([])

const totalCount = computed(() => bundleInfo.value?.processes?.length || 0)
const completedCount = computed(() =>
  (bundleInfo.value?.processes || []).filter(p => p.status === 2).length
)
const allDone = computed(() => totalCount.value > 0 && completedCount.value === totalCount.value)
const isShipped = computed(() => bundleInfo.value?.shipped === 1)
const progressPercent = computed(() =>
  totalCount.value === 0 ? 0 : Math.round((completedCount.value / totalCount.value) * 100)
)

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

// ===== 摄像头 =====
async function openCamera() {
  cameraStatus.value = 'loading'
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    cameraStatus.value = 'unsupported'
    return
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
      audio: false
    })
    await nextTick()
    const video = videoRef.value
    if (!video) throw new Error('video not mounted')
    video.srcObject = stream
    await video.play()
    cameraStatus.value = 'ready'
    startScanning()
  } catch (err) {
    stopCamera()
    cameraStatus.value = 'error'
  }
}

function triggerFileInput() {
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
    fileInputRef.value.click()
  }
}

async function handlePhotoScan(e) {
  const file = e.target.files[0]
  if (!file) return
  loading.value = true
  const img = new Image()
  img.onload = () => {
    const canvas = document.createElement('canvas')
    const maxSize = 1024
    let width = img.width
    let height = img.height
    if (width > maxSize || height > maxSize) {
      const ratio = Math.min(maxSize / width, maxSize / height)
      width = Math.floor(width * ratio)
      height = Math.floor(height * ratio)
    }
    canvas.width = width
    canvas.height = height
    canvas.getContext('2d').drawImage(img, 0, 0, width, height)
    const imageData = canvas.getContext('2d').getImageData(0, 0, width, height)
    try {
      const code = jsQR(imageData.data, imageData.width, imageData.height, { inversionAttempts: 'attemptBoth' })
      if (code && code.data) {
        e.target.value = ''
        loadBundle(code.data)
      } else {
        ElMessage.warning('未识别到二维码，请确保二维码清晰完整')
        loading.value = false
        e.target.value = ''
      }
    } catch (err) {
      ElMessage.warning('识别失败，请重新拍照')
      loading.value = false
      e.target.value = ''
    }
    URL.revokeObjectURL(img.src)
  }
  img.onerror = () => {
    ElMessage.error('图片加载失败')
    e.target.value = ''
    loading.value = false
    URL.revokeObjectURL(img.src)
  }
  img.src = URL.createObjectURL(file)
}

function stopCamera() {
  if (scanTimer) { cancelAnimationFrame(scanTimer); scanTimer = null }
  if (stream) { stream.getTracks().forEach(t => t.stop()); stream = null }
  if (videoRef.value) videoRef.value.srcObject = null
}

function startScanning() {
  function scanFrame() {
    if (!videoRef.value || !canvasRef.value || videoRef.value.readyState !== videoRef.value.HAVE_ENOUGH_DATA) {
      scanTimer = requestAnimationFrame(scanFrame)
      return
    }
    const video = videoRef.value
    const canvas = canvasRef.value
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
    try {
      const code = jsQR(imageData.data, imageData.width, imageData.height, { inversionAttempts: 'dontInvert' })
      if (code && code.data) {
        const now = Date.now()
        if (code.data !== lastScannedCode || now - lastScanTime > 2000) {
          lastScannedCode = code.data
          lastScanTime = now
          handleScanCode(code.data)
        }
      }
    } catch (e) {}
    scanTimer = requestAnimationFrame(scanFrame)
  }
  scanTimer = requestAnimationFrame(scanFrame)
}

async function handleScanCode(code) {
  if (loading.value) return
  await loadBundle(code)
}

function extractQrCode(raw) {
  try {
    const url = new URL(raw)
    const code = url.searchParams.get('code')
    if (code) return code
  } catch {}
  return raw
}

async function loadBundle(code) {
  loading.value = true
  const qrCode = extractQrCode(code)
  try {
    const res = await scanBundle(qrCode)
    bundleInfo.value = res.data
    stopCamera()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '二维码无效，请重试')
  } finally {
    loading.value = false
  }
}

function resetScan() {
  bundleInfo.value = null
  nextTick(() => { openCamera() })
}

// ===== 指派员工 =====
async function openAssignDialog(process) {
  assignTarget.value = process
  assignDialogVisible.value = true
  if (employeeList.value.length === 0) {
    await loadEmployees()
  }
}

async function loadEmployees() {
  employeeLoading.value = true
  try {
    const res = await getEmployeeList({ pageNum: 1, pageSize: 200, status: 1 })
    employeeList.value = res.data?.records || []
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '加载员工失败')
  } finally {
    employeeLoading.value = false
  }
}

async function handleAssign(emp) {
  if (!assignTarget.value) return
  assigning.value = true
  try {
    await submitWorkRecord({
      bundleId: bundleInfo.value.id,
      bundleProcessId: assignTarget.value.id,
      workerId: emp.userId
    })
    ElMessage.success(`已指派 ${emp.realName} 完成该工序`)
    assignDialogVisible.value = false
    await reloadBundle()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '指派失败')
  } finally {
    assigning.value = false
  }
}

async function reloadBundle() {
  if (!bundleInfo.value?.qrCode) return
  try {
    const res = await scanBundle(bundleInfo.value.qrCode)
    bundleInfo.value = res.data
  } catch (e) {}
}

// ===== 出货 / 撤销 =====
async function handleShip() {
  submitting.value = true
  try {
    await shipBundle({ bundleId: bundleInfo.value.id })
    ElMessage.success('出货成功')
    await reloadBundle()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '出货失败')
  } finally {
    submitting.value = false
  }
}

async function handleUnship() {
  try {
    await ElMessageBox.confirm('确定要撤销这扎货的出货状态吗？', '撤销出货', {
      confirmButtonText: '确定撤销',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  submitting.value = true
  try {
    await unshipBundle({ bundleId: bundleInfo.value.id })
    ElMessage.success('已撤销出货')
    await reloadBundle()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '撤销失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  const codeFromUrl = route.query.code
  if (codeFromUrl) {
    loadBundle(codeFromUrl)
  } else {
    openCamera()
  }
})

onBeforeUnmount(() => { stopCamera() })
</script>

<style scoped>
.mobile-ship {
  min-height: 100vh;
  background: var(--color-cream, #faf8f5);
  padding-bottom: calc(80px + env(safe-area-inset-bottom));
}

.scan-viewport { display: flex; flex-direction: column; min-height: 100vh; }

.camera-view {
  flex: 1; position: relative; background: #1a1a2e;
  display: flex; align-items: center; justify-content: center;
  min-height: 50vh; overflow: hidden;
}
.camera-video { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.hidden-canvas { display: none; }

.camera-placeholder {
  position: relative; z-index: 2; display: flex; flex-direction: column;
  align-items: center; gap: 16px; padding: 20px;
}
.photo-scan-icon {
  width: 100px; height: 100px; border-radius: 50%; background: rgba(255,255,255,0.1);
  display: flex; align-items: center; justify-content: center; margin-bottom: 8px;
}
.camera-hint { font-size: 16px; color: rgba(255,255,255,0.8); margin: 0; text-align: center; }
.camera-hint-small { font-size: 13px; color: rgba(255,255,255,0.5); margin: 0; text-align: center; }

.photo-scan-btn {
  display: inline-flex; align-items: center; gap: 10px; margin-top: 12px; padding: 14px 36px;
  background: linear-gradient(135deg, #10b981, #059669); color: #fff; font-size: 17px; font-weight: 600;
  border-radius: 14px; cursor: pointer; border: none; box-shadow: 0 4px 15px rgba(16,185,129,0.4);
}
.photo-scan-btn:active { transform: scale(0.96); }
.hidden-input { display: none; }

.scan-bottom { background: var(--bg-card); padding: 20px 16px; padding-bottom: calc(20px + env(safe-area-inset-bottom)); }
.scan-tip { font-size: 13px; color: #9ca3af; text-align: center; margin: 0; }

.scan-frame {
  position: absolute; width: 240px; height: 240px; top: 50%; left: 50%;
  transform: translate(-50%, -50%); z-index: 3;
}
.scan-corner { position: absolute; width: 28px; height: 28px; border-color: #10b981; border-style: solid; }
.scan-corner.top-left { top: 0; left: 0; border-width: 4px 0 0 4px; }
.scan-corner.top-right { top: 0; right: 0; border-width: 4px 4px 0 0; }
.scan-corner.bottom-left { bottom: 0; left: 0; border-width: 0 0 4px 4px; }
.scan-corner.bottom-right { bottom: 0; right: 0; border-width: 0 4px 4px 0; }
.scan-line {
  position: absolute; width: calc(100% - 8px); height: 2px;
  background: linear-gradient(90deg, transparent, #10b981, transparent);
  top: 0; left: 4px; animation: scanMove 2s linear infinite;
}
@keyframes scanMove {
  0% { top: 0; opacity: 1; } 50% { opacity: 1; } 100% { top: calc(100% - 2px); opacity: 0.3; }
}
.scan-hint-bar {
  position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%); z-index: 3;
  background: rgba(0,0,0,0.5); backdrop-filter: blur(8px); padding: 8px 20px; border-radius: 20px;
  color: rgba(255,255,255,0.9); font-size: 13px; white-space: nowrap;
}

/* ===== 扎货面板 ===== */
.bundle-panel { padding: 12px 16px; }

.bundle-card {
  background: linear-gradient(135deg, #10b981, #059669);
  border-radius: 16px; padding: 16px 20px; color: #fff; margin-bottom: 12px;
}
.bundle-card.shipped { background: linear-gradient(135deg, #6b7280, #4b5563); }
.bundle-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.bundle-tag { font-size: 12px; background: rgba(255,255,255,0.2); padding: 2px 10px; border-radius: 10px; }
.bundle-body { display: flex; justify-content: space-between; align-items: flex-end; }
.bundle-no { font-size: 24px; font-weight: 700; }
.bundle-meta { display: flex; gap: 12px; flex-wrap: wrap; justify-content: flex-end; }
.meta-item { font-size: 13px; opacity: 0.85; display: flex; align-items: center; gap: 4px; }

.shipped-banner {
  margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(255,255,255,0.2);
  display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600;
}
.ship-time { font-weight: 400; font-size: 12px; opacity: 0.85; margin-left: 4px; }
.ship-by { font-weight: 400; font-size: 12px; opacity: 0.85; }

.process-section { background: var(--bg-card); border-radius: 16px; padding: 16px; margin-bottom: 12px; }
.section-title {
  font-size: 15px; font-weight: 700; color: var(--color-slate, #2d2a26); margin-bottom: 12px;
  display: flex; justify-content: space-between; align-items: center;
}
.progress-count { font-size: 13px; color: #10b981; font-weight: 600; }

.progress-bar { height: 6px; background: #e5e7eb; border-radius: 3px; overflow: hidden; margin-bottom: 14px; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #10b981, #059669); border-radius: 3px; transition: width 0.5s ease; }

.process-list { display: flex; flex-direction: column; gap: 8px; }
.process-item {
  display: flex; align-items: center; gap: 12px; padding: 14px;
  border-radius: 12px; border: 2px solid #f3f4f6;
}
.process-item.done { border-color: #d1fae5; background: #f0fdf4; }
.process-check { flex-shrink: 0; }
.check-empty { width: 22px; height: 22px; border: 2px solid #d1d5db; border-radius: 50%; }
.process-info { flex: 1; min-width: 0; }
.process-name { font-size: 15px; font-weight: 600; color: var(--color-slate, #2d2a26); }
.process-worker { font-size: 12px; color: #059669; margin-top: 4px; }
.process-worker.pending { color: #9ca3af; }

.assign-btn {
  flex-shrink: 0; display: inline-flex; align-items: center; gap: 4px;
  padding: 7px 12px; font-size: 13px; font-weight: 600; color: #10b981;
  background: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 10px; cursor: pointer;
}
.assign-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.assign-btn:active:not(:disabled) { transform: scale(0.96); }

/* ===== 底部操作栏 ===== */
.submit-bar {
  position: fixed; bottom: calc(56px + env(safe-area-inset-bottom)); left: 50%; transform: translateX(-50%);
  width: 100%; max-width: 430px; padding: 12px 16px;
  background: rgba(255,255,255,0.95); backdrop-filter: blur(10px); z-index: 100;
}
.submit-btn {
  width: 100%; height: 48px; border: none; border-radius: 12px;
  background: linear-gradient(135deg, #10b981, #059669); color: #fff;
  font-size: 17px; font-weight: 700; cursor: pointer; transition: all 0.2s;
}
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.submit-btn:active:not(:disabled) { transform: scale(0.98); }
.unship-btn { background: linear-gradient(135deg, #ef4444, #dc2626); }

.loading-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 12px; color: #fff; font-size: 15px; z-index: 200;
}

/* ===== 指派弹窗 ===== */
.assign-process-name {
  font-size: 14px; color: #6b7280; margin-bottom: 12px;
  padding: 8px 12px; background: #f9fafb; border-radius: 8px;
}
.employee-list { max-height: 50vh; overflow-y: auto; }
.employee-item {
  display: flex; align-items: center; gap: 12px; padding: 12px;
  border-radius: 10px; cursor: pointer;
}
.employee-item:active { background: #f3f4f6; }
.emp-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  background: linear-gradient(135deg, #10b981, #059669); color: #fff;
  display: flex; align-items: center; justify-content: center; font-weight: 600; flex-shrink: 0;
}
.emp-info { flex: 1; min-width: 0; }
.emp-name { font-size: 15px; font-weight: 600; color: var(--color-slate, #2d2a26); }
.emp-phone { font-size: 12px; color: #9ca3af; }
.employee-empty { text-align: center; color: #9ca3af; padding: 30px 0; font-size: 14px; }
</style>
