<template>
  <div class="mobile-scan">
    <!-- 扫码区域 -->
    <div class="scan-viewport" v-if="!bundleInfo">
      <!-- 相机视图 -->
      <div class="camera-view">
        <!-- 视频元素（仅实时扫码时显示） -->
        <video
          v-show="cameraStatus === 'ready'"
          ref="videoRef"
          class="camera-video"
          autoplay
          playsinline
          muted
        ></video>
        <!-- 用于截取帧的隐藏 canvas -->
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

        <!-- 隐藏的文件选择（始终在DOM中） -->
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          capture="environment"
          class="hidden-input"
          @change="handlePhotoScan"
        />

        <!-- 扫描框（仅实时扫码时显示） -->
        <div v-if="cameraStatus === 'ready'" class="scan-frame">
          <div class="scan-corner top-left"></div>
          <div class="scan-corner top-right"></div>
          <div class="scan-corner bottom-left"></div>
          <div class="scan-corner bottom-right"></div>
          <div class="scan-line"></div>
        </div>

        <!-- 扫描提示 -->
        <div class="scan-hint-bar" v-if="cameraStatus === 'ready'">
          <span>将二维码放入框内，自动扫描</span>
        </div>
      </div>

      <!-- 底部操作区 -->
      <div class="scan-bottom">
        <!-- 拍照扫描按钮（备用入口） -->
        <div class="photo-scan-entry" v-if="cameraStatus === 'unsupported' || cameraStatus === 'error'">
          <button class="photo-scan-btn-bottom" @click="triggerFileInput">
            <el-icon size="22"><Camera /></el-icon>
            <span>拍照扫描二维码</span>
          </button>
        </div>

        <p class="scan-tip">将二维码放入框内自动扫描报工</p>
      </div>
    </div>

    <!-- 扎货信息 + 工序选择 -->
    <div class="bundle-panel" v-if="bundleInfo">
      <!-- 扎货信息卡片 -->
      <div class="bundle-card">
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
            <span class="meta-item" v-if="bundleInfo.size">
              尺码: {{ bundleInfo.size }}
            </span>
          </div>
        </div>
      </div>

      <!-- 工序选择 -->
      <div class="process-section">
        <div class="section-title">选择完成的工序</div>
        <div class="process-list">
          <div
            v-for="p in bundleInfo.processes"
            :key="p.id"
            class="process-item"
            :class="{
              selected: isSelected(p.id),
              disabled: isProcessDisabled(p)
            }"
            @click="!isProcessDisabled(p) && toggleProcess(p)"
          >
            <div class="process-check">
              <el-icon v-if="isProcessDisabled(p)" size="22"><Lock /></el-icon>
              <div v-else-if="isSelected(p.id)" class="check-active">
                <el-icon size="16"><Check /></el-icon>
              </div>
              <div v-else class="check-empty"></div>
            </div>
            <div class="process-info">
              <div class="process-name">{{ p.processName }}</div>
              <div class="process-price">
                <template v-if="p.price > 0">{{ p.price }}元/件 × {{ bundleInfo.quantity }}件 = {{ (p.price * bundleInfo.quantity).toFixed(2) }}元</template>
                <template v-else><span class="pricing-pending">定价中</span></template>
              </div>
              <!-- 已完成的工序显示工人信息 -->
              <div v-if="p.status === 2 && p.workerName" class="process-worker">
                <span>已完成 · {{ p.workerName }}</span>
                <span v-if="p.workerRemark" class="worker-remark">备注: {{ p.workerRemark }}</span>
              </div>
            </div>
            <div class="process-status-tag" :class="getStatusClass(p.status)">
              {{ getStatusText(p.status) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 备注输入 -->
      <div class="remark-section" v-if="selectedList.length > 0">
        <div class="section-title">备注（选填）</div>
        <el-input
          v-model="remark"
          type="textarea"
          :rows="2"
          placeholder="如有特殊情况可在此备注"
          maxlength="200"
          show-word-limit
        />
      </div>

      <!-- 汇总 -->
      <div class="summary-section" v-if="selectedList.length > 0">
        <div class="summary-row">
          <span>工序数</span>
          <span class="summary-val">{{ selectedList.length }}道</span>
        </div>
        <div class="summary-row">
          <span>总件数</span>
          <span class="summary-val">{{ totalPieces }}件</span>
        </div>
        <div class="summary-row total">
          <span>预计收入</span>
          <span class="summary-amount">{{ totalAmount }}元</span>
        </div>
      </div>
    </div>

    <!-- 提交按钮（固定底部） -->
    <div class="submit-bar" v-if="selectedList.length > 0">
      <button class="submit-btn" :disabled="submitting" @click="handleSubmit">
        <span v-if="!submitting">提交报工</span>
        <span v-else>提交中...</span>
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && !bundleInfo" class="loading-overlay">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>查询中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Camera, Close, Lock, Check, Loading,
  Box
} from '@element-plus/icons-vue'
import jsQR from 'jsqr'
import { scanBundle } from '../../api/bundle'
import { submitWorkRecord } from '../../api/workRecord'

const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const manualCode = ref('')
const bundleInfo = ref(null)
const remark = ref('')

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

// 选中的工序
const selectedMap = reactive({})

const selectedList = computed(() => {
  return Object.values(selectedMap).map(item => ({
    processId: item.id,
    processName: item.processName,
    price: item.price,
    quantity: bundleInfo.value?.quantity || 0
  }))
})

const totalPieces = computed(() => {
  return selectedList.value.reduce((sum, item) => sum + item.quantity, 0)
})

const totalAmount = computed(() => {
  return selectedList.value.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2)
})

function isSelected(processId) {
  return !!selectedMap[processId]
}

function isProcessDisabled(p) {
  return p.status === 2
}

function getStatusText(status) {
  const map = { 0: '未开始', 2: '已完成' }
  return map[status] || '未知'
}

function getStatusClass(status) {
  const map = { 0: 'waiting', 2: 'done' }
  return map[status] || ''
}

function toggleProcess(p) {
  if (selectedMap[p.id]) {
    delete selectedMap[p.id]
  } else {
    selectedMap[p.id] = {
      id: p.id,
      processName: p.processName,
      price: p.price
    }
  }
}

// ===== 摄像头相关 =====

async function openCamera() {
  cameraStatus.value = 'loading'
  cameraError.value = ''

  // 检查是否支持摄像头API（需要HTTPS或localhost）
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

// 拍照识别二维码
async function handlePhotoScan(e) {
  const file = e.target.files[0]
  if (!file) return

  console.log('[PhotoScan] 开始处理图片:', file.name, file.type, file.size)
  loading.value = true

  const img = new Image()
  img.onload = () => {
    console.log('[PhotoScan] 图片加载完成，尺寸:', img.width, 'x', img.height)

    const canvas = document.createElement('canvas')
    // 限制图片大小，避免内存溢出
    const maxSize = 1024
    let width = img.width
    let height = img.height
    if (width > maxSize || height > maxSize) {
      const ratio = Math.min(maxSize / width, maxSize / height)
      width = Math.floor(width * ratio)
      height = Math.floor(height * ratio)
      console.log('[PhotoScan] 缩放图片至:', width, 'x', height)
    }

    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0, width, height)
    const imageData = ctx.getImageData(0, 0, width, height)

    console.log('[PhotoScan] 开始识别二维码...')
    try {
      const code = jsQR(imageData.data, imageData.width, imageData.height, {
        inversionAttempts: 'attemptBoth'
      })
      console.log('[PhotoScan] jsQR 结果:', code)

      if (code && code.data) {
        console.log('[PhotoScan] 识别成功:', code.data)
        manualCode.value = code.data
        // 先清空 input，再调用 loadBundle
        e.target.value = ''
        loadBundle(code.data)
      } else {
        console.log('[PhotoScan] 未识别到二维码')
        ElMessage.warning('未识别到二维码，请确保二维码清晰完整')
        loading.value = false
        e.target.value = ''
      }
    } catch (err) {
      console.error('[PhotoScan] 识别出错:', err)
      ElMessage.warning('识别失败，请重新拍照')
      loading.value = false
      e.target.value = ''
    }

    // 释放 ObjectURL
    URL.revokeObjectURL(img.src)
  }
  img.onerror = (err) => {
    console.error('[PhotoScan] 图片加载失败:', err)
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
        // 防止同一二维码在短时间内重复触发（2秒内）
        if (code.data !== lastScannedCode || now - lastScanTime > 2000) {
          lastScannedCode = code.data
          lastScanTime = now
          // 扫码成功，自动查询
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

// 扫码成功后自动查询
async function handleScanCode(code) {
  if (loading.value) return
  await loadBundle(code)
}

function handleScan() {
  if (!manualCode.value.trim()) {
    ElMessage.warning('请输入二维码内容')
    return
  }
  loadBundle(manualCode.value.trim())
}

function extractQrCode(raw) {
  // 二维码内容可能是完整 URL（如 http://xxx/mobile/scan?code=xxx），也可能是纯编码
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
    // 扫码成功后停止摄像头
    stopCamera()
    // 清空之前的选择
    Object.keys(selectedMap).forEach(k => delete selectedMap[k])
    remark.value = ''
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '二维码无效，请重试')
  } finally {
    loading.value = false
  }
}

function resetScan() {
  bundleInfo.value = null
  manualCode.value = ''
  Object.keys(selectedMap).forEach(k => delete selectedMap[k])
  remark.value = ''
  // 重新打开摄像头
  nextTick(() => {
    openCamera()
  })
}

async function handleSubmit() {
  if (selectedList.value.length === 0) return

  submitting.value = true
  try {
    for (const item of selectedList.value) {
      await submitWorkRecord({
        bundleId: bundleInfo.value.id,
        bundleProcessId: item.processId,
        remark: remark.value
      })
    }
    ElMessage.success('报工成功')
    resetScan()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '提交失败')
  } finally {
    submitting.value = false
  }
}

// ===== 生命周期 =====

onMounted(() => {
  // 如果 URL 中有 code 参数（手机自带相机扫码跳转过来），直接加载扎货
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
.mobile-scan {
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

.manual-input-area {
  margin-bottom: 12px;
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
  background: linear-gradient(135deg, var(--color-amber, #d97706), var(--color-terracotta, #b45309));
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

/* ===== 工序选择 ===== */
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
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  border: 2px solid #f3f4f6;
  cursor: pointer;
  transition: all 0.2s ease;
}

.process-item:active {
  transform: scale(0.98);
}

.process-item.selected {
  border-color: var(--color-amber, #d97706);
  background: #fffbeb;
}

.process-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f9fafb;
}

.process-check {
  flex-shrink: 0;
}

.check-empty {
  width: 24px;
  height: 24px;
  border: 2px solid #d1d5db;
  border-radius: 50%;
}

.check-active {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-amber, #d97706);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
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
}

.pricing-pending {
  color: #f59e0b;
  font-weight: 500;
}

.process-worker {
  font-size: 12px;
  color: #059669;
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.worker-remark {
  color: #6b7280;
  font-size: 11px;
}

.process-status-tag {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 500;
  flex-shrink: 0;
}

.process-status-tag.waiting {
  background: #f3f4f6;
  color: #6b7280;
}
.process-status-tag.done {
  background: #ecfdf5;
  color: #059669;
}

/* ===== 备注输入 ===== */
.remark-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
}

/* ===== 汇总 ===== */
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

.summary-amount {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-amber, #d97706);
  font-variant-numeric: tabular-nums;
}

/* ===== 提交按钮 ===== */
.submit-bar {
  position: fixed;
  bottom: calc(56px + env(safe-area-inset-bottom));
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 430px;
  padding: 12px 16px;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(10px);
  z-index: 100;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--color-amber, #d97706), var(--color-terracotta, #b45309));
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.98);
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
