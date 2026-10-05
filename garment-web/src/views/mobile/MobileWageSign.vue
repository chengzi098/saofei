<template>
  <div class="mobile-wage-sign">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title">我的工资</div>
      <div class="page-subtitle">查看工资结算并确认</div>
    </div>

    <!-- 工资列表 -->
    <div v-if="!selectedSettlement">
      <div v-if="loading" class="loading-box">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="settlements.length === 0" class="empty-box">
        <div class="empty-icon">📋</div>
        <div class="empty-text">暂无工资结算记录</div>
      </div>

      <div v-else class="settlement-list">
        <div
          v-for="item in settlements"
          :key="item.settlementId"
          class="settlement-card"
          @click="selectSettlement(item)"
        >
          <div class="card-header">
            <span class="card-date">{{ item.startDate }} ~ {{ item.endDate }}</span>
            <el-tag :type="item.employeeConfirmed ? 'success' : 'warning'" size="small" round>
              {{ item.employeeConfirmed ? '已确认' : '待确认' }}
            </el-tag>
          </div>
          <div class="card-body">
            <div class="card-amount">
              <span class="currency">元</span>
              <span class="amount">{{ (item.finalAmount || 0).toFixed(2) }}</span>
            </div>
          </div>
          <div class="card-footer">
            <span v-if="!item.employeeConfirmed">点击查看详情并确认</span>
            <span v-else>已确认</span>
            <span class="arrow">→</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 结算详情（选中后） -->
    <div v-else class="detail-section">
      <div class="detail-back" @click="selectedSettlement = null">
        ← 返回列表
      </div>

      <div class="detail-card">
        <div class="detail-header">
          <div class="detail-date">{{ selectedSettlement.startDate }} ~ {{ selectedSettlement.endDate }}</div>
          <div class="detail-amount">
            <span class="currency">元</span>
            <span class="amount">{{ (selectedSettlement.finalAmount || 0).toFixed(2) }}</span>
          </div>
          <div class="amount-label">应发工资</div>
        </div>

        <!-- 分项明细 -->
        <div class="wage-breakdown">
          <div class="breakdown-item">
            <span class="breakdown-label">计件工资</span>
            <span class="breakdown-value">{{ (selectedSettlement.baseAmount || 0).toFixed(2) }}元</span>
          </div>
          <div class="breakdown-item" v-if="selectedSettlement.supplementAmount && selectedSettlement.supplementAmount !== 0">
            <span class="breakdown-label">调整工资</span>
            <span class="breakdown-value" :class="{ 'adjust-add': selectedSettlement.supplementAmount > 0, 'adjust-sub': selectedSettlement.supplementAmount < 0 }">
              {{ selectedSettlement.supplementAmount > 0 ? '+' : '' }}{{ (selectedSettlement.supplementAmount || 0).toFixed(2) }}元
            </span>
          </div>
          <div class="breakdown-item total">
            <span class="breakdown-label">应发工资</span>
            <span class="breakdown-value highlight">{{ (selectedSettlement.finalAmount || 0).toFixed(2) }}元</span>
          </div>
        </div>

        <div class="wage-tip">计件工资不代表实际工资，以实际发放为准</div>
      </div>

      <!-- 确认按钮 -->
      <div class="confirm-action" v-if="!selectedSettlement.employeeConfirmed">
        <el-button
          type="primary"
          size="large"
          :loading="confirming"
          style="width: 100%; border-radius: 12px; height: 48px; font-size: 16px; font-weight: 600"
          @click="handleConfirm"
        >
          确认工资无误
        </el-button>
        <div class="confirm-tip">确认后表示您已查看并知悉本次工资</div>
      </div>

      <div v-else class="confirmed-box">
        <div class="confirmed-icon">✓</div>
        <div class="confirmed-text">您已确认查看</div>
        <div class="confirmed-time" v-if="selectedSettlement.employeeConfirmTime">
          确认时间：{{ formatDateTime(selectedSettlement.employeeConfirmTime) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMySettlements, confirmViewing } from '../../api/wageSettlement'

const loading = ref(false)
const confirming = ref(false)
const settlements = ref([])
const selectedSettlement = ref(null)

onMounted(() => {
  loadSettlements()
})

async function loadSettlements() {
  loading.value = true
  try {
    const res = await getMySettlements()
    settlements.value = res.data || []
  } catch (err) {
    console.error('加载结算单失败:', err)
  } finally {
    loading.value = false
  }
}

function selectSettlement(item) {
  selectedSettlement.value = item
}

async function handleConfirm() {
  if (!selectedSettlement.value) return

  try {
    await ElMessageBox.confirm(
      '确认后表示您已查看并知悉本次工资，是否确认？',
      '确认工资',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  confirming.value = true
  try {
    await confirmViewing(selectedSettlement.value.settlementId)
    ElMessage.success('确认成功')
    selectedSettlement.value.employeeConfirmed = 1
    selectedSettlement.value.employeeConfirmTime = new Date().toISOString()
    loadSettlements()
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '确认失败')
  } finally {
    confirming.value = false
  }
}

function formatDateTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
</script>

<style scoped>
.mobile-wage-sign {
  padding: 16px;
  padding-bottom: 80px;
  min-height: 100vh;
  background: #f5f5f5;
}

.page-header {
  margin-bottom: 16px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
}

.page-subtitle {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}

.loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 60px 0;
  color: #9ca3af;
}

.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 0;
}

.empty-icon {
  font-size: 48px;
}

.empty-text {
  font-size: 14px;
  color: #9ca3af;
}

.settlement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.settlement-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: transform 0.15s;
}

.settlement-card:active {
  transform: scale(0.98);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-date {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-slate, #2d2a26);
}

.card-body {
  margin-bottom: 12px;
}

.card-amount {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.card-amount .currency {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-amber, #d97706);
}

.card-amount .amount {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-amber, #d97706);
  font-variant-numeric: tabular-nums;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: #3b82f6;
}

.arrow {
  font-size: 16px;
}

/* 详情区域 */
.detail-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-back {
  font-size: 14px;
  color: #3b82f6;
  cursor: pointer;
  padding: 4px 0;
}

.detail-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px;
}

.detail-header {
  text-align: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-date {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.detail-amount {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.detail-amount .currency {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-amber, #d97706);
}

.detail-amount .amount {
  font-size: 40px;
  font-weight: 700;
  color: var(--color-amber, #d97706);
  font-variant-numeric: tabular-nums;
}

.amount-label {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

/* 分项明细 */
.wage-breakdown {
  background: #f9fafb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.breakdown-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.breakdown-item:not(:last-child) {
  border-bottom: 1px solid #f0f0f0;
}

.breakdown-item.total {
  border-top: 2px solid #e5e7eb;
  margin-top: 4px;
  padding-top: 12px;
}

.breakdown-label {
  font-size: 14px;
  color: #6b7280;
}

.breakdown-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-slate, #2d2a26);
}

.breakdown-value.highlight {
  color: var(--color-amber, #d97706);
  font-size: 20px;
  font-weight: 700;
}

.adjust-add {
  color: #10b981 !important;
}

.adjust-sub {
  color: #ef4444 !important;
}

.wage-tip {
  text-align: center;
  font-size: 12px;
  color: #ef4444;
  padding: 8px;
  background: #fef2f2;
  border-radius: 8px;
}

/* 确认按钮 */
.confirm-action {
  padding: 8px 0;
}

.confirm-tip {
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 8px;
}

/* 已确认状态 */
.confirmed-box {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 24px;
  text-align: center;
}

.confirmed-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #10b981;
  color: #fff;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}

.confirmed-text {
  font-size: 16px;
  font-weight: 600;
  color: #10b981;
  margin-bottom: 8px;
}

.confirmed-time {
  font-size: 13px;
  color: #9ca3af;
}
</style>
