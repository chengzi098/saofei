<template>
  <div class="style-manage">
    <!-- 搜索工具栏 -->
    <div class="toolbar" :class="{ 'entered': mounted }">
      <div class="toolbar-left">
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="searchKeyword"
            placeholder="搜索款号..."
            class="search-input"
            @keyup.enter="pageNum = 1; loadList()"
          />
          <el-icon v-if="searchKeyword" class="clear-icon" @click="resetSearch"><CircleClose /></el-icon>
        </div>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="~"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          size="default"
          class="date-picker"
          @change="pageNum = 1; loadList()"
          :clearable="true"
          @clear="pageNum = 1; loadList()"
        />
      </div>
      <el-button type="primary" :icon="Plus" @click="openAddStyleDialog" class="add-btn">
        添加款式
      </el-button>
    </div>

    <!-- 面包屑导航（在款式详情页显示） -->
    <div class="breadcrumb" v-if="selectedStyleId">
      <span class="crumb clickable" @click="backToList">款式列表</span>
      <el-icon class="crumb-sep"><ArrowRight /></el-icon>
      <span class="crumb">{{ selectedStyle?.styleNo || '-' }}</span>
    </div>

    <!-- ===== Step 1: 款式卡片列表 ===== -->
    <div v-if="!selectedStyleId" class="cards-grid" :class="{ 'entered': mounted }">
      <div
        v-for="item in tableData"
        :key="item.id"
        class="style-card"
        @click="selectStyle(item)"
      >
        <div class="card-header">
          <span class="style-no">{{ item.styleNo || '-' }}</span>
          <span class="bed-count" :class="{ zero: item.bedCount === 0 }">
            {{ item.bedCount }} 个床号
          </span>
        </div>
        <div v-if="item.hasZeroPriceProcess" class="style-zero-price-warning">
          <el-icon class="warning-icon"><WarningFilled /></el-icon>
          <span class="warning-text">有工序未定价</span>
        </div>
        <div class="card-footer">
          <span class="date-text">{{ formatDate(item.createTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 空状态（款式列表） -->
    <div v-if="!loading && !selectedStyleId && tableData.length === 0" class="empty-state">
      <div class="empty-icon">
        <el-icon><Collection /></el-icon>
      </div>
      <p class="empty-title">{{ searchKeyword ? '没有找到匹配的款式' : '暂无款式' }}</p>
      <p class="empty-desc">{{ searchKeyword ? '换个关键词试试' : '点击右上角「添加款式」开始创建' }}</p>
    </div>

    <!-- 分页（款式列表） -->
    <div v-if="!selectedStyleId && tableData.length > 0" class="pagination-wrap">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadList"
        @current-change="loadList"
      />
    </div>

    <!-- ===== Step 2: 款式详情（床号列表） ===== -->
    <div v-if="selectedStyleId" class="detail-section">
      <!-- 款号信息栏 -->
      <div class="style-info-bar">
        <div class="style-info-left">
          <div class="info-block">
            <span class="info-label">款号</span>
            <span class="info-value">{{ selectedStyle?.styleNo || '-' }}</span>
          </div>
          <div class="info-block">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatDate(selectedStyle?.createTime) }}</span>
          </div>
        </div>
        <div class="style-info-actions">
          <el-button link type="primary" @click="openEditStyleDialog">
            <el-icon><Edit /></el-icon> 修改款号
          </el-button>
          <el-popconfirm
            title="确定删除该款式吗？关联的床号也会被删除。"
            @confirm="handleDeleteStyle"
          >
            <template #reference>
              <el-button link type="danger">
                <el-icon><Delete /></el-icon> 删除款式
              </el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>

      <!-- 床号列表头部 -->
      <div class="bed-list-header">
        <div class="section-title-wrap">
          <span class="section-icon">🛏️</span>
          <h4 class="section-title">床号列表</h4>
          <span class="bed-count-badge">{{ bedList.length }} 个</span>
        </div>
        <el-button type="primary" :icon="Plus" @click="openBedDialog('add')" class="add-bed-btn">
          添加床号
        </el-button>
      </div>

      <!-- 床号卡片网格 -->
      <div class="bed-cards-grid">
        <div
          v-for="(bed, idx) in bedList"
          :key="bed.id || idx"
          class="bed-card"
          @click="goBundle(bed)"
        >
          <div class="bed-card-header">
            <div class="bed-title-wrap">
              <span class="bed-number">#{{ idx + 1 }}</span>
              <div class="bed-title-text">
                <span class="bed-name">{{ bed.bedNo || '未命名' }}</span>
                <span class="bed-desc">{{ bed.name }}</span>
              </div>
            </div>
            <el-tag v-if="bed.hasWorkRecords" type="warning" size="small" class="bed-status-tag">
              已报工
            </el-tag>
            <el-tag v-else-if="bed.bundleCount > 0" type="success" size="small" class="bed-status-tag">
              已分 {{ bed.bundleCount }} 扎
            </el-tag>
          </div>

          <div class="bed-card-body">
            <div class="bed-info-grid">
              <div class="bed-info-item">
                <span class="info-value">{{ bed.quantity }}</span>
                <span class="info-label">件数</span>
              </div>
              <div class="bed-info-item">
                <span class="info-value">{{ bed.bundleCount || 0 }}</span>
                <span class="info-label">已分扎</span>
              </div>
              <div class="bed-info-item">
                <span class="info-value">{{ bed.processes ? bed.processes.length : 0 }}</span>
                <span class="info-label">工序数</span>
              </div>
            </div>
            <div
              v-if="hasZeroPriceProcess(bed)"
              class="zero-price-warning clickable"
              @click.stop="openBedDialog('edit', bed, idx)"
            >
              <el-icon class="warning-icon"><WarningFilled /></el-icon>
              <span class="warning-text">
                有 {{ getZeroPriceProcessNames(bed).length }} 道工序未定价
              </span>
              <el-tooltip
                :content="'点击编辑定价（未定价工序：' + getZeroPriceProcessNames(bed).join('、') + '）'"
                placement="top"
                :show-after="300"
              >
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
            <div v-if="bed.colors" class="bed-meta-row">
              <span class="meta-label">颜色</span>
              <span class="meta-value">{{ bed.colors }}</span>
            </div>
          </div>

          <div class="bed-card-footer" @click.stop>
            <el-button link type="primary" size="small" @click="openBedDialog('edit', bed, idx)">
              编辑
            </el-button>
            <el-popconfirm
              v-if="!bed.hasWorkRecords"
              :title="bed.bundleCount > 0 ? `确定删除该床号吗？将同时删除该床号下的 ${bed.bundleCount} 个扎货` : '确定删除该床号吗？'"
              confirm-button-text="确定删除"
              confirm-button-type="danger"
              @confirm="removeBedRow(bed, idx)"
            >
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
            <el-tooltip v-else content="该床号下已有员工报工，不能删除" placement="top">
              <el-button link type="info" size="small" disabled>删除</el-button>
            </el-tooltip>
          </div>
        </div>
      </div>

      <!-- 空状态（床号列表） -->
      <div v-if="bedList.length === 0" class="empty-state">
        <div class="empty-icon">
          <el-icon><Grid /></el-icon>
        </div>
        <p class="empty-title">暂无床号</p>
        <p class="empty-desc">点击「添加床号」开始配置</p>
      </div>
    </div>

    <!-- ===== 对话框1：添加/编辑款式 ===== -->
    <el-dialog
      v-model="styleDialogVisible"
      :title="styleDialogMode === 'add' ? '添加款式' : '修改款号'"
      width="480px"
      destroy-on-close
      class="style-dialog"
    >
      <div class="dialog-body">
        <div class="field-wrap">
          <label class="field-label">
            <span class="label-text">款号</span>
            <span class="label-required">*</span>
          </label>
          <el-input
            v-model="styleForm.styleNo"
            placeholder="请输入款号，如：TX-001"
            maxlength="50"
            size="large"
            class="large-input"
            @keyup.enter="handleSaveStyle"
          />
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="styleDialogVisible = false" class="footer-btn">取消</el-button>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSaveStyle" class="footer-btn">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 对话框2：添加/编辑床号 ===== -->
    <el-dialog
      v-model="bedDialogVisible"
      :title="bedDialogMode === 'add' ? '添加床号' : '编辑床号'"
      width="720px"
      destroy-on-close
      @close="resetBedDialog"
      class="bed-dialog"
    >
      <div class="dialog-body">
        <!-- 床号基本信息 -->
        <div class="bed-form-grid">
          <div class="form-row">
            <div class="form-col">
              <label class="field-label">
                <span class="label-text">床号编号</span>
                <span class="label-required">*</span>
              </label>
              <el-input v-model="bedForm.bedNo" placeholder="如：1号床" size="large" class="large-input" />
            </div>
            <div class="form-col">
              <label class="field-label">
                <span class="label-text">名称</span>
                <span class="label-required">*</span>
              </label>
              <el-input v-model="bedForm.name" placeholder="如：大货、返工" size="large" class="large-input" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-col">
              <label class="field-label">
                <span class="label-text">件数</span>
                <span class="label-required">*</span>
              </label>
              <el-input-number
                v-model="bedForm.quantity"
                :min="1"
                :step="1"
                size="large"
                class="large-input-number"
                controls-position="right"
              />
            </div>
            <div class="form-col">
              <label class="field-label">
                <span class="label-text">备注</span>
              </label>
              <el-input v-model="bedForm.remark" placeholder="选填备注信息" size="large" class="large-input" />
            </div>
          </div>
        </div>

        <!-- 工序配置 -->
        <div class="bed-process-section">
          <div class="bed-process-header">
            <div class="process-title-wrap">
              <span class="process-icon">⚙️</span>
              <span class="bed-process-title">工序配置</span>
            </div>
            <div class="process-header-actions">
              <el-button size="default" @click="openTemplateDialog" class="import-template-btn">
                📋 导入模板
              </el-button>
              <el-button type="primary" :icon="Plus" size="default" @click="addProcessRow" class="add-process-btn">
                添加工序
              </el-button>
            </div>
          </div>

          <div class="process-table-wrap">
            <el-table
              :data="bedForm.processes"
              style="width: 100%"
              class="process-table"
              :header-cell-style="headerStyle"
              size="default"
            >
              <el-table-column label="序号" width="60" align="center">
                <template #default="{ $index, row }">
                  <div class="seq-cell">
                    <el-tooltip v-if="row.priceLocked" content="定价已锁定，不能修改" placement="top">
                      <el-icon class="price-locked-icon"><Lock /></el-icon>
                    </el-tooltip>
                    <span class="seq-num">{{ $index + 1 }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="工序名称" min-width="140">
                <template #default="{ row }">
                  <el-input
                    v-model="row.name"
                    placeholder="如：裁剪、缝纫"
                    size="default"
                    class="process-input"
                    :disabled="row.priceLocked"
                  />
                </template>
              </el-table-column>
              <el-table-column label="单价（元）" min-width="200" align="center">
                <template #default="{ row }">
                  <div class="price-cell">
                    <div class="price-input-wrap">
                      <el-input-number
                        v-model="row.price"
                        :min="0"
                        :precision="2"
                        :step="0.1"
                        size="default"
                        class="process-price-input"
                        :disabled="row.priceLocked"
                      />
                    </div>
                    <!-- 效率信息（只在未锁定时显示） -->
                    <template v-if="!row.priceLocked">
                      <div v-if="row.speedPerHour" class="efficiency-info">
                        <span class="efficiency-item speed">
                          📊 {{ row.speedPerHour }} 件/小时
                        </span>
                        <span class="efficiency-item detail">
                          {{ row.dayQuantity }} 件 / {{ row.dayHours }}h
                        </span>
                      </div>
                      <div v-else-if="row.completedQuantity > 0" class="efficiency-info">
                        <span class="efficiency-item no-data">
                          📊 数据不足，无法计算效率
                        </span>
                      </div>
                      <!-- 价格预览 -->
                      <div v-if="row.price > 0 && row.completedQuantity > 0" class="price-preview">
                        <span class="preview-item completed">
                          已完成 {{ row.completedQuantity }} 件 = ¥{{ (row.completedQuantity * row.price).toFixed(2) }}
                        </span>
                        <span class="preview-item total">
                          预估 {{ row.totalQuantity }} 件 = ¥{{ (row.totalQuantity * row.price).toFixed(2) }}
                        </span>
                      </div>
                    </template>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center">
                <template #default="{ $index, row }">
                  <el-button link type="default" size="default" @click="moveProcessUp($index)" :disabled="$index === 0 || row.priceLocked" class="move-btn">
                    <el-icon><Top /></el-icon>
                  </el-button>
                  <el-button link type="default" size="default" @click="moveProcessDown($index)" :disabled="$index === bedForm.processes.length - 1 || row.priceLocked" class="move-btn">
                    <el-icon><Bottom /></el-icon>
                  </el-button>
                  <el-button link type="danger" size="default" @click="removeProcessRow($index)" :disabled="row.priceLocked" class="delete-process-btn">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-if="!bedForm.processes || bedForm.processes.length === 0" class="process-empty">
            <div class="empty-inner">
              <el-icon class="empty-icon-large"><Collection /></el-icon>
              <p>暂无工序配置</p>
              <span>点击「添加工序」开始添加工序</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <div class="footer-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>定价为0的工序可修改一次，设定后将锁定不能修改</span>
          </div>
          <div class="footer-actions">
            <el-button size="large" @click="bedDialogVisible = false" class="footer-btn">取消</el-button>
            <el-button type="primary" size="large" :loading="submitting" @click="handleSaveBed" class="footer-btn">
              保存
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 对话框3：选择模板 ===== -->
    <el-dialog
      v-model="templateDialogVisible"
      title="导入工序模板"
      width="480px"
      destroy-on-close
      class="style-dialog"
    >
      <div class="dialog-body">
        <div class="field-wrap">
          <label class="field-label">
            <span class="label-text">选择模板</span>
          </label>
          <el-select
            v-model="selectedTemplateId"
            placeholder="请选择一个模板"
            size="large"
            style="width: 100%"
          >
            <el-option
              v-for="t in templateList"
              :key="t.id"
              :label="t.name + '（' + t.itemCount + '道工序）'"
              :value="t.id"
            />
          </el-select>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="templateDialogVisible = false" class="footer-btn">取消</el-button>
          <el-button type="primary" size="large" @click="handleImportTemplate" class="footer-btn">
            确认导入
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Collection, CircleClose, Delete, Edit, ArrowRight, Grid, Top, Bottom, WarningFilled, InfoFilled, Lock } from '@element-plus/icons-vue'
import { getStyleList, addStyle, updateStyle, deleteStyle } from '../api/style'
import { getBeds, addBed, updateBed, deleteBed } from '../api/bed'
import { getProcessTemplateList, getProcessTemplateDetail } from '../api/processTemplate'
import { normalizeBedName, validateBedForm } from '../utils/bedFormValidation'
import { useRouter } from 'vue-router'

const router = useRouter()

// ===== 状态 =====
const mounted = ref(false)
const loading = ref(false)
const submitting = ref(false)
const searchKeyword = ref('')
const dateRange = ref(null)
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 选中状态（面包屑导航）
const selectedStyleId = ref(null)
const selectedStyle = ref(null)
const bedList = ref([])

// 款式对话框
const styleDialogVisible = ref(false)
const styleDialogMode = ref('add')
const styleForm = reactive({ styleNo: '' })

// 床号对话框
const bedDialogVisible = ref(false)
const bedDialogMode = ref('add')
const bedEditIndex = ref(-1)
const bedForm = reactive({
  id: null,
  bedNo: '',
  name: '',
  colors: '',
  quantity: 100,
  remark: '',
  bundleCount: 0,
  processes: []
})

const headerStyle = () => ({
  background: '#f5f3f0',
  fontWeight: 600,
  color: '#4a4846',
  fontSize: '14px',
  padding: '14px 16px',
  whiteSpace: 'nowrap'
})

// 模板导入相关
const templateList = ref([])
const templateDialogVisible = ref(false)
const selectedTemplateId = ref(null)

// ===== 生命周期 =====
onMounted(() => {
  requestAnimationFrame(() => { mounted.value = true })
  loadList()
})

// ===== 数据加载 =====
async function loadList() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getStyleList(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取款式列表失败:', err)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchKeyword.value = ''
  dateRange.value = null
  pageNum.value = 1
  loadList()
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

// 检查床号是否有未定价的工序
function hasZeroPriceProcess(bed) {
  if (!bed.processes || bed.processes.length === 0) return false
  return bed.processes.some(p => !p.price || p.price === 0)
}

// 获取未定价工序的名称列表
function getZeroPriceProcessNames(bed) {
  if (!bed.processes) return []
  return bed.processes.filter(p => !p.price || p.price === 0).map(p => p.name || '未命名工序')
}

// ===== 面包屑返回 =====
function backToList() {
  selectedStyleId.value = null
  selectedStyle.value = null
  bedList.value = []
}

// ===== 选择款式（进入床号列表） =====
async function selectStyle(item) {
  selectedStyleId.value = item.id
  selectedStyle.value = { ...item }

  try {
    const res = await getBeds(item.id)
    bedList.value = (res.data || []).map(b => ({
      id: b.id,
      bedNo: b.bedNo,
      name: b.name || '',
      colors: b.colors || '',
      quantity: b.quantity,
      remark: b.remark || '',
      bundleCount: b.bundleCount || 0,
      processes: (b.processes || []).map(p => ({
        id: p.id,
        name: p.name,
        seq: p.seq,
        price: p.price,
        priceLocked: p.priceLocked,
        totalQuantity: p.totalQuantity || 0,
        completedQuantity: p.completedQuantity || 0,
        dayQuantity: p.dayQuantity,
        dayHours: p.dayHours,
        speedPerHour: p.speedPerHour
      }))
    }))
  } catch (e) {
    bedList.value = []
  }
}

// 跳转到分扎管理（指定款式+床号）
function goBundle(bed) {
  router.push({
    name: 'Bundle',
    query: { styleId: selectedStyleId.value, bedId: bed.id }
  })
}

// ===== 款式操作 =====
function openAddStyleDialog() {
  styleDialogMode.value = 'add'
  styleForm.styleNo = ''
  styleDialogVisible.value = true
}

function openEditStyleDialog() {
  styleDialogMode.value = 'edit'
  styleForm.styleNo = selectedStyle.value?.styleNo || ''
  styleDialogVisible.value = true
}

async function handleSaveStyle() {
  if (!styleForm.styleNo || !styleForm.styleNo.trim()) {
    ElMessage.warning('请输入款号')
    return
  }
  submitting.value = true
  try {
    if (styleDialogMode.value === 'edit') {
      await updateStyle(selectedStyleId.value, { styleNo: styleForm.styleNo.trim() })
      ElMessage.success('保存成功')
      selectedStyle.value.styleNo = styleForm.styleNo.trim()
    } else {
      await addStyle({ styleNo: styleForm.styleNo.trim() })
      ElMessage.success('添加成功')
    }
    styleDialogVisible.value = false
    loadList()
  } catch (e) {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDeleteStyle() {
  try {
    await deleteStyle(selectedStyleId.value)
    ElMessage.success('已删除')
    backToList()
    loadList()
  } catch (e) {
    // handled
  }
}

// ===== 床号操作 =====
function openBedDialog(mode, bed = null, index = -1) {
  bedDialogMode.value = mode
  bedEditIndex.value = index

  if (mode === 'edit' && bed) {
    bedForm.id = bed.id
    bedForm.bedNo = bed.bedNo
    bedForm.name = bed.name
    bedForm.colors = bed.colors
    bedForm.quantity = bed.quantity
    bedForm.remark = bed.remark
    bedForm.bundleCount = bed.bundleCount || 0
    bedForm.processes = (bed.processes || []).map(p => ({
      id: p.id,
      name: p.name,
      seq: p.seq,
      price: p.price,
      priceLocked: p.priceLocked,
      totalQuantity: p.totalQuantity || 0,
      completedQuantity: p.completedQuantity || 0,
      dayQuantity: p.dayQuantity,
      dayHours: p.dayHours,
      speedPerHour: p.speedPerHour
    }))
  } else {
    bedForm.id = null
    bedForm.bedNo = ''
    bedForm.name = ''
    bedForm.colors = ''
    bedForm.quantity = 100
    bedForm.remark = ''
    bedForm.bundleCount = 0
    bedForm.processes = [{ name: '', seq: 1, price: 0 }]
  }

  bedDialogVisible.value = true
}

function resetBedDialog() {
  bedForm.id = null
  bedForm.bedNo = ''
  bedForm.name = ''
  bedForm.colors = ''
  bedForm.quantity = 100
  bedForm.remark = ''
  bedForm.bundleCount = 0
  bedForm.processes = []
}

function addProcessRow() {
  bedForm.processes.push({
    name: '',
    seq: bedForm.processes.length + 1,
    price: 0,
    totalQuantity: 0,
    completedQuantity: 0
  })
}

function removeProcessRow(index) {
  bedForm.processes.splice(index, 1)
}

function moveProcessUp(index) {
  if (index <= 0) return
  const temp = bedForm.processes[index]
  bedForm.processes[index] = bedForm.processes[index - 1]
  bedForm.processes[index - 1] = temp
}

function moveProcessDown(index) {
  if (index >= bedForm.processes.length - 1) return
  const temp = bedForm.processes[index]
  bedForm.processes[index] = bedForm.processes[index + 1]
  bedForm.processes[index + 1] = temp
}

async function handleSaveBed() {
  const validation = validateBedForm(bedForm)
  if (!validation.valid) {
    ElMessage.warning(validation.message)
    return
  }

  // 验证工序
  const processes = (bedForm.processes || []).filter(p => p.name && p.name.trim())
  for (let i = 0; i < processes.length; i++) {
    if (processes[i].price != null && processes[i].price < 0) {
      ElMessage.warning('工序「' + processes[i].name + '」的单价不能为负数')
      return
    }
  }

  const processPayload = processes.map((p, i) => ({
    id: p.id || null,
    name: p.name.trim(), seq: i + 1, price: p.price || 0
  }))

  submitting.value = true
  try {
    const data = {
      bedNo: bedForm.bedNo.trim(),
      name: normalizeBedName(bedForm.name),
      colors: bedForm.colors || null,
      quantity: bedForm.quantity,
      remark: bedForm.remark || null,
      processes: processPayload.length > 0 ? processPayload : null
    }

    if (bedForm.id) {
      await updateBed(bedForm.id, data)
      // 更新本地数据
      const idx = bedEditIndex.value
      if (idx >= 0) {
        bedList.value[idx] = {
          id: bedForm.id,
          bedNo: bedForm.bedNo.trim(),
          name: normalizeBedName(bedForm.name),
          colors: bedForm.colors,
          quantity: bedForm.quantity,
          remark: bedForm.remark,
          bundleCount: bedForm.bundleCount,
          processes: processPayload
        }
      }
    } else {
      const res = await addBed(selectedStyleId.value, data)
      bedList.value.push({
        id: res.data.id,
        bedNo: bedForm.bedNo.trim(),
        name: normalizeBedName(bedForm.name),
        colors: bedForm.colors,
        quantity: bedForm.quantity,
        remark: bedForm.remark,
        bundleCount: 0,
        processes: processPayload
      })
    }

    ElMessage.success('保存成功')
    bedDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '保存失败，请检查填写内容')
  } finally {
    submitting.value = false
  }
}

function removeBedRow(bed, index) {
  if (bed.id) {
    deleteBed(bed.id).then(() => {
      bedList.value.splice(index, 1)
      ElMessage.success('已删除')
    }).catch((e) => {
      ElMessage.error(e.response?.data?.msg || '删除失败')
    })
  } else {
    bedList.value.splice(index, 1)
  }
}

// ===== 模板导入 =====
async function openTemplateDialog() {
  try {
    const res = await getProcessTemplateList()
    templateList.value = res.data || []
    selectedTemplateId.value = null
    templateDialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取模板列表失败')
  }
}

async function handleImportTemplate() {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请选择一个模板')
    return
  }
  try {
    const res = await getProcessTemplateDetail(selectedTemplateId.value)
    const detail = res.data
    const items = (detail.items || []).map(p => ({
      name: p.name,
      seq: p.seq,
      price: p.price
    }))
    bedForm.processes = items
    templateDialogVisible.value = false
    ElMessage.success('已导入「' + detail.name + '」的 ' + items.length + ' 道工序')
  } catch (e) {
    ElMessage.error('导入失败')
  }
}
</script>

<style scoped>
.style-manage {
  max-width: 1100px;
}

/* ===== 统计卡片 ===== */
.stats-bar {
  display: grid;
  grid-template-columns: repeat(1, 280px);
  gap: 16px;
  margin-bottom: 20px;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.5s var(--ease-out);
}
.stats-bar.entered { opacity: 1; transform: translateY(0); }

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-soft);
  transition: transform 0.2s var(--ease-out);
}
.stat-card:hover { transform: translateY(-2px); }

.stat-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.stat-icon.total {
  background: linear-gradient(135deg, rgba(232, 168, 56, 0.12), rgba(232, 168, 56, 0.05));
  color: var(--color-amber);
}

.stat-info { display: flex; flex-direction: column; gap: 2px; }
.stat-value { font-size: 24px; font-weight: 700; color: var(--color-slate); line-height: 1; }
.stat-label { font-size: 12px; color: var(--color-mist); }

/* ===== 工具栏 ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.4s var(--ease-out) 0.1s;
}
.toolbar.entered { opacity: 1; transform: translateY(0); }
.toolbar-left { display: flex; align-items: center; gap: 16px; }

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-card);
  border: 1.5px solid var(--color-cream-dark);
  border-radius: var(--radius-md);
  padding: 0 12px;
  height: 36px;
  width: 260px;
  transition: border-color 0.2s;
}
.search-box:focus-within {
  border-color: var(--color-amber);
  box-shadow: 0 0 0 3px rgba(232, 168, 56, 0.08);
}
.search-icon { font-size: 15px; color: var(--color-mist); flex-shrink: 0; }
.search-input {
  border: none; outline: none; background: transparent;
  font-size: 13.5px; color: var(--color-slate); width: 100%;
}
.search-input::placeholder { color: #bbb; }
.clear-icon {
  font-size: 14px; color: var(--color-mist); cursor: pointer;
  flex-shrink: 0; transition: color 0.2s;
}
.clear-icon:hover { color: var(--color-slate-light); }
.date-picker {
  width: 260px;
}
.date-picker :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  height: 36px;
}
.add-btn { font-weight: 500; }

/* ===== 面包屑 ===== */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
  font-size: 13px;
}
.crumb { color: var(--color-mist); }
.crumb.clickable {
  color: var(--color-amber);
  cursor: pointer;
  font-weight: 500;
}
.crumb.clickable:hover { text-decoration: underline; }
.crumb-sep { font-size: 12px; color: var(--color-mist); }

/* ===== 款式卡片网格 ===== */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.4s var(--ease-out) 0.2s;
}
.cards-grid.entered { opacity: 1; transform: translateY(0); }

.style-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
  box-shadow: var(--shadow-soft);
  transition: all 0.25s var(--ease-out);
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
}
.style-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.style-no {
  font-size: 18px;
  font-weight: 700;
  color: #6366f1;
  font-family: 'Menlo', 'Consolas', monospace;
}

.bed-count {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-amber);
  background: rgba(232, 168, 56, 0.1);
  padding: 2px 10px;
  border-radius: 12px;
}
.bed-count.zero {
  background: rgba(156, 163, 175, 0.1);
  color: #9ca3af;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f0eeea;
  margin-top: auto;
}

.date-text { font-size: 12px; color: var(--color-mist); }

.style-zero-price-warning {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08), rgba(245, 158, 11, 0.04));
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.style-zero-price-warning .warning-icon {
  font-size: 16px;
  color: #f59e0b;
  flex-shrink: 0;
}

.style-zero-price-warning .warning-text {
  font-size: 12px;
  font-weight: 600;
  color: #d97706;
}

.style-card:hover .style-zero-price-warning {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(245, 158, 11, 0.08));
  border-color: rgba(245, 158, 11, 0.4);
}

/* ===== 空状态 ===== */
.empty-state { display: flex; flex-direction: column; align-items: center; padding: 60px 0; }
.empty-icon {
  width: 64px; height: 64px; border-radius: 50%;
  background: var(--color-cream);
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; color: var(--color-mist); margin-bottom: 16px;
}
.empty-title { font-size: 15px; font-weight: 600; color: var(--color-slate-light); margin-bottom: 6px; }
.empty-desc { font-size: 13px; color: var(--color-mist); }

/* ===== 分页 ===== */
.pagination-wrap {
  display: flex; justify-content: flex-end;
  margin-top: 20px; padding-top: 16px;
  border-top: 1px solid var(--color-cream-dark);
}

/* ===== 款式详情区 ===== */
.detail-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 款号信息栏 */
.style-info-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  box-shadow: var(--shadow-soft);
}

.style-info-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.info-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-mist);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate);
}

.style-info-actions {
  display: flex;
  gap: 4px;
}

/* 床号列表头部 */
.bed-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 20px;
  line-height: 1;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-slate);
  margin: 0;
}

.bed-count-badge {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-mist);
  background: var(--color-cream);
  padding: 2px 10px;
  border-radius: 10px;
}

.add-bed-btn {
  font-size: 14px;
  padding: 0 20px;
  height: 40px;
}

/* ===== 床号卡片网格 ===== */
.bed-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.bed-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-soft);
  transition: all 0.25s var(--ease-out);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  border: 2px solid transparent;
}
.bed-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
  border-color: var(--color-amber);
}

.bed-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0eeea;
}

.bed-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.bed-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  border-radius: 8px;
  flex-shrink: 0;
}

.bed-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate);
}

.bed-title-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.bed-desc {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-mist);
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bed-status-tag {
  font-weight: 500;
}

.bed-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bed-meta-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bed-info-grid {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.bed-info-item {
  flex: 1;
  text-align: center;
  background: #f8f9fb;
  border-radius: 6px;
  padding: 8px 4px;
}

.info-value {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.info-label {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.bed-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.meta-label {
  font-size: 12px;
  color: var(--color-mist);
}

.meta-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
}

.meta-value.highlight {
  color: var(--color-amber);
  font-size: 18px;
  font-weight: 700;
}

.zero-price-warning {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 12px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08), rgba(245, 158, 11, 0.04));
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.zero-price-warning.clickable {
  cursor: pointer;
}

.zero-price-warning.clickable:hover {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(245, 158, 11, 0.08));
  border-color: rgba(245, 158, 11, 0.4);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.15);
}

.warning-icon {
  font-size: 16px;
  color: #f59e0b;
  flex-shrink: 0;
}

.warning-text {
  font-size: 12px;
  font-weight: 600;
  color: #d97706;
  flex: 1;
}

.info-icon {
  font-size: 14px;
  color: #d97706;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.info-icon:hover {
  opacity: 1;
}

.bed-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f0eeea;
}

.process-count {
  font-size: 12px;
  font-weight: 600;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  padding: 2px 8px;
  border-radius: 8px;
}

.process-count.empty {
  color: var(--color-mist);
  background: rgba(156, 163, 175, 0.08);
}

.card-actions {
  display: flex;
  gap: 4px;
}

/* ========================================
   对话框样式
   ======================================== */

:deep(.style-dialog .el-dialog__header),
:deep(.bed-dialog .el-dialog__header) {
  padding: 20px 28px;
  margin: 0;
  border-bottom: 1px solid var(--color-cream-dark);
}

:deep(.style-dialog .el-dialog__title),
:deep(.bed-dialog .el-dialog__title) {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-slate);
}

:deep(.style-dialog .el-dialog__body),
:deep(.bed-dialog .el-dialog__body) {
  padding: 24px 28px;
}

:deep(.style-dialog .el-dialog__footer),
:deep(.bed-dialog .el-dialog__footer) {
  padding: 16px 28px;
  border-top: 1px solid var(--color-cream-dark);
}

.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

.footer-tip .el-icon {
  font-size: 14px;
  color: #f59e0b;
}

.footer-actions {
  display: flex;
  gap: 12px;
}

.footer-btn {
  font-size: 15px;
  padding: 0 28px;
  height: 44px;
  font-weight: 500;
}

/* ===== 表单字段 ===== */
.field-wrap {
  width: 100%;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
}

.label-required {
  color: #ef4444;
  font-weight: 700;
}

.large-input {
  width: 100%;
}

.large-input :deep(.el-input__inner) {
  height: 44px;
  font-size: 15px;
  padding: 0 16px;
}

.large-input :deep(.el-input__wrapper) {
  padding: 0;
}

.large-input-number {
  width: 100%;
}

.large-input-number :deep(.el-input__inner) {
  height: 44px;
  font-size: 15px;
  text-align: left;
  padding-left: 16px;
}

.large-input-number :deep(.el-input-number__increase),
.large-input-number :deep(.el-input-number__decrease) {
  width: 36px;
  font-size: 16px;
}

/* ===== 床号对话框内的表单 ===== */
.bed-form-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 24px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-col {
  display: flex;
  flex-direction: column;
}

.form-col.full-width {
  grid-column: 1 / -1;
}

/* ===== 工序配置区域 ===== */
.bed-process-section {
  margin-top: 4px;
}

.bed-process-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.process-title-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.process-icon {
  font-size: 16px;
}

.bed-process-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate);
}

.add-process-btn {
  font-size: 14px;
  padding: 0 16px;
  height: 36px;
}

.process-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.import-template-btn {
  font-size: 14px;
  padding: 0 16px;
  height: 36px;
}

/* ===== 工序表格 ===== */
.process-table-wrap {
  border: 1px solid #ebe8e3;
  border-radius: 12px;
  overflow: hidden;
}

.process-table :deep(.el-table__cell) {
  padding: 10px 12px;
}

.process-table :deep(.el-table__header th) {
  font-size: 13px;
  white-space: nowrap;
}

.process-input {
  width: 100%;
}

.process-input :deep(.el-input__inner) {
  height: 38px;
  font-size: 14px;
  padding: 0 12px;
}

.process-input :deep(.el-input__wrapper) {
  padding: 0;
}

.process-input.is-disabled :deep(.el-input__inner) {
  color: #9ca3af;
  background-color: #f9fafb;
  cursor: not-allowed;
}

.process-price-input {
  width: 140px;
}

.process-price-input :deep(.el-input__inner) {
  height: 38px;
  font-size: 14px;
  text-align: center;
}

.price-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.price-input-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.efficiency-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 11px;
  line-height: 1.4;
}

.efficiency-item {
  white-space: nowrap;
}

.efficiency-item.speed {
  color: #3b82f6;
  font-weight: 600;
}

.efficiency-item.detail {
  color: #6b7280;
  font-size: 10px;
}

.efficiency-item.no-data {
  color: #9ca3af;
  font-style: italic;
}

.price-preview {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 11px;
  line-height: 1.4;
}

.preview-item {
  white-space: nowrap;
}

.preview-item.completed {
  color: #10b981;
  font-weight: 600;
}

.preview-item.total {
  color: #6b7280;
}

.seq-num {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-amber);
}

.seq-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.seq-cell .price-locked-icon {
  font-size: 14px;
  color: #f59e0b;
}

.move-btn {
  font-size: 14px;
  color: var(--color-mist);
}
.move-btn:hover:not(:disabled) {
  color: var(--color-amber);
}
.move-btn:disabled {
  cursor: not-allowed;
  opacity: 0.3;
}

.delete-process-btn {
  font-size: 16px;
}

/* ===== 空状态 ===== */
.process-empty {
  padding: 32px 20px;
  text-align: center;
  background: #f5f3f0;
  border-radius: 12px;
  margin-top: 12px;
}

.empty-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.empty-icon-large {
  font-size: 40px;
  color: #d5d0c8;
  margin-bottom: 4px;
}

.process-empty p {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate-light);
  margin: 0;
}

.process-empty span {
  font-size: 13px;
  color: var(--color-mist);
}

/* ===== 手机端 ===== */
@media (max-width: 640px) {
  .cards-grid, .bed-cards-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .search-box { width: 100%; }
  .date-picker { width: 100%; }
  .toolbar-left { flex-direction: column; align-items: stretch; gap: 10px; }

  .style-info-bar {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .style-info-left {
    gap: 20px;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}
</style>
