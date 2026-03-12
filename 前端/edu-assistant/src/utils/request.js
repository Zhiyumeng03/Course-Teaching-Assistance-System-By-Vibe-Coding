import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearAuthSession, getToken } from '@/utils/auth'

// 鍒涘缓 axios 瀹炰緥锛屽叏灞€閰嶇疆鍚庣鎺ュ彛鍦板潃
const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 璇锋眰鎷︽埅鍣細鑷姩鎼哄甫 JWT Token
request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 鍝嶅簲鎷︽埅鍣細缁熶竴澶勭悊閿欒
request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message || '璇锋眰澶辫触锛岃绋嶅悗閲嶈瘯'

    if (status === 401) {
      // Token 鏃犳晥鎴栬繃鏈?鈫?娓呴櫎鐧诲綍鐘舵€佸苟璺宠浆鐧诲綍椤?      ElMessage.error('鐧诲綍宸茶繃鏈燂紝璇烽噸鏂扮櫥褰?)
      clearAuthSession()
      window.location.href = '/login'
    } else if (status === 403) {
      // 宸茬櫥褰曚絾鏉冮檺涓嶈冻锛堝瀛︾敓璁块棶鏁欏笀鎺ュ彛锛?      ElMessage.error('鏉冮檺涓嶈冻锛屾棤娉曟墽琛岃鎿嶄綔')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default request
