<template>
  <div class="quick-entry">
    <!-- Hero 区域 -->
    <div class="qe-hero">
      <div class="qe-hero-icon">
        <svg viewBox="0 0 48 48" fill="none">
          <rect x="4" y="8" width="40" height="32" rx="4" stroke="currentColor" stroke-width="2.5" fill="none"/>
          <path d="M4 18h40" stroke="currentColor" stroke-width="2"/>
          <circle cx="12" cy="13" r="2" fill="currentColor"/>
          <circle cx="20" cy="13" r="2" fill="currentColor"/>
          <path d="M14 28h8M14 34h20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M30 25l4 4-4 4" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <div>
        <h2>快速录入</h2>
        <p>支持手动录入和 AI 批量录入两种方式</p>
      </div>
    </div>

    <!-- 模式切换 -->
    <div class="mode-switch">
      <button
        class="mode-btn"
        :class="{ active: mode === 'manual' }"
        @click="mode = 'manual'"
      >
        <el-icon><Edit /></el-icon>
        手动录入
      </button>
      <button
        class="mode-btn"
        :class="{ active: mode === 'ai' }"
        @click="mode = 'ai'"
      >
        <el-icon><MagicStick /></el-icon>
        AI 批量录入
      </button>
    </div>

    <!-- 手动录入模式 -->
    <template v-if="mode === 'manual'">
      <!-- Step 1: 选择款式 -->
      <template v-if="!selectedStyleId">
        <div class="qe-search-area">
          <div class="search-box">
            <el-icon class="search-icon"><Search /></el-icon>
            <input
              v-model="searchKeyword"
              placeholder="搜索款号…"
              class="search-input"
              @input="onSearchInput"
            />
            <el-icon v-if="searchKeyword" class="clear-icon" @click="searchKeyword = ''"><CircleClose /></el-icon>
          </div>
        </div>

        <div v-loading="loadingStyles" class="style-grid">
          <div
            v-for="(s, idx) in styleList"
            :key="s.id"
            class="style-card"
            :style="{ animationDelay: `${idx * 0.04}s` }"
            @click="selectStyle(s)"
          >
            <div class="style-card-top">
              <span class="style-no">{{ s.styleNo }}</span>
              <span v-if="s.bundleCount > 0" class="bundle-pill has">已分扎</span>
              <span v-else class="bundle-pill empty">未分扎</span>
            </div>
            <div class="style-card-name">{{ s.name || '未命名' }}</div>
            <div class="style-card-arrow">
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
          <el-empty v-if="!loadingStyles && styleList.length === 0" description="暂无款式，请先在生产管理中创建" />
        </div>
      </template>

      <!-- Step 2: 快速录入 -->
      <template v-else>
        <div class="qe-top-bar">
          <button type="button" class="back-btn" @click="backToStyle">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回</span>
          </button>
          <div class="qe-breadcrumb">
            <span class="bc-item">{{ selectedStyle.styleNo }}</span>
            <el-icon class="bc-sep"><ArrowRight /></el-icon>
            <span class="bc-item active">快速录入</span>
          </div>
        </div>

        <div class="entry-layout">
          <!-- 左侧：录入表单 -->
          <div class="form-panel">
            <div class="form-panel-header">
              <div class="form-panel-icon">
                <el-icon :size="20"><DocumentAdd /></el-icon>
              </div>
              <div>
                <h3>录入床号</h3>
                <p>填写信息后自动创建床号并分扎</p>
              </div>
            </div>

            <div class="form-body">
              <div class="form-field">
                <label class="field-label">床号编号 <span class="required">*</span></label>
                <input
                  ref="bedNoInput"
                  v-model="entryForm.bedNo"
                  class="field-input"
                  placeholder="例如：1号床"
                  @keyup.enter="focusNext('nameInput')"
                />
              </div>

              <div class="form-field">
                <label class="field-label">名称 <span class="required">*</span></label>
                <input
                  ref="nameInput"
                  v-model="entryForm.name"
                  class="field-input"
                  placeholder="例如：大货"
                  @keyup.enter="focusNext('qtyInput')"
                />
              </div>

              <div class="form-row-2">
                <div class="form-field">
                  <label class="field-label">件数 <span class="required">*</span></label>
                  <input
                    ref="qtyInput"
                    v-model.number="entryForm.quantity"
                    type="number"
                    class="field-input"
                    min="1"
                    placeholder="100"
                  />
                </div>
                <div class="form-field">
                  <label class="field-label">颜色</label>
                  <input
                    v-model="entryForm.colors"
                    class="field-input"
                    placeholder="例如：黑色"
                  />
                </div>
              </div>

              <div class="form-row-2">
                <div class="form-field">
                  <label class="field-label">尺码</label>
                  <div class="size-select-wrap">
                    <select v-model="entryForm.size" class="field-select">
                      <option value="">选择尺码</option>
                      <option v-for="s in commonSizes" :key="s" :value="s">{{ s }}</option>
                    </select>
                    <input
                      v-if="entryForm.size === '__custom'"
                      v-model="entryForm.customSize"
                      class="field-input size-custom"
                      placeholder="输入尺码"
                    />
                  </div>
                </div>
                <div class="form-field">
                  <label class="field-label">工序模板</label>
                  <select v-model="entryForm.templateId" class="field-select">
                    <option :value="null">不使用模板</option>
                    <option v-for="t in processTemplates" :key="t.id" :value="t.id">
                      {{ t.name }}（{{ t.itemCount || 0 }}道）
                    </option>
                  </select>
                </div>
              </div>

              <div class="form-field">
                <label class="field-label">自动分扎</label>
                <div class="bundle-mode-group">
                  <button
                    v-for="opt in bundleOptions"
                    :key="opt.value"
                    type="button"
                    class="bundle-mode-btn"
                    :class="{ active: entryForm.autoBundle === opt.value }"
                    @click="entryForm.autoBundle = opt.value"
                  >
                    <span class="bm-icon">{{ opt.icon }}</span>
                    <span class="bm-label">{{ opt.label }}</span>
                  </button>
                </div>
              </div>
            </div>

            <div class="form-actions">
              <button type="button" class="btn btn-primary" :disabled="saving" @click="saveAndContinue">
                <el-icon v-if="!saving"><Check /></el-icon>
                <span v-if="saving" class="btn-spinner"></span>
                保存并继续
              </button>
              <button type="button" class="btn btn-outline" :disabled="saving" @click="saveAndPrint">
                <el-icon v-if="!saving"><Printer /></el-icon>
                保存并打印
              </button>
            </div>
          </div>

          <!-- 右侧：已录入列表 -->
          <div class="list-panel">
            <div class="list-panel-header">
              <h3>已录入床号</h3>
            </div>

            <div v-if="entryList.length === 0" class="list-empty">
              <div class="empty-illustration">
                <svg viewBox="0 0 120 80" fill="none">
                  <rect x="10" y="20" width="100" height="50" rx="6" stroke="#d1cdc7" stroke-width="2" stroke-dasharray="6 4"/>
                  <path d="M40 40h40M50 50h20" stroke="#d1cdc7" stroke-width="2" stroke-linecap="round"/>
                </svg>
              </div>
              <p>还没有录入床号</p>
              <span>在左侧填写信息后点击保存</span>
            </div>

            <div v-else class="entry-list">
              <TransitionGroup name="entry-item">
                <div
                  v-for="(item, idx) in entryList"
                  :key="item.bedId"
                  class="entry-card"
                  :style="{ animationDelay: `${idx * 0.03}s` }"
                >
                  <div class="ec-top">
                    <div class="ec-bed-no">{{ item.bedNo }}</div>
                    <div class="ec-actions">
                      <button
                        v-if="item.bundleCount > 0"
                        type="button"
                        class="ec-print-btn"
                        @click="printSingle(item)"
                      >
                        <el-icon><Printer /></el-icon>
                      </button>
                    </div>
                  </div>
                  <div class="ec-detail">
                    <span class="ec-name">{{ item.name }}</span>
                    <span class="ec-sep">·</span>
                    <span class="ec-qty">{{ item.quantity }}件</span>
                    <span v-if="item.size" class="ec-sep">·</span>
                    <span v-if="item.size" class="ec-size">{{ item.size }}</span>
                  </div>
                  <div class="ec-bottom">
                    <span v-if="item.colors" class="ec-color">{{ item.colors }}</span>
                    <span v-if="item.bundleCount > 0" class="ec-bundle-tag">{{ item.bundleCount }}扎</span>
                    <span v-else class="ec-bundle-tag empty">未分扎</span>
                  </div>
                </div>
              </TransitionGroup>
            </div>
          </div>
        </div>
      </template>
    </template>

    <!-- AI 批量录入模式 -->
    <template v-if="mode === 'ai'">
      <!-- 鉴权状态 -->
      <el-alert
        v-if="isAuthenticated"
        title="鉴权成功"
        type="success"
        :closable="false"
        show-icon
        class="auth-alert"
      >
        <template #default>
          <span>Token 和公司 ID 已自动填入，点击下方按钮一键复制全部内容给 AI</span>
        </template>
      </el-alert>
      <el-alert
        v-else
        title="未鉴权"
        type="warning"
        :closable="false"
        show-icon
        class="auth-alert"
      >
        <template #default>
          <span>请点击"一键鉴权"按钮，自动获取 Token 和公司 ID</span>
        </template>
      </el-alert>

      <div class="ai-layout">
        <!-- 左侧：操作区 -->
        <div class="ai-action-panel">
          <div class="action-card">
            <h3>一键复制给 AI</h3>
            <p>复制包含 API 地址、参数说明、请求示例的完整内容，直接发给 AI 即可</p>

            <div class="action-buttons">
              <el-button type="success" @click="handleAuth" size="large">
                <el-icon><Key /></el-icon>
                一键鉴权
              </el-button>
              <el-button
                type="primary"
                @click="copyAllForAI"
                :disabled="!isAuthenticated"
                size="large"
              >
                <el-icon><CopyDocument /></el-icon>
                一键复制全部（发给AI）
              </el-button>
            </div>
          </div>

          <!-- 请求示例 -->
          <div class="example-card">
            <h4>请求示例</h4>
            <div class="example-tabs">
              <button
                v-for="tab in exampleTabs"
                :key="tab.key"
                class="tab-btn"
                :class="{ active: activeTab === tab.key }"
                @click="activeTab = tab.key"
              >
                {{ tab.label }}
              </button>
            </div>
            <pre class="code-block">{{ currentExample }}</pre>
            <el-button size="small" plain @click="copyExample" class="copy-example-btn">
              <el-icon><CopyDocument /></el-icon> 复制示例
            </el-button>
          </div>
        </div>

        <!-- 右侧：说明区 -->
        <div class="ai-docs-panel">
          <div class="docs-card">
            <h3>
              <el-icon><InfoFilled /></el-icon>
              API 参数说明
            </h3>
            <div class="param-table">
              <div class="param-row header">
                <span class="col-name">字段</span>
                <span class="col-type">类型</span>
                <span class="col-required">必填</span>
                <span class="col-desc">说明</span>
              </div>
              <div class="param-row">
                <span class="col-name">styleNo</span>
                <span class="col-type">string</span>
                <span class="col-required yes">是</span>
                <span class="col-desc">款号，不存在则自动创建</span>
              </div>
              <div class="param-row">
                <span class="col-name">styleName</span>
                <span class="col-type">string</span>
                <span class="col-required yes">是</span>
                <span class="col-desc">款式名称</span>
              </div>
              <div class="param-row">
                <span class="col-name">bedNo</span>
                <span class="col-type">string</span>
                <span class="col-required yes">是</span>
                <span class="col-desc">床号，不存在则自动创建</span>
              </div>
              <div class="param-row">
                <span class="col-name">bedName</span>
                <span class="col-type">string</span>
                <span class="col-required yes">是</span>
                <span class="col-desc">床号名称</span>
              </div>
              <div class="param-row">
                <span class="col-name">colors</span>
                <span class="col-type">string</span>
                <span class="col-required">否</span>
                <span class="col-desc">颜色</span>
              </div>
              <div class="param-row">
                <span class="col-name">processes</span>
                <span class="col-type">array</span>
                <span class="col-required">否</span>
                <span class="col-desc">工序列表（仅新建床号时生效）</span>
              </div>
              <div class="param-row">
                <span class="col-name">└ name</span>
                <span class="col-type">string</span>
                <span class="col-required yes">是</span>
                <span class="col-desc">工序名称</span>
              </div>
              <div class="param-row">
                <span class="col-name">└ price</span>
                <span class="col-type">number</span>
                <span class="col-required">否</span>
                <span class="col-desc">计件单价，默认 0</span>
              </div>
              <div class="param-row">
                <span class="col-name">bundles</span>
                <span class="col-type">array</span>
                <span class="col-required yes">是</span>
                <span class="col-desc">扎货列表</span>
              </div>
              <div class="param-row">
                <span class="col-name">└ quantity</span>
                <span class="col-type">number</span>
                <span class="col-required yes">是</span>
                <span class="col-desc">件数，必须大于 0</span>
              </div>
              <div class="param-row">
                <span class="col-name">└ size</span>
                <span class="col-type">string</span>
                <span class="col-required">否</span>
                <span class="col-desc">尺码（如 S、M、L、XL）</span>
              </div>
            </div>
          </div>

          <div class="docs-card">
            <h3>
              <el-icon><QuestionFilled /></el-icon>
              使用说明
            </h3>
            <ul class="usage-list">
              <li>点击"一键鉴权"获取 Token 和公司 ID</li>
              <li>点击"一键复制全部"复制完整 API 信息</li>
              <li>将复制的内容发给 AI（如 ChatGPT、Claude）</li>
              <li>告诉 AI 你的数据，AI 会自动调用 API 录入</li>
              <li>款号和床号不存在会自动创建，已存在则直接使用</li>
              <li>扎号会自动生成（如 A001、A002...）</li>
            </ul>
          </div>
        </div>
      </div>
    </template>

    <!-- 打印抽屉 -->
    <BundlePrintDrawer
      v-model="printDrawerVisible"
      :bed-id="printBedId"
      :style-no="selectedStyle?.styleNo || ''"
      :bed-no="printBedNo"
      @printed="onPrinted"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, CircleClose, ArrowLeft, ArrowRight, Check, Printer, DocumentAdd,
  Edit, MagicStick, Key, CopyDocument, InfoFilled, QuestionFilled
} from '@element-plus/icons-vue'
import { getStyleList } from '../api/style'
import { addBed } from '../api/bed'
import { batchSaveBundles, getBundleList } from '../api/bundle'
import { getProcessTemplateList, getProcessTemplateDetail } from '../api/processTemplate'
import { useUserStore } from '../stores/user'
import BundlePrintDrawer from '../components/BundlePrintDrawer.vue'

const userStore = useUserStore()

// ===== 模式切换 =====
const mode = ref('manual')

// ===== 手动录入相关 =====
const commonSizes = ['XS', 'S', 'M', 'L', 'XL', 'XXL', '2XL', '3XL']
const bundleOptions = [
  { value: '0', label: '不分扎', icon: '—' },
  { value: '1', label: '分1扎', icon: '①' },
  { value: '2', label: '分2扎', icon: '②' }
]

const loadingStyles = ref(false)
const styleList = ref([])
const searchKeyword = ref('')
let searchTimer = null

async function loadStyles() {
  loadingStyles.value = true
  try {
    const res = await getStyleList({
      pageNum: 1,
      pageSize: 100,
      keyword: searchKeyword.value || undefined
    })
    const records = res.data.records || []
    const checkPromises = records.map(async (s) => {
      try {
        const bRes = await getBundleList({ styleId: s.id, pageSize: 1 })
        s.bundleCount = bRes.data?.total || 0
      } catch { s.bundleCount = 0 }
    })
    await Promise.all(checkPromises)
    styleList.value = records
  } catch {
    styleList.value = []
  } finally {
    loadingStyles.value = false
  }
}

function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadStyles, 300)
}

onMounted(loadStyles)

const selectedStyleId = ref(null)
const selectedStyle = ref(null)

function selectStyle(s) {
  selectedStyleId.value = s.id
  selectedStyle.value = s
  loadProcessTemplates()
  nextTick(() => bedNoInput.value?.focus())
}

function backToStyle() {
  selectedStyleId.value = null
  selectedStyle.value = null
  entryList.value = []
  loadStyles()
}

const processTemplates = ref([])

async function loadProcessTemplates() {
  try {
    const res = await getProcessTemplateList()
    processTemplates.value = res.data || []
  } catch {
    processTemplates.value = []
  }
}

const bedNoInput = ref(null)
const nameInput = ref(null)
const qtyInput = ref(null)
const saving = ref(false)

const entryForm = reactive({
  bedNo: '',
  name: '大货',
  quantity: 100,
  colors: '',
  size: '',
  customSize: '',
  templateId: null,
  autoBundle: '1'
})

const entryList = ref([])

function focusNext(refName) {
  const refs = { nameInput, qtyInput }
  refs[refName]?.value?.focus()
}

function resetForm() {
  entryForm.bedNo = ''
  entryForm.quantity = 100
  entryForm.colors = ''
  nextTick(() => bedNoInput.value?.focus())
}

async function doSave() {
  const bedNo = entryForm.bedNo.trim()
  const name = entryForm.name.trim()
  if (!bedNo) { ElMessage.warning('请填写床号编号'); return null }
  if (!name) { ElMessage.warning('请填写名称'); return null }
  if (!entryForm.quantity || entryForm.quantity < 1) { ElMessage.warning('件数必须大于0'); return null }

  saving.value = true
  try {
    let processes = null
    if (entryForm.templateId) {
      const templateRes = await getProcessTemplateDetail(entryForm.templateId)
      processes = (templateRes.data?.items || []).map((item, index) => ({
        name: item.name,
        seq: item.seq || index + 1,
        price: item.price || 0
      }))
    }

    const res = await addBed(selectedStyleId.value, {
      bedNo,
      name,
      quantity: entryForm.quantity,
      colors: entryForm.colors.trim() || null,
      remark: null,
      processes
    })
    const createdBed = res.data

    let bundleCount = 0
    const autoBundleCount = parseInt(entryForm.autoBundle)
    if (autoBundleCount > 0) {
      const totalQty = entryForm.quantity
      const baseQty = Math.floor(totalQty / autoBundleCount)
      const remainder = totalQty % autoBundleCount
      const bundles = []
      for (let i = 0; i < autoBundleCount; i++) {
        bundles.push({
          quantity: baseQty + (i < remainder ? 1 : 0),
          size: entryForm.size || entryForm.customSize || null
        })
      }
      await batchSaveBundles({ bedId: createdBed.id, bundles })
      bundleCount = autoBundleCount
    }

    const entry = {
      bedId: createdBed.id,
      bedNo,
      name,
      quantity: entryForm.quantity,
      size: entryForm.size || entryForm.customSize,
      colors: entryForm.colors,
      bundleCount
    }
    entryList.value.unshift(entry)
    ElMessage.success(`床号 ${bedNo} 已创建${bundleCount > 0 ? `，自动分${bundleCount}扎` : ''}`)
    return entry
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '保存失败')
    return null
  } finally {
    saving.value = false
  }
}

async function saveAndContinue() {
  const entry = await doSave()
  if (entry) resetForm()
}

async function saveAndPrint() {
  const entry = await doSave()
  if (entry) {
    resetForm()
    if (entry.bundleCount > 0) printSingle(entry)
  }
}

const printDrawerVisible = ref(false)
const printBedId = ref(null)
const printBedNo = ref('')

function printSingle(item) {
  printBedId.value = item.bedId
  printBedNo.value = item.bedNo
  printDrawerVisible.value = true
}

function onPrinted() {}

// ===== AI 批量录入相关 =====
const authData = ref({
  token: '',
  companyId: ''
})

const isAuthenticated = computed(() => {
  return authData.value.token && authData.value.companyId
})

const activeTab = ref('simple')
const exampleTabs = [
  { key: 'simple', label: '简单录入' },
  { key: 'full', label: '完整录入' },
  { key: 'batch', label: '多床录入' }
]

const examples = {
  simple: JSON.stringify({
    styleNo: "TX-001",
    styleName: "夏季T恤",
    bedNo: "1号床",
    bedName: "大货",
    bundles: [
      { quantity: 50, size: "M" },
      { quantity: 50, size: "L" }
    ]
  }, null, 2),

  full: JSON.stringify({
    styleNo: "TX-001",
    styleName: "夏季T恤",
    bedNo: "1号床",
    bedName: "大货",
    colors: "黑色",
    processes: [
      { name: "裁剪", price: 0.5 },
      { name: "缝制", price: 1.0 },
      { name: "整烫", price: 0.3 }
    ],
    bundles: [
      { quantity: 30, size: "S" },
      { quantity: 50, size: "M" },
      { quantity: 50, size: "L" },
      { quantity: 20, size: "XL" }
    ]
  }, null, 2),

  batch: JSON.stringify([
    {
      styleNo: "TX-001",
      styleName: "夏季T恤",
      bedNo: "1号床",
      bedName: "大货",
      bundles: [
        { quantity: 50, size: "M" },
        { quantity: 50, size: "L" }
      ]
    },
    {
      styleNo: "TX-001",
      styleName: "夏季T恤",
      bedNo: "2号床",
      bedName: "大货",
      bundles: [
        { quantity: 40, size: "M" },
        { quantity: 60, size: "L" }
      ]
    }
  ], null, 2)
}

const currentExample = computed(() => examples[activeTab.value])

function getBaseUrl() {
  const isDev = window.location.port === '5173'
  if (isDev) {
    const hostname = window.location.hostname
    return `http://${hostname}:8080`
  }
  return window.location.origin
}

function handleAuth() {
  const token = localStorage.getItem('garment_token')
  const companyId = userStore.currentCompanyId

  if (!token) {
    ElMessage.warning('未找到 Token，请先登录')
    return
  }

  if (!companyId) {
    ElMessage.warning('未找到公司 ID，请先选择公司')
    return
  }

  authData.value.token = token
  authData.value.companyId = companyId.toString()
  ElMessage.success('鉴权成功，现在可以点击"一键复制全部"')
}

function copyAllForAI() {
  if (!isAuthenticated.value) {
    ElMessage.warning('请先点击"一键鉴权"按钮')
    return
  }

  const baseUrl = getBaseUrl()

  const allContent = `=== 制衣厂 AI 一键录入 API ===

【API 地址】
POST ${baseUrl}/api/quick-import

【请求头】
Content-Type: application/json
Authorization: Bearer ${authData.value.token}
X-Company-Id: ${authData.value.companyId}

【cURL 命令】
curl -X POST "${baseUrl}/api/quick-import" \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer ${authData.value.token}" \\
  -H "X-Company-Id: ${authData.value.companyId}" \\
  -d '{你的JSON数据}'

【请求参数说明】
- styleNo (string, 必填): 款号，不存在则自动创建
- styleName (string, 可选): 款式名称（新建款号时使用）
- bedNo (string, 必填): 床号，不存在则自动创建
- bedName (string, 可选): 床号名称，默认"大货"
- colors (string, 可选): 颜色
- processes (array, 可选): 工序列表（仅新建床号时生效）
  - name (string, 必填): 工序名称
  - price (number, 可选): 计件单价，默认 0
- bundles (array, 必填): 扎货列表
  - quantity (number, 必填): 件数，必须大于 0
  - size (string, 可选): 尺码（如 S、M、L、XL）

【请求示例 - 简单录入】
{
  "styleNo": "TX-001",
  "bedNo": "1号床",
  "bundles": [
    { "quantity": 50, "size": "M" },
    { "quantity": 50, "size": "L" }
  ]
}

【请求示例 - 完整录入】
{
  "styleNo": "TX-001",
  "styleName": "夏季T恤",
  "bedNo": "1号床",
  "bedName": "大货",
  "colors": "黑色",
  "processes": [
    { "name": "裁剪", "price": 0.5 },
    { "name": "缝制", "price": 1.0 },
    { "name": "整烫", "price": 0.3 }
  ],
  "bundles": [
    { "quantity": 30, "size": "S" },
    { "quantity": 50, "size": "M" },
    { "quantity": 50, "size": "L" },
    { "quantity": 20, "size": "XL" }
  ]
}

【使用说明】
1. 复制上方 cURL 命令，替换 -d 后面的 JSON 数据即可调用
2. 款号和床号如果不存在会自动创建，已存在则直接使用
3. 扎号会自动生成（如 A001、A002...），无需手动指定
4. 每扎会自动生成唯一二维码，用于后续扫码报工

请根据我提供的数据，帮我调用这个 API 进行录入。`

  copyToClipboard(allContent, '全部内容已复制，可直接发给 AI')
}

function copyExample() {
  copyToClipboard(currentExample.value, '示例已复制到剪贴板')
}

function copyToClipboard(text, message) {
  ElMessage.success(message)
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).catch(() => fallbackCopy(text))
    } else {
      fallbackCopy(text)
    }
  } catch {
    fallbackCopy(text)
  }
}

function fallbackCopy(text) {
  try {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  } catch (err) {
    console.error('复制失败:', err)
  }
}
</script>

<style scoped>
.quick-entry {
  max-width: 1200px;
  padding-bottom: 40px;
}

/* ===== Hero ===== */
.qe-hero {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 24px;
  padding: 24px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  animation: fadeInUp 0.4s var(--ease-out) both;
}

.qe-hero-icon {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--color-amber), #d4952e);
  color: #fff;
  box-shadow: 0 4px 14px rgba(232, 168, 56, 0.3);
}

.qe-hero-icon svg {
  width: 30px;
  height: 30px;
}

.qe-hero h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-charcoal);
}

.qe-hero p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--color-mist);
}

/* ===== 模式切换 ===== */
.mode-switch {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: fadeInUp 0.4s var(--ease-out) 0.05s both;
}

.mode-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px;
  border: 2px solid #e8e4de;
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate-light);
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.mode-btn:hover {
  border-color: var(--color-amber);
  color: var(--color-amber-dark);
}

.mode-btn.active {
  border-color: var(--color-amber);
  background: rgba(232, 168, 56, 0.08);
  color: var(--color-amber-dark);
  box-shadow: 0 0 0 3px rgba(232, 168, 56, 0.1);
}

/* ===== 搜索 ===== */
.qe-search-area {
  margin-bottom: 18px;
  animation: fadeInUp 0.4s var(--ease-out) 0.1s both;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-card);
  border: 1.5px solid var(--color-cream-dark);
  border-radius: var(--radius-md);
  padding: 0 14px;
  height: 40px;
  width: 300px;
  transition: all 0.2s;
}

.search-box:focus-within {
  border-color: var(--color-amber);
  box-shadow: 0 0 0 3px rgba(232, 168, 56, 0.08);
}

.search-icon { font-size: 16px; color: var(--color-mist); flex-shrink: 0; }

.search-input {
  border: none; outline: none; background: transparent;
  font-size: 14px; color: var(--color-slate); width: 100%;
  font-family: inherit;
}

.search-input::placeholder { color: #bbb; }

.clear-icon {
  font-size: 14px; color: var(--color-mist); cursor: pointer;
  flex-shrink: 0; transition: color 0.2s;
}

.clear-icon:hover { color: var(--color-slate-light); }

/* ===== 款式卡片网格 ===== */
.style-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.style-card {
  position: relative;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 18px;
  cursor: pointer;
  border: 2px solid transparent;
  box-shadow: var(--shadow-soft);
  transition: all 0.25s var(--ease-out);
  animation: fadeInUp 0.4s var(--ease-out) both;
  overflow: hidden;
}

.style-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card);
  border-color: var(--color-amber);
}

.style-card:hover .style-card-arrow {
  opacity: 1;
  transform: translateX(0);
}

.style-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.style-no {
  font-size: 13px;
  font-weight: 700;
  color: #6366f1;
  font-family: 'Menlo', 'Consolas', monospace;
  background: rgba(99, 102, 241, 0.08);
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  letter-spacing: 0.3px;
}

.bundle-pill {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
}

.bundle-pill.has { color: #10b981; background: rgba(16, 185, 129, 0.1); }
.bundle-pill.empty { color: var(--color-mist); background: rgba(156, 163, 175, 0.1); }

.style-card-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate);
  margin-bottom: 8px;
}

.style-card-arrow {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%) translateX(-4px);
  opacity: 0;
  color: var(--color-amber);
  font-size: 18px;
  transition: all 0.25s var(--ease-out);
}

/* ===== 顶部栏 ===== */
.qe-top-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding: 14px 20px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  animation: fadeInUp 0.3s var(--ease-out) both;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border: 2px solid var(--color-charcoal);
  border-radius: 10px;
  background: var(--bg-card);
  color: var(--color-charcoal);
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.back-btn:hover {
  border-color: var(--color-amber);
  background: var(--color-amber);
  color: #fff;
  transform: translateX(-2px);
  box-shadow: 0 4px 12px rgba(232, 168, 56, 0.25);
}

.qe-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  flex: 1;
}

.bc-item { color: var(--color-mist); }
.bc-item.active { color: var(--color-slate); font-weight: 600; }
.bc-sep { font-size: 12px; color: var(--color-mist); }

/* ===== 录入布局 ===== */
.entry-layout {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 20px;
  align-items: start;
  animation: fadeInUp 0.4s var(--ease-out) 0.1s both;
}

@media (max-width: 900px) {
  .entry-layout { grid-template-columns: 1fr; }
}

/* ===== 左侧表单面板 ===== */
.form-panel {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
  position: sticky;
  top: 76px;
}

.form-panel-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #1e2328 0%, #282e35 100%);
  color: #fff;
}

.form-panel-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(232, 168, 56, 0.2);
  color: var(--color-amber);
  flex-shrink: 0;
}

.form-panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.form-panel-header p {
  margin: 2px 0 0;
  font-size: 12px;
  color: rgba(255,255,255,0.5);
}

.form-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-slate-light);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.required { color: #ef4444; }

.field-input {
  height: 40px;
  padding: 0 12px;
  border: 1.5px solid #e8e4de;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-slate);
  background: var(--bg-card);
  outline: none;
  transition: all 0.2s;
  width: 100%;
}

.field-input:focus {
  border-color: var(--color-amber);
  box-shadow: 0 0 0 3px rgba(232, 168, 56, 0.1);
}

.field-input::placeholder { color: #c0b8ac; }

.field-select {
  height: 40px;
  padding: 0 12px;
  border: 1.5px solid #e8e4de;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-slate);
  background: var(--bg-card);
  outline: none;
  transition: all 0.2s;
  width: 100%;
  cursor: pointer;
  -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath d='M3 5l3 3 3-3' stroke='%2394a3b8' stroke-width='1.5' fill='none' stroke-linecap='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
}

.field-select:focus {
  border-color: var(--color-amber);
  box-shadow: 0 0 0 3px rgba(232, 168, 56, 0.1);
}

.size-select-wrap {
  display: flex;
  gap: 8px;
}

.size-custom {
  flex: 1;
}

.bundle-mode-group {
  display: flex;
  gap: 8px;
}

.bundle-mode-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  border: 2px solid #e8e4de;
  border-radius: var(--radius-md);
  background: var(--bg-card);
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.bundle-mode-btn:hover {
  border-color: var(--color-amber);
  background: rgba(232, 168, 56, 0.04);
}

.bundle-mode-btn.active {
  border-color: var(--color-amber);
  background: rgba(232, 168, 56, 0.08);
  box-shadow: 0 0 0 3px rgba(232, 168, 56, 0.1);
}

.bm-icon {
  font-size: 20px;
  line-height: 1;
}

.bm-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-slate-light);
}

.bundle-mode-btn.active .bm-label {
  color: var(--color-amber-dark);
}

.form-actions {
  padding: 16px 24px 20px;
  display: flex;
  gap: 10px;
  border-top: 1px solid #f0eeea;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 42px;
  padding: 0 20px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  flex: 1;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: var(--color-amber);
  color: #fff;
  box-shadow: 0 2px 8px rgba(232, 168, 56, 0.3);
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-amber-dark);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(232, 168, 56, 0.35);
}

.btn-outline {
  background: var(--bg-card);
  color: var(--color-slate);
  border: 1.5px solid #e8e4de;
}

.btn-outline:hover:not(:disabled) {
  border-color: var(--color-amber);
  color: var(--color-amber-dark);
  background: rgba(232, 168, 56, 0.04);
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 右侧列表面板 ===== */
.list-panel {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.list-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid #f0eeea;
}

.list-panel-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate);
}

.list-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 24px;
  text-align: center;
}

.empty-illustration {
  width: 120px;
  height: 80px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.list-empty p {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate-light);
}

.list-empty span {
  font-size: 12px;
  color: var(--color-mist);
  margin-top: 4px;
}

.entry-list {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: calc(100vh - 240px);
  overflow-y: auto;
}

.entry-card {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1.5px solid #f0eeea;
  transition: all 0.2s;
  animation: fadeInUp 0.3s var(--ease-out) both;
}

.entry-card:hover {
  border-color: var(--color-cream-dark);
  box-shadow: var(--shadow-soft);
}

.ec-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.ec-bed-no {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-charcoal);
}

.ec-actions {
  display: flex;
  gap: 4px;
}

.ec-print-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid #e8e4de;
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--color-slate-light);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.ec-print-btn:hover {
  border-color: var(--color-amber);
  color: var(--color-amber);
  background: rgba(232, 168, 56, 0.06);
}

.ec-detail {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-slate-light);
  margin-bottom: 8px;
}

.ec-name { font-weight: 500; }
.ec-sep { color: #d1cdc7; }
.ec-qty { font-variant-numeric: tabular-nums; }
.ec-size { font-variant-numeric: tabular-nums; }

.ec-bottom {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ec-color {
  font-size: 12px;
  color: var(--color-mist);
}

.ec-bundle-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
}

.ec-bundle-tag.empty {
  color: var(--color-mist);
  background: rgba(156, 163, 175, 0.1);
}

.entry-item-enter-active {
  transition: all 0.3s var(--ease-out);
}

.entry-item-leave-active {
  transition: all 0.2s ease-in;
}

.entry-item-enter-from {
  opacity: 0;
  transform: translateX(-12px);
}

.entry-item-leave-to {
  opacity: 0;
  transform: translateX(12px);
}

/* ===== AI 批量录入模式 ===== */
.auth-alert {
  margin-bottom: 20px;
  animation: fadeInUp 0.4s var(--ease-out) 0.1s both;
}

.ai-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  animation: fadeInUp 0.4s var(--ease-out) 0.15s both;
}

@media (max-width: 900px) {
  .ai-layout { grid-template-columns: 1fr; }
}

.ai-action-panel, .ai-docs-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.action-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-soft);
}

.action-card h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-slate);
}

.action-card p {
  margin: 0 0 20px;
  font-size: 14px;
  color: var(--color-mist);
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-buttons .el-button {
  width: 100%;
  height: 48px;
  font-size: 15px;
}

.example-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-soft);
}

.example-card h4 {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
}

.example-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.tab-btn {
  padding: 6px 12px;
  border: 1.5px solid #e8e4de;
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  font-size: 12px;
  font-weight: 500;
  color: var(--color-slate-light);
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.tab-btn:hover {
  border-color: var(--color-amber);
  color: var(--color-amber-dark);
}

.tab-btn.active {
  border-color: var(--color-amber);
  background: rgba(232, 168, 56, 0.08);
  color: var(--color-amber-dark);
  font-weight: 600;
}

.code-block {
  padding: 14px;
  margin: 0 0 12px;
  background: #1e2328;
  color: #e8e8e8;
  border-radius: var(--radius-md);
  font-family: 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  max-height: 250px;
  overflow-y: auto;
}

.copy-example-btn {
  width: 100%;
}

.docs-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-soft);
}

.docs-card h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate);
}

.docs-card h3 .el-icon {
  color: var(--color-amber);
}

.param-table {
  border: 1px solid #e8e4de;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.param-row {
  display: grid;
  grid-template-columns: 100px 70px 50px 1fr;
  padding: 8px 12px;
  font-size: 12px;
  border-bottom: 1px solid #f0eeea;
}

.param-row:last-child {
  border-bottom: none;
}

.param-row.header {
  background: #faf9f7;
  font-weight: 600;
  color: var(--color-slate-light);
}

.col-name {
  font-family: 'Menlo', 'Consolas', monospace;
  color: var(--color-slate);
}

.col-type {
  color: #6366f1;
}

.col-required {
  color: var(--color-mist);
}

.col-required.yes {
  color: #ef4444;
  font-weight: 600;
}

.usage-list {
  margin: 0;
  padding: 0 0 0 20px;
  font-size: 13px;
  color: var(--color-slate-light);
  line-height: 2;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
