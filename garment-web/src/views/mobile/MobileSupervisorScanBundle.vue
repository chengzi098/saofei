<template>
  <div class="mobile-scan-bundle">
    <!-- 扫码区域 -->
    <div class="scan-viewport" v-if="!bundleInfo">
      <!-- 相机视图 -->
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

        <!-- 相机未就绪时的占位 -->
        <div v-if="cameraStatus === 'loading'" class="camera-placeholder">
          <el-icon class="is-loading" :size="48" color="rgba(255,255,255,0.7)"><Loading /></el-icon>
          <p class="camera-hint">正在打开摄像头...</p>
        </div>

        <!-- 不支持实时扫码 或 相机错误：显示拍照扫描 -->
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

        <!-- 隐藏的文件选择 -->
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          capture="environment"
          class="hidden-input"
          @change="handlePhotoScan"
        />

        <!-- 扫描框 -->
        <div v-if="cameraStatus === 'ready'" class="scan-frame">
          <div class="scan-corner top-left"></div>
          <div class="scan-corner top-right"></div>
          <div class="scan-corner bottom-left"></div>
          <div class="scan-corner bottom-right"></div>
          <div class="scan-line"></div>
        </div>

        <!-- 扫描提示 -->
        <div class="scan-hint-bar" v-if="cameraStatus === 'ready'">
          <span>将二维码放入框内，自动扫描查扎</span>
        </div>
      </div>

      <!-- 底部操作区 -->
      <div class="scan-bottom">
        <div class="photo-scan-entry" v-if="cameraStatus === 'unsupported' || cameraStatus === 'error'">
          <button class="photo-scan-btn-bottom" @click="triggerFileInput">
            <el-icon size="22"><Camera /></el-icon>
            <span>拍照扫描二维码</span>
          </button>
        </div>
        <p class="scan-tip">将二维码放入框内自动扫描查扎</p>
      </div>
    </div>

    <!-- 扎货详情 -->
    <div class="bundle-panel" v-if="bundleInfo">
      <!-- 扎货信息卡片 -->
      <div class="bundle-card">
        <div class="bundle-header">
          <div class="bundle-tag">{{ bundleInfo.styleNo || bundleInfo.styleName || '未知款式' }}</div>
          <el-button link type="info" size="small" @click="resetScan" style="color: rgba(255,255,255,0.85);">
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
            <span class="meta-item" v-if="bundleInfo.size">
              尺码: {{ bundleInfo.size }}
            </span>
          </div>
        </div>
      </div>

      <!-- 工序完成情况 -->
      <div class="process-section">
        <div class="section-title">工序完成情况</div>
        <div class="process-list">
          <div
            v-for="p in bundleInfo.processes"
            :key="p.id"
            class="process-item"
            :class="{ completed: p.status === 2, returned: p.status === 3 }"
          >
            <div class="process-left">
              <div class="process-index">{{ p.sortNo || '' }}</div>
              <div class="process-info">
                <div class="process-name">{{ p.processName }}</div>
                <div class="process-price">
                  <template v-if="p.price > 0">{{ p.price }}元/件 × {{ bundleInfo.quantity }}件 = {{ (p.price * bundleInfo.quantity).toFixed(2) }}元</template>
                  <template v-else><span class="pricing-pending">定价中</span></template>
                </div>
                <div v-if="p.status === 2 && p.workerName" class="process-worker">
                  <span>{{ p.workerName }} · {{ formatTime(p.workTime) }}</span>
                </div>
                <div v-if="p.workerRemark" class="process-remark">备注: {{ p.workerRemark }}</div>
              </div>
            </div>
            <div class="process-right">
              <span class="status-tag" :class="getProcessStatusClass(p.status)">
                {{ getProcessStatusText(p.status) }}
              </span>
              <el-button
                v-if="p.status === 2 && p.workRecordId"
                type="danger"
                size="small"
                link
                @click="openReturnDialog(p)"
              >
                退工
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 进度汇总 -->
      <div class="summary-section">
        <div class="summary-row">
          <span>总工序</span>
          <span class="summary-val">{{ bundleInfo.processes?.length || 0 }}道</span>
        </div>
        <div class="summary-row">
          <span>已完成</span>
          <span class="summary-val done">{{ completedCount }}道</span>
        </div>
        <div class="summary-row">
          <span>未开始</span>
          <span class="summary-val">{{ pendingCount }}道</span>
        </div>
        <div class="summary-row total">
          <span>完成进度</span>
          <span class="summary-amount">{{ progressPercent }}%</span>
        </div>
      </div>
    </div>

    <!-- 退工弹窗 -->
    <el-dialog
      v-model="showReturnDialog"
      title="退工确认"
      width="90%"
      class="return-dialog"
      destroy-on-close
    >
      <div v-if="returnTarget" class="return-content">
        <div class="return-info">
          <div class="return-label">工序</div>
          <div class="return-value">{{ returnTarget.processName }}</div>
        </div>
        <div class="return-info">
          <div class="return-label">工人</div>
          <div class="return-value">{{ returnTarget.workerName }}</div>
        </div>
        <div class="return-reason">
          <div class="return-label">退回原因</div>
          <div class="return-default-reasons">
            <el-tag
              v-for="r in defaultReturnReasons"
              :key="r"
              :type="returnReason === r ? '' : 'info'"
              @click="returnReason = r"
              style="cursor: pointer"
            >{{ r }}</el-tag>
          </div>
          <el-input
            v-model="returnReason"
            type="textarea"
            :rows="3"
            placeholder="请输入退回原因（必填）"
            maxlength="200"
            show-word-limit
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="showReturnDialog = false" size="large" style="flex: 1">取消</el-button>
        <el-button type="danger" @click="confirmReturn" size="large" style="flex: 1" :loading="returnLoading">
          确认退回
        </el-button>
      </template>
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
  Camera, Close, Loading, Box
} from '@element-plus/icons-vue'
import jsQR from 'jsqr'
import { scanBundle } from '../../api/bundle'
import { returnWorkRecord } from '../../api/workRecord'

const route = useRoute()

const loading = ref(false)
const manualCode = ref('')
const bundleInfo = ref(null)

// 摄像头相关
const videoRef = ref(null)
const canvasRef = ref(null)
const fileInputRef = ref(null)
const cameraStatus = ref('loading') // loading | ready | error | unsupported
const cameraError = ref('')
let stream = null
let scanTimer = null
let lastScannedCode = ''
let lastScanTime = 0

// 退工相关
const showReturnDialog = ref(false)
const returnTarget = ref(null)
const returnReason = ref('')
const defaultReturnReasons = ['扫错码', '质量不合格', '尺寸不对', '做错了需要返工', '线头太多', '工序漏做']
const returnLoading = ref(false)

// 工序统计
const completedCount = computed(() =>
  bundleInfo.value?.processes?.filter(p => p.status === 2).length || 0
)
const pendingCount = computed(() =>
  bundleInfo.value?.processes?.filter(p => p.status === 0).length || 0
)
const progressPercent = computed(() => {
  const total = bundleInfo.value?.processes?.length || 0
  if (total === 0) return 0
  return Math.round((completedCount.value / total) * 100)
})

function getProcessStatusText(status) {
  const map = { 0: '未开始', 2: '已完成', 3: '已退回' }
  return map[status] || '未知'
}

function getProcessStatusClass(status) {
  const map = { 0: 'waiting', 2: 'done', 3: 'returned' }
  return map[status] || ''
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hour = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${month}月${day}日 ${hour}:${min}`
}

// ===== 摄像头相关 =====

async function openCamera() {
  cameraStatus.value = 'loading'
  cameraError.value = ''

  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    console.log('[Camera] 不支持实时摄像头，使用拍照模式')
    cameraStatus.value = 'unsupported'
    return
  }

  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: {
        facingMode: { ideal: 'environment' },
        width: { ideal: 1280 },
        height: { ideal: 720 }
      },
      audio: false
    })

    await nextTick()

    const video = videoRef.value
    if (!video) {
      throw new Error('video element is not mounted')
    }

    video.srcObject = stream
    await video.play()
    cameraStatus.value = 'ready'
    startScanning()
  } catch (err) {
    console.error('打开摄像头失败:', err)
    stopCamera()
    cameraStatus.value = 'error'
    if (err.name === 'NotAllowedError') {
      cameraError.value = '摄像头权限被拒绝'
    } else if (err.name === 'NotFoundError') {
      cameraError.value = '未找到摄像头设备'
    } else {
      cameraError.value = '无法打开摄像头'
    }
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
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0, width, height)
    const imageData = ctx.getImageData(0, 0, width, height)

    try {
      const code = jsQR(imageData.data, imageData.width, imageData.height, {
        inversionAttempts: 'attemptBoth'
      })

      if (code && code.data) {
        manualCode.value = code.data
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
  if (scanTimer) {
    cancelAnimationFrame(scanTimer)
    scanTimer = null
  }
  if (stream) {
    stream.getTracks().forEach(track => track.stop())
    stream = null
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
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
      const code = jsQR(imageData.data, imageData.width, imageData.height, {
        inversionAttempts: 'dontInvert'
      })

      if (code && code.data) {
        const now = Date.now()
        if (code.data !== lastScannedCode || now - lastScanTime > 2000) {
          lastScannedCode = code.data
          lastScanTime = now
          manualCode.value = code.data
          handleScanCode(code.data)
        }
      }
    } catch (e) {
      // jsQR 解析错误，忽略继续扫描
    }

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
  } catch {
    // 不是合法 URL，当作纯编码使用
  }
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
  manualCode.value = ''
  nextTick(() => {
    openCamera()
  })
}

// ===== 退工相关 =====

function openReturnDialog(process) {
  returnTarget.value = {
    bundleProcessId: process.id,
    processName: process.processName,
    workerName: process.workerName,
    workRecordId: process.workRecordId
  }
  returnReason.value = ''
  showReturnDialog.value = true
}

async function confirmReturn() {
  if (!returnReason.value.trim()) {
    ElMessage.warning('请输入退回原因')
    return
  }

  try {
    await ElMessageBox.confirm('确定要退回这条报工记录吗？', '退工确认', {
      confirmButtonText: '确认退回',
      cancelButtonText: '取消',
      type: 'warning'
    })

    returnLoading.value = true
    await returnWorkRecord(returnTarget.value.bundleProcessId, {
      reason: returnReason.value.trim()
    })
    ElMessage.success('退工成功')
    showReturnDialog.value = false

    // 刷新扎货详情
    if (manualCode.value) {
      loadBundle(manualCode.value)
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.msg || '退工失败')
    }
  } finally {
    returnLoading.value = false
  }
}

// ===== 生命周期 =====

onMounted(() => {
  const codeFromUrl = route.query.code
  if (codeFromUrl) {
    loadBundle(codeFromUrl)
  } else {
    openCamera()
  }
})

onBeforeUnmount(() => {
  stopCamera()
})
</script>

<style scoped>
.mobile-scan-bundle {
  min-height: 100vh;
  background: var(--color-cream, #faf8f5);
  padding-bottom: calc(80px + env(safe-area-inset-bottom));
}

/* ===== 扫码视图 ===== */
.scan-viewport {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.camera-view {
  flex: 1;
  position: relative;
  background: #1a1a2e;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
  overflow: hidden;
}

.camera-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hidden-canvas {
  display: none;
}

.camera-placeholder {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.photo-scan-icon {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.camera-hint {
  font-size: 16px;
  color: rgba(255,255,255,0.8);
  margin: 0;
  text-align: center;
}

.camera-hint-small {
  font-size: 13px;
  color: rgba(255,255,255,0.5);
  margin: 0;
  text-align: center;
}

.photo-scan-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding: 14px 36px;
  background: linear-gradient(135deg, var(--color-amber, #d97706), var(--color-terracotta, #b45309));
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 15px rgba(217, 119, 6, 0.4);
}

.photo-scan-btn:active {
  transform: scale(0.96);
}

.hidden-input {
  display: none;
}

/* 底部操作 */
.scan-bottom {
  background: var(--bg-card);
  padding: 20px 16px;
  padding-bottom: calc(20px + env(safe-area-inset-bottom));
}

.photo-scan-entry {
  margin-bottom: 16px;
}

.photo-scan-btn-bottom {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px;
  background: linear-gradient(135deg, var(--color-amber, #d97706), var(--color-terracotta, #b45309));
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.photo-scan-btn-bottom:active {
  transform: scale(0.98);
}

.scan-tip {
  font-size: 13px;
  color: #9ca3af;
  text-align: center;
  margin: 0;
}

/* 扫描框 */
.scan-frame {
  position: absolute;
  width: 240px;
  height: 240px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 3;
}

.scan-corner {
  position: absolute;
  width: 28px;
  height: 28px;
  border-color: var(--color-amber, #d97706);
  border-style: solid;
}

.scan-corner.top-left {
  top: 0;
  left: 0;
  border-width: 4px 0 0 4px;
}
.scan-corner.top-right {
  top: 0;
  right: 0;
  border-width: 4px 4px 0 0;
}
.scan-corner.bottom-left {
  bottom: 0;
  left: 0;
  border-width: 0 0 4px 4px;
}
.scan-corner.bottom-right {
  bottom: 0;
  right: 0;
  border-width: 0 4px 4px 0;
}

.scan-line {
  position: absolute;
  width: calc(100% - 8px);
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--color-amber, #d97706), transparent);
  top: 0;
  left: 4px;
  animation: scanMove 2s linear infinite;
}

@keyframes scanMove {
  0% { top: 0; opacity: 1; }
  50% { opacity: 1; }
  100% { top: calc(100% - 2px); opacity: 0.3; }
}

/* 扫描提示条 */
.scan-hint-bar {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 3;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  padding: 8px 20px;
  border-radius: 20px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  white-space: nowrap;
}

/* ===== 扎货面板 ===== */
.bundle-panel {
  padding: 12px 16px;
}

.bundle-card {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border-radius: 16px;
  padding: 16px 20px;
  color: #fff;
  margin-bottom: 12px;
}

.bundle-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.bundle-tag {
  font-size: 12px;
  background: rgba(255,255,255,0.2);
  padding: 2px 10px;
  border-radius: 10px;
}

.bundle-body {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.bundle-no {
  font-size: 24px;
  font-weight: 700;
}

.bundle-meta {
  display: flex;
  gap: 12px;
}

.meta-item {
  font-size: 13px;
  opacity: 0.85;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ===== 工序列表 ===== */
.process-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
  margin-bottom: 12px;
}

.process-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.process-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  border: 2px solid #f3f4f6;
  background: var(--bg-card);
  transition: all 0.2s ease;
}

.process-item.completed {
  border-color: #d1fae5;
  background: #f0fdf4;
}

.process-item.returned {
  border-color: #fecaca;
  background: #fef2f2;
}

.process-left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.process-index {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
}

.process-item.completed .process-index {
  background: #d1fae5;
  color: #059669;
}

.process-info {
  flex: 1;
  min-width: 0;
}

.process-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate, #2d2a26);
}

.process-price {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 2px;
}

.pricing-pending {
  color: #f59e0b;
  font-weight: 500;
}

.process-worker {
  font-size: 12px;
  color: #059669;
  margin-top: 4px;
}

.process-remark {
  font-size: 11px;
  color: #6b7280;
  margin-top: 2px;
}

.process-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.status-tag {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.status-tag.waiting {
  background: #f3f4f6;
  color: #6b7280;
}
.status-tag.done {
  background: #ecfdf5;
  color: #059669;
}
.status-tag.returned {
  background: #fef2f2;
  color: #ef4444;
}

/* ===== 进度汇总 ===== */
.summary-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  color: #6b7280;
}

.summary-row.total {
  border-top: 1px solid #f3f4f6;
  margin-top: 4px;
  padding-top: 12px;
}

.summary-val {
  color: var(--color-slate, #2d2a26);
  font-weight: 600;
}

.summary-val.done {
  color: #059669;
}

.summary-amount {
  font-size: 20px;
  font-weight: 700;
  color: #6366f1;
  font-variant-numeric: tabular-nums;
}

/* ===== 退工弹窗 ===== */
:deep(.return-dialog) {
  max-width: 430px;
}

.return-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.return-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.return-label {
  font-size: 13px;
  color: #6b7280;
  flex-shrink: 0;
  width: 60px;
}

.return-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate, #2d2a26);
}

.return-reason {
  margin-top: 8px;
}

.return-default-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.return-reason .return-label {
  margin-bottom: 8px;
}

/* ===== 加载 ===== */
.loading-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #fff;
  font-size: 15px;
  z-index: 200;
}
</style>
