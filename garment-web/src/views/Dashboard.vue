<template>
  <div class="dashboard">
    <!-- 欢迎区 -->
    <div class="welcome-section" :class="{ 'entered': mounted }">
      <div class="welcome-text">
        <h2 class="welcome-title">{{ greeting }}，{{ userName }}</h2>
        <p class="welcome-sub">今天是 {{ todayStr }} {{ weekDay }}</p>
      </div>
      <div class="welcome-deco">
        <svg viewBox="0 0 120 80" fill="none" width="120">
          <path d="M10 70 Q30 20 60 40 T110 30" stroke="var(--color-amber)" stroke-width="2.5" stroke-linecap="round" fill="none" opacity="0.3"/>
          <path d="M10 70 Q40 30 70 50 T110 40" stroke="var(--color-amber)" stroke-width="1.5" stroke-linecap="round" fill="none" opacity="0.15"/>
          <circle cx="60" cy="40" r="4" fill="var(--color-amber)" opacity="0.5"/>
          <circle cx="90" cy="32" r="3" fill="var(--color-terracotta)" opacity="0.4"/>
        </svg>
      </div>
    </div>

    <!-- 老板专属：到期时间提醒 -->
    <div v-if="isBoss && bossData.expireTime" class="expire-banner" :class="[expireStatus, { 'entered': mounted }]">
      <div class="expire-icon">
        <el-icon :size="24"><Timer /></el-icon>
      </div>
      <div class="expire-info">
        <span class="expire-label">系统到期时间</span>
        <span class="expire-date">{{ formatExpireDate(bossData.expireTime) }}</span>
      </div>
      <div class="expire-tag">
        {{ expireText }}
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div
        v-for="(stat, index) in currentStats"
        :key="stat.label"
        class="stat-card"
        :style="{ animationDelay: `${0.15 + index * 0.08}s` }"
      >
        <div class="stat-header">
          <div class="stat-icon" :style="{ background: stat.iconBg }">
            <el-icon :size="20" :color="stat.iconColor">
              <component :is="stat.icon" />
            </el-icon>
          </div>
          <span class="stat-trend" :class="stat.trend">{{ stat.trendLabel }}</span>
        </div>
        <div class="stat-value">{{ stat.value }}</div>
        <div class="stat-label">{{ stat.label }}</div>
        <div class="stat-bar">
          <div class="stat-bar-fill" :style="{ width: stat.barWidth, background: stat.barColor }"></div>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="section-card quick-actions" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">快捷操作</span>
      </div>
      <div class="actions-grid">
        <div
          v-for="action in currentActions"
          :key="action.label"
          class="action-item"
          @click="action.handler ? action.handler() : (action.route && $router.push(action.route))"
        >
          <div class="action-icon" :style="{ background: action.bg }">
            <el-icon :size="22" :color="action.color">
              <component :is="action.icon" />
            </el-icon>
          </div>
          <span class="action-label">{{ action.label }}</span>
        </div>
      </div>
    </div>

    <!-- 主管专属：我的款式进度 -->
    <div v-if="isSupervisor && supervisorData.myStyles && supervisorData.myStyles.length" class="section-card" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">我的款式进度</span>
        <span class="section-sub">我创建的扎货所在款式</span>
      </div>
      <div class="style-progress-list">
        <div
          v-for="style in supervisorData.myStyles"
          :key="style.styleId"
          class="style-progress-item"
          @click="$router.push('/dashboard-board')"
        >
          <div class="style-progress-info">
            <span class="style-progress-name">{{ style.styleNo }}</span>
            <span class="style-progress-count">{{ style.myBundleCount }}/{{ style.totalBundleCount }} 扎</span>
          </div>
          <div class="style-progress-bar">
            <div
              class="style-progress-bar-fill"
              :style="{ width: style.progressPercent + '%' }"
              :class="{ 'completed': style.progressPercent === 100 }"
            ></div>
          </div>
          <span class="style-progress-percent">{{ style.progressPercent }}%</span>
        </div>
      </div>
    </div>

    <!-- 主管专属：最近报工 -->
    <div v-if="isSupervisor && supervisorData.recentReports && supervisorData.recentReports.length" class="section-card" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">最近报工</span>
        <span class="section-sub" @click="$router.push('/work-records')" style="cursor:pointer;color:var(--color-amber)">查看全部 →</span>
      </div>
      <div class="recent-report-list">
        <div
          v-for="report in supervisorData.recentReports"
          :key="report.id"
          class="recent-report-item"
        >
          <div class="report-main">
            <span class="report-worker">{{ report.workerName }}</span>
            <span class="report-process">{{ report.processName }}</span>
            <span class="report-bundle">{{ report.bundleNo }}</span>
          </div>
          <div class="report-meta">
            <span class="report-qty">{{ report.quantity }} 件</span>
            <span class="report-amount" v-if="report.amount">{{ report.amount }}元</span>
            <span class="report-time">{{ report.createTime }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 老板专属：公司信息 -->
    <div v-if="isBoss && companyInfo" class="section-card company-info-card" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">公司信息</span>
        <span class="section-sub">{{ companyInfo.companyName }}</span>
      </div>
      <div class="company-info-grid">
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><OfficeBuilding /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">公司名称</span>
            <span class="company-info-value">{{ companyInfo.companyName }}</span>
          </div>
        </div>
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><User /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">联系人</span>
            <span class="company-info-value">{{ companyInfo.contactPerson || '-' }}</span>
          </div>
        </div>
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><Phone /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">联系电话</span>
            <span class="company-info-value">{{ companyInfo.contactPhone || '-' }}</span>
          </div>
        </div>
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><Location /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">地址</span>
            <span class="company-info-value">{{ companyInfo.address || '-' }}</span>
          </div>
        </div>
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><Avatar /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">员工数量</span>
            <span class="company-info-value">{{ companyInfo.workerCount }} 人</span>
          </div>
        </div>
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">主管数量</span>
            <span class="company-info-value">{{ companyInfo.supervisorCount }} 人</span>
          </div>
        </div>
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">款式数量</span>
            <span class="company-info-value">{{ companyInfo.styleCount }} 款</span>
          </div>
        </div>
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><Grid /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">床次总数</span>
            <span class="company-info-value">{{ companyInfo.bedCount }} 床</span>
          </div>
        </div>
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">本月产量</span>
            <span class="company-info-value">{{ companyInfo.monthQuantity }} 件</span>
          </div>
        </div>
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><Money /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">本月工资</span>
            <span class="company-info-value">{{ companyInfo.monthAmount }} 元</span>
          </div>
        </div>
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">到期时间</span>
            <span class="company-info-value">{{ formatExpireDate(companyInfo.expireTime) }}</span>
          </div>
        </div>
        <div class="company-info-item">
          <div class="company-info-icon">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="company-info-content">
            <span class="company-info-label">状态</span>
            <span class="company-info-value" :class="{ 'status-active': companyInfo.status === 1, 'status-disabled': companyInfo.status === 0 }">
              {{ companyInfo.status === 1 ? '正常' : '禁用' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 老板专属：数据分析API -->
    <div v-if="isBoss" class="section-card api-card" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">
          <el-icon><Link /></el-icon>
          数据分析API
        </span>
        <span class="section-sub">复制链接发给AI进行分析</span>
      </div>
      <div class="api-list">
        <div
          v-for="api in analysisApis"
          :key="api.name"
          class="api-item"
        >
          <div class="api-info">
            <div class="api-header">
              <span class="api-method">{{ api.method }}</span>
              <span class="api-name">{{ api.name }}</span>
              <el-tag v-if="api.recommended" type="success" size="small" class="api-tag">推荐</el-tag>
            </div>
            <span class="api-desc">{{ api.description }}</span>
            <span class="api-url">{{ api.url }}</span>
          </div>
          <el-button
            type="primary"
            size="small"
            plain
            @click="copyApiUrl(api.url)"
            class="copy-btn"
          >
            <el-icon><CopyDocument /></el-icon>
            复制
          </el-button>
        </div>
      </div>
      <div class="api-tip">
        <el-icon><InfoFilled /></el-icon>
        <span>将API链接复制后，配合Token和公司ID调用，获取数据发给AI分析</span>
      </div>
    </div>

    <!-- 老板专属：全公司款式进度 -->
    <div v-if="isBoss && bossData.styles && bossData.styles.length" class="section-card" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">全公司款式进度</span>
        <span class="section-sub" @click="$router.push('/dashboard-board')" style="cursor:pointer;color:var(--color-amber)">工作看板 →</span>
      </div>
      <div class="style-progress-list">
        <div
          v-for="style in bossData.styles"
          :key="style.styleId"
          class="style-progress-item"
          @click="$router.push('/dashboard-board')"
        >
          <div class="style-progress-info">
            <span class="style-progress-name">{{ style.styleNo }}</span>
            <span class="style-progress-count">{{ style.totalBundleCount }} 扎</span>
          </div>
          <div class="style-progress-bar">
            <div
              class="style-progress-bar-fill"
              :style="{ width: style.progressPercent + '%' }"
              :class="{ 'completed': style.progressPercent === 100 }"
            ></div>
          </div>
          <span class="style-progress-percent">{{ style.progressPercent }}%</span>
        </div>
      </div>
    </div>

    <!-- ===== 超级管理员专属区域 ===== -->

    <!-- 活跃公司排名 -->
    <div v-if="isAdmin && adminData.activeCompanies && adminData.activeCompanies.length" class="section-card" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">活跃公司排名</span>
        <span class="section-sub">本月产值 Top {{ adminData.activeCompanies.length }}</span>
      </div>
      <div class="active-company-list">
        <div
          v-for="(company, index) in adminData.activeCompanies"
          :key="company.id"
          class="active-company-item"
        >
          <div class="company-rank" :class="{ 'rank-top': index < 3 }">{{ index + 1 }}</div>
          <div class="company-info">
            <span class="company-name">{{ company.name }}</span>
            <span class="company-meta">{{ company.employeeCount }} 人 · {{ company.styleCount }} 款</span>
          </div>
          <div class="company-amount">
            <span class="amount-value">{{ company.monthAmount }}元</span>
          </div>
          <el-button type="primary" size="small" plain class="enter-btn" @click="enterCompanyFromDashboard(company)">
            进入查看
          </el-button>
        </div>
      </div>
    </div>

    <!-- 即将到期提醒 -->
    <div v-if="isAdmin && adminData.expiringCompanies && adminData.expiringCompanies.length" class="section-card expire-card" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">
          <el-icon><Timer /></el-icon>
          即将到期提醒
        </span>
        <span class="section-sub">{{ adminData.expiringCompanies.length }} 家公司30天内到期</span>
      </div>
      <div class="expire-company-list">
        <div
          v-for="company in adminData.expiringCompanies"
          :key="company.id"
          class="expire-company-item"
          :class="{ 'urgent': company.remainingDays <= 7 }"
        >
          <div class="expire-company-name">{{ company.name }}</div>
          <div class="expire-company-date">{{ formatExpireDate(company.expireTime) }}</div>
          <div class="expire-company-tag" :class="{ 'urgent': company.remainingDays <= 7 }">
            剩余 {{ company.remainingDays }} 天
          </div>
        </div>
      </div>
    </div>

    <!-- 产量趋势图 -->
    <div v-if="isAdmin" class="section-card chart-card" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">近6个月产值趋势</span>
        <span class="section-sub">全局报工产值统计</span>
      </div>
      <div ref="trendChartRef" class="chart-container"></div>
    </div>

    <!-- 公司员工分布 -->
    <div v-if="isAdmin && adminData.companyEmployeeStats && adminData.companyEmployeeStats.length" class="section-card chart-card" :class="{ 'entered': mounted }">
      <div class="section-header">
        <span class="section-title">公司员工分布</span>
        <span class="section-sub">员工数 Top 10</span>
      </div>
      <div ref="employeeChartRef" class="chart-container"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { getCompanyList, getCompanyInfo } from '../api/company'
import { getSupervisorStats, getBossStats, getAdminStats } from '../api/dashboard'
import * as echarts from 'echarts'
import { useThemeStore } from '../stores/theme'
import { getEchartsThemeOpts } from '../utils/echartsTheme'
import {
  Document, Timer, User, Warning,
  Plus, Search, DataLine, Setting,
  OfficeBuilding, Avatar,
  Checked, View, Grid,
  Money, Phone, Location,
  UserFilled, CircleCheck,
  Link, CopyDocument, InfoFilled
} from '@element-plus/icons-vue'

const router = useRouter()

const userStore = useUserStore()
const mounted = ref(false)
const companyCount = ref(0)
const trendChartRef = ref(null)
const employeeChartRef = ref(null)
let trendChart = null
let employeeChart = null

const supervisorData = ref({
  myBundleCount: 0,
  myTotalQuantity: 0,
  todayReportCount: 0,
  todayAmount: 0,
  activeStyleCount: 0,
  returnCount: 0,
  myStyles: [],
  recentReports: []
})

const bossData = ref({
  expireTime: null,
  todayAmount: 0,
  todayReportCount: 0,
  monthAmount: 0,
  activeEmployeeCount: 0,
  totalEmployeeCount: 0,
  activeStyleCount: 0,
  returnCount: 0,
  styles: []
})

const companyInfo = ref(null)

const analysisApis = ref([
  {
    name: '综合分析报告',
    method: 'GET',
    url: '/api/analysis/report',
    description: '一次性获取所有分析数据，最适合AI分析',
    recommended: true
  },
  {
    name: '数据摘要',
    method: 'GET',
    url: '/api/analysis/summary',
    description: '获取公司数据摘要，快速了解概况',
    recommended: false
  },
  {
    name: '员工绩效分析',
    method: 'GET',
    url: '/api/analysis/employee-performance?topN=10',
    description: '获取员工绩效排名，分析员工表现',
    recommended: false
  },
  {
    name: '产量趋势分析',
    method: 'GET',
    url: '/api/analysis/production-trend?days=30',
    description: '获取最近30天产量趋势，分析产量变化',
    recommended: false
  },
  {
    name: '款式进度分析',
    method: 'GET',
    url: '/api/analysis/style-progress',
    description: '获取款式完成进度，监控生产进度',
    recommended: false
  },
  {
    name: '工序效率分析',
    method: 'GET',
    url: '/api/analysis/process-efficiency',
    description: '获取各工序效率，优化工序配置',
    recommended: false
  },
  {
    name: '工资分布分析',
    method: 'GET',
    url: '/api/analysis/wage-distribution',
    description: '获取工资分布情况，分析薪酬结构',
    recommended: false
  }
])

const adminData = ref({
  totalCompanyCount: 0,
  activeCompanyCount: 0,
  disabledCompanyCount: 0,
  totalEmployeeCount: 0,
  totalStyleCount: 0,
  monthAmount: 0,
  todayReportCount: 0,
  todayAmount: 0,
  newCompanyCount: 0,
  expiringCompanies: [],
  activeCompanies: [],
  totalBundleCount: 0,
  totalWorkRecordCount: 0,
  monthTrends: [],
  companyEmployeeStats: []
})

onMounted(() => {
  requestAnimationFrame(() => { mounted.value = true })
  if (userStore.isSuperAdmin() && !userStore.isPenetrating()) {
    loadAdminStats()
  } else if (userStore.isSupervisor()) {
    loadSupervisorStats()
  } else if (userStore.isBoss()) {
    loadBossStats()
    loadCompanyInfo()
  }

  window.addEventListener('resize', handleResize)
})

function handleResize() {
  if (trendChart) {
    trendChart.resize()
  }
  if (employeeChart) {
    employeeChart.resize()
  }
}

async function loadAdminStats() {
  try {
    const res = await getAdminStats()
    if (res.code === 200 && res.data) {
      adminData.value = res.data
      await nextTick()
      initCharts()
    }
  } catch (err) {
    console.error('获取全局统计失败:', err)
  }
}

function initCharts() {
  initTrendChart()
  initEmployeeChart()
}

// 主题切换时重渲染图表，让坐标轴/文字色跟随主题
const themeStore = useThemeStore()
watch(() => themeStore.mode, () => {
  nextTick(() => initCharts())
})

function initTrendChart() {
  if (!trendChartRef.value || !adminData.value.monthTrends || adminData.value.monthTrends.length === 0) return

  if (trendChart) {
    trendChart.dispose()
  }
  trendChart = echarts.init(trendChartRef.value)

  const xData = adminData.value.monthTrends.map(t => t.yearMonth)
  const yData = adminData.value.monthTrends.map(t => t.amount || 0)
  const t = getEchartsThemeOpts()

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>产值：{c}元'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: {
        color: t.xAxis.axisLabel.color
      },
      axisLine: {
        lineStyle: {
          color: t.xAxis.axisLine.lineStyle.color
        }
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: t.yAxis.axisLabel.color,
        formatter: '{value}元'
      },
      splitLine: {
        lineStyle: {
          color: t.yAxis.splitLine.lineStyle.color
        }
      }
    },
    series: [{
      data: yData,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: {
        color: '#e8a838',
        width: 3
      },
      itemStyle: {
        color: '#e8a838',
        borderColor: '#fff',
        borderWidth: 2
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [{
            offset: 0,
            color: 'rgba(232, 168, 56, 0.3)'
          }, {
            offset: 1,
            color: 'rgba(232, 168, 56, 0.05)'
          }]
        }
      }
    }]
  })
}

function initEmployeeChart() {
  if (!employeeChartRef.value || !adminData.value.companyEmployeeStats || adminData.value.companyEmployeeStats.length === 0) return

  if (employeeChart) {
    employeeChart.dispose()
  }
  employeeChart = echarts.init(employeeChartRef.value)

  const xData = adminData.value.companyEmployeeStats.map(s => s.companyName)
  const employeeData = adminData.value.companyEmployeeStats.map(s => s.employeeCount)
  const amountData = adminData.value.companyEmployeeStats.map(s => s.monthAmount || 0)
  const t = getEchartsThemeOpts()

  employeeChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['员工数', '本月产值'],
      top: 0,
      right: 0,
      textStyle: {
        color: t.legend.textStyle.color
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: {
        color: t.xAxis.axisLabel.color,
        rotate: 30,
        fontSize: 11
      },
      axisLine: {
        lineStyle: {
          color: t.xAxis.axisLine.lineStyle.color
        }
      }
    },
    yAxis: [{
      type: 'value',
      name: '人数',
      axisLabel: {
        color: t.yAxis.axisLabel.color
      },
      splitLine: {
        lineStyle: {
          color: t.yAxis.splitLine.lineStyle.color
        }
      }
    }, {
      type: 'value',
      name: '产值(元)',
      axisLabel: {
        color: t.yAxis.axisLabel.color
      },
      splitLine: {
        show: false
      }
    }],
    series: [{
      name: '员工数',
      type: 'bar',
      barWidth: '40%',
      data: employeeData,
      itemStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [{
            offset: 0,
            color: '#60a5fa'
          }, {
            offset: 1,
            color: '#3b82f6'
          }]
        },
        borderRadius: [4, 4, 0, 0]
      }
    }, {
      name: '本月产值',
      type: 'line',
      yAxisIndex: 1,
      data: amountData,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        color: '#e8a838',
        width: 2
      },
      itemStyle: {
        color: '#e8a838'
      }
    }]
  })
}

async function loadCompanyCount() {
  try {
    const res = await getCompanyList({ pageNum: 1, pageSize: 1 })
    companyCount.value = res.data.total || 0
  } catch (err) {
    console.error('获取公司数量失败:', err)
  }
}

async function loadSupervisorStats() {
  try {
    const res = await getSupervisorStats()
    supervisorData.value = res.data
  } catch (err) {
    console.error('获取主管统计失败:', err)
  }
}

async function loadBossStats() {
  try {
    const res = await getBossStats()
    bossData.value = res.data
  } catch (err) {
    console.error('获取老板统计失败:', err)
  }
}

async function loadCompanyInfo() {
  try {
    const res = await getCompanyInfo()
    if (res.code === 200 && res.data) {
      companyInfo.value = res.data
    }
  } catch (err) {
    console.error('获取公司信息失败:', err)
  }
}

function copyApiUrl(url) {
  const baseUrl = window.location.origin
  const fullUrl = baseUrl + url
  navigator.clipboard.writeText(fullUrl).then(() => {
    ElMessage.success('API链接已复制到剪贴板')
  }).catch(() => {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = fullUrl
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('API链接已复制到剪贴板')
  })
}

const userName = computed(() => userStore.realName || userStore.username || '用户')
const isAdmin = computed(() => userStore.isSuperAdmin() && !userStore.isPenetrating())
const isSupervisor = computed(() => userStore.isSupervisor())
const isBoss = computed(() => userStore.isBoss())
const isPenetrating = computed(() => userStore.isPenetrating())

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

const weekDay = computed(() => {
  const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return days[new Date().getDay()]
})

// 到期时间状态：expired=已过期, urgent=1个月内, warning=3个月内, safe=安全
const expireStatus = computed(() => {
  if (!bossData.value.expireTime) return ''
  const now = new Date()
  const expire = new Date(bossData.value.expireTime)
  const diff = expire - now
  if (diff < 0) return 'expired'
  if (diff < 30 * 24 * 60 * 60 * 1000) return 'urgent'
  if (diff < 90 * 24 * 60 * 60 * 1000) return 'warning'
  return 'safe'
})

const expireText = computed(() => {
  if (!bossData.value.expireTime) return ''
  const now = new Date()
  const expire = new Date(bossData.value.expireTime)
  const diff = expire - now
  if (diff < 0) return '已过期'
  const days = Math.ceil(diff / (24 * 60 * 60 * 1000))
  if (days <= 30) return `剩余 ${days} 天`
  if (days <= 90) return `剩余 ${days} 天`
  return `剩余 ${days} 天`
})

function formatExpireDate(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

// ===== 管理员统计 =====
const adminStats = computed(() => {
  const d = adminData.value
  return [
    {
      label: '入驻公司',
      value: d.totalCompanyCount || 0,
      icon: OfficeBuilding,
      iconBg: 'rgba(232, 168, 56, 0.1)',
      iconColor: 'var(--color-amber)',
      trend: 'up',
      trendLabel: `${d.activeCompanyCount || 0} 家正常`,
      barWidth: d.totalCompanyCount > 0 ? `${(d.activeCompanyCount / d.totalCompanyCount * 100).toFixed(0)}%` : '0%',
      barColor: 'var(--color-amber)'
    },
    {
      label: '总员工数',
      value: d.totalEmployeeCount || 0,
      icon: Avatar,
      iconBg: 'rgba(96, 165, 250, 0.1)',
      iconColor: 'var(--color-info)',
      trend: 'neutral',
      trendLabel: '人',
      barWidth: '100%',
      barColor: 'var(--color-info)'
    },
  ]
})

// ===== 主管统计 =====
const supervisorStats = computed(() => {
  const d = supervisorData.value
  return [
    {
      label: '我的扎货',
      value: d.myBundleCount || 0,
      icon: Grid,
      iconBg: 'rgba(232, 168, 56, 0.1)',
      iconColor: 'var(--color-amber)',
      trend: 'up',
      trendLabel: `${d.myTotalQuantity || 0} 件`,
      barWidth: d.myBundleCount > 0 ? '100%' : '0%',
      barColor: 'var(--color-amber)'
    },
    {
      label: '今日报工',
      value: d.todayReportCount || 0,
      icon: Timer,
      iconBg: 'rgba(96, 165, 250, 0.1)',
      iconColor: 'var(--color-info)',
      trend: 'up',
      trendLabel: d.todayAmount ? `${d.todayAmount}元` : '条',
      barWidth: d.todayReportCount > 0 ? '100%' : '0%',
      barColor: 'var(--color-info)'
    },
    {
      label: '进行中款式',
      value: d.activeStyleCount || 0,
      icon: Document,
      iconBg: 'rgba(74, 222, 128, 0.1)',
      iconColor: 'var(--color-success)',
      trend: 'neutral',
      trendLabel: '款',
      barWidth: d.activeStyleCount > 0 ? '100%' : '0%',
      barColor: 'var(--color-success)'
    },
    {
      label: '退回记录',
      value: d.returnCount || 0,
      icon: Warning,
      iconBg: d.returnCount > 0 ? 'rgba(248, 113, 113, 0.1)' : 'rgba(74, 222, 128, 0.1)',
      iconColor: d.returnCount > 0 ? 'var(--color-danger)' : 'var(--color-success)',
      trend: d.returnCount > 0 ? 'down' : 'neutral',
      trendLabel: d.returnCount > 0 ? '需关注' : '正常',
      barWidth: d.returnCount > 0 ? '100%' : '0%',
      barColor: d.returnCount > 0 ? 'var(--color-danger)' : 'var(--color-success)'
    }
  ]
})

// ===== 老板统计 =====
const bossStats = computed(() => {
  const d = bossData.value
  return [
    {
      label: '本月产值',
      value: d.monthAmount ? `${d.monthAmount}元` : '0元',
      icon: DataLine,
      iconBg: 'rgba(232, 168, 56, 0.1)',
      iconColor: 'var(--color-amber)',
      trend: 'up',
      trendLabel: '累计',
      barWidth: d.monthAmount > 0 ? '100%' : '0%',
      barColor: 'var(--color-amber)'
    },
    {
      label: '本周产值',
      value: d.weekAmount ? `${d.weekAmount}元` : '0元',
      icon: Money,
      iconBg: 'rgba(96, 165, 250, 0.1)',
      iconColor: 'var(--color-info)',
      trend: 'up',
      trendLabel: '本周',
      barWidth: d.weekAmount > 0 ? '100%' : '0%',
      barColor: 'var(--color-info)'
    },
    {
      label: '本日产值',
      value: d.todayAmount ? `${d.todayAmount}元` : '0元',
      icon: Money,
      iconBg: 'rgba(74, 222, 128, 0.1)',
      iconColor: 'var(--color-success)',
      trend: 'up',
      trendLabel: d.todayReportCount ? `${d.todayReportCount} 条` : '暂无',
      barWidth: d.todayAmount > 0 ? '100%' : '0%',
      barColor: 'var(--color-success)'
    },
    {
      label: '本月工资',
      value: d.monthWage ? `${d.monthWage}元` : '0元',
      icon: Money,
      iconBg: d.monthWage > 0 ? 'rgba(148, 163, 184, 0.12)' : 'rgba(148, 163, 184, 0.12)',
      iconColor: 'var(--color-slate-light)',
      trend: 'neutral',
      trendLabel: '结算',
      barWidth: d.monthWage > 0 ? '100%' : '0%',
      barColor: 'var(--color-slate-light)'
    }
  ]
})

const currentStats = computed(() => {
  if (isAdmin.value) return adminStats.value
  if (isSupervisor.value) return supervisorStats.value
  return bossStats.value
})

function enterCompanyFromDashboard(company) {
  userStore.enterCompany(company.id, company.name)
  ElMessage.success(`已进入「${company.name}」，可查看该公司全部数据`)
  setTimeout(() => window.location.reload(), 300)
}

// ===== 快捷操作 =====
const adminActions = [
  { label: '公司管理', icon: OfficeBuilding, bg: 'rgba(232, 168, 56, 0.1)', color: 'var(--color-amber)', route: '/company-manage' },
  { label: '续费记录', icon: Timer, bg: 'rgba(96, 165, 250, 0.1)', color: 'var(--color-info)', route: '/renew-logs' }
]

const supervisorActions = [
  { label: '报工审核', icon: Checked, bg: 'rgba(96, 165, 250, 0.1)', color: 'var(--color-info)', route: '/work-review' },
  { label: '工作看板', icon: View, bg: 'rgba(74, 222, 128, 0.1)', color: 'var(--color-success)', route: '/dashboard-board' },
  { label: '分扎管理', icon: Grid, bg: 'rgba(148, 163, 184, 0.12)', color: 'var(--color-slate-light)', route: '/bundle' }
]

const bossActions = [
  { label: '员工管理', icon: User, bg: 'rgba(232, 168, 56, 0.1)', color: 'var(--color-amber)', route: '/employee' },
  { label: '款式管理', icon: Document, bg: 'rgba(96, 165, 250, 0.1)', color: 'var(--color-info)', route: '/style' },
  { label: '报工审核', icon: Checked, bg: 'rgba(96, 165, 250, 0.1)', color: 'var(--color-info)', route: '/work-review' },
  { label: '工资结算', icon: Money, bg: 'rgba(74, 222, 128, 0.1)', color: 'var(--color-success)', route: '/wage-settlement' },
  { label: '分扎管理', icon: Grid, bg: 'rgba(148, 163, 184, 0.12)', color: 'var(--color-slate-light)', route: '/bundle' }
]

const currentActions = computed(() => {
  if (isAdmin.value) return adminActions
  if (isSupervisor.value) return supervisorActions
  return bossActions
})
</script>

<style scoped>
.dashboard {
  max-width: 1100px;
}

/* ===== 欢迎区 ===== */
.welcome-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 32px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  margin-bottom: 20px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s var(--ease-out) 0.1s;
}

.welcome-section.entered {
  opacity: 1;
  transform: translateY(0);
}

.welcome-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-slate);
  margin-bottom: 4px;
}

.welcome-sub {
  font-size: 14px;
  color: var(--color-mist);
}

.welcome-deco {
  flex-shrink: 0;
}

/* ===== 到期时间提醒 ===== */
.expire-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 24px;
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s var(--ease-out) 0.15s;
}

.expire-banner.entered {
  opacity: 1;
  transform: translateY(0);
}

.expire-banner.safe {
  background: linear-gradient(135deg, #e8f5e9, #c8e6c9);
  border: 1px solid #a5d6a7;
}

.expire-banner.warning {
  background: linear-gradient(135deg, #fff8e1, #ffecb3);
  border: 1px solid #ffe082;
}

.expire-banner.urgent {
  background: linear-gradient(135deg, #ffebee, #ffcdd2);
  border: 1px solid #ef9a9a;
}

.expire-banner.expired {
  background: linear-gradient(135deg, #ffebee, #ffcdd2);
  border: 1px solid #e57373;
}

.expire-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.expire-banner.safe .expire-icon {
  background: rgba(76, 175, 80, 0.15);
  color: #43a047;
}

.expire-banner.warning .expire-icon {
  background: rgba(255, 193, 7, 0.15);
  color: #f9a825;
}

.expire-banner.urgent .expire-icon {
  background: rgba(244, 67, 54, 0.15);
  color: #e53935;
}

.expire-banner.expired .expire-icon {
  background: rgba(244, 67, 54, 0.2);
  color: #c62828;
}

.expire-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.expire-label {
  font-size: 13px;
  color: var(--color-mist);
  margin-bottom: 2px;
}

.expire-date {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-slate);
}

.expire-tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.expire-banner.safe .expire-tag {
  background: rgba(76, 175, 80, 0.15);
  color: #2e7d32;
}

.expire-banner.warning .expire-tag {
  background: rgba(255, 193, 7, 0.2);
  color: #f57f17;
}

.expire-banner.urgent .expire-tag {
  background: rgba(244, 67, 54, 0.15);
  color: #c62828;
}

.expire-banner.expired .expire-tag {
  background: rgba(244, 67, 54, 0.2);
  color: #b71c1c;
}

/* ===== 统计卡片 ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 22px;
  box-shadow: var(--shadow-soft);
  animation: fadeInUp 0.5s var(--ease-out) both;
  transition: all 0.25s var(--ease-out);
}

.stat-card:hover {
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}

.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-trend {
  font-size: 11.5px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 20px;
  color: var(--color-mist);
  background: var(--color-cream);
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-slate);
  line-height: 1;
  margin-bottom: 6px;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 13px;
  color: var(--color-mist);
  margin-bottom: 14px;
}

.stat-bar {
  height: 4px;
  background: var(--color-cream);
  border-radius: 2px;
  overflow: hidden;
}

.stat-bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.8s var(--ease-out) 0.4s;
}

/* ===== 快捷操作 ===== */
.section-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-soft);
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.5s var(--ease-out) 0.3s;
  margin-bottom: 20px;
}

.section-card.entered {
  opacity: 1;
  transform: translateY(0);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate);
}

.section-sub {
  font-size: 12px;
  color: var(--color-mist);
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
}

.action-item:hover {
  background: var(--color-cream);
}

.action-item:hover .action-icon {
  transform: scale(1.08);
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.25s var(--ease-spring);
}

.action-label {
  font-size: 13px;
  color: var(--color-slate-light);
  font-weight: 500;
}

/* ===== 款式进度 ===== */
.style-progress-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.style-progress-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  background: var(--color-cream);
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
}

.style-progress-item:hover {
  background: rgba(232, 168, 56, 0.08);
}

.style-progress-info {
  display: flex;
  flex-direction: column;
  min-width: 100px;
}

.style-progress-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
}

.style-progress-count {
  font-size: 12px;
  color: var(--color-mist);
}

.style-progress-bar {
  flex: 1;
  height: 8px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 4px;
  overflow: hidden;
}

.style-progress-bar-fill {
  height: 100%;
  border-radius: 4px;
  background: var(--color-amber);
  transition: width 0.8s var(--ease-out);
}

.style-progress-bar-fill.completed {
  background: var(--color-success);
}

.style-progress-percent {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
  min-width: 40px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

/* ===== 最近报工 ===== */
.recent-report-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recent-report-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  background: var(--color-cream);
  transition: all 0.2s var(--ease-out);
}

.recent-report-item:hover {
  background: rgba(232, 168, 56, 0.08);
}

.report-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.report-worker {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
}

.report-process {
  font-size: 13px;
  color: var(--color-amber);
  background: rgba(232, 168, 56, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
}

.report-bundle {
  font-size: 12px;
  color: var(--color-mist);
}

.report-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.report-qty {
  font-size: 13px;
  color: var(--color-slate-light);
  font-weight: 500;
}

.report-amount {
  font-size: 13px;
  color: var(--color-amber);
  font-weight: 600;
}

.report-time {
  font-size: 12px;
  color: var(--color-mist);
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-container {
    height: 280px !important;
  }
}

@media (max-width: 500px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .welcome-section {
    flex-direction: column;
    text-align: center;
  }
  .welcome-deco {
    display: none;
  }
  .recent-report-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .report-meta {
    width: 100%;
    justify-content: space-between;
  }
}

/* ===== 活跃公司排名 ===== */
.active-company-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.active-company-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  background: var(--color-cream);
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
}

.active-company-item:hover {
  background: rgba(232, 168, 56, 0.08);
}

.company-rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-mist);
  background: rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
}

.company-rank.rank-top {
  background: linear-gradient(135deg, var(--color-amber), var(--color-terracotta));
  color: #fff;
}

.company-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.company-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
}

.company-meta {
  font-size: 12px;
  color: var(--color-mist);
}

.company-amount {
  flex-shrink: 0;
}

.amount-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-amber);
  font-variant-numeric: tabular-nums;
}

.active-company-item .enter-btn {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.active-company-item:hover .enter-btn {
  opacity: 1;
}

/* ===== 即将到期提醒 ===== */
.expire-card .section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-amber);
}

.expire-company-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.expire-company-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  background: var(--color-cream);
  transition: all 0.2s var(--ease-out);
}

.expire-company-item:hover {
  background: rgba(232, 168, 56, 0.06);
}

.expire-company-item.urgent {
  background: rgba(248, 113, 113, 0.06);
  border: 1px solid rgba(248, 113, 113, 0.15);
}

.expire-company-item.urgent:hover {
  background: rgba(248, 113, 113, 0.1);
}

.expire-company-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
  flex: 1;
}

.expire-company-date {
  font-size: 13px;
  color: var(--color-mist);
  font-variant-numeric: tabular-nums;
}

.expire-company-tag {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(232, 168, 56, 0.1);
  color: var(--color-amber);
  flex-shrink: 0;
}

.expire-company-tag.urgent {
  background: rgba(248, 113, 113, 0.12);
  color: var(--color-danger);
}

/* ===== 图表卡片 ===== */
.chart-card {
  padding: 24px;
}

.chart-container {
  width: 100%;
  height: 320px;
}

/* ===== 公司信息卡片 ===== */
.company-info-card {
  padding: 24px;
}

.company-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.company-info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  background: var(--color-cream);
  transition: all 0.2s var(--ease-out);
}

.company-info-item:hover {
  background: rgba(232, 168, 56, 0.08);
}

.company-info-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  background: rgba(232, 168, 56, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-amber);
  flex-shrink: 0;
}

.company-info-content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.company-info-label {
  font-size: 12px;
  color: var(--color-mist);
  margin-bottom: 2px;
}

.company-info-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.company-info-value.status-active {
  color: var(--color-success);
}

.company-info-value.status-disabled {
  color: var(--color-danger);
}

@media (max-width: 900px) {
  .company-info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 500px) {
  .company-info-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== API卡片 ===== */
.api-card {
  padding: 24px;
}

.api-card .section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-amber);
}

.api-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.api-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-radius: var(--radius-sm);
  background: var(--color-cream);
  transition: all 0.2s var(--ease-out);
}

.api-item:hover {
  background: rgba(232, 168, 56, 0.08);
}

.api-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.api-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.api-method {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--color-success);
  padding: 2px 8px;
  border-radius: 4px;
  flex-shrink: 0;
}

.api-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
}

.api-tag {
  flex-shrink: 0;
}

.api-desc {
  font-size: 12px;
  color: var(--color-mist);
}

.api-url {
  font-size: 13px;
  color: var(--color-amber);
  font-family: monospace;
  background: rgba(232, 168, 56, 0.08);
  padding: 4px 10px;
  border-radius: 4px;
  word-break: break-all;
}

.copy-btn {
  flex-shrink: 0;
  margin-left: 16px;
}

.api-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(96, 165, 250, 0.08);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--color-info);
}

.api-tip .el-icon {
  flex-shrink: 0;
}

@media (max-width: 600px) {
  .api-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .copy-btn {
    margin-left: 0;
    width: 100%;
  }
}
</style>
