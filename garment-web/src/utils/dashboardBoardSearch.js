function normalize(value) {
  return String(value || '').trim().toLowerCase()
}

export function getMatchedBeds(style, keyword) {
  const normalizedKeyword = normalize(keyword)
  if (!normalizedKeyword) return []
  return (style?.beds || []).filter((bed) =>
    normalize(bed.bedNo).includes(normalizedKeyword)
  )
}

export function filterDashboardBeds(beds, keyword) {
  const normalizedKeyword = normalize(keyword)
  if (!normalizedKeyword) return beds || []

  return (beds || []).filter((bed) =>
    normalize(bed.bedNo).includes(normalizedKeyword)
  )
}

export function filterDashboardStyles(styles, keyword) {
  const normalizedKeyword = normalize(keyword)
  if (!normalizedKeyword) return styles

  return styles.filter((style) =>
    normalize(style.styleNo).includes(normalizedKeyword) ||
    getMatchedBeds(style, normalizedKeyword).length > 0
  )
}
