export function normalizeBundleNo(value) {
  return String(value || '').trim().toUpperCase()
}

export function buildQrPrintParams({ styleId, bedId, bundleNo, pageSize = 500 } = {}) {
  const params = { pageNum: 1, pageSize }
  if (styleId) params.styleId = styleId
  if (bedId) params.bedId = bedId
  const normalizedBundleNo = normalizeBundleNo(bundleNo)
  if (normalizedBundleNo) params.bundleNo = normalizedBundleNo
  return params
}

export function filterSelectedBed(beds, bedId) {
  if (!bedId) return ''
  return beds.some((bed) => Number(bed.id) === Number(bedId)) ? Number(bedId) : ''
}

export function formatBedDisplayName(bed) {
  const bedNo = String(bed?.bedNo || '').trim()
  const bedName = String(bed?.bedName || bed?.name || '').trim()
  if (bedNo && bedName) return `${bedNo} · ${bedName}`
  return bedNo || bedName || '-'
}
