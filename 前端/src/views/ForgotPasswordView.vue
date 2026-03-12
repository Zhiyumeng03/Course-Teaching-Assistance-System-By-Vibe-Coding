<template>
  <div class="forgot-password-container">
    <div class="forgot-password-card">
      <div class="forgot-password-header">
        <h1 class="forgot-password-title">重置密码</h1>
        <p class="forgot-password-subtitle">通过安全问题验证身份并重置密码</p>
      </div>
      
      <!-- 步骤1：输入邮箱 -->
      <div v-if="currentStep === 1" class="step-content">
        <el-form
          ref="emailFormRef"
          :model="emailForm"
          :rules="emailRules"
          class="forgot-password-form"
          @submit.prevent="handleGetSecurityQuestion"
        >
          <el-form-item prop="email">
            <el-input
              v-model="emailForm.email"
              placeholder="请输入注册时使用的邮箱地址"
              size="large"
              :prefix-icon="Message"
              clearable
            />
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="action-button"
              :loading="loading"
              @click="handleGetSecurityQuestion"
            >
              {{ loading ? '验证中...' : '下一步' }}
            </el-button>
          </el-form-item>
          
          <el-form-item>
            <div class="back-link">
              <el-link type="primary" @click="goToLogin">
                ← 返回登录
              </el-link>
            </div>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 步骤2：回答安全问题 -->
      <div v-if="currentStep === 2" class="step-content">
        <el-form
          ref="securityFormRef"
          :model="securityForm"
          :rules="securityRules"
          class="forgot-password-form"
          @submit.prevent="handleVerifySecurityAnswer"
        >
          <el-form-item>
            <div class="email-info">
              <el-icon><Message /></el-icon>
              <span>邮箱：{{ emailForm.email }}</span>
            </div>
          </el-form-item>
          
          <el-form-item>
            <div class="security-question">
              <h3>安全问题：</h3>
              <p>{{ securityQuestion }}</p>
            </div>
          </el-form-item>
          
          <el-form-item prop="answer">
            <el-input
              v-model="securityForm.answer"
              placeholder="请输入您的答案"
              size="large"
              :prefix-icon="QuestionFilled"
              clearable
            />
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="action-button"
              :loading="verifying"
              @click="handleVerifySecurityAnswer"
            >
              {{ verifying ? '验证中...' : '验证答案' }}
            </el-button>
          </el-form-item>
          
          <el-form-item>
            <div class="back-link">
              <el-link type="primary" @click="goBackToStep1">
                ← 返回上一步
              </el-link>
            </div>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 步骤3：重置密码 -->
      <div v-if="currentStep === 3" class="step-content">
        <el-form
          ref="resetFormRef"
          :model="resetForm"
          :rules="resetRules"
          class="forgot-password-form"
          @submit.prevent="handleResetPassword"
        >
          <el-form-item>
            <div class="success-info">
              <el-icon><CircleCheck /></el-icon>
              <span>身份验证成功，请设置新密码</span>
            </div>
          </el-form-item>
          
          <el-form-item prop="newPassword">
            <el-input
              v-model="resetForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              clearable
              @input="checkPasswordStrength"
            />
            <!-- 密码强度指示器 -->
            <div v-if="resetForm.newPassword" class="password-strength">
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
              v-model="resetForm.confirmPassword"
              type="password"
              placeholder="请确认新密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="action-button"
              :loading="resetting"
              @click="handleResetPassword"
            >
              {{ resetting ? '重置中...' : '重置密码' }}
            </el-button>
          </el-form-item>
          
          <el-form-item>
            <div class="back-link">
              <el-link type="primary" @click="goBackToStep2">
                ← 返回上一步
              </el-link>
            </div>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 步骤4：重置成功 -->
      <div v-if="currentStep === 4" class="step-content success-content">
        <div class="success-icon">
          <el-icon size="64" color="#2ecc71">
            <CircleCheck />
          </el-icon>
        </div>
        <h2 class="success-title">密码重置成功！</h2>
        <p class="success-message">您的密码已成功重置，请使用新密码登录</p>
        <el-button
          type="primary"
          size="large"
          class="action-button"
          @click="goToLogin"
        >
          立即登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Message, QuestionFilled, Lock, CircleCheck } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// 表单引用
const emailFormRef = ref()
const securityFormRef = ref()
const resetFormRef = ref()

// 当前步骤
const currentStep = ref(1)

// 加载状态
const loading = ref(false)
const verifying = ref(false)
const resetting = ref(false)

// 邮箱表单数据
const emailForm = reactive({
  email: ''
})

// 安全问题表单数据
const securityForm = reactive({
  answer: ''
})

// 重置密码表单数据
const resetForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

// 安全问题
const securityQuestion = ref('')

// 密码强度相关
const passwordStrength = ref(0)
const strengthText = ref('')
const strengthClass = ref('')

// 计算密码强度百分比
const strengthPercentage = computed(() => {
  return Math.min(passwordStrength.value * 25, 100)
})

// 邮箱验证规则
const emailRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// 安全问题验证规则
const securityRules = {
  answer: [
    { required: true, message: '请输入安全问题答案', trigger: 'blur' },
    { min: 2, max: 50, message: '答案长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

// 重置密码验证规则
const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
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
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 检查密码强度
const checkPasswordStrength = () => {
  const password = resetForm.newPassword
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

// 获取安全问题
const handleGetSecurityQuestion = async () => {
  if (!emailFormRef.value) return
  
  try {
    const valid = await emailFormRef.value.validate()
    if (!valid) return
    
    loading.value = true
    
    // 调用获取安全问题API
    const result = await authStore.getSecurityQuestion(emailForm.email)
    
    if (result.success) {
      securityQuestion.value = result.question
      currentStep.value = 2
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('获取安全问题失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 验证安全问题答案
const handleVerifySecurityAnswer = async () => {
  if (!securityFormRef.value) return
  
  try {
    const valid = await securityFormRef.value.validate()
    if (!valid) return
    
    verifying.value = true
    
    // 调用验证API
    const result = await authStore.verifySecurityAnswer({
      email: emailForm.email,
      answer: securityForm.answer
    })
    
    if (result.success) {
      ElMessage.success(result.message)
      currentStep.value = 3
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('验证失败，请稍后重试')
  } finally {
    verifying.value = false
  }
}

// 重置密码
const handleResetPassword = async () => {
  if (!resetFormRef.value) return
  
  try {
    const valid = await resetFormRef.value.validate()
    if (!valid) return
    
    resetting.value = true
    
    // 调用重置密码API
    const result = await authStore.resetPassword({
      email: emailForm.email,
      newPassword: resetForm.newPassword
    })
    
    if (result.success) {
      ElMessage.success(result.message)
      currentStep.value = 4
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('重置失败，请稍后重试')
  } finally {
    resetting.value = false
  }
}

// 返回第一步
const goBackToStep1 = () => {
  currentStep.value = 1
  securityForm.answer = ''
  securityQuestion.value = ''
}

// 返回第二步
const goBackToStep2 = () => {
  currentStep.value = 2
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  passwordStrength.value = 0
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.forgot-password-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.forgot-password-card {
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

.forgot-password-header {
  text-align: center;
  margin-bottom: 30px;
}

.forgot-password-title {
  font-size: 28px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 8px 0;
}

.forgot-password-subtitle {
  color: #7f8c8d;
  margin: 0;
  font-size: 14px;
}

.step-content {
  width: 100%;
}

.forgot-password-form {
  width: 100%;
}

.email-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  color: #606266;
  font-size: 14px;
}

.security-question {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #3498db;
}

.security-question h3 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 16px;
}

.security-question p {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.success-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #d4edda;
  border-radius: 8px;
  color: #155724;
  font-size: 14px;
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

.action-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #3498db, #2ecc71);
  border: none;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.action-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(52, 152, 219, 0.3);
}

.back-link {
  text-align: center;
}

.success-content {
  text-align: center;
}

.success-icon {
  margin-bottom: 20px;
  animation: bounceIn 0.6s ease-out;
}

@keyframes bounceIn {
  0% {
    opacity: 0;
    transform: scale(0.3);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
  70% {
    transform: scale(0.9);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

.success-title {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 12px 0;
}

.success-message {
  color: #7f8c8d;
  margin: 0 0 30px 0;
  font-size: 14px;
  line-height: 1.5;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .forgot-password-container {
    padding: 10px;
  }
  
  .forgot-password-card {
    padding: 30px 20px;
  }
  
  .forgot-password-title {
    font-size: 24px;
  }
  
  .success-title {
    font-size: 20px;
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

:deep(.el-link) {
  font-weight: 500;
}

:deep(.el-form-item__error) {
  font-size: 12px;
  margin-top: 4px;
}
</style>

