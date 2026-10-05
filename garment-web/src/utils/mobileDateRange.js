import { formatLocalDate } from './mobileRecordFilters.js'

export function normalizeDateRange(range = []) {
  const start = range?.[0] || ''
  const end = range?.[1] || ''
  if (!start || !end) return [start, end]
  return end < start ? [start, start] : [start, end]
}

export function getMobileDatePreset(preset, now = new Date()) {
  const end = formatLocalDate(now)

  if (preset === 'today') return [end, end]

  if (preset === 'recent7') {
    const start = new Date(now)
    start.setDate(now.getDate() - 6)
    return [formatLocalDate(start), end]
  }

  if (preset === 'week') {
    const dayOfWeek = now.getDay() || 7
    const start = new Date(now)
    start.setDate(now.getDate() - dayOfWeek + 1)
    return [formatLocalDate(start), end]
  }

  if (preset === 'month') {
    return [
      `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`,
      end
    ]
  }

  return ['', '']
}
