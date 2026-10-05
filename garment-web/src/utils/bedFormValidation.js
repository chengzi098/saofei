export function normalizeBedName(value) {
  return String(value || '').trim()
}

export function validateBedForm(form) {
  if (!form.bedNo || !String(form.bedNo).trim()) {
    return { valid: false, message: '请输入床号编号' }
  }
  if (!normalizeBedName(form.name)) {
    return { valid: false, message: '请输入名称' }
  }
  if (!form.quantity || form.quantity <= 0) {
    return { valid: false, message: '件数必须大于0' }
  }
  return { valid: true }
}
