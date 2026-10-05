import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '../utils/auth'
import { login as loginApi, getCompanies } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const userId = ref(localStorage.getItem('user_id') ? Number(localStorage.getItem('user_id')) : null)
  const username = ref(localStorage.getItem('username') || '')
  const realName = ref(localStorage.getItem('real_name') || '')
  const userType = ref(localStorage.getItem('user_type') !== null ? Number(localStorage.getItem('user_type')) : 1)
  const currentCompanyId = ref(localStorage.getItem('current_company_id') || '')
  const currentCompanyName = ref(localStorage.getItem('current_company_name') || '')
  const currentRole = ref(localStorage.getItem('current_role') || '')
  const needChangePassword = ref(localStorage.getItem('need_change_password') === 'true')

  async function login(usernameVal, password) {
    const res = await loginApi({ username: usernameVal, password })
    token.value = res.data.token
    userId.value = res.data.userId
    username.value = res.data.username
    realName.value = res.data.realName
    userType.value = res.data.userType
    needChangePassword.value = res.data.needChangePassword || false
    setToken(res.data.token)
    localStorage.setItem('user_id', res.data.userId)
    localStorage.setItem('username', res.data.username)
    localStorage.setItem('real_name', res.data.realName || '')
    localStorage.setItem('user_type', res.data.userType)
    localStorage.setItem('need_change_password', res.data.needChangePassword ? 'true' : 'false')
    return res.data
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    realName.value = ''
    userType.value = 1
    currentCompanyId.value = ''
    currentCompanyName.value = ''
    currentRole.value = ''
    needChangePassword.value = false
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

  function setCurrentRole(role) {
    currentRole.value = role
    localStorage.setItem('current_role', role)
  }

  function isBoss() {
    return currentRole.value === 'boss'
  }

  function isSupervisor() {
    return currentRole.value === 'supervisor'
  }

  function setCurrentCompanyId(id) {
    currentCompanyId.value = id
    localStorage.setItem('current_company_id', id)
  }

  function setCurrentCompanyName(name) {
    currentCompanyName.value = name
    localStorage.setItem('current_company_name', name)
  }

  function isSuperAdmin() {
    return userType.value === 0
  }

  /** 超管穿透进入某公司 */
  function enterCompany(companyId, companyName) {
    currentCompanyId.value = companyId
    currentCompanyName.value = companyName
    currentRole.value = 'boss'
    localStorage.setItem('current_company_id', companyId)
    localStorage.setItem('current_company_name', companyName)
    localStorage.setItem('current_role', 'boss')
  }

  /** 退出穿透，恢复超管全局视角 */
  function exitCompany() {
    currentCompanyId.value = ''
    currentCompanyName.value = ''
    currentRole.value = ''
    localStorage.removeItem('current_company_id')
    localStorage.removeItem('current_company_name')
    localStorage.removeItem('current_role')
  }

  /** 超管是否正在穿透进入某公司 */
  function isPenetrating() {
    return isSuperAdmin() && !!currentCompanyId.value
  }

  /** 清除强制改密标记（修改密码成功后调用） */
  function clearChangePasswordFlag() {
    needChangePassword.value = false
    localStorage.setItem('need_change_password', 'false')
  }

  /**
   * 从服务器刷新当前公司在 user_company 中的角色
   * 解决老板修改职位后手机端不更新的问题
   * @returns {Promise<{changed: boolean, oldRole: string, newRole: string}>}
   */
  async function refreshRole() {
    if (!currentCompanyId.value) return { changed: false, oldRole: currentRole.value, newRole: currentRole.value }
    try {
      const res = await getCompanies()
      const companies = res.data || []
      const current = companies.find(c => String(c.companyId) === String(currentCompanyId.value))
      if (current && current.role !== currentRole.value) {
        const oldRole = currentRole.value
        setCurrentRole(current.role)
        return { changed: true, oldRole, newRole: current.role }
      }
      return { changed: false, oldRole: currentRole.value, newRole: currentRole.value }
    } catch (e) {
      return { changed: false, oldRole: currentRole.value, newRole: currentRole.value }
    }
  }

  return {
    token, userId, username, realName, userType, currentCompanyId, currentCompanyName, currentRole, needChangePassword,
    login, logout, setCurrentCompanyId, setCurrentCompanyName, setCurrentRole, isSuperAdmin, isBoss, isSupervisor,
    enterCompany, exitCompany, isPenetrating, clearChangePasswordFlag, refreshRole
  }
})
