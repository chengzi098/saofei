import axios from 'axios'
import { getToken, clearAllAuth } from '../utils/auth'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动加 token 和 company_id
request.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  // 登录接口不添加 X-Company-Id，避免公司被禁用/过期时无法登录
  if (!config.url.includes('/auth/login')) {
    const companyId = localStorage.getItem('current_company_id')
    if (companyId) {
      config.headers['X-Company-Id'] = companyId
    }
  }
  return config
})

// 响应拦截器：处理错误
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      if (res.code === 401) {
        clearAllAuth()
        router.push('/login')
      }
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  error => {
    if (error.response?.status === 401) {
      clearAllAuth()
      router.push('/login')
    } else {
      ElMessage.error(error.response?.data?.msg || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
