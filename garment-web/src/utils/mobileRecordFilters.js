export function formatLocalDate(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function buildMobileRecordParams({
  role,
  userId,
  activeStatus = 'all',
  activeDateTab = 'today',
  dateRange = [],
  now = new Date(),
  pageNum = 1,
  pageSize = 100
} = {}) {
  const params = { pageNum, pageSize }

  if (role === 'worker' && userId) {
    params.workerId = userId
  }

  if (activeStatus === 'passed') {
    params.status = 1
  } else if (activeStatus === 'returned') {
    params.status = 3
  }

  const today = formatLocalDate(now)

  switch (activeDateTab) {
    case 'today':
      params.startDate = today
      params.endDate = today
      break
    case 'week': {
      const dayOfWeek = now.getDay() || 7
      const startOfWeek = new Date(now)
      startOfWeek.setDate(now.getDate() - dayOfWeek + 1)
      params.startDate = formatLocalDate(startOfWeek)
      params.endDate = today
      break
    }
    case 'month':
      params.startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
      params.endDate = today
      break
    case 'custom':
      if (dateRange && dateRange.length === 2) {
        params.startDate = dateRange[0]
        params.endDate = dateRange[1]
      }
      break
  }

  return params
}
