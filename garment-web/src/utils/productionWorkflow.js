export const PRODUCTION_STAGES = [
  { key: 'style', label: '选择款式' },
  { key: 'bed', label: '添加床号' },
  { key: 'bundle', label: '分扎' },
  { key: 'print', label: '打印' }
]

export function getProductionStage({ styleId, bedId, printOpen = false }) {
  if (!styleId) return 'style'
  if (!bedId) return 'bed'
  if (printOpen) return 'print'
  return 'bundle'
}
