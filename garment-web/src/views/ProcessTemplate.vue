<template>
  <div class="template-manage">
    <!-- 工具栏 -->
    <div class="toolbar" :class="{ 'entered': mounted }">
      <div class="toolbar-left">
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="searchKeyword"
            placeholder="搜索模板、备注或工序"
            class="search-input"
            @keyup.enter="filterList"
          />
          <el-icon v-if="searchKeyword" class="clear-icon" @click="searchKeyword = ''"><CircleClose /></el-icon>
        </div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog('add')" class="add-btn">
        新建模板
      </el-button>
    </div>

    <!-- 模板卡片网格 -->
    <div class="cards-grid" :class="{ 'entered': mounted }">
      <div
        v-for="item in filteredList"
        :key="item.id"
        class="template-card"
        @click="openDialog('edit', item)"
      >
        <div class="card-header">
          <span class="template-name">{{ item.name }}</span>
          <span class="item-count" :class="{ zero: item.itemCount === 0 }">
            {{ item.itemCount }} 道工序
          </span>
        </div>
        <div v-if="item.remark" class="card-body">
          <span class="remark-text">{{ item.remark }}</span>
        </div>
        <div class="card-footer">
          <span class="date-text">{{ formatDate(item.createTime) }}</span>
          <div class="card-actions" @click.stop>
            <el-popconfirm
              title="确定删除该模板吗？"
              @confirm="handleDelete(item)"
            >
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && filteredList.length === 0" class="empty-state">
      <div class="empty-icon">
        <el-icon><Collection /></el-icon>
      </div>
      <p class="empty-title">{{ searchKeyword ? '没有找到匹配的模板' : '暂无工序模板' }}</p>
      <p class="empty-desc">{{ searchKeyword ? '换个关键词试试' : '点击右上角「新建模板」开始创建' }}</p>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新建模板' : '编辑模板'"
      width="720px"
      destroy-on-close
      class="template-dialog"
    >
      <div class="dialog-body">
        <!-- 基本信息 -->
        <div class="form-row">
          <div class="form-col">
            <label class="field-label">
              <span class="label-text">模板名称</span>
              <span class="label-required">*</span>
            </label>
            <el-input
              v-model="form.name"
              placeholder="如：T恤通用、卫衣标准"
              size="large"
              class="large-input"
            />
          </div>
          <div class="form-col">
            <label class="field-label">
              <span class="label-text">备注</span>
            </label>
            <el-input v-model="form.remark" placeholder="选填" size="large" class="large-input" />
          </div>
        </div>

        <!-- 工序列表 -->
        <div class="process-section">
          <div class="process-header">
            <div class="process-title-wrap">
              <span class="process-icon">⚙️</span>
              <span class="process-title">工序列表</span>
            </div>
            <el-button type="primary" :icon="Plus" size="default" @click="addItemRow" class="add-process-btn">
              添加工序
            </el-button>
          </div>

          <div class="process-table-wrap">
            <el-table
              :data="form.items"
              style="width: 100%"
              class="process-table"
              :header-cell-style="headerStyle"
              size="default"
            >
              <el-table-column label="序号" width="90" align="center">
                <template #default="{ $index }">
                  <span class="seq-num">{{ $index + 1 }}</span>
                </template>
              </el-table-column>
              <el-table-column label="工序名称" min-width="100">
                <template #default="{ row }">
                  <el-input v-model="row.name" placeholder="如：裁剪、缝纫" size="default" class="process-input" />
                </template>
              </el-table-column>
              <el-table-column label="单价（元）" width="220" align="center">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.price"
                    :min="0"
                    :precision="2"
                    :step="0.1"
                    size="default"
                    class="process-price-input"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" size="default" @click="removeItemRow($index)" class="delete-process-btn">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-if="form.items.length === 0" class="process-empty">
            <div class="empty-inner">
              <el-icon class="empty-icon-large"><Collection /></el-icon>
              <p>暂无工序</p>
              <span>点击「添加工序」开始添加</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="dialogVisible = false" class="footer-btn">取消</el-button>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSave" class="footer-btn">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Collection, CircleClose, Delete } from '@element-plus/icons-vue'
import { getProcessTemplateList, getProcessTemplateDetail, addProcessTemplate, updateProcessTemplate, deleteProcessTemplate } from '../api/processTemplate'
import { filterProcessTemplates } from '../utils/productionManagementSearch'

const mounted = ref(false)
const loading = ref(false)
const submitting = ref(false)
const searchKeyword = ref('')
const list = ref([])

const dialogVisible = ref(false)
const dialogMode = ref('add')
const editId = ref(null)
const form = reactive({
  name: '',
  remark: '',
  items: []
})

const headerStyle = () => ({
  background: '#f5f3f0',
  fontWeight: 600,
  color: '#4a4846',
  fontSize: '14px',
  padding: '14px 16px',
  whiteSpace: 'nowrap'
})

const filteredList = computed(() =>
  filterProcessTemplates(list.value, searchKeyword.value)
)

function filterList() {
  // computed 自动过滤，此方法用于回车触发
}

onMounted(() => {
  requestAnimationFrame(() => { mounted.value = true })
  loadList()
})

async function loadList() {
  loading.value = true
  try {
    const res = await getProcessTemplateList()
    const templates = res.data || []
    const details = await Promise.all(
      templates.map(async (template) => {
        try {
          const detailRes = await getProcessTemplateDetail(template.id)
          return {
            ...template,
            items: detailRes.data?.items || []
          }
        } catch (e) {
          return {
            ...template,
            items: []
          }
        }
      })
    )
    list.value = details
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

function openDialog(mode, item = null) {
  dialogMode.value = mode
  editId.value = null
  form.name = ''
  form.remark = ''
  form.items = []

  if (mode === 'edit' && item) {
    editId.value = item.id
    // 加载详情
    getProcessTemplateDetail(item.id).then(res => {
      const detail = res.data
      form.name = detail.name
      form.remark = detail.remark || ''
      form.items = (detail.items || []).map(i => ({
        name: i.name,
        seq: i.seq,
        price: i.price
      }))
    })
  }

  dialogVisible.value = true
}

function addItemRow() {
  form.items.push({
    name: '',
    seq: form.items.length + 1,
    price: 0
  })
}

function removeItemRow(index) {
  form.items.splice(index, 1)
}

async function handleSave() {
  if (!form.name || !form.name.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }

  const items = form.items.filter(i => i.name && i.name.trim())
  for (let i = 0; i < items.length; i++) {
    if (items[i].price != null && items[i].price < 0) {
      ElMessage.warning('工序「' + items[i].name + '」的单价不能为负数')
      return
    }
  }

  const payload = {
    name: form.name.trim(),
    remark: form.remark || null,
    items: items.map((i, idx) => ({
      name: i.name.trim(),
      seq: idx + 1,
      price: i.price || 0
    }))
  }

  submitting.value = true
  try {
    if (editId.value) {
      await updateProcessTemplate(editId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await addProcessTemplate(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (e) {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(item) {
  try {
    await deleteProcessTemplate(item.id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) {
    // handled
  }
}
</script>

<style scoped>
.template-manage {
  width: 100%;
  min-width: 0;
}

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
.add-btn { font-weight: 500; }

/* ===== 卡片网格 ===== */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.4s var(--ease-out) 0.2s;
}
.cards-grid.entered { opacity: 1; transform: translateY(0); }

.template-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
  box-shadow: var(--shadow-soft);
  transition: all 0.25s var(--ease-out);
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
}
.template-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.template-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-slate);
}

.item-count {
  font-size: 12px;
  font-weight: 500;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  padding: 2px 10px;
  border-radius: 12px;
}
.item-count.zero {
  color: #9ca3af;
  background: rgba(156, 163, 175, 0.1);
}

.card-body {
  padding: 0;
}

.remark-text {
  font-size: 13px;
  color: var(--color-mist);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
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

.card-actions { display: flex; gap: 4px; }

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

/* ===== 对话框 ===== */
:deep(.template-dialog .el-dialog__header) {
  padding: 20px 28px;
  margin: 0;
  border-bottom: 1px solid var(--color-cream-dark);
}
:deep(.template-dialog .el-dialog__title) {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-slate);
}
:deep(.template-dialog .el-dialog__body) {
  padding: 24px 28px;
}
:deep(.template-dialog .el-dialog__footer) {
  padding: 16px 28px;
  border-top: 1px solid var(--color-cream-dark);
}

.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
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

.field-label {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-slate);
}
.label-required { color: #ef4444; font-weight: 700; }
.label-text { }

.large-input :deep(.el-input__inner) {
  height: 44px;
  font-size: 15px;
  padding: 0 16px;
}
.large-input :deep(.el-input__wrapper) { padding: 0; }

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}
.footer-btn {
  font-size: 15px;
  padding: 0 28px;
  height: 44px;
  font-weight: 500;
}

/* ===== 工序配置区域 ===== */
.process-section {
  margin-top: 4px;
}

.process-header {
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

.process-icon { font-size: 16px; }

.process-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-slate);
}

.add-process-btn {
  font-size: 14px;
  padding: 0 16px;
  height: 36px;
}

.process-table-wrap {
  border: 1px solid #ebe8e3;
  border-radius: 12px;
  overflow: hidden;
}

.process-table :deep(.el-table__cell) {
  padding: 12px 14px;
}

.process-input :deep(.el-input__inner) {
  height: 38px;
  font-size: 14px;
  padding: 0 12px;
}

.process-price-input {
  width: 180px;
}
.process-price-input :deep(.el-input__inner) {
  height: 38px;
  font-size: 14px;
  text-align: center;
}

.seq-num {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-amber);
}

.delete-process-btn { font-size: 16px; }

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
  .cards-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .search-box { width: 100%; }
  .toolbar-left { flex: 1; }
  .form-row {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}
</style>
