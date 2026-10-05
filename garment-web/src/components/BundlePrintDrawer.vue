<template>
  <el-drawer
    :model-value="modelValue"
    title="打印二维码"
    size="720px"
    destroy-on-close
    @close="$emit('update:modelValue', false)"
  >
    <div class="drawer-toolbar">
      <div>
        <div class="drawer-title">{{ styleNo || '-' }} · {{ bedNo || '-' }}</div>
        <div class="drawer-subtitle">共 {{ bundleList.length }} 个标签</div>
      </div>
      <el-button type="primary" :icon="Printer" :disabled="bundleList.length === 0" @click="printAll">
        打印全部
      </el-button>
    </div>

    <div v-if="loading" class="drawer-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在加载标签…</span>
    </div>
    <el-empty v-else-if="bundleList.length === 0" description="当前床号没有可打印标签" />

    <div v-else class="label-grid">
      <div v-for="bundle in bundleList" :key="bundle.id" class="label-card">
        <div class="label-info">
          <div class="company">{{ companyName }}</div>
          <div>款号：{{ bundle.styleNo || styleNo || '-' }}</div>
          <div>床号：{{ bundle.bedNo || bedNo || '-' }}</div>
          <div class="bundle-no">扎号：{{ bundle.bundleNo }}</div>
          <div>件数：{{ bundle.quantity }}</div>
          <div>码数：{{ bundle.size || '-' }}</div>
          <div>颜色：{{ bundle.color || bundle.colors || '-' }}</div>
        </div>
        <div :data-print-qr="String(bundle.id)">
          <qrcode-vue :value="getQrUrl(bundle.qrCode)" :size="96" level="M" render-as="svg" />
        </div>
        <el-button plain type="primary" :icon="Printer" @click="printOne(bundle)">打印此标签</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Printer } from '@element-plus/icons-vue'
import QrcodeVue from 'qrcode.vue'
import { getBundleList, markPrintedBundles } from '../api/bundle'
import { getQrUrl, openPrintWindow } from '../utils/bundleLabelPrint'
import { useUserStore } from '../stores/user'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  bedId: { type: [Number, String], default: null },
  styleNo: { type: String, default: '' },
  bedNo: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'printed'])
const userStore = useUserStore()
const loading = ref(false)
const bundleList = ref([])
const companyName = userStore.currentCompanyName || ''

watch(
  () => [props.modelValue, props.bedId],
  ([open, bedId]) => {
    if (open && bedId) loadBundles()
  },
  { immediate: true }
)

async function loadBundles() {
  loading.value = true
  try {
    const res = await getBundleList({ bedId: props.bedId, pageNum: 1, pageSize: 500 })
    bundleList.value = res.data?.records || []
  } catch (error) {
    bundleList.value = []
    ElMessage.error('加载打印标签失败')
  } finally {
    loading.value = false
  }
}

function getPrintFallback() {
  return { companyName, styleNo: props.styleNo, bedNo: props.bedNo }
}

async function markPrinted(labels) {
  const ids = labels.map((label) => label.id).filter(Boolean)
  if (ids.length === 0) return
  try {
    await markPrintedBundles(ids)
    emit('printed')
  } catch (error) {
    ElMessage.warning('标签已调用打印，但打印状态更新失败')
  }
}

function printAll() {
  openPrintWindow(bundleList.value, getPrintFallback())
  markPrinted(bundleList.value)
}

function printOne(bundle) {
  openPrintWindow([bundle], getPrintFallback())
  markPrinted([bundle])
}
</script>

<style scoped>
.drawer-toolbar{display:flex;align-items:center;justify-content:space-between;margin-bottom:18px;padding-bottom:14px;border-bottom:1px solid #ebe8e3}
.drawer-title{font-size:17px;font-weight:700;color:var(--color-slate)}.drawer-subtitle{margin-top:4px;font-size:12px;color:var(--color-mist)}
.drawer-state{display:flex;justify-content:center;gap:8px;padding:60px 0;color:#909399}
.label-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}
.label-card{display:flex;flex-direction:column;align-items:center;gap:10px;padding:14px;border:1px solid #ebe8e3;border-radius:12px;background:#fff}
.label-info{width:100%;font-size:13px;line-height:1.6;color:#303133}.company{font-size:15px;font-weight:700}.bundle-no{font-size:14px;font-weight:700}
@media(max-width:720px){.label-grid{grid-template-columns:1fr}}
</style>
