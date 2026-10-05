<template>
  <div class="wage-print-page" v-loading="loading" element-loading-text="加载工资数据...">
    <template v-if="printData">
      <div class="print-sheet">
        <!-- 页头 -->
        <div class="sheet-header">
          <h1 class="company-title">{{ companyName }} — 工资条</h1>
          <div class="settlement-info">
            <span>结算时间：<strong>{{ printData.settlement.startDate }} ~ {{ printData.settlement.endDate }}</strong></span>
            <span>打印日期：<strong>{{ today }}</strong></span>
          </div>
        </div>

        <!-- 工资表格 -->
        <table class="wage-table">
          <thead>
            <tr>
              <th class="col-name">员工姓名</th>
              <th class="col-amount">计件工资</th>
              <th class="col-amount">调整工资</th>
              <th class="col-amount highlight">实发工资</th>
              <th class="col-sign">签名</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="worker in printData.workers" :key="worker.workerId">
              <td class="col-name">{{ worker.workerName }}</td>
              <td class="col-amount">{{ (worker.baseAmount || 0).toFixed(2) }}</td>
              <td class="col-amount" :class="{ positive: worker.supplementAmount > 0, negative: worker.supplementAmount < 0 }">
                {{ worker.supplementAmount > 0 ? '+' : '' }}{{ (worker.supplementAmount || 0).toFixed(2) }}
              </td>
              <td class="col-amount highlight">{{ (worker.finalAmount || 0).toFixed(2) }}</td>
              <td class="col-sign"></td>
            </tr>
          </tbody>
          <tfoot>
            <tr class="total-row">
              <td class="col-name">合计</td>
              <td class="col-amount">{{ (printData.settlement.totalAmount || 0).toFixed(2) }}</td>
              <td class="col-amount"></td>
              <td class="col-amount highlight">{{ (printData.settlement.totalAmount || 0).toFixed(2) }}</td>
              <td class="col-sign"></td>
            </tr>
          </tfoot>
        </table>
      </div>

      <!-- 打印按钮（屏幕显示，打印时隐藏） -->
      <div class="no-print actions-bar">
        <el-button type="primary" size="large" @click="doPrint">
          <el-icon><Printer /></el-icon>
          打印
        </el-button>
        <el-button size="large" @click="goBack">返回</el-button>
      </div>
    </template>

    <!-- 空状态 -->
    <div v-if="!loading && !printData" class="empty-state">
      <p>暂无数据</p>
      <el-button @click="goBack">返回</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Printer } from '@element-plus/icons-vue'
import { getPrintData } from '../api/wageSettlement'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const printData = ref(null)

const companyName = computed(() => userStore.currentCompanyName || '制衣厂')
const today = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

onMounted(async () => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    const res = await getPrintData(id)
    printData.value = res.data || null
    setTimeout(() => {
      window.print()
    }, 300)
  } catch (err) {
    console.error('加载打印数据失败:', err)
  } finally {
    loading.value = false
  }
})

function doPrint() {
  window.print()
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.wage-print-page {
  min-height: 100vh;
  background: #fff;
  padding: 20px;
}

.print-sheet {
  max-width: 700px;
  margin: 0 auto;
  padding: 30px;
}

/* 页头 */
.sheet-header {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #333;
}
.company-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 10px;
  color: #1a1a1a;
}
.settlement-info {
  display: flex;
  justify-content: center;
  gap: 24px;
  font-size: 13px;
  color: #666;
}
.settlement-info strong {
  color: #333;
}

/* 工资表格 */
.wage-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.wage-table th,
.wage-table td {
  padding: 10px 12px;
  border: 1px solid #333;
  text-align: left;
}
.wage-table th {
  background: #f0f0f0;
  font-weight: 600;
  color: #333;
  font-size: 13px;
}
.wage-table tbody tr:nth-child(even) {
  background: #fafafa;
}
.col-name { width: 120px; }
.col-amount { width: 100px; text-align: right; font-variant-numeric: tabular-nums; }
.col-amount.highlight { font-weight: 700; color: #d97706; }
.col-sign { width: 100px; }
.positive { color: #10b981; }
.negative { color: #ef4444; }

.total-row {
  background: #f8f8f8 !important;
  font-weight: 700;
}

/* 操作栏 */
.actions-bar {
  max-width: 700px;
  margin: 20px auto;
  display: flex;
  justify-content: center;
  gap: 16px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
  gap: 16px;
  color: #999;
}

/* 打印样式 */
@media print {
  .wage-print-page {
    padding: 0;
    background: #fff;
  }
  .no-print {
    display: none !important;
  }
  .print-sheet {
    margin: 0;
    padding: 10px;
  }
  .wage-table th {
    background: #f0f0f0 !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  .total-row {
    background: #f5f5f5 !important;
    -webkit-print-coloradjust: exact;
    print-color-adjust: exact;
  }
}
</style>
