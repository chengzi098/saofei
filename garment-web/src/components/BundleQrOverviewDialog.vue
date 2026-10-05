<template>
  <el-dialog
    :model-value="modelValue"
    title="全部扎货二维码"
    width="1100px"
    top="4vh"
    destroy-on-close
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input
        v-model="keyword"
        class="filter-keyword"
        placeholder="款号 / 床号 / 名称 / 扎号"
        clearable
        :prefix-icon="Search"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select
        v-model="styleId"
        class="filter-style"
        placeholder="全部款式"
        clearable
        filterable
        @change="onStyleChange"
      >
        <el-option
          v-for="s in styleOptions"
          :key="s.id"
          :label="s.styleNo"
          :value="s.id"
        />
      </el-select>
      <el-select
        v-model="bedId"
        class="filter-bed"
        placeholder="全部床号"
        clearable
        filterable
        :disabled="!styleId"
        :no-data-text="styleId ? '该款式下暂无床号' : '请先选择款式'"
      >
        <el-option
          v-for="b in bedOptions"
          :key="b.id"
          :label="b.bedNo + (b.name ? ' · ' + b.name : '')"
          :value="b.id"
        />
      </el-select>
      <el-select v-model="printed" class="filter-printed" placeholder="打印状态">
        <el-option label="全部" :value="undefined" />
        <el-option label="未打印" :value="0" />
        <el-option label="已打印" :value="1" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 状态 -->
    <div v-if="loading" class="state-tip">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在加载…</span>
    </div>
    <el-empty v-else-if="bundleList.length === 0" description="没有符合条件的扎货" />

    <!-- 二维码网格 -->
    <div v-else class="label-grid">
      <div
        v-for="bundle in bundleList"
        :key="bundle.id"
        class="label-card"
        :class="{ selected: selectedIds.includes(bundle.id) }"
        @click="toggleSelect(bundle.id)"
      >
        <div class="card-top">
          <el-checkbox
            :model-value="selectedIds.includes(bundle.id)"
            @click.stop
            @change="(val) => toggleSelect(bundle.id, val)"
          />
          <span v-if="bundle.printed === 1" class="badge printed">已打印</span>
          <span v-else class="badge unprinted">未打印</span>
        </div>
        <div :data-print-qr="String(bundle.id)" class="qr-host">
          <qrcode-vue :value="getQrUrl(bundle.qrCode)" :size="92" level="M" render-as="svg" />
        </div>
        <div class="label-info">
          <div>款号：{{ bundle.styleNo || '-' }}</div>
          <div>床号：{{ bundle.bedNo || '-' }}</div>
          <div class="bundle-no">扎号：{{ bundle.bundleNo }}</div>
          <div>件数：{{ bundle.quantity }} · 码：{{ bundle.size || '-' }}</div>
          <div v-if="bundle.color || bundle.colors" class="muted">颜色：{{ bundle.color || bundle.colors }}</div>
        </div>
      </div>
    </div>

    <!-- 分页 + 操作 -->
    <div class="footer-bar">
      <div class="footer-left">
        <el-checkbox
          :model-value="isAllPageSelected"
          :indeterminate="isSomePageSelected"
          @change="toggleSelectPage"
        >全选当前页</el-checkbox>
        <span class="selected-count">已选 {{ selectedIds.length }} 个</span>
      </div>
      <div class="footer-right">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="loadBundles"
        />
        <el-button
          type="primary"
          :icon="Printer"
          :disabled="selectedIds.length === 0"
          @click="printSelected"
        >打印选中</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Printer, Search } from '@element-plus/icons-vue'
import QrcodeVue from 'qrcode.vue'
import { getBundleList, markPrintedBundles } from '../api/bundle'
import { getStyleList } from '../api/style'
import { getBeds } from '../api/bed'
import { getQrUrl, openPrintWindow } from '../utils/bundleLabelPrint'
import { useUserStore } from '../stores/user'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const userStore = useUserStore()
const companyName = userStore.currentCompanyName || ''

const loading = ref(false)
const bundleList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 40

const keyword = ref('')
const styleId = ref(null)
const bedId = ref(null)
const printed = ref(undefined)
const styleOptions = ref([])
const bedOptions = ref([])

const selectedIds = ref([])

const pageIdSet = computed(() => new Set(bundleList.value.map(b => b.id)))
const isAllPageSelected = computed(() =>
  bundleList.value.length > 0 && bundleList.value.every(b => selectedIds.value.includes(b.id))
)
const isSomePageSelected = computed(() =>
  !isAllPageSelected.value && bundleList.value.some(b => selectedIds.value.includes(b.id))
)

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      loadStyleOptions()
      search()
    }
  }
)

async function loadStyleOptions() {
  if (styleOptions.value.length > 0) return
  try {
    const res = await getStyleList({ pageNum: 1, pageSize: 200 })
    styleOptions.value = res.data?.records || []
  } catch (e) {
    styleOptions.value = []
  }
}

async function onStyleChange(val) {
  bedId.value = null
  bedOptions.value = []
  if (!val) return
  try {
    const res = await getBeds(val)
    bedOptions.value = res.data || []
  } catch (e) {
    bedOptions.value = []
  }
}

async function loadBundles() {
  loading.value = true
  try {
    const res = await getBundleList({
      pageNum: pageNum.value,
      pageSize,
      keyword: keyword.value || undefined,
      styleId: styleId.value || undefined,
      bedId: bedId.value || undefined,
      printed: printed.value
    })
    bundleList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    bundleList.value = []
    total.value = 0
    ElMessage.error(e.response?.data?.msg || '加载扎货列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  loadBundles()
}

function resetFilter() {
  keyword.value = ''
  styleId.value = null
  bedId.value = null
  bedOptions.value = []
  printed.value = undefined
  pageNum.value = 1
  loadBundles()
}

function toggleSelect(id, forced) {
  if (forced === true) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
    return
  }
  if (forced === false) {
    selectedIds.value = selectedIds.value.filter(x => x !== id)
    return
  }
  // 卡片点击：切换
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter(x => x !== id)
  } else {
    selectedIds.value.push(id)
  }
}

function toggleSelectPage(val) {
  const pageIds = bundleList.value.map(b => b.id)
  if (val) {
    const set = new Set([...selectedIds.value, ...pageIds])
    selectedIds.value = Array.from(set)
  } else {
    selectedIds.value = selectedIds.value.filter(id => !pageIdSet.value.has(id))
  }
}

async function printSelected() {
  const selected = bundleList.value.filter(b => selectedIds.value.includes(b.id))
  if (selected.length === 0) return
  // 仅打印当前页已选中的（跨页选中的不在 DOM 中，无法抓取 SVG）
  const printable = selected.filter(b =>
    document.querySelector(`[data-print-qr="${b.id}"]`)
  )
  if (printable.length === 0) {
    ElMessage.warning('请勾选当前页面中的标签再打印')
    return
  }
  openPrintWindow(printable, { companyName })
  try {
    await markPrintedBundles(printable.map(b => b.id))
    ElMessage.success(`已打印 ${printable.length} 个标签`)
    await loadBundles()
  } catch (e) {
    ElMessage.warning('标签已调用打印，但打印状态更新失败')
  }
}
</script>

<style scoped>
.filter-bar{display:flex;flex-wrap:wrap;gap:10px;align-items:center;margin-bottom:16px}
.filter-keyword{width:230px}
.filter-style{width:170px}
.filter-bed{width:170px}
.filter-printed{width:130px}
.state-tip{display:flex;justify-content:center;gap:8px;padding:60px 0;color:#909399}
.label-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px;max-height:58vh;overflow-y:auto;padding:4px}
.label-card{display:flex;flex-direction:column;align-items:center;gap:8px;padding:12px;border:1px solid #ebe8e3;border-radius:12px;background:#fff;cursor:pointer;transition:border-color .15s,box-shadow .15s}
.label-card:hover{border-color:#c9b8a6}
.label-card.selected{border-color:var(--el-color-primary);box-shadow:0 0 0 2px var(--el-color-primary-light-8)}
.card-top{width:100%;display:flex;align-items:center;justify-content:space-between}
.badge{font-size:11px;padding:1px 6px;border-radius:6px}
.badge.printed{background:#f0f9eb;color:#67c23a}
.badge.unprinted{background:#fef0f0;color:#f56c6c}
.qr-host{display:flex;justify-content:center}
.label-info{width:100%;font-size:12px;line-height:1.55;color:#303133;text-align:center}
.label-info .bundle-no{font-size:13px;font-weight:700}
.label-info .muted{color:#909399}
.footer-bar{display:flex;align-items:center;justify-content:space-between;margin-top:16px;padding-top:14px;border-top:1px solid #ebe8e3}
.footer-left{display:flex;align-items:center;gap:14px}
.selected-count{font-size:13px;color:#606266}
.footer-right{display:flex;align-items:center;gap:14px}
@media(max-width:900px){.label-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
