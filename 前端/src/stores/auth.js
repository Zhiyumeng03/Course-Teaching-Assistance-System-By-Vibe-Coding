import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')
  const isLoggedIn = computed(() => !!token.value)

  // 登录
  const login = async (loginData) => {
    try {
      // 模拟API调用
      const response = await mockLogin(loginData)
      
      if (response.success) {
        user.value = response.user
        token.value = response.token
        localStorage.setItem('token', response.token)
        localStorage.setItem('user', JSON.stringify(response.user))
        
        return { success: true, message: '登录成功' }
      } else {
        return { success: false, message: response.message }
      }
    } catch (error) {
      return { success: false, message: '登录失败，请稍后重试' }
    }
  }

  // 注册
  const register = async (registerData) => {
    try {
      // 模拟API调用
      const response = await mockRegister(registerData)
      
      if (response.success) {
        return { success: true, message: '注册成功，请登录' }
      } else {
        return { success: false, message: response.message }
      }
    } catch (error) {
      return { success: false, message: '注册失败，请稍后重试' }
    }
  }

  // 忘记密码 - 获取安全问题
  const getSecurityQuestion = async (email) => {
    try {
      // 模拟API调用
      const response = await mockGetSecurityQuestion(email)
      return response
    } catch (error) {
      return { success: false, message: '获取安全问题失败，请稍后重试' }
    }
  }

  // 验证安全问题答案
  const verifySecurityAnswer = async (data) => {
    try {
      // 模拟API调用
      const response = await mockVerifySecurityAnswer(data)
      return response
    } catch (error) {
      return { success: false, message: '验证失败，请稍后重试' }
    }
  }

  // 重置密码
  const resetPassword = async (resetData) => {
    try {
      // 模拟API调用
      const response = await mockResetPassword(resetData)
      return response
    } catch (error) {
      return { success: false, message: '重置失败，请稍后重试' }
    }
  }

  // 登出
  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // 初始化用户信息
  const initUser = () => {
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')
    
    if (savedToken && savedUser) {
      token.value = savedToken
      user.value = JSON.parse(savedUser)
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    login,
    register,
    getSecurityQuestion,
    verifySecurityAnswer,
    resetPassword,
    logout,
    initUser
  }
})

// 模拟API函数
const mockLogin = async (data) => {
  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 1000))
  
  // 简单的模拟验证
  if (data.username === 'admin' && data.password === '123456') {
    return {
      success: true,
      user: { 
        id: 1, 
        username: 'admin', 
        email: 'admin@example.com',
        securityQuestion: '您最喜欢的颜色是什么？'
      },
      token: 'mock-jwt-token-' + Date.now()
    }
  } else {
    return {
      success: false,
      message: '用户名或密码错误'
    }
  }
}

const mockRegister = async (data) => {
  await new Promise(resolve => setTimeout(resolve, 1000))
  
  // 检查用户名是否已存在
  if (data.username === 'admin') {
    return {
      success: false,
      message: '用户名已存在'
    }
  }
  
  return {
    success: true,
    message: '注册成功'
  }
}

const mockGetSecurityQuestion = async (email) => {
  await new Promise(resolve => setTimeout(resolve, 1000))
  
  // 模拟安全问题
  const questions = [
    '您最喜欢的颜色是什么？',
    '您的出生地是哪里？',
    '您的小学班主任姓什么？',
    '您的第一个宠物的名字是什么？',
    '您最喜欢的电影是什么？'
  ]
  
  return {
    success: true,
    question: questions[Math.floor(Math.random() * questions.length)]
  }
}

const mockVerifySecurityAnswer = async (data) => {
  await new Promise(resolve => setTimeout(resolve, 1000))
  
  // 模拟验证（实际项目中应该与数据库中的答案比较）
  const correctAnswers = ['蓝色', '北京', '李老师', '小白', '泰坦尼克号']
  
  if (correctAnswers.includes(data.answer.toLowerCase())) {
    return {
      success: true,
      message: '验证成功'
    }
  } else {
    return {
      success: false,
      message: '安全问题答案错误'
    }
  }
}

const mockResetPassword = async (data) => {
  await new Promise(resolve => setTimeout(resolve, 1000))
  
  return {
    success: true,
    message: '密码重置成功'
  }
}

