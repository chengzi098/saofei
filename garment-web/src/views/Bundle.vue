<template>
  <div class="bundle-manage">
    <div class="workflow-card">
      <div class="workflow-heading">
        <div>
          <h2>生产管理</h2>
          <p>按款式、床号、分扎、打印连续操作，当前上下文不会丢失。</p>
        </div>
        <div class="workflow-actions">
          <el-button type="primary" plain :icon="Picture" @click="qrOverviewVisible = true">
            查看全部二维码
          </el-button>
          <el-button v-if="!selectedStyleId" type="primary" :icon="Plus" @click="openStyleDialog">
            新增款式
          </el-button>
        </div>
      </div>
      <div class="workflow-steps">
        <div
          v-for="(stage, index) in productionStages"
          :key="stage.key"
          class="workflow-step"
          :class="{ active: stage.key === currentStage, done: index < currentStageIndex }"
        >
          <span class="step-index">{{ index + 1 }}</span>
          <span>{{ stage.label }}</span>
        </div>
      </div>
    </div>

    <!-- 搜索工具栏 -->
    <div v-if="!selectedBedId" class="toolbar" :class="{ 'entered': mounted }">
      <div class="toolbar-left">
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="searchKeyword"
            :placeholder="searchPlaceholder"
            class="search-input"
          />
          <el-icon v-if="searchKeyword" class="clear-icon" @click="searchKeyword = ''"><CircleClose /></el-icon>
        </div>
      </div>
    </div>

    <button
      v-if="selectedStyleId"
      type="button"
      class="level-back-button"
      @click="selectedBedId ? backToBed() : backToStyle()"
    >
      <el-icon><ArrowLeft /></el-icon>
      <span>{{ selectedBedId ? '返回床号列表' : '返回款式列表' }}</span>
    </button>

    <!-- 面包屑导航 -->
    <div class="breadcrumb" v-if="selectedStyleId">
      <span class="crumb">款式</span>
      <el-icon class="crumb-sep"><ArrowRight /></el-icon>
      <span class="crumb" v-if="!selectedBedId">{{ selectedStyleName }}</span>
      <span class="crumb" v-if="selectedBedId">{{ selectedStyleName }}</span>
      <template v-if="selectedBedId">
        <el-icon class="crumb-sep"><ArrowRight /></el-icon>
        <span class="crumb">{{ selectedBedNo }}</span>
      </template>
    </div>

    <!-- Step 1: 款式卡片网格 -->
    <div v-if="!selectedStyleId" class="cards-grid" :class="{ 'entered': mounted }">
      <div
        v-for="s in filteredStyles"
        :key="s.id"
        class="style-card"
        @click="selectStyle(s)"
      >
        <div class="card-header">
          <span class="style-no">{{ s.styleNo || '-' }}</span>
          <span class="bundle-tag" :class="s._hasBundles ? 'has-bundle' : 'no-bundle'">
            {{ s._hasBundles ? '已分扎' : '未分扎' }}
          </span>
        </div>
        <div class="card-body">
          <h3 class="style-name">{{ s.name }}</h3>
          <div class="card-meta">
            <span v-if="s.quantity > 0" class="meta-item">
              <el-icon><Aim /></el-icon> {{ s.quantity }} 件
            </span>
            <span v-if="s.processCount > 0" class="meta-item">
              <el-icon><Tickets /></el-icon> {{ s.processCount }} 工序
            </span>
          </div>
        </div>
        <div class="card-footer" v-if="s._bundleCount > 0">
          <span class="bundle-count">{{ s._bundleCount }} 扎已保存</span>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && !selectedStyleId && filteredStyles.length === 0" class="empty-state">
      <div class="empty-icon"><el-icon><Collection /></el-icon></div>
      <p class="empty-title">{{ searchKeyword ? '没有找到匹配的款式' : '暂无款式' }}</p>
      <p class="empty-desc">{{ searchKeyword ? '换个关键词试试' : '请先去「款式管理」添加款式' }}</p>
    </div>

    <!-- 款式分页 -->
    <div v-if="!selectedStyleId" class="pagination-wrap">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="totalStyles"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadStyles"
        @current-change="loadStyles"
      />
    </div>

    <!-- Step 2: 床号卡片网格 -->
    <div v-if="selectedStyleId && !selectedBedId" class="bed-section">
      <!-- 床号筛选栏 -->
      <div class="bed-filter-bar">
        <el-radio-group v-model="bedFilter" size="small">
          <el-radio-button value="all">全部 ({{ bedList.length }})</el-radio-button>
          <el-radio-button value="bundled">已分扎 ({{ bedBundledCount }})</el-radio-button>
          <el-radio-button value="unbundled">未分扎 ({{ bedUnbundledCount }})</el-radio-button>
        </el-radio-group>
        <el-button type="primary" :icon="Plus" @click="openBedDialog">新增床号</el-button>
      </div>

      <div class="bed-cards-grid">
        <div
          v-for="bed in paginatedBeds"
          :key="bed.id"
          class="bed-card"
          @click="selectBed(bed)"
        >
          <div class="bed-card-header">
            <span class="bed-no">{{ bed.bedNo }}</span>
            <el-tag
              v-if="bed.hasWorkRecords"
              type="warning"
              size="small"
            >
              已报工
            </el-tag>
            <el-tag
              v-else
              :type="bed.bundleCount > 0 ? 'success' : 'info'"
              size="small"
            >
              {{ bed.bundleCount > 0 ? '已分扎' : '未分扎' }}
            </el-tag>
          </div>
          <div class="bed-card-body">
            <div class="bed-name" v-if="bed.name">{{ bed.name }}</div>
            <div class="bed-info-grid">
              <div class="bed-info-item">
                <span class="bed-info-val">{{ bed.quantity }}</span>
                <span class="bed-info-lbl">件数</span>
              </div>
              <div class="bed-info-item">
                <span class="bed-info-val">{{ bed.bundleCount || 0 }}</span>
                <span class="bed-info-lbl">已分扎</span>
              </div>
              <div class="bed-info-item">
                <span class="bed-info-val">{{ bed.processCount || 0 }}</span>
                <span class="bed-info-lbl">工序数</span>
              </div>
            </div>
            <div class="bed-processes">
              <span class="bed-process-label">工序</span>
              <div v-if="bed.processes?.length" class="bed-process-list">
                <span
                  v-for="process in bed.processes"
                  :key="process.id || `${bed.id}-${process.seq}-${process.name}`"
                  class="bed-process-chip"
                  :title="process.name"
                >
                  {{ process.name }}
                </span>
              </div>
              <span v-else class="bed-process-empty">暂无工序</span>
            </div>
            <div v-if="bed.colors" class="bed-meta">
              <span class="meta-label">颜色</span>
              <span class="meta-value">{{ bed.colors }}</span>
            </div>
          </div>
          <div class="bed-card-footer" @click.stop>
            <el-button link type="primary" size="small" @click="openProcessEditor(bed)">
              <el-icon><Edit /></el-icon>
              编辑工序
            </el-button>
            <el-popconfirm
              v-if="!bed.hasWorkRecords"
              :title="bed.bundleCount > 0 ? `确定删除该床号吗？将同时删除 ${bed.bundleCount} 个扎货` : '确定删除该床号吗？'"
              confirm-button-text="确定删除"
              confirm-button-type="danger"
              @confirm="handleDeleteBed(bed)"
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

      <!-- 床号分页 -->
      <div v-if="filteredBeds.length > bedPageSize" class="pagination-wrap">
        <el-pagination
          v-model:current-page="bedPageNum"
          :page-size="bedPageSize"
          :total="filteredBeds.length"
          layout="prev, pager, next"
          :pager-count="5"
        />
      </div>

      <div v-if="bedList.length === 0" class="empty-state">
        <div class="empty-icon"><el-icon><Grid /></el-icon></div>
        <p class="empty-title">暂无床号</p>
        <p class="empty-desc">请先去「款式管理」为该款式添加床号</p>
      </div>
      <div v-else-if="filteredBeds.length === 0" class="empty-state">
        <div class="empty-icon"><el-icon><Grid /></el-icon></div>
        <p class="empty-title">没有匹配的床号</p>
        <p class="empty-desc">试试切换其他筛选条件</p>
      </div>
    </div>

    <!-- Step 3: 扎货编辑区（选中床号后显示） -->
    <transition name="slide-up">
      <div class="edit-section" v-if="selectedBedId">
        <div class="edit-header">
          <div class="edit-title-row">
            <h3 class="edit-title">{{ selectedBedNo }}</h3>
            <span class="edit-badge" :class="quantityMatch ? 'badge-ok' : 'badge-warn'">
              {{ totalQuantity }} / {{ bedQuantity }} 件
            </span>
          </div>

          <!-- 进度条 -->
          <div class="progress-bar-wrap" v-if="bedQuantity > 0">
            <div class="progress-bar">
              <div
                class="progress-fill"
                :class="{ 'over': totalQuantity > bedQuantity }"
                :style="{ width: progressPercent + '%' }"
              ></div>
            </div>
            <div class="progress-text">
              <span v-if="quantityMatch" class="text-ok"><el-icon><CircleCheck /></el-icon> 数量已匹配</span>
              <span v-else-if="totalQuantity < bedQuantity">还差 <b>{{ bedQuantity - totalQuantity }}</b> 件</span>
              <span v-else class="text-over">超出 <b>{{ totalQuantity - bedQuantity }}</b> 件</span>
            </div>
          </div>
        </div>

        <!-- 交叉表模式 -->
        <div v-if="crossTableData.rows.length > 0" class="cross-table-section">
          <div class="cross-table-header">
            <span class="cross-table-title">分扎表格</span>
            <el-button size="small" type="warning" plain @click="resetToSetup">
              <el-icon><RefreshLeft /></el-icon> 重新生成表格
            </el-button>
          </div>
          <div class="table-wrap">
            <table class="cross-table">
              <thead>
                <tr>
                  <th class="cross-col-color">颜色</th>
                  <template v-for="size in crossTableData.sizes" :key="size.name">
                    <th class="cross-col-bundleno" :colspan="1">{{ size.name }} 扎号</th>
                    <th class="cross-col-qty">{{ size.name }} 数量</th>
                  </template>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, rowIdx) in crossTableData.rows" :key="rowIdx">
                  <td class="cross-col-color">
                    <div class="color-cell">
                      <input
                        class="table-input color-input"
                        v-model="row.color"
                        placeholder="颜色"
                        @focus="$event.target.select()"
                      />
                      <button
                        v-if="rowIdx > 0 && !row.color"
                        class="copy-up-btn"
                        title="复制上一行颜色"
                        @mousedown.prevent="row.color = crossTableData.rows[rowIdx - 1].color"
                      >
                        ↑ 同上
                      </button>
                    </div>
                  </td>
                  <template v-for="size in crossTableData.sizes" :key="`${rowIdx}-${size.name}`">
                    <td class="cross-col-bundleno">
                      <template v-if="row.cells[size.name]">
                        <span class="bundle-no-readonly">{{ row.cells[size.name].bundleNo }}</span>
                      </template>
                      <template v-else>
                        <span class="cell-disabled">-</span>
                      </template>
                    </td>
                    <td class="cross-col-qty">
                      <template v-if="row.cells[size.name]">
                        <div class="qty-cell-wrap">
                          <input
                            class="table-input num-input"
                            type="number"
                            :value="row.cells[size.name].quantity"
                            @input="row.cells[size.name].quantity = $event.target.value ? Number($event.target.value) : null"
                            min="1"
                            max="9999"
                            placeholder="0"
                            @focus="$event.target.select()"
                          />
                          <button
                            v-if="rowIdx > 0 && !row.cells[size.name].quantity && crossTableData.rows[rowIdx - 1].cells[size.name]?.quantity"
                            class="copy-up-btn"
                            title="复制上一行数量"
                            @mousedown.prevent="row.cells[size.name].quantity = crossTableData.rows[rowIdx - 1].cells[size.name].quantity"
                          >
                            ↑ 同上
                          </button>
                        </div>
                      </template>
                      <template v-else>
                        <span class="cell-disabled">-</span>
                      </template>
                    </td>
                  </template>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td class="cross-col-color total-label">合计</td>
                  <template v-for="size in crossTableData.sizes" :key="`total-${size.name}`">
                    <td class="cross-col-bundleno">
                      {{ size.bundleCount }}扎
                      <button class="add-bundle-btn remove-bundle-btn" title="减少一扎" @click="removeBundleForSize(size.name)">－</button>
                      <button class="add-bundle-btn" title="添加一扎" @click="addBundleForSize(size.name)">＋</button>
                    </td>
                    <td class="cross-col-qty total-qty-cell">
                      {{ crossTableData.rows.reduce((sum, r) => sum + (r.cells[size.name]?.quantity || 0), 0) }}
                    </td>
                  </template>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>

        <!-- 传统模式：已保存的扎货列表 -->
        <div class="table-wrap" v-else-if="bundleList.length > 0" @paste="handleTablePaste">
          <table class="bundle-table">
            <thead>
              <tr>
                <th class="col-no">#</th>
                <th class="col-bundle-no">扎号</th>
                <th class="col-qty">件数</th>
                <th class="col-size">尺码</th>
                <th class="col-color">颜色</th>
                <th class="col-action"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(b, idx) in bundleList" :key="idx" :class="{ 'printed-row': b.printed === 1 }">
                <td class="col-no">
                  <span class="row-num">{{ idx + 1 }}</span>
                </td>
                <td class="col-bundle-no">
                  <input
                    :ref="el => setInputRef(el, idx, 'bundleNo')"
                    class="table-input mono"
                    v-model="b.bundleNo"
                    :placeholder="`${myPrefix.value}${String(idx + 1).padStart(3, '0')}`"
                    :disabled="b.printed === 1"
                    @keydown="e => onTableKeydown(e, idx, 'bundleNo')"
                  />
                </td>
                <td class="col-qty">
                  <input
                    :ref="el => setInputRef(el, idx, 'quantity')"
                    class="table-input num-input"
                    type="number"
                    :value="b.quantity"
                    @input="b.quantity = $event.target.value ? Number($event.target.value) : null"
                    min="1"
                    max="9999"
                    :disabled="b.printed === 1"
                    @focus="$event.target.select()"
                    @keydown="e => onTableKeydown(e, idx, 'quantity')"
                  />
                </td>
                <td class="col-size">
                  <div class="size-cell">
                    <input
                      :ref="el => setInputRef(el, idx, 'size')"
                      class="table-input size-input"
                      v-model="b.size"
                      placeholder="尺码"
                      :disabled="b.printed === 1"
                      @focus="$event.target.select(); onSizeFocus(idx)"
                      @blur="onSizeBlur(idx)"
                      @keydown="e => onTableKeydown(e, idx, 'size')"
                    />
                    <button
                      v-if="idx > 0 && !b.size && b.printed !== 1"
                      class="copy-up-btn"
                      title="复制上一行尺码"
                      @mousedown.prevent="b.size = bundleList[idx - 1].size"
                    >
                      ↑ 同上
                    </button>
                    <div
                      v-if="activeSizeRow === idx && b.printed !== 1"
                      class="size-picker"
                      :class="{ 'picker-up': idx >= bundleList.length - 3 }"
                      @mousedown.prevent
                    >
                      <div class="size-picker-row">
                        <button
                          v-for="s in quickSizes"
                          :key="s"
                          class="size-pick-btn"
                          :class="{ active: b.size === s }"
                          @mousedown.prevent="b.size = s; focusNext(idx, 'size')"
                        >
                          {{ s }}
                        </button>
                      </div>
                    </div>
                  </div>
                </td>
                <td class="col-color">
                  <div class="color-cell">
                    <input
                      :ref="el => setInputRef(el, idx, 'color')"
                      class="table-input color-input"
                      v-model="b.color"
                      placeholder="颜色"
                      :disabled="b.printed === 1"
                      @focus="$event.target.select()"
                      @keydown="e => onTableKeydown(e, idx, 'color')"
                    />
                    <button
                      v-if="idx > 0 && !b.color && b.printed !== 1"
                      class="copy-up-btn"
                      title="复制上一行颜色"
                      @mousedown.prevent="b.color = bundleList[idx - 1].color"
                    >
                      ↑ 同上
                    </button>
                  </div>
                </td>
                <td class="col-action">
                  <span v-if="b.printed === 1" class="printed-badge">已打印</span>
                  <el-button v-else type="danger" text size="small" @click="removeBundle(idx)" class="row-del">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="2" class="total-label">合计 {{ bundleList.length }} 扎</td>
                <td class="total-qty">{{ totalQuantity }}</td>
                <td colspan="3"></td>
              </tr>
            </tfoot>
          </table>
        </div>

        <!-- 底部操作 -->
        <div class="section-actions">
          <el-tooltip :disabled="!hasPrintedBundles" content="已打印二维码，无法清空" placement="top">
            <el-button @click="clearAll" size="large" :disabled="bundleList.length === 0 || hasPrintedBundles">清空</el-button>
          </el-tooltip>
          <el-tooltip :disabled="!hasPrintedBundles" content="已打印二维码，无法保存" placement="top">
            <el-button
              type="primary"
              size="large"
              @click="saveBundles"
              :loading="saving"
              :disabled="!canSave || hasPrintedBundles"
              class="save-btn"
            >
              {{ saveBtnText }}
            </el-button>
          </el-tooltip>
        </div>

        <!-- 打印按钮（有已保存的扎货时显示） -->
        <el-button
          v-if="hasSavedBundles"
          type="success"
          size="large"
          @click="goPrint"
          class="print-btn"
        >
          <el-icon><Printer /></el-icon>
          打印二维码
        </el-button>
      </div>
    </transition>

    <BundlePrintDrawer
      v-model="printDrawerVisible"
      :bed-id="selectedBedId"
      :style-no="selectedStyle?.styleNo || ''"
      :bed-no="selectedBed?.bedNo || ''"
      @printed="reloadSavedBundles"
    />

    <BundleQrOverviewDialog v-model="qrOverviewVisible" />

    <el-dialog v-model="styleDialogVisible" title="新增款式" width="460px" destroy-on-close>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="款号" required>
          <el-input
            v-model="styleForm.styleNo"
            placeholder="例如：TX-001"
            maxlength="50"
            size="large"
            @keyup.enter="saveStyle"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="styleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingStyle" @click="saveStyle">保存并添加床号</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bedDialogVisible" title="新增床号" width="640px" destroy-on-close>
      <el-form label-position="top" @submit.prevent>
        <div class="bed-form-grid">
          <el-form-item label="床号编号" required>
            <el-input v-model="bedForm.bedNo" placeholder="例如：1号床" />
          </el-form-item>
          <el-form-item label="名称" required>
            <el-input v-model="bedForm.name" placeholder="例如：大货" />
          </el-form-item>
          <el-form-item label="件数" required>
            <el-input-number v-model="bedForm.quantity" :min="1" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="工序模板">
          <el-select
            v-model="bedForm.templateId"
            clearable
            filterable
            placeholder="可选；模板本身不会被修改"
            style="width: 100%"
          >
            <el-option
              v-for="template in processTemplates"
              :key="template.id"
              :label="`${template.name}（${template.itemCount || 0}道工序）`"
              :value="template.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bedForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>

        <!-- 工序列表 -->
        <div class="bed-dialog-processes">
          <div class="bed-dialog-processes-header">
            <span class="bed-dialog-processes-title">⚙️ 工序列表</span>
            <el-button size="small" text type="primary" @click="addBedProcessRow">
              <el-icon><Plus /></el-icon> 添加工序
            </el-button>
          </div>

          <el-input
            v-model="quickProcessName"
            placeholder="输入工序名称，按回车快速添加"
            size="default"
            @keyup.enter="quickAddBedProcess"
            class="bed-dialog-quick-input"
          >
            <template #prefix><el-icon><Plus /></el-icon></template>
          </el-input>

          <div v-if="bedForm.processes.length === 0" class="bed-dialog-process-empty">
            暂无工序，可通过上方输入框快速添加或选择模板导入
          </div>

          <div v-else class="bed-dialog-process-list">
            <div
              v-for="(p, index) in bedForm.processes"
              :key="index"
              class="bed-dialog-process-item"
            >
              <span class="bed-dialog-process-seq">{{ index + 1 }}</span>
              <el-input
                v-model="p.name"
                placeholder="工序名称"
                size="small"
                class="bed-dialog-process-name"
              />
              <el-input-number
                v-model="p.price"
                :min="0"
                :precision="2"
                :step="0.1"
                size="small"
                controls-position="right"
                class="bed-dialog-process-price"
              />
              <div class="bed-dialog-process-actions">
                <el-button :icon="Top" circle text size="small" :disabled="index === 0" @click="moveBedProcess(index, -1)" />
                <el-button :icon="Bottom" circle text size="small" :disabled="index === bedForm.processes.length - 1" @click="moveBedProcess(index, 1)" />
                <el-button :icon="Delete" circle text type="danger" size="small" @click="removeBedProcess(index)" />
              </div>
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="bedDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingBed" @click="saveBed">保存并开始分扎</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="processEditorVisible"
      :title="`${processForm.bedNo || '床号'} · 编辑工序`"
      width="720px"
      destroy-on-close
      class="process-editor-dialog"
    >
      <div class="process-editor-tip">
        单价为 0 的工序可修改名称和单价；单价大于 0 后将锁定。
      </div>

      <div class="process-editor-list">
        <div
          v-for="(row, index) in processForm.rows"
          :key="row.key"
          class="process-editor-row"
        >
          <span class="process-editor-seq">{{ index + 1 }}</span>
          <el-input
            v-model="row.name"
            placeholder="工序名称"
            :disabled="row.locked"
            class="process-name-input"
          />
          <el-input-number
            v-model="row.price"
            :min="0"
            :precision="2"
            :step="0.1"
            :disabled="row.locked"
            controls-position="right"
            class="process-price-input"
          />
          <div class="process-row-actions">
            <el-tooltip v-if="row.locked" content="该工序已定价，不能修改" placement="top">
              <el-icon class="process-lock"><Lock /></el-icon>
            </el-tooltip>
            <template v-else>
              <el-button
                :icon="Top"
                circle
                text
                title="上移"
                :disabled="index === 0 || processForm.rows[index - 1]?.locked"
                @click="moveProcess(index, -1)"
              />
              <el-button
                :icon="Bottom"
                circle
                text
                title="下移"
                :disabled="index === processForm.rows.length - 1 || processForm.rows[index + 1]?.locked"
                @click="moveProcess(index, 1)"
              />
              <el-button
                :icon="Delete"
                circle
                text
                type="danger"
                :disabled="row.hasWorkRecords"
                :title="row.hasWorkRecords ? '已有报工，不能删除' : '删除工序'"
                @click="removeProcess(index)"
              />
              <span v-if="row.hasWorkRecords" class="reported-process-tip">已有报工，不能删除</span>
            </template>
          </div>
        </div>
      </div>

      <el-button type="primary" plain :icon="Plus" @click="addProcess">
        添加工序
      </el-button>

      <template #footer>
        <el-button @click="processEditorVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProcesses" @click="saveProcesses">
          保存工序
        </el-button>
      </template>
    </el-dialog>

    <!-- 分扎设置对话框 -->
    <el-dialog
      v-model="bundleSetupVisible"
      title="分扎设置"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="setup-hint">
        <p>请输入本床号包含的<b>尺码</b>和每个尺码的<b>扎数</b>，确定后将生成分扎表格。</p>
      </div>

      <div class="setup-sizes-section">
        <div class="setup-sizes-header">
          <span class="setup-sizes-title">尺码与扎数</span>
          <el-button size="small" text type="primary" @click="addSetupSize">
            <el-icon><Plus /></el-icon> 添加尺码
          </el-button>
        </div>

        <div v-if="bundleSetupForm.sizes.length === 0" class="setup-sizes-empty">
          点击"添加尺码"开始
        </div>

        <div v-else class="setup-sizes-list">
          <div
            v-for="(item, index) in bundleSetupForm.sizes"
            :key="index"
            class="setup-size-item"
          >
            <span class="setup-size-index">{{ index + 1 }}</span>
            <el-select
              v-model="item.name"
              placeholder="选择尺码"
              filterable
              allow-create
              default-first-option
              size="default"
              class="setup-size-select"
            >
              <el-option
                v-for="s in quickSizes"
                :key="s"
                :label="s"
                :value="s"
              />
            </el-select>
            <div class="setup-bundle-count">
              <label>扎数</label>
              <el-input-number
                v-model="item.bundleCount"
                :min="1"
                :max="99"
                size="default"
                controls-position="right"
                style="width: 100px"
              />
            </div>
            <div class="setup-size-actions">
              <el-button :icon="Top" circle text size="small" :disabled="index === 0" @click="moveSetupSize(index, -1)" />
              <el-button :icon="Bottom" circle text size="small" :disabled="index === bundleSetupForm.sizes.length - 1" @click="moveSetupSize(index, 1)" />
              <el-button :icon="Delete" circle text type="danger" size="small" @click="removeSetupSize(index)" />
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="bundleSetupVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBundleSetup">确定生成表格</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Delete, Plus, CircleCheck, Tickets, Printer,
  Aim, MagicStick, CircleClose, Search, Collection,
  ArrowLeft, ArrowRight, Grid,
  Edit, Lock, Top, Bottom, Picture, RefreshLeft
} from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import { getStyleList, addStyle } from '../api/style'
import { getBeds, addBed, updateBed, deleteBed } from '../api/bed'
import { batchSaveBundles, deleteBundle, updateBundle, getBundleList, getMyPrefix } from '../api/bundle'
import { getProcessTemplateList, getProcessTemplateDetail } from '../api/processTemplate'
import BundlePrintDrawer from '../components/BundlePrintDrawer.vue'
import BundleQrOverviewDialog from '../components/BundleQrOverviewDialog.vue'
import { PRODUCTION_STAGES, getProductionStage } from '../utils/productionWorkflow'
import { filterProductionBeds } from '../utils/productionManagementSearch'

const route = useRoute()

const mounted = ref(false)
const loading = ref(false)
const saving = ref(false)
const searchKeyword = ref('')
const styleList = ref([])

// 选中状态
const selectedStyleId = ref(null)
const selectedBedId = ref(null)
const bedList = ref([])

const bundleList = ref([])
const savedBundles = ref([])
const hasSavedBundles = ref(false)
const printDrawerVisible = ref(false)
const qrOverviewVisible = ref(false)
const styleDialogVisible = ref(false)
const bedDialogVisible = ref(false)
const creatingStyle = ref(false)
const creatingBed = ref(false)
const processEditorVisible = ref(false)
const savingProcesses = ref(false)
const processTemplates = ref([])
const styleForm = reactive({ styleNo: '' })
const bedForm = reactive({
  bedNo: '',
  name: '',
  quantity: 100,
  remark: '',
  templateId: null,
  processes: []
})
const quickProcessName = ref('')
const processForm = reactive({
  bedId: null,
  bedNo: '',
  name: '',
  quantity: 0,
  remark: '',
  rows: []
})

const productionStages = PRODUCTION_STAGES
const currentStage = computed(() => getProductionStage({
  styleId: selectedStyleId.value,
  bedId: selectedBedId.value,
  printOpen: printDrawerVisible.value
}))
const currentStageIndex = computed(() =>
  productionStages.findIndex((stage) => stage.key === currentStage.value)
)
const searchPlaceholder = computed(() =>
  selectedStyleId.value ? '搜索床号、名称、颜色或工序' : '搜索款号'
)

// 款式分页
const pageNum = ref(1)
const pageSize = ref(12)
const totalStyles = ref(0)

// 床号筛选与分页
const bedFilter = ref('all')
const bedPageNum = ref(1)
const bedPageSize = ref(12)

const bedBundledCount = computed(() => bedList.value.filter(b => b.bundleCount > 0).length)
const bedUnbundledCount = computed(() => bedList.value.filter(b => !b.bundleCount || b.bundleCount === 0).length)

const filteredBeds = computed(() => {
  let beds = bedList.value
  if (bedFilter.value === 'bundled') beds = beds.filter(b => b.bundleCount > 0)
  if (bedFilter.value === 'unbundled') beds = beds.filter(b => !b.bundleCount || b.bundleCount === 0)
  return filterProductionBeds(beds, searchKeyword.value)
})

const paginatedBeds = computed(() => {
  const start = (bedPageNum.value - 1) * bedPageSize.value
  return filteredBeds.value.slice(start, start + bedPageSize.value)
})

const myPrefix = ref('A')
const genCount = ref(5)
const commonSizes = ['XS', 'S', 'M', 'L', 'XL', 'XXL', '2XL', '3XL']
const quickSizes = ['XS', 'S', 'M', 'L', 'XL', '2XL', '3XL', '29', '30', '31', '32', '33', '34', '35', '36']

// 分扎设置对话框
const bundleSetupVisible = ref(false)
const bundleSetupForm = reactive({
  sizes: []
})

// 交叉表数据
const crossTableData = reactive({
  sizes: [],
  maxRows: 0,
  rows: []
})

// 快捷填充
const fillQty = ref(null)
const fillSize = ref('')

// 表格输入框引用，用于键盘导航
const inputRefs = ref(new Map())
const activeSizeRow = ref(-1)

const setInputRef = (el, rowIdx, col) => {
  if (el) {
    inputRefs.value.set(`${rowIdx}-${col}`, el)
  }
}

// 常用尺码 + 数字尺码
const sizeSuggestions = (input) => {
  if (!input) return []
  const all = [...commonSizes, '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44']
  return all.filter(s => s.toLowerCase().includes(input.toLowerCase()) && s !== input).slice(0, 6)
}

// 表格键盘导航
const colOrder = ['bundleNo', 'quantity', 'size', 'color']

function focusNext(rowIdx, col) {
  const colIdx = colOrder.indexOf(col)
  if (colIdx < colOrder.length - 1) {
    // 同一行，下一列
    const nextEl = inputRefs.value.get(`${rowIdx}-${colOrder[colIdx + 1]}`)
    if (nextEl) { nextEl.focus(); nextEl.select() }
  } else {
    // 最后一列，跳到下一行第一列
    const nextRowEl = inputRefs.value.get(`${rowIdx + 1}-bundleNo`)
    if (nextRowEl) { nextRowEl.focus(); nextRowEl.select() }
  }
}

function focusPrev(rowIdx, col) {
  const colIdx = colOrder.indexOf(col)
  if (colIdx > 0) {
    const prevEl = inputRefs.value.get(`${rowIdx}-${colOrder[colIdx - 1]}`)
    if (prevEl) { prevEl.focus(); prevEl.select() }
  } else if (rowIdx > 0) {
    const prevRowEl = inputRefs.value.get(`${rowIdx - 1}-size`)
    if (prevRowEl) { prevRowEl.focus(); prevRowEl.select() }
  }
}

function focusUp(rowIdx, col) {
  if (rowIdx > 0) {
    const upEl = inputRefs.value.get(`${rowIdx - 1}-${col}`)
    if (upEl) { upEl.focus(); upEl.select() }
  }
}

function onSizeFocus(idx) {
  activeSizeRow.value = idx
}

let blurTimer = null
function onSizeBlur(idx) {
  blurTimer = setTimeout(() => {
    if (activeSizeRow.value === idx) activeSizeRow.value = -1
  }, 200)
}



// 表格批量粘贴
function handleTablePaste(e) {
  e.preventDefault()
  const text = e.clipboardData.getData('text')
  if (!text.trim()) return

  const lines = text.split('\n')
    .map(l => l.trim())
    .filter(l => l.length > 0)

  if (lines.length === 0) return

  // 找到当前焦点所在的行索引
  const activeEl = document.activeElement
  let startRow = 0
  if (activeEl && activeEl.tagName === 'INPUT') {
    // 从input的ref反向查找行号
    for (const [key, el] of inputRefs.value.entries()) {
      if (el === activeEl) {
        const match = key.match(/^(\d+)-/)
        if (match) startRow = parseInt(match[1])
        break
      }
    }
  }

  // 解析每行数据（制表符或空格分隔）
  let imported = 0
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    // 优先按制表符分割，否则按空白分割
    const parts = line.includes('\t')
      ? line.split('\t').filter(p => p.trim() !== '')
      : line.split(/\s+/).filter(p => p.length > 0)

    if (parts.length < 2) continue

    const targetIdx = startRow + i
    const bundleNo = parts[0].trim()
    const quantity = Number(parts[1].trim())
    const size = parts[2] ? parts[2].trim() : ''
    const color = parts[3] ? parts[3].trim() : ''

    if (isNaN(quantity) || quantity <= 0) continue

    if (targetIdx < bundleList.value.length) {
      // 覆盖现有行
      bundleList.value[targetIdx].bundleNo = bundleNo || bundleList.value[targetIdx].bundleNo
      bundleList.value[targetIdx].quantity = Math.floor(quantity)
      bundleList.value[targetIdx].size = size
      bundleList.value[targetIdx].color = color
    } else {
      // 添加新行
      bundleList.value.push({
        bundleNo: bundleNo || `${myPrefix.value}${String(targetIdx + 1).padStart(3, '0')}`,
        quantity: Math.floor(quantity),
        size,
        color
      })
    }
    imported++
  }

  if (imported > 0) {
    ElMessage.success(`粘贴填充 ${imported} 扎`)
    if (bedQuantity.value > 0 && totalQuantity.value !== bedQuantity.value) {
      ElMessage.warning(`总件数为 ${totalQuantity.value}，目标为 ${bedQuantity.value} 件`)
    }
  }
}

function focusDown(rowIdx, col) {
  const downEl = inputRefs.value.get(`${rowIdx + 1}-${col}`)
  if (downEl) { downEl.focus(); downEl.select() }
}

function onTableKeydown(e, rowIdx, col) {
  if (e.key === 'Enter') {
    e.preventDefault()
    focusNext(rowIdx, col)
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    focusDown(rowIdx, col)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    focusUp(rowIdx, col)
  } else if (e.key === 'ArrowRight' && col === 'color') {
    // 在颜色列按右箭头跳到下一行扎号
    e.preventDefault()
    const nextEl = inputRefs.value.get(`${rowIdx + 1}-bundleNo`)
    if (nextEl) { nextEl.focus(); nextEl.select() }
  } else if (e.key === 'ArrowLeft' && col === 'bundleNo' && rowIdx > 0) {
    // 在扎号列按左箭头跳到上一行颜色
    e.preventDefault()
    const prevEl = inputRefs.value.get(`${rowIdx - 1}-color`)
    if (prevEl) { prevEl.focus(); prevEl.select() }
  }
}

const filteredStyles = computed(() => styleList.value)

// 搜索防抖
let searchTimer = null
watch(searchKeyword, () => {
  clearTimeout(searchTimer)
  if (selectedStyleId.value) {
    bedPageNum.value = 1
    return
  }
  searchTimer = setTimeout(() => {
    if (selectedStyleId.value) return
    pageNum.value = 1
    loadStyles()
  }, 300)
})

// 床号筛选变化时重置页码
watch(bedFilter, () => { bedPageNum.value = 1 })

const totalQuantity = computed(() => {
  if (crossTableData.rows.length > 0) {
    return crossTableTotalQty.value
  }
  return bundleList.value.reduce((sum, b) => sum + (b.quantity || 0), 0)
})

const selectedStyle = computed(() =>
  styleList.value.find(s => s.id === selectedStyleId.value)
)

const selectedStyleName = computed(() => {
  const s = selectedStyle.value
  if (!s) return ''
  return s.name ? `${s.styleNo || '-'} · ${s.name}` : (s.styleNo || '-')
})

const selectedBed = computed(() =>
  bedList.value.find(b => b.id === selectedBedId.value)
)

const selectedBedNo = computed(() => {
  const bed = selectedBed.value
  if (!bed) return ''
  return `${bed.bedNo}（${bed.quantity}件）`
})

const bedQuantity = computed(() => selectedBed.value?.quantity || 0)

const quantityMatch = computed(() => {
  if (crossTableData.rows.length > 0) {
    return crossTableQtyMatch.value
  }
  return bundleList.value.length > 0 && bedQuantity.value > 0 && totalQuantity.value === bedQuantity.value
})

// 交叉表模式下计算扎数
const crossTableBundleCount = computed(() => {
  let count = 0
  for (const row of crossTableData.rows) {
    for (const size of crossTableData.sizes) {
      const cell = row.cells[size.name]
      if (cell && cell.quantity > 0) count++
    }
  }
  return count
})

const canSave = computed(() => {
  // 交叉表模式
  if (crossTableData.rows.length > 0) {
    return crossTableBundleCount.value > 0 &&
      bedQuantity.value > 0 &&
      crossTableQtyMatch.value &&
      crossTableData.rows.some(row => row.color) &&
      crossTableData.rows.every(row =>
        crossTableData.sizes.every(size => {
          const cell = row.cells[size.name]
          return !cell || cell.quantity > 0
        })
      )
  }
  // 传统模式
  return bundleList.value.length > 0 &&
    bedQuantity.value > 0 &&
    quantityMatch.value &&
    bundleList.value.every(b => b.bundleNo && b.quantity > 0 && b.size)
})

const saveBtnText = computed(() => {
  // 交叉表模式
  if (crossTableData.rows.length > 0) {
    const count = crossTableBundleCount.value
    if (hasSavedBundles.value) return `更新保存（${count}扎）`
    if (crossTableQtyMatch.value) return `确认保存（${count}扎）`
    if (!bedQuantity.value) return '请先设置床号件数'
    if (!crossTableQtyMatch.value) return `件数不匹配（${crossTableTotalQty.value}/${bedQuantity.value}）`
    const hasColor = crossTableData.rows.some(r => r.color)
    if (!hasColor) return '请至少填写一个颜色'
    return '请先填写完整'
  }
  // 传统模式
  if (hasSavedBundles.value) return `更新保存（${bundleList.value.length}扎）`
  if (canSave.value) return `确认保存（${bundleList.value.length}扎）`
  if (!bedQuantity.value) return '请先设置床号件数'
  if (!quantityMatch.value) return '件数与目标不匹配'
  const noNo = bundleList.value.filter(b => !b.bundleNo).length
  const noQty = bundleList.value.filter(b => !b.quantity || b.quantity <= 0).length
  const noSize = bundleList.value.filter(b => !b.size).length
  if (noNo) return `${noNo}扎未填扎号`
  if (noQty) return `${noQty}扎未填件数`
  if (noSize) return `${noSize}扎未填尺码`
  return '请先填写完整'
})

const progressPercent = computed(() => {
  if (!bedQuantity.value) return 0
  return Math.min(120, Math.round((totalQuantity.value / bedQuantity.value) * 100))
})

// 是否有已打印的扎货（打印后禁用清空、导入、导出、保存）
const hasPrintedBundles = computed(() =>
  bundleList.value.some(b => b.printed === 1)
)

onMounted(() => {
  requestAnimationFrame(() => { mounted.value = true })
  loadMyPrefix()

  // 如果路由带有 styleId + bedId 参数，直接进入扎货页面
  const qStyleId = route.query.styleId
  const qBedId = route.query.bedId
  if (qStyleId && qBedId) {
    initWithQuery(Number(qStyleId), Number(qBedId))
  } else {
    loadStyles()
  }
})

// 从路由参数直接进入指定床号的扎货页面
async function initWithQuery(styleId, bedId) {
  loading.value = true
  try {
    // 加载款式列表（面包屑需要）
    const res = await getStyleList({ pageNum: 1, pageSize: 999 })
    styleList.value = res.data?.records || []

    // 设置选中款式并加载床号
    selectedStyleId.value = styleId
    const bedRes = await getBeds(styleId)
    bedList.value = bedRes.data || []

    // 找到目标床号并进入扎货
    const bed = bedList.value.find(b => b.id === bedId)
    if (bed) {
      await selectBed(bed)
    } else {
      ElMessage.warning('未找到对应床号')
    }
  } catch (e) {
    console.error('从路由参数加载失败:', e)
    ElMessage.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

function openStyleDialog() {
  styleForm.styleNo = ''
  styleDialogVisible.value = true
}

async function saveStyle() {
  const styleNo = styleForm.styleNo.trim()
  if (!styleNo) {
    ElMessage.warning('请输入款号')
    return
  }

  creatingStyle.value = true
  try {
    const res = await addStyle({ styleNo })
    const createdStyle = { id: res.data, styleNo, name: '', _hasBundles: false, _bundleCount: 0 }
    styleList.value.unshift(createdStyle)
    totalStyles.value += 1
    styleDialogVisible.value = false
    await selectStyle(createdStyle)
    ElMessage.success('款式已创建，请继续添加床号')
  } finally {
    creatingStyle.value = false
  }
}

async function openBedDialog() {
  Object.assign(bedForm, {
    bedNo: '',
    name: '',
    quantity: 100,
    remark: '',
    templateId: null,
    processes: []
  })
  quickProcessName.value = ''
  try {
    const res = await getProcessTemplateList()
    processTemplates.value = res.data || []
  } catch (error) {
    processTemplates.value = []
  }
  bedDialogVisible.value = true
}

function quickAddBedProcess() {
  const name = quickProcessName.value.trim()
  if (!name) return
  bedForm.processes.push({ name, price: 0 })
  quickProcessName.value = ''
}

function addBedProcessRow() {
  bedForm.processes.push({ name: '', price: 0 })
}

function removeBedProcess(index) {
  bedForm.processes.splice(index, 1)
}

function moveBedProcess(index, direction) {
  const target = index + direction
  if (target < 0 || target >= bedForm.processes.length) return
  const [row] = bedForm.processes.splice(index, 1)
  bedForm.processes.splice(target, 0, row)
}

// 选择模板时自动填充工序列表
watch(() => bedForm.templateId, async (newId) => {
  if (!newId) return
  try {
    const res = await getProcessTemplateDetail(newId)
    const items = res.data?.items || []
    bedForm.processes = items.map((item, index) => ({
      name: item.name || '',
      price: item.price || 0
    }))
  } catch (e) {
    console.error('加载模板工序失败:', e)
  }
})

async function saveBed() {
  const bedNo = bedForm.bedNo.trim()
  const name = bedForm.name.trim()
  if (!bedNo || !name) {
    ElMessage.warning('请填写床号编号和名称')
    return
  }
  if (!selectedStyleId.value) {
    ElMessage.warning('请先选择款式')
    return
  }

  creatingBed.value = true
  try {
    // 构建工序列表：过滤空名称，补充序号
    const validProcesses = bedForm.processes
      .filter(p => p.name && p.name.trim())
      .map((p, index) => ({
        name: p.name.trim(),
        seq: index + 1,
        price: p.price || 0
      }))
    const processes = validProcesses.length > 0 ? validProcesses : null

    const res = await addBed(selectedStyleId.value, {
      bedNo,
      name,
      quantity: bedForm.quantity,
      remark: bedForm.remark.trim() || null,
      processes
    })
    const createdBed = {
      ...res.data,
      bedNo,
      name,
      quantity: bedForm.quantity,
      remark: bedForm.remark.trim(),
      bundleCount: 0,
      processCount: processes?.length || 0,
      processes: processes || []
    }
    bedList.value.push(createdBed)
    bedDialogVisible.value = false
    await selectBed(createdBed)
    ElMessage.success('床号已创建，请继续分扎')
  } finally {
    creatingBed.value = false
  }
}

function openProcessEditor(bed) {
  Object.assign(processForm, {
    bedId: bed.id,
    bedNo: bed.bedNo,
    name: bed.name || '',
    quantity: bed.quantity,
    remark: bed.remark || '',
    rows: (bed.processes || []).map((process, index) => {
      const originalPrice = Number(process.price || 0)
      return {
        key: process.id || `existing-${index}`,
        id: process.id || null,
        name: process.name || '',
        price: originalPrice,
        originalPrice,
        hasWorkRecords: Number(process.completedQuantity || 0) > 0,
        locked: originalPrice > 0 || process.priceLocked === true || process.priceLocked === 1
      }
    })
  })
  processEditorVisible.value = true
}

function addProcess() {
  processForm.rows.push({
    key: `new-${Date.now()}-${processForm.rows.length}`,
    id: null,
    name: '',
    price: 0,
    originalPrice: 0,
    hasWorkRecords: false,
    locked: false
  })
}

function removeProcess(index) {
  if (processForm.rows[index]?.locked || processForm.rows[index]?.hasWorkRecords) return
  processForm.rows.splice(index, 1)
}

function moveProcess(index, direction) {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= processForm.rows.length) return
  if (processForm.rows[index]?.locked || processForm.rows[targetIndex]?.locked) return
  const [row] = processForm.rows.splice(index, 1)
  processForm.rows.splice(targetIndex, 0, row)
}

async function reloadBeds() {
  if (!selectedStyleId.value) return
  const res = await getBeds(selectedStyleId.value)
  bedList.value = res.data || []
}

async function saveProcesses() {
  const emptyRow = processForm.rows.find(row => !row.name.trim())
  if (emptyRow) {
    ElMessage.warning('请填写工序名称')
    return
  }

  savingProcesses.value = true
  try {
    const processes = processForm.rows.map((row, index) => ({
      id: row.id,
      name: row.name.trim(),
      seq: index + 1,
      price: Number(row.price || 0)
    }))
    await updateBed(processForm.bedId, {
      bedNo: processForm.bedNo,
      name: processForm.name,
      quantity: processForm.quantity,
      remark: processForm.remark || null,
      processes
    })
    await reloadBeds()
    processEditorVisible.value = false
    ElMessage.success('工序已保存')
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '工序保存失败')
  } finally {
    savingProcesses.value = false
  }
}

async function loadMyPrefix() {
  try {
    const res = await getMyPrefix()
    myPrefix.value = res.data || 'A'
  } catch (e) {
    myPrefix.value = 'A'
  }
}

async function loadStyles() {
  loading.value = true
  try {
    const res = await getStyleList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined
    })
    const records = res.data.records || []
    totalStyles.value = res.data.total || 0

    // 批量查询每个款式是否有已保存的扎货
    const checkPromises = records.map(async (s) => {
      try {
        const bRes = await getBundleList({ styleId: s.id, pageSize: 1 })
        const count = bRes.data?.total || 0
        s._hasBundles = count > 0
        s._bundleCount = count
      } catch (e) {
        s._hasBundles = false
        s._bundleCount = 0
      }
    })
    await Promise.all(checkPromises)
    styleList.value = records
  } catch (err) {
    console.error('获取款式列表失败:', err)
    styleList.value = []
    totalStyles.value = 0
  } finally {
    loading.value = false
  }
}

// Step 1: 选择款式 → 加载床号
async function selectStyle(s) {
  selectedStyleId.value = s.id
  searchKeyword.value = ''
  selectedBedId.value = null
  bundleList.value = []
  savedBundles.value = []
  hasSavedBundles.value = false
  bedFilter.value = 'all'
  bedPageNum.value = 1

  try {
    const res = await getBeds(s.id)
    bedList.value = res.data || []
  } catch (e) {
    bedList.value = []
  }
}

// Step 2: 选择床号 → 加载扎货
async function selectBed(bed) {
  selectedBedId.value = bed.id
  searchKeyword.value = ''
  bundleList.value = []

  try {
    const res = await getBundleList({ bedId: bed.id, pageSize: 100 })
    savedBundles.value = res.data.records || []
    hasSavedBundles.value = savedBundles.value.length > 0

    if (hasSavedBundles.value) {
      bundleList.value = savedBundles.value.map(b => ({
        id: b.id,
        bundleNo: b.bundleNo,
        quantity: b.quantity,
        size: b.size || '',
        color: b.color || '',
        printed: b.printed || 0
      }))
      // 转为交叉表格式显示
      savedBundlesToCrossTable(savedBundles.value)
    } else {
      if (bed.quantity > 0) {
        openBundleSetup()
      }
    }
  } catch (e) {
    savedBundles.value = []
    hasSavedBundles.value = false
    if (bed.quantity > 0) {
      openBundleSetup()
    }
  }
}

function backToStyle() {
  const shouldReloadStyles = !searchKeyword.value
  selectedStyleId.value = null
  searchKeyword.value = ''
  selectedBedId.value = null
  bedList.value = []
  bundleList.value = []
  savedBundles.value = []
  hasSavedBundles.value = false
  crossTableData.sizes = []
  crossTableData.maxRows = 0
  crossTableData.rows = []
  if (shouldReloadStyles) {
    pageNum.value = 1
    loadStyles()
  }
}

function backToBed() {
  selectedBedId.value = null
  searchKeyword.value = ''
  bundleList.value = []
  savedBundles.value = []
  hasSavedBundles.value = false
  crossTableData.sizes = []
  crossTableData.maxRows = 0
  crossTableData.rows = []
}

async function handleDeleteBed(bed) {
  try {
    await deleteBed(bed.id)
    ElMessage.success('已删除')
    // 从列表中移除
    const idx = bedList.value.findIndex(b => b.id === bed.id)
    if (idx !== -1) bedList.value.splice(idx, 1)
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '删除失败')
  }
}

function quickGenerate() {
  const total = bedQuantity.value
  const count = genCount.value
  if (!total) return

  const baseQty = Math.floor(total / count)
  const remainder = total % count

  bundleList.value = []
  for (let i = 0; i < count; i++) {
    bundleList.value.push({
      quantity: baseQty + (i < remainder ? 1 : 0),
      size: '',
      color: '',
      bundleNo: `${myPrefix.value}${String(i + 1).padStart(3, '0')}`
    })
  }
}

function removeBundle(idx) {
  bundleList.value.splice(idx, 1)
}

function clearAll() {
  bundleList.value = []
  crossTableData.sizes = []
  crossTableData.maxRows = 0
  crossTableData.rows = []
}

// ===== 分扎设置 =====

function openBundleSetup() {
  bundleSetupForm.sizes = []
  bundleSetupVisible.value = true
}

function addSetupSize() {
  bundleSetupForm.sizes.push({ name: '', bundleCount: 1 })
}

function removeSetupSize(index) {
  bundleSetupForm.sizes.splice(index, 1)
}

function moveSetupSize(index, direction) {
  const target = index + direction
  if (target < 0 || target >= bundleSetupForm.sizes.length) return
  const [item] = bundleSetupForm.sizes.splice(index, 1)
  bundleSetupForm.sizes.splice(target, 0, item)
}

function confirmBundleSetup() {
  const validSizes = bundleSetupForm.sizes.filter(s => s.name && s.bundleCount > 0)
  if (validSizes.length === 0) {
    ElMessage.warning('请至少添加一个尺码')
    return
  }
  if (!bedQuantity.value || bedQuantity.value <= 0) {
    ElMessage.warning('该床号未设置件数，请先编辑床号')
    return
  }
  generateCrossTable()
  bundleSetupVisible.value = false
}

function generateCrossTable() {
  const sizes = bundleSetupForm.sizes
    .filter(s => s.name && s.bundleCount > 0)
    .map(s => ({ name: s.name, bundleCount: s.bundleCount }))

  const maxRows = Math.max(...sizes.map(s => s.bundleCount))

  // 按全局顺序生成扎号
  const rows = []
  for (let i = 0; i < maxRows; i++) {
    rows.push({ color: '', cells: {} })
  }

  let globalIdx = 1
  for (const size of sizes) {
    for (let i = 0; i < size.bundleCount; i++) {
      rows[i].cells[size.name] = {
        bundleNo: `${myPrefix.value}${String(globalIdx).padStart(3, '0')}`,
        quantity: null
      }
      globalIdx++
    }
  }

  crossTableData.sizes = sizes
  crossTableData.maxRows = maxRows
  crossTableData.rows = rows
}

// 交叉表总件数
const crossTableTotalQty = computed(() => {
  let total = 0
  for (const row of crossTableData.rows) {
    for (const size of crossTableData.sizes) {
      const cell = row.cells[size.name]
      if (cell && cell.quantity > 0) {
        total += cell.quantity
      }
    }
  }
  return total
})

// 交叉表是否匹配目标件数
const crossTableQtyMatch = computed(() =>
  crossTableData.rows.length > 0 &&
  bedQuantity.value > 0 &&
  crossTableTotalQty.value === bedQuantity.value
)

// 将交叉表转为 bundleList 格式
function crossTableToBundleList() {
  const bundles = []
  let lastColor = ''
  for (const row of crossTableData.rows) {
    // 继承上一行的颜色（用户可以只在第一行填颜色）
    if (row.color) lastColor = row.color
    const color = row.color || lastColor
    for (const size of crossTableData.sizes) {
      const cell = row.cells[size.name]
      if (cell && cell.quantity > 0) {
        bundles.push({
          bundleNo: cell.bundleNo,
          quantity: cell.quantity,
          size: size.name,
          color
        })
      }
    }
  }
  return bundles
}

// 回退到设置对话框（重新设置尺码）
function resetToSetup() {
  crossTableData.sizes = []
  crossTableData.maxRows = 0
  crossTableData.rows = []
  bundleSetupVisible.value = true
}

// 为某个尺码添加一扎
function addBundleForSize(sizeName) {
  const size = crossTableData.sizes.find(s => s.name === sizeName)
  if (!size) return

  size.bundleCount++

  // 如果新的扎数超过了当前最大行数，添加新行
  const newMaxRows = Math.max(...crossTableData.sizes.map(s => s.bundleCount))
  while (crossTableData.rows.length < newMaxRows) {
    crossTableData.rows.push({ color: '', cells: {} })
  }

  // 重新生成所有扎号
  reindexBundleNumbers()
}

// 为某个尺码减少一扎
function removeBundleForSize(sizeName) {
  const size = crossTableData.sizes.find(s => s.name === sizeName)
  if (!size || size.bundleCount <= 1) return

  size.bundleCount--

  // 重新生成所有扎号
  reindexBundleNumbers()
}

// 将已保存的扎货列表转为交叉表格式
function savedBundlesToCrossTable(bundles) {
  // 按尺码分组
  const sizeMap = new Map()
  for (const b of bundles) {
    const sizeName = b.size || '未知'
    if (!sizeMap.has(sizeName)) {
      sizeMap.set(sizeName, [])
    }
    sizeMap.get(sizeName).push(b)
  }

  const sizes = []
  for (const [name, items] of sizeMap) {
    sizes.push({ name, bundleCount: items.length })
  }

  const maxRows = Math.max(...sizes.map(s => s.bundleCount), 1)
  const rows = []
  for (let i = 0; i < maxRows; i++) {
    rows.push({ color: '', cells: {} })
  }

  // 填充数据
  for (const [name, items] of sizeMap) {
    for (let i = 0; i < items.length; i++) {
      rows[i].cells[name] = {
        bundleNo: items[i].bundleNo,
        quantity: items[i].quantity
      }
      if (items[i].color) rows[i].color = items[i].color
    }
  }

  crossTableData.sizes = sizes
  crossTableData.maxRows = maxRows
  crossTableData.rows = rows
}

// 重新编号所有扎号
function reindexBundleNumbers() {
  let globalIdx = 1
  for (const size of crossTableData.sizes) {
    for (let i = 0; i < size.bundleCount; i++) {
      if (!crossTableData.rows[i].cells[size.name]) {
        crossTableData.rows[i].cells[size.name] = {
          bundleNo: '',
          quantity: null
        }
      }
      crossTableData.rows[i].cells[size.name].bundleNo =
        `${myPrefix.value}${String(globalIdx).padStart(3, '0')}`
      globalIdx++
    }
  }
  // 清除超出扎数的单元格
  for (const size of crossTableData.sizes) {
    for (let i = size.bundleCount; i < crossTableData.rows.length; i++) {
      delete crossTableData.rows[i].cells[size.name]
    }
  }
  crossTableData.maxRows = crossTableData.rows.length
}

function applyFillQty() {
  if (!fillQty.value) return
  bundleList.value.forEach(b => { b.quantity = fillQty.value })
}

function applyFillSize() {
  if (!fillSize.value) return
  bundleList.value.forEach(b => { b.size = fillSize.value })
}

function autoFillEqual() {
  const count = bundleList.value.length
  const total = bedQuantity.value
  if (!count || !total) return
  const base = Math.floor(total / count)
  const remainder = total % count
  bundleList.value.forEach((b, i) => {
    b.quantity = base + (i < remainder ? 1 : 0)
  })
}

async function saveBundles() {
  if (!selectedBedId.value) {
    ElMessage.warning('请先选择床号')
    return
  }

  // 交叉表模式：先转为 bundleList
  if (crossTableData.rows.length > 0) {
    bundleList.value = crossTableToBundleList()
  }

  if (!canSave.value) {
    ElMessage.warning('请确认件数与目标总量匹配，且每扎都填写完整')
    return
  }

  saving.value = true
  try {
    if (hasSavedBundles.value) {
      // 更新模式（已打印的扎货不允许修改/删除）
      const currentIds = new Set(bundleList.value.filter(b => b.id).map(b => b.id))
      const toDelete = savedBundles.value.filter(b => !currentIds.has(b.id) && b.printed !== 1)
      const toUpdate = bundleList.value.filter(b => b.id && b.printed !== 1)
      const toCreate = bundleList.value.filter(b => !b.id)

      await Promise.all(toDelete.map(b => deleteBundle(b.id)))

      await Promise.all(
        toUpdate.map(b => updateBundle(b.id, {
          bundleNo: b.bundleNo,
          quantity: b.quantity,
          size: b.size || null,
          color: b.color || null
        }))
      )

      if (toCreate.length > 0) {
        await batchSaveBundles({
          bedId: selectedBedId.value,
          bundles: toCreate.map(b => ({
            quantity: b.quantity,
            size: b.size || null,
            color: b.color || null
          }))
        })
      }
      ElMessage.success('更新成功')
    } else {
      // 新建模式
      const data = {
        bedId: selectedBedId.value,
        bundles: bundleList.value.map(b => ({
          quantity: b.quantity,
          size: b.size || null,
          color: b.color || null
        }))
      }
      await batchSaveBundles(data)
      ElMessage.success(`成功保存 ${bundleList.value.length} 扎`)
    }

    await reloadSavedBundles()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '保存失败')
  } finally {
    saving.value = false
  }
}

async function reloadSavedBundles() {
  if (!selectedBedId.value) return
  try {
    const res = await getBundleList({ bedId: selectedBedId.value, pageSize: 100 })
    savedBundles.value = res.data.records || []
    hasSavedBundles.value = savedBundles.value.length > 0

    if (hasSavedBundles.value) {
      bundleList.value = savedBundles.value.map(b => ({
        id: b.id,
        bundleNo: b.bundleNo,
        quantity: b.quantity,
        size: b.size || '',
        color: b.color || '',
        printed: b.printed || 0
      }))
      // 转为交叉表格式显示
      savedBundlesToCrossTable(savedBundles.value)
    }

    // 刷新床号列表的分扎状态
    const bed = bedList.value.find(b => b.id === selectedBedId.value)
    if (bed) {
      bed.bundleCount = savedBundles.value.length
    }
  } catch (e) { /* ignore */ }
}

function goPrint() {
  if (!hasSavedBundles.value) {
    ElMessage.warning('没有可打印的扎货')
    return
  }
  printDrawerVisible.value = true
}
</script>

<style scoped>
.bundle-manage {
  padding-bottom: 40px;
  width: 100%;
  min-width: 0;
}

.workflow-card {
  margin-bottom: 18px;
  padding: 18px 20px;
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  box-shadow: var(--shadow-soft);
}

.workflow-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.workflow-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.workflow-heading h2 {
  margin: 0;
  font-size: 20px;
  color: var(--color-slate);
}

.workflow-heading p {
  margin: 5px 0 0;
  font-size: 13px;
  color: var(--color-mist);
}

.workflow-steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 18px;
}

.workflow-step {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 38px;
  border-radius: 10px;
  background: #f5f3f0;
  color: var(--color-mist);
  font-size: 13px;
  font-weight: 600;
}

.workflow-step.active {
  background: rgba(232, 168, 56, 0.14);
  color: var(--color-amber);
  box-shadow: inset 0 0 0 1px rgba(232, 168, 56, 0.25);
}

.workflow-step.done {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}

.step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  font-size: 12px;
}

.bed-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

/* ===== 新增床号弹框 - 工序列表 ===== */
.bed-dialog-processes {
  margin-top: 8px;
  padding: 14px;
  background: #fafaf8;
  border: 1px solid #ebe8e3;
  border-radius: 10px;
}

.bed-dialog-processes-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.bed-dialog-processes-title {
  font-size: 15px;
  font-weight: 600;
  color: #3a3632;
}

.bed-dialog-quick-input {
  margin-bottom: 10px;
}

.bed-dialog-quick-input :deep(.el-input__wrapper) {
  border: 2px dashed #d9d5cf;
  background: var(--bg-card);
  border-radius: 8px;
  box-shadow: none;
}

.bed-dialog-quick-input :deep(.el-input__wrapper:hover),
.bed-dialog-quick-input :deep(.el-input__wrapper.is-focus) {
  border-color: #8b7e6a;
  border-style: solid;
}

.bed-dialog-process-empty {
  text-align: center;
  padding: 16px 0;
  color: #b0aaa0;
  font-size: 13px;
}

.bed-dialog-process-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bed-dialog-process-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: var(--bg-card);
  border-radius: 8px;
  border: 1px solid #ebe8e3;
}

.bed-dialog-process-seq {
  width: 24px;
  text-align: center;
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.bed-dialog-process-name {
  flex: 1;
  min-width: 0;
}

.bed-dialog-process-price {
  width: 120px;
  flex-shrink: 0;
}

.bed-dialog-process-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.process-editor-tip {
  margin-bottom: 14px;
  padding: 10px 12px;
  border-left: 3px solid var(--color-amber);
  background: #fff8e8;
  color: #6b5a34;
  font-size: 13px;
}

.process-editor-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;
}

.process-editor-row {
  display: grid;
  grid-template-columns: 32px minmax(160px, 1fr) 180px 112px;
  align-items: center;
  gap: 10px;
}

.process-editor-seq {
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.process-price-input {
  width: 100%;
}

.process-row-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
}

.process-row-actions .el-button + .el-button {
  margin-left: 2px;
}

.process-lock {
  margin-right: 10px;
  color: #909399;
}

.reported-process-tip {
  margin-left: 4px;
  color: #909399;
  font-size: 11px;
  white-space: nowrap;
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

/* ===== 面包屑 ===== */
.level-back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  margin-bottom: 14px;
  padding: 0 18px;
  border: 2px solid var(--color-slate);
  border-radius: 12px;
  background: var(--bg-card);
  color: var(--color-slate);
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 3px 10px rgba(45, 42, 38, 0.1);
  transition: all 0.2s ease;
}

.level-back-button:hover {
  border-color: var(--color-amber);
  background: var(--color-amber);
  color: #fff;
  transform: translateX(-2px);
  box-shadow: 0 5px 14px rgba(232, 168, 56, 0.25);
}

.level-back-button .el-icon {
  font-size: 18px;
}

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

/* ===== 卡片网格 ===== */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
  margin-bottom: 20px;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.4s var(--ease-out) 0.2s;
}
.cards-grid.entered { opacity: 1; transform: translateY(0); }

.style-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  box-shadow: var(--shadow-soft);
  transition: all 0.25s var(--ease-out);
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
  border: 2px solid transparent;
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
  font-size: 12px;
  font-weight: 600;
  color: #6366f1;
  font-family: 'Menlo', 'Consolas', monospace;
  background: rgba(99, 102, 241, 0.08);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.bundle-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
}
.bundle-tag.has-bundle {
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
}
.bundle-tag.no-bundle {
  color: var(--color-mist);
  background: rgba(156, 163, 175, 0.1);
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.style-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate);
  margin: 0;
}

.card-meta {
  display: flex;
  gap: 12px;
}
.meta-item {
  font-size: 12px;
  color: var(--color-slate-light);
  display: flex;
  align-items: center;
  gap: 3px;
}

.card-footer {
  padding-top: 8px;
  border-top: 1px solid #f0eeea;
}
.bundle-count {
  font-size: 12px;
  color: var(--color-terracotta);
  font-weight: 500;
}

/* ===== 床号卡片 ===== */
.bed-section {
  margin-bottom: 20px;
}
.bed-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.bed-filter-bar :deep(.el-radio-button__inner) {
  font-size: 12px;
  padding: 6px 14px;
}
.bed-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.bed-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 18px;
  box-shadow: var(--shadow-soft);
  transition: all 0.25s var(--ease-out);
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
}

.bed-no {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-slate);
}

.bed-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bed-name {
  font-size: 13px;
  color: #606266;
}

.bed-info-grid {
  display: flex;
  gap: 10px;
}

.bed-info-item {
  flex: 1;
  text-align: center;
  background: #f8f9fb;
  border-radius: 6px;
  padding: 10px 4px;
}

.bed-info-val {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.bed-info-lbl {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-top: 3px;
}

.bed-processes {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding-top: 2px;
}

.bed-process-label {
  flex: 0 0 auto;
  padding-top: 3px;
  font-size: 12px;
  color: #909399;
}

.bed-process-list {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.bed-process-chip {
  max-width: 100%;
  padding: 3px 7px;
  overflow: hidden;
  border: 1px solid #d8dee8;
  border-radius: 4px;
  background: #f4f7fa;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bed-process-empty {
  padding-top: 3px;
  font-size: 12px;
  color: #b1b3b8;
}

.bed-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.bed-meta .meta-label {
  color: #909399;
}

.bed-meta .meta-value {
  color: #606266;
}

.bed-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-slate);
  margin-bottom: 4px;
}

.bed-card-footer {
  padding-top: 8px;
  border-top: 1px solid #f0eeea;
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  margin-top: 0; padding-top: 16px;
  border-top: 1px solid var(--color-cream-dark);
  margin-bottom: 20px;
}

/* ===== 编辑区 ===== */
.edit-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-soft);
}

.edit-header {
  margin-bottom: 16px;
}

.edit-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.edit-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-slate);
  margin: 0;
}

.edit-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 20px;
}
.badge-ok { color: #10b981; background: rgba(16, 185, 129, 0.1); }
.badge-warn { color: #ef4444; background: rgba(239, 68, 68, 0.08); }

/* ===== 进度条 ===== */
.progress-bar-wrap { margin-bottom: 4px; }

.progress-bar {
  height: 7px;
  background: var(--color-cream);
  border-radius: 4px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-amber), #10b981);
  border-radius: 4px;
  transition: width 0.3s var(--ease-out);
}
.progress-fill.over {
  background: linear-gradient(90deg, #ef4444, #dc2626);
}
.progress-text {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-mist);
}
.progress-text b { color: var(--color-slate); font-weight: 700; }
.text-ok { color: #10b981; font-weight: 600; display: flex; align-items: center; gap: 4px; }
.text-over { color: #ef4444; font-weight: 600; }

/* ===== 快捷操作栏 ===== */
.quick-bar {
  display: flex;
  gap: 10px;
  padding: 10px 14px;
  background: var(--color-cream);
  border-radius: var(--radius-md);
  margin-bottom: 14px;
  flex-wrap: wrap;
  align-items: center;
}

.fill-group {
  display: flex;
  align-items: center;
  gap: 6px;
}
.fill-group label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-slate-light);
  white-space: nowrap;
}

/* ===== 表格 ===== */
.table-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.bundle-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  border: 1px solid #f0eeea;
  border-radius: var(--radius-md);
  overflow: hidden;
  font-size: 14px;
}

.bundle-table thead th {
  background: #f8f6f3;
  font-weight: 600;
  color: var(--color-slate-light);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #f0eeea;
  white-space: nowrap;
}

.bundle-table tbody tr {
  transition: background 0.15s;
}
.bundle-table tbody tr:hover {
  background: rgba(232, 168, 56, 0.08);
}
.bundle-table td:nth-child(odd) {
  background: #ffffff;
}
.bundle-table td:nth-child(even) {
  background: #f8f6f3;
}

.bundle-table td {
  padding: 6px 10px;
  border-bottom: 1px solid #f5f2ed;
  vertical-align: middle;
}

.bundle-table tfoot td {
  padding: 10px 12px;
  background: #f8f6f3;
  font-weight: 600;
  font-size: 13px;
  color: var(--color-slate);
  border-top: 2px solid #efe9e0;
}
.total-label { text-align: right; padding-right: 16px !important; }
.total-qty {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-amber);
  text-align: center;
}

.col-no { width: 40px; text-align: center; }
.col-bundle-no { width: 110px; }
.col-qty { width: 130px; }
.col-size { width: 110px; }
.col-color { width: 100px; }
.col-action { width: 50px; text-align: center; }

.row-num {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-mist);
}

/* 尺码单元格（用于下拉定位） */
.size-cell {
  position: relative;
}
.size-input {
  position: relative;
  z-index: 2;
}

/* 颜色单元格 */
.color-cell {
  position: relative;
}
.color-input {
  position: relative;
  z-index: 2;
}

/* 尺码快捷选择面板 */
.size-picker {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--bg-card);
  border: 1px solid var(--color-cream-dark);
  border-radius: var(--radius-sm);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  z-index: 100;
  padding: 8px;
}
.size-picker-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
}
.size-pick-btn {
  height: 34px;
  border: 1.5px solid #e8e4de;
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-slate);
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
  padding: 0;
}
.size-pick-btn:hover {
  border-color: var(--color-amber);
  background: rgba(232, 168, 56, 0.08);
}
.size-pick-btn.active {
  border-color: var(--color-amber);
  background: var(--color-amber);
  color: #fff;
}

/* 底部行向上弹出 */
.size-picker.picker-up {
  top: auto;
  bottom: calc(100% + 4px);
}

/* 同上行按钮 */
.copy-up-btn {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  height: 28px;
  padding: 0 8px;
  border: 1px solid #e8e4de;
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  font-size: 11px;
  font-weight: 600;
  color: var(--color-amber);
  cursor: pointer;
  transition: all 0.15s;
  z-index: 3;
  white-space: nowrap;
}
.copy-up-btn:hover {
  background: var(--color-amber);
  color: #fff;
  border-color: var(--color-amber);
}
.size-input {
  padding-right: 48px;
}

/* 表格输入框 */
.table-input {
  width: 100%;
  height: 40px;
  padding: 0 10px;
  border: 1.5px solid #e8e4de;
  border-radius: var(--radius-sm);
  font-size: 15px;
  font-family: inherit;
  color: var(--color-slate);
  background: var(--bg-card);
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  -webkit-appearance: none;
  appearance: none;
}
.table-input:focus {
  border-color: var(--color-amber);
  box-shadow: 0 0 0 3px rgba(232, 168, 56, 0.12);
}
.table-input.mono {
  font-family: 'Outfit', monospace;
  font-weight: 600;
  color: var(--color-amber);
}
.table-input.num-input {
  text-align: center;
  font-weight: 600;
  font-family: 'Outfit', monospace;
  font-size: 16px;
  line-height: 40px;
  padding-top: 0;
  padding-bottom: 0;
}
input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
input[type="number"] {
  -moz-appearance: textfield;
}

.row-del {
  font-size: 13px;
  font-weight: 500;
}

/* 已打印行 */
.printed-row {
  background: #f5f7fa;
}
.printed-row .table-input {
  background: transparent;
  color: #999;
  cursor: not-allowed;
}
.printed-badge {
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
  white-space: nowrap;
}

/* 添加行 */
.add-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  margin-top: 8px;
  border: 1.5px dashed #e0dbd4;
  border-radius: var(--radius-md);
  color: var(--color-mist);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}
.add-row:hover {
  border-color: var(--color-amber);
  color: var(--color-amber);
  background: rgba(232, 168, 56, 0.03);
}

/* 操作按钮 */
.section-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--color-cream-dark);
}
.save-btn {
  flex: 2;
  font-weight: 600;
  font-size: 15px;
  height: 44px;
}
.section-actions .el-button:not(.save-btn) { flex: 1; }

.print-btn {
  width: 100%;
  margin-top: 12px;
  font-weight: 600;
  font-size: 15px;
  height: 46px;
  border-radius: var(--radius-md);
}

/* ===== 过渡 ===== */
.slide-up-enter-active { transition: all 0.4s var(--ease-out); }
.slide-up-leave-active { transition: all 0.25s ease-in; }
.slide-up-enter-from { opacity: 0; transform: translateY(20px); }
.slide-up-leave-to { opacity: 0; transform: translateY(-10px); }

/* ===== 分扎设置对话框 ===== */
.setup-hint {
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--color-slate-light);
}
.setup-hint p { margin: 0; }

.setup-sizes-section {
  padding: 14px;
  background: #fafaf8;
  border: 1px solid #ebe8e3;
  border-radius: 10px;
  margin-bottom: 16px;
}

.setup-sizes-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.setup-sizes-title {
  font-size: 15px;
  font-weight: 600;
  color: #3a3632;
}

.setup-sizes-empty {
  text-align: center;
  padding: 20px 0;
  color: #b0aaa0;
  font-size: 13px;
}

.setup-sizes-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setup-size-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: var(--bg-card);
  border-radius: 8px;
  border: 1px solid #ebe8e3;
}

.setup-size-index {
  width: 24px;
  text-align: center;
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.setup-size-select {
  width: 140px;
  flex-shrink: 0;
}

.setup-bundle-count {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.setup-bundle-count label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-slate-light);
  white-space: nowrap;
}

.setup-size-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* ===== 交叉表 ===== */
.cross-table-section {
  margin-bottom: 16px;
}

.cross-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.cross-table-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-slate);
}

.cross-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  border: 1px solid #f0eeea;
  border-radius: var(--radius-md);
  overflow: hidden;
  font-size: 14px;
}

.cross-table thead th {
  background: #f8f6f3;
  font-weight: 600;
  color: var(--color-slate-light);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  padding: 10px 12px;
  text-align: center;
  border-bottom: 1px solid #f0eeea;
  white-space: nowrap;
}

.cross-table thead th.cross-col-color {
  text-align: left;
  width: 100px;
  text-transform: none;
  font-size: 13px;
}

.cross-table tbody tr {
  transition: background 0.15s;
}

.cross-table tbody tr:hover {
  background: rgba(232, 168, 56, 0.08);
}

.cross-table td:nth-child(odd) {
  background: #ffffff;
}

.cross-table td:nth-child(even) {
  background: #f8f6f3;
}

.cross-table td {
  padding: 6px 8px;
  border-bottom: 1px solid #f5f2ed;
  vertical-align: middle;
  text-align: center;
}

.cross-table td.cross-col-color {
  text-align: left;
}

.cross-table tfoot td {
  padding: 10px 12px;
  background: #f8f6f3;
  font-weight: 600;
  font-size: 13px;
  color: var(--color-slate);
  border-top: 2px solid #efe9e0;
  text-align: center;
}

.cross-table tfoot td.cross-col-color {
  text-align: right;
  padding-right: 16px;
}

.cross-col-bundleno {
  width: 90px;
  text-align: center;
}

.cross-col-qty {
  width: 90px;
  text-align: center;
}

.cross-col-color {
  width: 100px;
}

.bundle-no-readonly {
  font-family: 'Outfit', monospace;
  font-weight: 600;
  color: var(--color-amber);
  font-size: 13px;
}

.cell-disabled {
  color: #d0ccc6;
  font-size: 13px;
}

.total-qty-cell {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-amber);
}

.cross-table .color-input {
  padding-right: 48px;
}

.add-bundle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin-left: 4px;
  border: 1.5px solid #e0dbd4;
  border-radius: 4px;
  background: var(--bg-card);
  color: var(--color-amber);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  vertical-align: middle;
  transition: all 0.15s;
  line-height: 1;
  padding: 0;
}

.add-bundle-btn:hover {
  background: var(--color-amber);
  color: #fff;
  border-color: var(--color-amber);
}

.remove-bundle-btn {
  color: #ef4444;
  border-color: #fecaca;
}

.remove-bundle-btn:hover {
  background: #ef4444;
  color: #fff;
  border-color: #ef4444;
}

.qty-cell-wrap {
  position: relative;
  display: inline-block;
  width: 100%;
}

.qty-cell-wrap .num-input {
  padding-right: 48px;
}

/* ===== 手机端 ===== */
@media (max-width: 640px) {
  .workflow-heading, .bed-filter-bar {
    align-items: stretch;
    flex-direction: column;
  }
  .workflow-steps {
    grid-template-columns: repeat(2, 1fr);
  }
  .bed-form-grid {
    grid-template-columns: 1fr;
  }
  .bed-dialog-process-item {
    flex-wrap: wrap;
  }
  .bed-dialog-process-name {
    flex: 1 1 100%;
    order: 1;
  }
  .bed-dialog-process-price {
    width: 100px;
  }
  .bed-dialog-process-actions {
    margin-left: auto;
  }
  .process-editor-row {
    grid-template-columns: 28px minmax(0, 1fr) 128px;
  }
  .process-row-actions {
    grid-column: 2 / 4;
  }
  .cards-grid, .bed-cards-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .search-box { width: 100%; }

  .quick-bar {
    flex-direction: column;
    gap: 8px;
    padding: 10px 12px;
  }
  .fill-group { flex-wrap: wrap; }

  .col-bundle-no { width: 90px; }
  .col-qty { width: 100px; }
  .col-size { width: 90px; }
  .col-color { width: 80px; }

  .table-input { height: 44px; font-size: 16px; padding: 0 8px; }
  .table-input.num-input {
    line-height: 44px;
    text-align: center;
    text-align-last: center;
  }

  .section-actions { flex-direction: column; }

  .edit-section { padding: 14px; }

  .setup-size-item {
    flex-wrap: wrap;
    gap: 8px;
  }
  .setup-size-select {
    width: 100%;
    flex: 1;
  }
  .setup-bundle-count {
    width: 100%;
  }

  .cross-table {
    font-size: 12px;
  }
  .cross-table thead th {
    padding: 8px 6px;
    font-size: 11px;
  }
  .cross-table td {
    padding: 4px 4px;
  }
  .cross-col-bundleno,
  .cross-col-qty {
    width: 70px;
  }
}
</style>
