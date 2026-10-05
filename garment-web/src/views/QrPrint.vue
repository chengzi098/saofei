<template>
  <div class="qr-print-page">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button @click="$router.back()" :icon="ArrowLeft">返回</el-button>
      <div class="toolbar-right">
        <span class="label-count">共 {{ bundleList.length }} 个标签</span>
        <el-button
          type="primary"
          :icon="Printer"
          @click="printAll"
          :disabled="bundleList.length === 0"
        >
          打印全部
        </el-button>
      </div>
    </div>

    <div class="filter-panel">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="款号">
          <el-select
            v-model="selectedStyleId"
            filterable
            clearable
            placeholder="选择款号"
            style="width: 220px"
            @change="handleStyleChange"
          >
            <el-option
              v-for="style in styleOptions"
              :key="style.id"
              :label="formatStyleLabel(style)"
              :value="style.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="床号">
          <el-select
            v-model="selectedBedId"
            filterable
            clearable
            placeholder="选择床号"
            style="width: 180px"
            :disabled="!selectedStyleId"
          >
            <el-option
              v-for="bed in bedOptions"
              :key="bed.id"
              :label="formatBedLabel(bed)"
              :value="bed.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="扎号">
          <el-input
            v-model="bundleNoKeyword"
            clearable
            placeholder="如 A001"
            style="width: 160px"
            @keyup.enter="loadBundles"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadBundles">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 标签预览网格 -->
    <div class="label-grid" v-if="bundleList.length">
      <div v-for="b in bundleList" :key="b.id" class="label-card">
        <div class="label-info">
          <div class="label-row label-company">{{ companyName }}</div>
          <div class="label-row">款号: {{ b.styleNo || '-' }}</div>
          <div class="label-row">床号: {{ b.bedNo || '-' }}</div>
          <div class="label-row">名称: {{ b.bedName || '-' }}</div>
          <div class="label-row label-bundle">扎号: {{ b.bundleNo }}</div>
          <div class="label-row">件数: {{ b.quantity }}</div>
          <div class="label-row">码数: {{ b.size || '-' }}</div>
          <div class="label-row">颜色: {{ b.color || b.colors || '-' }}</div>
        </div>
        <div class="qr-wrap" :data-print-qr="String(b.id)">
          <qrcode-vue :value="getQrUrl(b.qrCode)" :size="90" level="M" render-as="svg" />
        </div>
        <el-button
          class="btn-print-one"
          size="small"
          type="primary"
          plain
          :icon="Printer"
          @click="printOne(b)"
        >
          打印此标签
        </el-button>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && bundleList.length === 0" class="empty-state">
      <el-empty description="没有可打印的标签" />
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Printer, Loading } from '@element-plus/icons-vue'
import QrcodeVue from 'qrcode.vue'
import { getBundleList, markPrintedBundles } from '../api/bundle'
import { getStyleList } from '../api/style'
import { getBeds } from '../api/bed'
import { useUserStore } from '../stores/user'
import { buildQrPrintParams, filterSelectedBed, formatBedDisplayName } from '../utils/qrPrintFilters'

const route = useRoute()
const userStore = useUserStore()

const bundleList = ref([])
const loading = ref(true)
const companyName = ref(userStore.currentCompanyName || '')
const styleOptions = ref([])
const bedOptions = ref([])
const selectedStyleId = ref(route.query.styleId ? Number(route.query.styleId) : '')
const selectedBedId = ref(route.query.bedId ? Number(route.query.bedId) : '')
const bundleNoKeyword = ref(route.query.bundleNo || '')

onMounted(async () => {
  await loadStyles()
  if (selectedStyleId.value) {
    await loadBeds(selectedStyleId.value)
  }
  await loadBundles()
})

async function loadStyles() {
  try {
    const res = await getStyleList({ pageNum: 1, pageSize: 999 })
    styleOptions.value = res.data?.records || []
    if (!selectedStyleId.value && selectedBedId.value) {
      await resolveStyleFromBed(selectedBedId.value)
    }
  } catch (e) {
    styleOptions.value = []
    ElMessage.error('加载款号失败')
  }
}

async function resolveStyleFromBed(bedId) {
  for (const style of styleOptions.value) {
    try {
      const res = await getBeds(style.id)
      const beds = res.data || []
      if (beds.some((bed) => Number(bed.id) === Number(bedId))) {
        selectedStyleId.value = style.id
        bedOptions.value = beds
        selectedBedId.value = Number(bedId)
        return
      }
    } catch (e) {
      // Continue checking other styles.
    }
  }
}

async function loadBeds(styleId) {
  if (!styleId) {
    bedOptions.value = []
    selectedBedId.value = ''
    return
  }
  try {
    const res = await getBeds(styleId)
    bedOptions.value = res.data || []
    selectedBedId.value = filterSelectedBed(bedOptions.value, selectedBedId.value)
  } catch (e) {
    bedOptions.value = []
    selectedBedId.value = ''
    ElMessage.error('加载床号失败')
  }
}

async function handleStyleChange(styleId) {
  selectedBedId.value = ''
  await loadBeds(styleId)
  await loadBundles()
}

async function loadBundles() {
  loading.value = true
  try {
    const params = buildQrPrintParams({
      styleId: selectedStyleId.value,
      bedId: selectedBedId.value,
      bundleNo: bundleNoKeyword.value
    })
    const res = await getBundleList(params)
    bundleList.value = res.data.records || []
  } catch (e) {
    ElMessage.error('加载扎货失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  selectedStyleId.value = ''
  selectedBedId.value = ''
  bundleNoKeyword.value = ''
  bedOptions.value = []
  loadBundles()
}

function formatStyleLabel(style) {
  return style.name ? `${style.styleNo || '-'} · ${style.name}` : (style.styleNo || '-')
}

function formatBedLabel(bed) {
  const bedLabel = formatBedDisplayName(bed)
  return bed.quantity ? `${bedLabel}（${bed.quantity}件）` : bedLabel
}

function getQrUrl(qrCode) {
  const base = window.location.origin
  return `${base}/mobile/scan?code=${qrCode}`
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

function getRenderedQrSvg(id) {
  const host = Array.from(document.querySelectorAll('[data-print-qr]'))
    .find(element => element.dataset.printQr === String(id))
  return host?.querySelector('svg')?.outerHTML || ''
}

/**
 * 生成单张标签的 HTML
 */
function buildLabelHtml(label) {
  return `
    <div class="label">
      <div class="label-info">
        <div class="row company">${escapeHtml(companyName.value)}</div>
        <div class="row">款号: ${escapeHtml(label.styleNo || '-')}</div>
        <div class="row">床号: ${escapeHtml(label.bedNo || '-')}</div>
        <div class="row">名称: ${escapeHtml(label.bedName || '-')}</div>
        <div class="row bundle">扎号: ${escapeHtml(label.bundleNo)}</div>
        <div class="row">件数: ${escapeHtml(label.quantity)}</div>
        <div class="row">码数: ${escapeHtml(label.size || '-')}</div>
        <div class="row">颜色: ${escapeHtml(label.color || label.colors || '-')}</div>
      </div>
      <div class="qr">${getRenderedQrSvg(label.id)}</div>
    </div>
  `
}

/**
 * 内嵌 iframe 打印
 */
function openPrintWindow(labels) {
  const labelHtmls = labels.map(buildLabelHtml)

  const html = `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>打印标签</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body {
      font-family: "Microsoft YaHei", "SimHei", sans-serif;
      margin: 0;
      padding: 0;
      width: 40mm;
      height: 70mm;
    }
    .label {
      width: 40mm;
      height: 70mm;
      padding: 1mm 3mm;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: flex-start;
      page-break-after: always;
      overflow: hidden;
    }
    .label:last-child { page-break-after: auto; }
    .label-info {
      width: 100%;
      text-align: left;
      border-bottom: 1px dashed #999;
      padding-bottom: 1mm;
      margin-bottom: 1mm;
    }
    .row {
      font-size: 2.8mm;
      line-height: 1.4;
      color: #000;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .row.company {
      font-weight: bold;
      font-size: 3.2mm;
      margin-bottom: 0.5mm;
    }
    .row.bundle {
      font-weight: bold;
      font-size: 3mm;
    }
    .qr {
      flex-shrink: 0;
      margin-top: 1mm;
    }
    .qr img {
      display: block;
      width: 22mm;
      height: 22mm;
    }
    @page {
      size: 40mm 70mm;
      margin: 0 !important;
    }
  </style>
</head>
<body>
  ${labelHtmls.join('\n')}
  <script>
    setTimeout(function() { window.print(); }, 100);
  <\/script>
</body>
</html>`

  // 创建隐藏的 iframe
  const iframe = document.createElement('iframe')
  iframe.style.position = 'fixed'
  iframe.style.top = '-9999px'
  iframe.style.left = '-9999px'
  iframe.style.width = '0'
  iframe.style.height = '0'
  iframe.style.border = 'none'
  document.body.appendChild(iframe)

  // 写入内容
  const iframeDoc = iframe.contentDocument || iframe.contentWindow.document
  iframeDoc.open()
  iframeDoc.write(html)
  iframeDoc.close()

  // 打印完成后移除 iframe
  setTimeout(() => {
    document.body.removeChild(iframe)
  }, 1000)
}

async function printAll() {
  if (bundleList.value.length === 0) {
    ElMessage.warning('没有可打印的标签')
    return
  }
  openPrintWindow(bundleList.value)
  // 打印后标记已打印
  const ids = bundleList.value.map(b => b.id).filter(Boolean)
  if (ids.length > 0) {
    try {
      await markPrintedBundles(ids)
      bundleList.value.forEach(b => { b.printed = 1 })
    } catch (e) { /* 静默失败 */ }
  }
}

async function printOne(bundle) {
  openPrintWindow([bundle])
  // 打印后标记已打印
  if (bundle.id) {
    try {
      await markPrintedBundles([bundle.id])
      bundle.printed = 1
    } catch (e) { /* 静默失败 */ }
  }
}
</script>

<style scoped>
.qr-print-page {
  padding: 16px;
  background: #f5f5f5;
  min-height: 100vh;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.label-count {
  font-size: 14px;
  color: #666;
}

.filter-panel {
  max-width: 1200px;
  margin: 0 auto 16px;
  padding: 14px 16px 2px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #999;
}

.empty-state {
  padding: 60px;
}

.label-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  max-width: 1200px;
  margin: 0 auto;
}

.label-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: box-shadow 0.2s;
}

.label-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.label-info {
  width: 100%;
  text-align: left;
  padding-bottom: 8px;
  border-bottom: 1px dashed #ddd;
  margin-bottom: 8px;
}

.label-row {
  font-size: 13px;
  color: #333;
  line-height: 1.6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.label-company {
  font-weight: 700;
  font-size: 15px;
}

.label-bundle {
  font-weight: 700;
  font-size: 14px;
}

.qr-wrap {
  margin-bottom: 8px;
}

.qr-wrap canvas {
  display: block;
}

.btn-print-one {
  width: 100%;
}
</style>
