<template>
  <div class="auth-page">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bubble bubble-1"></div>
      <div class="bubble bubble-2"></div>
      <div class="bubble bubble-3"></div>
      <div class="bubble bubble-4"></div>
    </div>

    <!-- 主卡片 -->
    <div class="auth-card">
      <!-- 左侧品牌区 -->
      <div class="brand-panel">
        <div class="brand-logo">
          <el-icon size="48"><Reading /></el-icon>
        </div>
        <h1 class="brand-title">知予萌</h1>
        <p class="brand-subtitle">课程教学辅助系统</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>智能课程管理</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>在线互动教学</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>学习进度追踪</span>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-panel">
        <!-- Tab 切换 -->
        <div class="tab-bar">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'login' }"
            @click="switchTab('login')"
          >
            登录
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'register' }"
            @click="switchTab('register')"
          >
            注册
          </button>
          <div
            class="tab-indicator"
            :class="{ right: activeTab === 'register' }"
          ></div>
        </div>

        <!-- 登录表单 -->
        <Transition name="slide-fade" mode="out-in">
          <div v-if="activeTab === 'login'" key="login" class="form-content">
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-desc">请输入您的账户信息</p>
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              size="large"
              @keyup.enter="handleLogin"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="loginForm.username"
                  placeholder="请输入用户名"
                  :prefix-icon="User"
                  clearable
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  :prefix-icon="Lock"
                  show-password
                  clearable
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  class="submit-btn"
                  :loading="loginLoading"
                  @click="handleLogin"
                >
                  {{ loginLoading ? "登录中..." : "立即登录" }}
                </el-button>
              </el-form-item>
            </el-form>
            <p class="switch-tip">
              还没有账号？
              <span class="link" @click="switchTab('register')">立即注册</span>
            </p>
          </div>

          <!-- 注册表单 -->
          <div v-else key="register" class="form-content">
            <h2 class="form-title">创建账户</h2>
            <p class="form-desc">填写信息完成注册</p>
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              size="large"
              label-position="top"
            >
              <!-- 角色选择 -->
              <el-form-item prop="role" label="身份">
                <el-radio-group v-model="registerForm.role" class="role-group">
                  <el-radio-button value="STUDENT">
                    <el-icon><UserFilled /></el-icon> 学生
                  </el-radio-button>
                  <el-radio-button value="teacher">
                    <el-icon><Avatar /></el-icon> 教师
                  </el-radio-button>
                </el-radio-group>
              </el-form-item>

              <div class="form-row">
                <el-form-item prop="username" label="用户名">
                  <el-input
                    v-model="registerForm.username"
                    placeholder="设置用户名"
                    :prefix-icon="User"
                    clearable
                  />
                </el-form-item>
                <el-form-item prop="realName" label="真实姓名">
                  <el-input
                    v-model="registerForm.realName"
                    placeholder="请输入真实姓名"
                    :prefix-icon="Edit"
                    clearable
                  />
                </el-form-item>
              </div>

              <!-- 学生编号（仅学生显示） -->
              <el-form-item
                v-if="registerForm.role === 'STUDENT'"
                prop="STUDENTNo"
                label="学号"
              >
                <el-input
                  v-model="registerForm.STUDENTNo"
                  placeholder="请输入学号"
                  :prefix-icon="Postcard"
                  clearable
                />
              </el-form-item>

              <!-- 教师编号（仅教师显示） -->
              <el-form-item
                v-if="registerForm.role === 'teacher'"
                prop="teacherNo"
                label="工号"
              >
                <el-input
                  v-model="registerForm.teacherNo"
                  placeholder="请输入工号"
                  :prefix-icon="Postcard"
                  clearable
                />
              </el-form-item>

              <div class="form-row">
                <el-form-item prop="email" label="邮箱">
                  <el-input
                    v-model="registerForm.email"
                    placeholder="请输入邮箱"
                    :prefix-icon="Message"
                    clearable
                  />
                </el-form-item>
                <el-form-item prop="phone" label="手机号">
                  <el-input
                    v-model="registerForm.phone"
                    placeholder="请输入手机号"
                    :prefix-icon="Phone"
                    clearable
                  />
                </el-form-item>
              </div>

              <el-form-item prop="password" label="密码">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="请设置密码（至少6位）"
                  :prefix-icon="Lock"
                  show-password
                  clearable
                />
              </el-form-item>

              <el-form-item prop="confirmPassword" label="确认密码">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入密码"
                  :prefix-icon="Lock"
                  show-password
                  clearable
                />
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  class="submit-btn"
                  :loading="registerLoading"
                  @click="handleRegister"
                >
                  {{ registerLoading ? "注册中..." : "立即注册" }}
                </el-button>
              </el-form-item>
            </el-form>
            <p class="switch-tip">
              已有账号？
              <span class="link" @click="switchTab('login')">立即登录</span>
            </p>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  User,
  Lock,
  Edit,
  Message,
  Phone,
  Postcard,
  Check,
  Reading,
  UserFilled,
  Avatar,
} from "@element-plus/icons-vue";
import { login, register } from "@/api/user";
import { setAuthSession } from "@/utils/auth";

const router = useRouter();

// ==================== Tab 切换 ====================
const activeTab = ref("login");
const switchTab = (tab) => {
  activeTab.value = tab;
};

// ==================== 登录逻辑 ====================
const loginFormRef = ref(null);
const loginLoading = ref(false);

const loginForm = reactive({
  username: "",
  password: "",
});

const loginRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 2, max: 20, message: "用户名长度为 2~20 位", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 0, message: "密码不少于 6 位", trigger: "blur" },
  ],
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;
  const valid = await loginFormRef.value.validate().catch(() => false);
  if (!valid) return;

  loginLoading.value = true;
  try {
    const res = await login({
      username: loginForm.username,
      password: loginForm.password,
    });
    // 保存 token 和用户信息
    setAuthSession(res.token, res.user);
    ElMessage.success(
      `欢迎回来，${res.user?.realName || res.user?.username}！`,
    );
    router.push("/home");
  } catch {
    // 错误已由拦截器统一处理
  } finally {
    loginLoading.value = false;
  }
};

// ==================== 注册逻辑 ====================
const registerFormRef = ref(null);
const registerLoading = ref(false);

const registerForm = reactive({
  username: "",
  password: "",
  confirmPassword: "",
  role: "STUDENT",
  realName: "",
  STUDENTNo: "",
  teacherNo: "",
  email: "",
  phone: "",
});

const validateConfirmPwd = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

const registerRules = {
  role: [{ required: true, message: "请选择身份", trigger: "change" }],
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 2, max: 20, message: "用户名长度为 2~20 位", trigger: "blur" },
  ],
  realName: [{ required: true, message: "请输入真实姓名", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码不少于 6 位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认密码", trigger: "blur" },
    { validator: validateConfirmPwd, trigger: "blur" },
  ],
  email: [{ type: "email", message: "请输入正确的邮箱格式", trigger: "blur" }],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号",
      trigger: "blur",
    },
  ],
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;
  const valid = await registerFormRef.value.validate().catch(() => false);
  if (!valid) return;

  registerLoading.value = true;
  try {
    const payload = {
      username: registerForm.username,
      password: registerForm.password,
      role: registerForm.role,
      realName: registerForm.realName,
      email: registerForm.email || null,
      phone: registerForm.phone || null,
      STUDENTNo:
        registerForm.role === "STUDENT" ? registerForm.STUDENTNo : null,
      teacherNo:
        registerForm.role === "teacher" ? registerForm.teacherNo : null,
    };
    await register(payload);
    ElMessage.success("注册成功！请登录");
    switchTab("login");
    loginForm.username = registerForm.username;
  } catch {
    // 错误已由拦截器统一处理
  } finally {
    registerLoading.value = false;
  }
};
</script>

<style scoped>
/* ==================== 页面整体 ==================== */
.auth-page {
  width: 100vw;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1c2e 0%, #16213e 40%, #0f3460 100%);
  overflow-y: auto;
  position: relative;
  padding: 24px 16px;
}

/* ==================== 背景气泡动画 ==================== */
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bubble {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
  animation: float 8s ease-in-out infinite;
}

.bubble-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #6c63ff, transparent);
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.bubble-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #4facfe, transparent);
  bottom: -80px;
  right: 10%;
  animation-delay: 2s;
}

.bubble-3 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, #a18cd1, transparent);
  top: 30%;
  right: -50px;
  animation-delay: 4s;
}

.bubble-4 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, #fbc2eb, transparent);
  bottom: 20%;
  left: 5%;
  animation-delay: 1s;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-25px) scale(1.04);
  }
}

/* ==================== 主卡片 ==================== */
.auth-card {
  display: flex;
  width: min(900px, 100%);
  min-height: 520px;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
}

/* ==================== 左侧品牌区 ==================== */
.brand-panel {
  width: 300px;
  flex-shrink: 0;
  background: linear-gradient(160deg, #6c63ff 0%, #3b82f6 60%, #06b6d4 100%);
  padding: 36px 28px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.brand-panel::before {
  content: "";
  position: absolute;
  width: 280px;
  height: 280px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  top: -80px;
  right: -80px;
}

.brand-panel::after {
  content: "";
  position: absolute;
  width: 180px;
  height: 180px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
  bottom: -40px;
  left: -40px;
}

.brand-logo {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 20px;
  backdrop-filter: blur(4px);
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  color: white;
  margin: 0 0 8px 0;
  letter-spacing: 2px;
}

.brand-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 40px 0;
  letter-spacing: 1px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.feature-item .el-icon {
  width: 24px;
  height: 24px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

/* ==================== 右侧表单区 ==================== */
.form-panel {
  flex: 1;
  background: #ffffff;
  padding: 28px 36px;
  display: flex;
  flex-direction: column;
}

/* ==================== Tab 切换 ==================== */
.tab-bar {
  display: flex;
  position: relative;
  background: #f0f2f5;
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 20px;
  width: 200px;
  flex-shrink: 0;
}

.tab-btn {
  flex: 1;
  padding: 8px 0;
  border: none;
  background: transparent;
  font-size: 15px;
  font-weight: 500;
  color: #8c8c8c;
  cursor: pointer;
  border-radius: 9px;
  transition: color 0.3s;
  position: relative;
  z-index: 1;
}

.tab-btn.active {
  color: #6c63ff;
}

.tab-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: white;
  border-radius: 9px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-indicator.right {
  transform: translateX(100%);
}

/* ==================== 表单内容 ==================== */
.form-content {
  flex: 1;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 6px 0;
}

.form-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0 0 16px 0;
}

/* ==================== 角色选择 ==================== */
.role-group {
  width: 100%;
}

.role-group :deep(.el-radio-button__inner) {
  padding: 8px 24px;
  font-size: 14px;
}

/* ==================== 表单行分组 ==================== */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

/* ==================== 提交按钮 ==================== */
.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #6c63ff, #3b82f6);
  border: none;
  letter-spacing: 1px;
  transition: all 0.3s;
  margin-top: 8px;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #5b52e8, #2563eb);
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(108, 99, 255, 0.4);
}

.submit-btn:active {
  transform: translateY(0);
}

/* ==================== 底部切换提示 ==================== */
.switch-tip {
  text-align: center;
  font-size: 14px;
  color: #8c8c8c;
  margin-top: 16px;
}

.link {
  color: #6c63ff;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s;
}

.link:hover {
  color: #3b82f6;
  text-decoration: underline;
}

/* ==================== 过渡动画 ==================== */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.28s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* ==================== Element Plus 样式覆盖 ==================== */
:deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e4e7ed inset;
  transition: all 0.25s;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #6c63ff inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 2px rgba(108, 99, 255, 0.2) inset,
    0 0 0 1px #6c63ff inset;
}

:deep(.el-form-item__label) {
  font-size: 13px;
  color: #555;
  font-weight: 500;
  margin-bottom: 4px;
}

:deep(.el-form-item) {
  margin-bottom: 12px;
}

/* 注册表单 label 紧凑 */
:deep(.el-form-item__label) {
  padding-bottom: 2px !important;
  line-height: 1.4 !important;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #6c63ff, #3b82f6);
  border-color: #6c63ff;
  box-shadow: none;
}
</style>
