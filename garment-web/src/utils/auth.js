const TOKEN_KEY = 'garment_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 清除所有认证相关的 localStorage 数据
 */
export function clearAllAuth() {
  removeToken()
  localStorage.removeItem('user_id')
  localStorage.removeItem('username')
  localStorage.removeItem('real_name')
  localStorage.removeItem('user_type')
  localStorage.removeItem('current_company_id')
  localStorage.removeItem('current_company_name')
  localStorage.removeItem('current_role')
  localStorage.removeItem('need_change_password')
}

/**
 * 检查 token 是否有效（存在且未过期）
 */
export function isTokenValid() {
  const token = getToken()
  if (!token) return false

  try {
    // 解析 JWT payload（第二段 base64）
    const payload = JSON.parse(atob(token.split('.')[1]))
    // exp 是秒级时间戳
    if (payload.exp && Date.now() >= payload.exp * 1000) {
      // token 已过期，清除
      removeToken()
      return false
    }
    return true
  } catch {
    // token 格式异常，清除
    removeToken()
    return false
  }
}
