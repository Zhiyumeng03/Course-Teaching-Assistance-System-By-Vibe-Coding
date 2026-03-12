<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h1 class="register-title">创建账户</h1>
        <p class="register-subtitle">加入我们，开始您的学习之旅</p>
      </div>
      
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱地址"
            size="large"
            :prefix-icon="Message"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            clearable
            @input="checkPasswordStrength"
          />
          <!-- 密码强度指示器 -->
          <div v-if="registerForm.password" class="password-strength">
            <div class="strength-bar">
              <div 
                class="strength-fill" 
                :class="strengthClass"
                :style="{ width: strengthPercentage + '%' }"
              ></div>
            </div>
            <span class="strength-text" :class="strengthClass">
              {{ strengthText }}
            </span>
          </div>
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="securityQuestion">
          <el-select
            v-model="registerForm.securityQuestion"
            placeholder="请选择安全问题"
            size="large"
            class="w-full"
          >
            <el-option
              v-for="question in securityQuestions"
              :key="question"
              :label="question"
              :value="question"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item prop="securityAnswer">
          <el-input
            v-model="registerForm.securityAnswer"
            placeholder="请输入安全问题答案"
            size="large"
            :prefix-icon="QuestionFilled"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="captcha">
          <div class="captcha-container">
            <el-input
              v-model="registerForm.captcha"
              placeholder="请输入验证码"
              size="large"
             
              clearable
              class="captcha-input"
            />
            <div class="captcha-image" @click="refreshCaptcha">
              <img :src="captchaImage" alt="验证码" />
              <div class="captcha-refresh">
                <el-icon><Refresh /></el-icon>
              </div>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="register-button"
            :loading="loading"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '立即注册' }}
          </el-button>
        </el-form-item>
        
        <el-form-item>
          <div class="login-link">
            已有账户？
            <el-link type="primary" @click="goToLogin">
              立即登录
            </el-link>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Message, Lock, Refresh, QuestionFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// 表单引用
const registerFormRef = ref()

// 加载状态
const loading = ref(false)

// 注册表单数据
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  securityQuestion: '',
  securityAnswer: '',
  captcha: ''
})

// 验证码图片
const captchaImage = ref('')

// 安全问题选项
const securityQuestions = [
  '您最喜欢的颜色是什么？',
  '您的出生地是哪里？',
  '您的小学班主任姓什么？',
  '您的第一个宠物的名字是什么？',
  '您最喜欢的电影是什么？',
  '您的中学学校名称是什么？',
  '您最喜欢的运动是什么？',
  '您的童年绰号是什么？'
]

// 密码强度相关
const passwordStrength = ref(0)
const strengthText = ref('')
const strengthClass = ref('')

// 计算密码强度百分比
const strengthPercentage = computed(() => {
  return Math.min(passwordStrength.value * 25, 100)
})

// 表单验证规则
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度在 8 到 20 个字符', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value && passwordStrength.value < 2) {
          callback(new Error('密码强度太弱，请包含字母、数字和特殊字符'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  securityQuestion: [
    { required: true, message: '请选择安全问题', trigger: 'change' }
  ],
  securityAnswer: [
    { required: true, message: '请输入安全问题答案', trigger: 'blur' },
    { min: 2, max: 50, message: '答案长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码长度为4位', trigger: 'blur' }
  ]
}

// 检查密码强度
const checkPasswordStrength = () => {
  const password = registerForm.password
  if (!password) {
    passwordStrength.value = 0
    strengthText.value = ''
    strengthClass.value = ''
    return
  }

  let strength = 0
  const checks = {
    length: password.length >= 8,
    lowercase: /[a-z]/.test(password),
    uppercase: /[A-Z]/.test(password),
    numbers: /\d/.test(password),
    symbols: /[!@#$%^&*(),.?":{}|<>]/.test(password)
  }

  // 计算强度分数
  Object.values(checks).forEach(check => {
    if (check) strength++
  })

  passwordStrength.value = strength

  // 设置强度文本和样式
  if (strength <= 1) {
    strengthText.value = '很弱'
    strengthClass.value = 'weak'
  } else if (strength <= 2) {
    strengthText.value = '较弱'
    strengthClass.value = 'weak'
  } else if (strength <= 3) {
    strengthText.value = '一般'
    strengthClass.value = 'medium'
  } else if (strength <= 4) {
    strengthText.value = '较强'
    strengthClass.value = 'strong'
  } else {
    strengthText.value = '很强'
    strengthClass.value = 'very-strong'
  }
}

// 生成验证码图片
const generateCaptcha = () => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let result = ''
  for (let i = 0; i < 4; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  canvas.width = 120
  canvas.height = 40
  
  // 背景
  ctx.fillStyle = '#f0f0f0'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  
  // 文字
  ctx.font = '20px Arial'
  ctx.fillStyle = '#333'
  ctx.textAlign = 'center'
  ctx.fillText(result, canvas.width / 2, canvas.height / 2 + 7)
  
  // 干扰线
  for (let i = 0; i < 5; i++) {
    ctx.strokeStyle = '#ccc'
    ctx.beginPath()
    ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height)
    ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height)
    ctx.stroke()
  }
  
  captchaImage.value = canvas.toDataURL()
  return result
}

// 刷新验证码
const refreshCaptcha = () => {
  generateCaptcha()
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    const valid = await registerFormRef.value.validate()
    if (!valid) return
    
    loading.value = true
    
    // 调用注册API
    const result = await authStore.register(registerForm)
    
    if (result.success) {
      ElMessage.success(result.message)
      // 注册成功后跳转到登录页面
      router.push('/login')
    } else {
      ElMessage.error(result.message)
      refreshCaptcha() // 注册失败时刷新验证码
    }
  } catch (error) {
    ElMessage.error('注册失败，请稍后重试')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}

// 组件挂载时生成验证码
onMounted(() => {
  generateCaptcha()
})
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 100%;
  max-width: 450px;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-title {
  font-size: 28px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 8px 0;
}

.register-subtitle {
  color: #7f8c8d;
  margin: 0;
  font-size: 14px;
}

.register-form {
  width: 100%;
}

.password-strength {
  margin-top: 8px;
}

.strength-bar {
  width: 100%;
  height: 4px;
  background: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 4px;
}

.strength-fill {
  height: 100%;
  transition: all 0.3s ease;
  border-radius: 2px;
}

.strength-fill.weak {
  background: #ff4757;
}

.strength-fill.medium {
  background: #ffa502;
}

.strength-fill.strong {
  background: #2ed573;
}

.strength-fill.very-strong {
  background: #1e90ff;
}

.strength-text {
  font-size: 12px;
  font-weight: 500;
}

.strength-text.weak {
  color: #ff4757;
}

.strength-text.medium {
  color: #ffa502;
}

.strength-text.strong {
  color: #2ed573;
}

.strength-text.very-strong {
  color: #1e90ff;
}

.captcha-container {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  position: relative;
  width: 120px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
}

.captcha-image:hover {
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-refresh {
  position: absolute;
  top: 0;
  right: 0;
  width: 20px;
  height: 20px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.captcha-image:hover .captcha-refresh {
  opacity: 1;
}

.register-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #3498db, #2ecc71);
  border: none;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.register-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(52, 152, 219, 0.3);
}

.login-link {
  text-align: center;
  color: #7f8c8d;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .register-container {
    padding: 10px;
  }
  
  .register-card {
    padding: 30px 20px;
  }
  
  .register-title {
    font-size: 24px;
  }
  
  .captcha-container {
    flex-direction: column;
    gap: 8px;
  }
  
  .captcha-image {
    width: 100%;
    height: 50px;
  }
}

/* Element Plus 样式覆盖 */
:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dcdfe6;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #3498db;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-select .el-input__wrapper) {
  width: 100%;
}

:deep(.el-link) {
  font-weight: 500;
}

:deep(.el-form-item__error) {
  font-size: 12px;
  margin-top: 4px;
}
</style>

