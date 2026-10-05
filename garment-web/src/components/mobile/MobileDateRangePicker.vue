<template>
  <div class="mobile-date-range">
    <div class="preset-row">
      <button
        v-for="preset in presets"
        :key="preset.value"
        type="button"
        class="preset-button"
        @click="applyPreset(preset.value)"
      >
        {{ preset.label }}
      </button>
    </div>

    <div class="date-fields">
      <label class="date-field">
        <span>开始日期</span>
        <input
          v-model="startDate"
          type="date"
          :max="endDate || today"
          @change="handleStartChange"
        />
      </label>
      <span class="date-separator">至</span>
      <label class="date-field">
        <span>结束日期</span>
        <input
          v-model="endDate"
          type="date"
          :min="startDate"
          :max="today"
          @change="handleEndChange"
        />
      </label>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { formatLocalDate } from '../../utils/mobileRecordFilters'
import { getMobileDatePreset, normalizeDateRange } from '../../utils/mobileDateRange'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'change'])
const today = formatLocalDate(new Date())
const startDate = ref(props.modelValue?.[0] || '')
const endDate = ref(props.modelValue?.[1] || '')
const presets = [
  { label: '今天', value: 'today' },
  { label: '最近7天', value: 'recent7' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' }
]

watch(
  () => props.modelValue,
  (value) => {
    startDate.value = value?.[0] || ''
    endDate.value = value?.[1] || ''
  },
  { deep: true }
)

function commit() {
  const normalized = normalizeDateRange([startDate.value, endDate.value])
  startDate.value = normalized[0]
  endDate.value = normalized[1]
  emit('update:modelValue', normalized)
  if (normalized[0] && normalized[1]) emit('change', normalized)
}

function handleStartChange() {
  if (startDate.value && endDate.value && endDate.value < startDate.value) {
    endDate.value = startDate.value
  }
  commit()
}

function handleEndChange() {
  commit()
}

function applyPreset(preset) {
  const range = getMobileDatePreset(preset)
  startDate.value = range[0]
  endDate.value = range[1]
  commit()
}
</script>

<style scoped>
.mobile-date-range {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.06);
}

.preset-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.preset-button {
  min-height: 38px;
  padding: 0 4px;
  border: 1px solid #e5e7eb;
  border-radius: 9px;
  background: #f9fafb;
  color: #4b5563;
  font: inherit;
  font-size: 12px;
  font-weight: 600;
}

.preset-button:active {
  border-color: var(--color-amber, #d97706);
  background: rgba(217, 119, 6, 0.1);
  color: var(--color-amber, #d97706);
}

.date-fields {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 18px minmax(0, 1fr);
  align-items: end;
  gap: 6px;
}

.date-field {
  min-width: 0;
}

.date-field span {
  display: block;
  margin-bottom: 5px;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
}

.date-field input {
  display: block;
  width: 100%;
  min-width: 0;
  min-height: 46px;
  padding: 0 8px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  color: #111827;
  font: inherit;
  font-size: 14px;
  color-scheme: light;
}

.date-field input:focus {
  border-color: var(--color-amber, #d97706);
  outline: none;
  box-shadow: 0 0 0 3px rgba(217, 119, 6, 0.1);
}

.date-separator {
  padding-bottom: 14px;
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
}

@media (max-width: 360px) {
  .preset-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .date-fields {
    grid-template-columns: 1fr;
  }
  .date-separator {
    display: none;
  }
}
</style>
