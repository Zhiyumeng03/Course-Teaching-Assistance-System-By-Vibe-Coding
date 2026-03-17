<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand-panel">
        <h1>课程教学辅助系统</h1>
        <p>支持账号密码与短信验证码登录</p>
      </div>

      <div class="form-panel">
        <div class="tab-bar">
          <button class="tab-btn" :class="{ active: activeTab === 'login' }" @click="switchTab('login')">登录</button>
          <button class="tab-btn" :class="{ active: activeTab === 'register' }" @click="switchTab('register')">注册</button>
        </div>

        <div v-if="activeTab === 'login'" class="form-content">
          <h2>欢迎回来</h2>
          <el-radio-group v-model="loginType" class="login-type">
            <el-radio-button label="password">账号密码</el-radio-button>
            <el-radio-button label="sms">手机号验证码</el-radio-button>
          </el-radio-group>

          <el-form
            v-if="loginType === 'password'"
            ref="passwordLoginFormRef"
            :model="passwordLoginForm"
            :rules="passwordLoginRules"
            @keyup.enter="handlePasswordLogin"
          >
            <el-form-item prop="username">
              <el-input v-model="passwordLoginForm.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="passwordLoginForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loginLoading" @click="handlePasswordLogin">登录</el-button>
          </el-form>

          <el-form
            v-else
            ref="smsLoginFormRef"
            :model="smsLoginForm"
            :rules="smsLoginRules"
            @keyup.enter="handleSmsLogin"
          >
            <el-form-item prop="phone">
              <el-input v-model="smsLoginForm.phone" placeholder="请输入手机号" :prefix-icon="Phone" clearable />
            </el-form-item>
            <el-form-item prop="code">
              <div class="sms-row">
                <el-input v-model="smsLoginForm.code" maxlength="6" placeholder="请输入验证码" :prefix-icon="Message" clearable />
                <el-button :disabled="smsLoginCountdown > 0" @click="sendLoginCode">
                  {{ smsLoginCountdown > 0 ? `${smsLoginCountdown}s后重发` : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loginLoading" @click="handleSmsLogin">登录</el-button>
          </el-form>
        </div>

        <div v-else class="form-content">
          <h2>创建账号</h2>
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top">
            <el-form-item prop="role" label="身份">
              <el-radio-group v-model="registerForm.role">
                <el-radio-button label="STUDENT">学生</el-radio-button>
                <el-radio-button label="TEACHER">教师</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <div class="form-row">
              <el-form-item prop="username" label="用户名">
                <el-input v-model="registerForm.username" clearable />
              </el-form-item>
              <el-form-item prop="realName" label="真实姓名">
                <el-input v-model="registerForm.realName" clearable />
              </el-form-item>
            </div>

            <el-form-item v-if="registerForm.role === 'STUDENT'" prop="studentNo" label="学号">
              <el-input v-model="registerForm.studentNo" clearable />
            </el-form-item>
            <el-form-item v-if="registerForm.role === 'TEACHER'" prop="teacherNo" label="工号">
              <el-input v-model="registerForm.teacherNo" clearable />
            </el-form-item>

            <div class="form-row">
              <el-form-item prop="email" label="邮箱">
                <el-input v-model="registerForm.email" clearable />
              </el-form-item>
              <el-form-item prop="phone" label="手机号">
                <el-input v-model="registerForm.phone" clearable />
              </el-form-item>
            </div>

            <el-form-item prop="smsCode" label="手机验证码">
              <div class="sms-row">
                <el-input v-model="registerForm.smsCode" maxlength="6" placeholder="请输入验证码" clearable />
                <el-button :disabled="registerCountdown > 0" @click="sendRegisterCode">
                  {{ registerCountdown > 0 ? `${registerCountdown}s后重发` : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>

            <el-form-item prop="password" label="密码">
              <el-input v-model="registerForm.password" type="password" show-password clearable />
            </el-form-item>
            <el-form-item prop="confirmPassword" label="确认密码">
              <el-input v-model="registerForm.confirmPassword" type="password" show-password clearable />
            </el-form-item>

            <el-button type="primary" class="submit-btn" :loading="registerLoading" @click="handleRegister">注册</el-button>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Lock, Message, Phone, User } from "@element-plus/icons-vue";
import { login, loginBySms, register, sendSmsCode } from "@/api/user";
import { setAuthSession } from "@/utils/auth";

const PHONE_REGEX = /^1[3-9]\d{9}$/;

const router = useRouter();
const activeTab = ref("login");
const loginType = ref("password");

const loginLoading = ref(false);
const registerLoading = ref(false);

const passwordLoginFormRef = ref();
const smsLoginFormRef = ref();
const registerFormRef = ref();

const passwordLoginForm = reactive({ username: "", password: "" });
const smsLoginForm = reactive({ phone: "", code: "" });
const registerForm = reactive({
  username: "",
  password: "",
  confirmPassword: "",
  role: "STUDENT",
  realName: "",
  studentNo: "",
  teacherNo: "",
  email: "",
  phone: "",
  smsCode: "",
});

const passwordLoginRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 5, message: "密码不能少于6位", trigger: "blur" },
  ],
};

const smsLoginRules = {
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: PHONE_REGEX, message: "手机号格式不正确", trigger: "blur" },
  ],
  code: [
    { required: true, message: "请输入验证码", trigger: "blur" },
    { pattern: /^\d{6}$/, message: "验证码必须为6位数字", trigger: "blur" },
  ],
};

const validateConfirmPwd = (_, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

const registerRules = {
  role: [{ required: true, message: "请选择身份", trigger: "change" }],
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  realName: [{ required: true, message: "请输入真实姓名", trigger: "blur" }],
  studentNo: [
    {
      validator: (_, value, callback) => {
        if (registerForm.role !== "STUDENT") return callback();
        if (!value || !value.trim()) return callback(new Error("请输入学号"));
        callback();
      },
      trigger: "blur",
    },
  ],
  teacherNo: [
    {
      validator: (_, value, callback) => {
        if (registerForm.role !== "TEACHER") return callback();
        if (!value || !value.trim()) return callback(new Error("请输入工号"));
        callback();
      },
      trigger: "blur",
    },
  ],
  email: [{ type: "email", message: "邮箱格式不正确", trigger: "blur" }],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: PHONE_REGEX, message: "手机号格式不正确", trigger: "blur" },
  ],
  smsCode: [
    { required: true, message: "请输入验证码", trigger: "blur" },
    { pattern: /^\d{6}$/, message: "验证码必须为6位数字", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 5, message: "密码不能少于6位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认密码", trigger: "blur" },
    { validator: validateConfirmPwd, trigger: "blur" },
  ],
};

let smsLoginTimer = null;
let registerTimer = null;
const smsLoginCountdown = ref(0);
const registerCountdown = ref(0);

function switchTab(tab) {
  activeTab.value = tab;
}

function startTimer(type) {
  if (type === "login") {
    smsLoginCountdown.value = 60;
    clearInterval(smsLoginTimer);
    smsLoginTimer = setInterval(() => {
      smsLoginCountdown.value -= 1;
      if (smsLoginCountdown.value <= 0) {
        clearInterval(smsLoginTimer);
      }
    }, 1000);
    return;
  }
  registerCountdown.value = 60;
  clearInterval(registerTimer);
  registerTimer = setInterval(() => {
    registerCountdown.value -= 1;
    if (registerCountdown.value <= 0) {
      clearInterval(registerTimer);
    }
  }, 1000);
}

async function sendLoginCode() {
  const valid = await smsLoginFormRef.value?.validateField("phone").catch(() => false);
  if (valid === false) return;
  await sendSmsCode({ phone: smsLoginForm.phone.trim(), scene: "LOGIN" });
  ElMessage.success("验证码已发送，请注意查收");
  startTimer("login");
}

async function sendRegisterCode() {
  const valid = await registerFormRef.value?.validateField("phone").catch(() => false);
  if (valid === false) return;
  await sendSmsCode({ phone: registerForm.phone.trim(), scene: "REGISTER" });
  ElMessage.success("验证码已发送，请注意查收");
  startTimer("register");
}

async function handlePasswordLogin() {
  const valid = await passwordLoginFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  loginLoading.value = true;
  try {
    const res = await login({
      username: passwordLoginForm.username.trim(),
      password: passwordLoginForm.password,
    });
    setAuthSession(res.token, res.user);
    ElMessage.success(`欢迎回来，${res.user?.realName || res.user?.username}`);
    router.push("/home");
  } finally {
    loginLoading.value = false;
  }
}

async function handleSmsLogin() {
  const valid = await smsLoginFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  loginLoading.value = true;
  try {
    const res = await loginBySms({
      phone: smsLoginForm.phone.trim(),
      code: smsLoginForm.code.trim(),
    });
    setAuthSession(res.token, res.user);
    ElMessage.success(`欢迎回来，${res.user?.realName || res.user?.username}`);
    router.push("/home");
  } finally {
    loginLoading.value = false;
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  registerLoading.value = true;
  try {
    await register({
      username: registerForm.username.trim(),
      password: registerForm.password,
      smsCode: registerForm.smsCode.trim(),
      role: registerForm.role,
      realName: registerForm.realName.trim(),
      studentNo: registerForm.role === "STUDENT" ? registerForm.studentNo.trim() : null,
      teacherNo: registerForm.role === "TEACHER" ? registerForm.teacherNo.trim() : null,
      email: registerForm.email.trim() || null,
      phone: registerForm.phone.trim(),
    });
    ElMessage.success("注册成功，请登录");
    activeTab.value = "login";
    loginType.value = "password";
    passwordLoginForm.username = registerForm.username.trim();
  } finally {
    registerLoading.value = false;
  }
}

onBeforeUnmount(() => {
  clearInterval(smsLoginTimer);
  clearInterval(registerTimer);
});
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  background: linear-gradient(130deg, #11203f 0%, #244a73 60%, #2e6b87 100%);
}

.auth-card {
  width: min(980px, 100%);
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  display: grid;
  grid-template-columns: 320px 1fr;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.brand-panel {
  color: #fff;
  padding: 40px 28px;
  background: linear-gradient(160deg, #0b3c5d, #116466);
}

.brand-panel h1 {
  font-size: 26px;
  margin-bottom: 12px;
}

.form-panel {
  padding: 28px;
}

.tab-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
}

.tab-btn {
  border: 1px solid #d9e1ea;
  background: #f7fafc;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
}

.tab-btn.active {
  color: #0b3c5d;
  border-color: #0b3c5d;
  background: #eef5fb;
}

.login-type {
  margin-bottom: 16px;
}

.form-content h2 {
  margin-bottom: 14px;
  color: #1f2d3d;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.sms-row {
  display: grid;
  grid-template-columns: 1fr 130px;
  gap: 10px;
  width: 100%;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

@media (max-width: 860px) {
  .auth-card {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    padding: 20px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
