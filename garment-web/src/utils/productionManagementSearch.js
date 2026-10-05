function normalize(value) {
  return String(value ?? '').trim().toLowerCase()
}

function includesKeyword(value, keyword) {
  return normalize(value).includes(keyword)
}

export function filterProductionBeds(beds, keyword) {
  const normalizedKeyword = normalize(keyword)
  if (!normalizedKeyword) return beds || []

  return (beds || []).filter((bed) =>
    includesKeyword(bed.bedNo, normalizedKeyword) ||
    includesKeyword(bed.name, normalizedKeyword) ||
    includesKeyword(bed.colors, normalizedKeyword) ||
    (bed.processes || []).some((process) =>
      includesKeyword(process.name, normalizedKeyword)
    )
  )
}

export function filterProcessTemplates(templates, keyword) {
  const normalizedKeyword = normalize(keyword)
  if (!normalizedKeyword) return templates || []

  return (templates || []).filter((template) =>
    includesKeyword(template.name, normalizedKeyword) ||
    includesKeyword(template.remark, normalizedKeyword) ||
    (template.items || []).some((item) =>
      includesKeyword(item.name, normalizedKeyword)
    )
  )
}
