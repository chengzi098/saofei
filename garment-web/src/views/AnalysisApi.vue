<template>
  <div class="analysis-api-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-info">
        <h2 class="page-title">数据分析 API</h2>
        <p class="page-desc">点击鉴权后，直接复制链接给AI分析</p>
      </div>
      <div class="header-actions">
        <el-button type="success" @click="handleAuth">
          <el-icon><Key /></el-icon>
          一键鉴权
        </el-button>
        <el-button type="primary" plain @click="copyAllApis">
          <el-icon><CopyDocument /></el-icon>
          复制全部API
        </el-button>
      </div>
    </div>

    <!-- 鉴权状态提示 -->
    <el-alert
      v-if="isAuthenticated"
      title="鉴权成功"
      type="success"
      :closable="false"
      show-icon
      class="auth-alert"
    >
      <template #default>
        <span>Token和公司ID已自动填入，直接复制下方链接给AI即可</span>
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
        <span>请点击"一键鉴权"按钮，自动获取Token和公司ID</span>
      </template>
    </el-alert>

    <!-- API列表 -->
    <div class="api-grid">
      <div
        v-for="api in analysisApis"
        :key="api.name"
        class="api-card"
        :class="{ 'recommended': api.recommended }"
      >
        <div class="api-card-header">
          <div class="api-badge">
            <span class="api-method">{{ api.method }}</span>
            <el-tag v-if="api.recommended" type="success" size="small" effect="dark">推荐</el-tag>
          </div>
          <h3 class="api-name">{{ api.name }}</h3>
        </div>
        <p class="api-desc">{{ api.description }}</p>
        <div class="api-url-box">
          <code class="api-url">{{ getFullUrl(api.url) }}</code>
          <el-button
            type="primary"
            size="small"
            plain
            @click="copyApiUrl(api.url)"
            class="copy-btn"
          >
            <el-icon><CopyDocument /></el-icon>
            复制URL
          </el-button>
        </div>
        <div class="api-curl-box" v-if="isAuthenticated">
          <code class="api-curl">{{ getCurlCommand(api.url) }}</code>
          <el-button
            size="small"
            plain
            @click="copyText(getCurlCommand(api.url), 'cURL命令已复制，可直接在终端运行')"
            class="copy-btn"
          >
            <el-icon><CopyDocument /></el-icon>
            复制cURL
          </el-button>
        </div>
      </div>
    </div>

    <!-- 使用说明 -->
    <div class="usage-section">
      <h3 class="section-title">
        <el-icon><InfoFilled /></el-icon>
        使用说明
      </h3>
      <div class="usage-grid">
        <div class="usage-card">
          <div class="usage-step">1</div>
          <h4>一键鉴权</h4>
          <p>点击"一键鉴权"按钮，自动获取Token和公司ID</p>
        </div>
        <div class="usage-card">
          <div class="usage-step">2</div>
          <h4>复制cURL命令</h4>
          <p>点击"复制cURL"按钮，获取完整的API调用命令</p>
        </div>
        <div class="usage-card">
          <div class="usage-step">3</div>
          <h4>终端运行</h4>
          <p>在终端/命令行中粘贴运行cURL命令，获取JSON数据</p>
        </div>
        <div class="usage-card">
          <div class="usage-step">4</div>
          <h4>发给AI分析</h4>
          <p>将返回的JSON数据复制给AI，获取运营分析建议</p>
        </div>
      </div>
      <div class="usage-note">
        <el-icon><Warning /></el-icon>
        <span><strong>注意：</strong>不能直接在浏览器打开URL，必须通过请求头传递Token和公司ID。推荐使用cURL命令或Postman等工具。</span>
      </div>
    </div>

    <!-- AI提示词模板 -->
    <div class="prompt-section">
      <h3 class="section-title">
        <el-icon><ChatDotRound /></el-icon>
        AI分析提示词模板
      </h3>
      <div class="prompt-list">
        <div
          v-for="prompt in promptTemplates"
          :key="prompt.title"
          class="prompt-card"
        >
          <div class="prompt-header">
            <h4>{{ prompt.title }}</h4>
            <el-button size="small" plain @click="copyPrompt(prompt.content)">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </div>
          <pre class="prompt-content">{{ prompt.content }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, InfoFilled, ChatDotRound, Key, Warning } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

// 鉴权相关
const authData = ref({
  token: '',
  companyId: ''
})

const isAuthenticated = computed(() => {
  return authData.value.token && authData.value.companyId
})

const analysisApis = ref([
  {
    name: '综合分析报告',
    method: 'GET',
    url: '/api/analysis/report',
    description: '一次性获取所有分析数据，包括公司信息、员工绩效、产量趋势、款式进度、工序效率、工资分布等',
    recommended: true,
    params: null
  },
  {
    name: '数据摘要',
    method: 'GET',
    url: '/api/analysis/summary',
    description: '获取公司数据摘要，快速了解公司概况',
    recommended: false,
    params: null
  },
  {
    name: '员工绩效分析',
    method: 'GET',
    url: '/api/analysis/employee-performance',
    description: '获取员工绩效排名，分析员工表现',
    recommended: false,
    params: 'topN=10（返回前N名员工）'
  },
  {
    name: '产量趋势分析',
    method: 'GET',
    url: '/api/analysis/production-trend',
    description: '获取最近N天的产量趋势，分析产量变化',
    recommended: false,
    params: 'days=30（最近N天）'
  },
  {
    name: '款式进度分析',
    method: 'GET',
    url: '/api/analysis/style-progress',
    description: '获取款式完成进度，监控生产进度',
    recommended: false,
    params: null
  },
  {
    name: '工序效率分析',
    method: 'GET',
    url: '/api/analysis/process-efficiency',
    description: '获取各工序效率，优化工序配置',
    recommended: false,
    params: null
  },
  {
    name: '工资分布分析',
    method: 'GET',
    url: '/api/analysis/wage-distribution',
    description: '获取工资分布情况，分析薪酬结构',
    recommended: false,
    params: null
  },
  {
    name: '员工工作详情',
    method: 'GET',
    url: '/api/analysis/employee-work-details',
    description: '获取员工工作明细，包括床号、扎号、工序、数量、金额等详细信息',
    recommended: false,
    params: 'startDate=2026-06-01（必填）、endDate=2026-06-14（必填）、workerId=1（可选，员工ID）'
  }
])

const promptTemplates = ref([
  {
    title: '综合分析（推荐）',
    content: `请分析以下制衣厂数据，并给出：
1. 公司整体运营状况评估（优秀/良好/一般/较差）
2. 员工绩效分析：哪些员工表现优秀，哪些需要改进
3. 款式进度分析：哪些款式进度滞后，可能的原因
4. 工资结构分析：工资分布是否合理，是否需要调整
5. 产能瓶颈识别：当前最大的生产瓶颈是什么
6. 改进建议：针对发现的问题，提出3-5条具体可行的改进建议

数据如下：
[粘贴API返回的JSON数据]`
  },
  {
    title: '员工绩效分析',
    content: `请分析以下员工绩效数据，并给出：
1. Top 3 高绩效员工的共同特征
2. 低绩效员工的改进方案
3. 激励机制设计建议
4. 培训需求分析

数据如下：
[粘贴API返回的JSON数据]`
  },
  {
    title: '产量趋势分析',
    content: `请分析以下产量趋势数据，并给出：
1. 产量波动的主要原因
2. 未来7天的产量预测
3. 生产计划优化建议
4. 产能提升方案

数据如下：
[粘贴API返回的JSON数据]`
  },
  {
    title: '工资分布分析',
    content: `请分析以下工资分布数据，并给出：
1. 工资结构是否合理
2. 高工资和低工资员工的特征
3. 薪酬激励效果评估
4. 成本控制建议

数据如下：
[粘贴API返回的JSON数据]`
  }
])

function copyApiUrl(url) {
  const fullUrl = getFullUrl(url)
  copyToClipboard(fullUrl, 'API链接已复制到剪贴板')
}

function copyAllApis() {
  if (!isAuthenticated.value) {
    ElMessage.warning('请先点击"一键鉴权"按钮')
    return
  }

  const allApis = `=== 制衣厂数据分析API ===

鉴权信息（请求头）：
- Authorization: Bearer ${authData.value.token}
- X-Company-Id: ${authData.value.companyId}

=== API列表 ===

${analysisApis.value.map(api => {
  return `【${api.name}】
用途：${api.description}
URL：${getFullUrl(api.url)}
cURL命令：${getCurlCommand(api.url)}`
}).join('\n\n')}

=== 使用说明 ===
1. 复制上方cURL命令，直接在终端运行即可获取数据
2. 或使用Postman等工具，设置Authorization和X-Company-Id请求头
3. 将返回的JSON数据发给AI，即可获得分析建议

注意：不能直接在浏览器打开URL，需要通过请求头传递Token和公司ID。

推荐使用"综合分析报告"接口，一次获取所有数据。`

  copyToClipboard(allApis, '全部API信息已复制，可直接发给AI分析')
}

function copyPrompt(content) {
  copyToClipboard(content, '提示词已复制到剪贴板')
}

function copyToClipboard(text, message) {
  // 先显示提示
  ElMessage.success(message)

  // 然后尝试复制
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).catch(() => {
        // 降级方案
        fallbackCopy(text)
      })
    } else {
      // 降级方案
      fallbackCopy(text)
    }
  } catch (err) {
    // 降级方案
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

function handleAuth() {
  // 从localStorage获取Token（正确的key是garment_token）
  const token = localStorage.getItem('garment_token')
  // 从userStore获取公司ID
  const companyId = userStore.currentCompanyId

  if (!token) {
    ElMessage.warning('未找到Token，请先登录')
    return
  }

  if (!companyId) {
    ElMessage.warning('未找到公司ID，请先选择公司')
    return
  }

  authData.value.token = token
  authData.value.companyId = companyId.toString()
  ElMessage.success('鉴权成功，现在可以直接复制链接给AI')
}

function getBaseUrl() {
  // 获取后端API地址
  // 开发环境：当前IP + 8080端口
  // 生产环境：当前域名（无端口或80端口）
  const isDev = window.location.port === '5173'
  if (isDev) {
    // 开发环境，使用当前访问的IP地址，端口改为8080
    const hostname = window.location.hostname
    return `http://${hostname}:8080`
  }
  return window.location.origin
}

function getFullUrl(url) {
  const baseUrl = getBaseUrl()
  return baseUrl + url
}

function getCurlCommand(url) {
  if (!isAuthenticated.value) return ''

  // 为需要参数的接口添加默认参数
  let fullUrl = getFullUrl(url)
  if (url === '/api/analysis/employee-work-details') {
    // 获取最近7天的日期范围
    const endDate = new Date().toISOString().split('T')[0]
    const startDate = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
    fullUrl += `?startDate=${startDate}&endDate=${endDate}`
  }

  return `curl -X GET "${fullUrl}" -H "Authorization: Bearer ${authData.value.token}" -H "X-Company-Id: ${authData.value.companyId}"`
}

function copyText(text, message) {
  if (!text) {
    ElMessage.warning('内容为空')
    return
  }
  copyToClipboard(text, message)
}
</script>

<style scoped>
.analysis-api-page {
  max-width: 1200px;
}

/* ===== 页面标题 ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding: 24px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-slate);
  margin-bottom: 4px;
}

.page-desc {
  font-size: 14px;
  color: var(--color-mist);
}

/* ===== API网格 ===== */
.api-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.api-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-soft);
  transition: all 0.2s var(--ease-out);
  border: 2px solid transparent;
}

.api-card:hover {
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}

.api-card.recommended {
  border-color: var(--color-success);
  background: linear-gradient(135deg, #fff, #f0fdf4);
}

.api-card-header {
  margin-bottom: 12px;
}

.api-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.api-method {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--color-success);
  padding: 3px 10px;
  border-radius: 4px;
}

.api-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-slate);
}

.api-desc {
  font-size: 13px;
  color: var(--color-mist);
  line-height: 1.6;
  margin-bottom: 16px;
}

.api-url-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-cream);
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
}

.api-url {
  flex: 1;
  font-size: 12px;
  color: var(--color-amber);
  font-family: monospace;
  word-break: break-all;
}

.api-curl-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(74, 222, 128, 0.08);
  border-radius: var(--radius-sm);
  border: 1px solid rgba(74, 222, 128, 0.2);
}

.api-curl {
  flex: 1;
  font-size: 11px;
  color: var(--color-success);
  font-family: monospace;
  word-break: break-all;
}

.copy-btn {
  flex-shrink: 0;
}

.auth-alert {
  margin-bottom: 20px;
}

.api-params {
  font-size: 12px;
  color: var(--color-mist);
}

.params-label {
  font-weight: 500;
}

.params-value {
  color: var(--color-info);
}

/* ===== 使用说明 ===== */
.usage-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-soft);
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-slate);
  margin-bottom: 20px;
}

.section-title .el-icon {
  color: var(--color-amber);
}

.usage-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.usage-card {
  padding: 20px;
  background: var(--color-cream);
  border-radius: var(--radius-md);
  text-align: center;
}

.usage-step {
  width: 32px;
  height: 32px;
  margin: 0 auto 12px;
  background: var(--color-amber);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.usage-card h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
  margin-bottom: 8px;
}

.usage-card p {
  font-size: 12px;
  color: var(--color-mist);
  line-height: 1.6;
}

.usage-card code {
  font-size: 11px;
  background: rgba(232, 168, 56, 0.1);
  color: var(--color-amber);
  padding: 2px 6px;
  border-radius: 4px;
}

.usage-note {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 20px;
  padding: 14px;
  background: rgba(234, 88, 12, 0.08);
  border: 1px solid rgba(234, 88, 12, 0.2);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: #ea580c;
  line-height: 1.6;
}

.usage-note .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

/* ===== 提示词模板 ===== */
.prompt-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-soft);
}

.prompt-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.prompt-card {
  padding: 16px;
  background: var(--color-cream);
  border-radius: var(--radius-md);
}

.prompt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.prompt-header h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
}

.prompt-content {
  font-size: 13px;
  color: var(--color-slate-light);
  line-height: 1.8;
  white-space: pre-wrap;
  background: var(--bg-card);
  padding: 16px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .api-grid {
    grid-template-columns: 1fr;
  }

  .usage-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .usage-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== 鉴权弹窗 ===== */
.auth-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.auth-info {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px;
  background: rgba(96, 165, 250, 0.08);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-info);
  line-height: 1.6;
}

.auth-info .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-slate);
}

.auth-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.auth-result h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
  margin: 0;
}

.curl-example {
  background: var(--color-cream);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.curl-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: rgba(0, 0, 0, 0.03);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-slate);
}

.curl-code {
  padding: 16px;
  font-size: 12px;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  color: var(--color-slate);
  background: var(--bg-card);
  margin: 0;
  overflow-x: auto;
  white-space: pre;
  line-height: 1.6;
}

.header-actions {
  display: flex;
  gap: 12px;
}
</style>
