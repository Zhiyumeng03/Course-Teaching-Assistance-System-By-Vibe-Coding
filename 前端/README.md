# 基于Web的课程教学辅助系统

一个现代化的Vue 3 + Element Plus登录注册系统，具有完整的用户认证功能。

## 功能特性

### 🔐 登录模块
- ✅ 用户名/邮箱输入框
- ✅ 密码输入框（可显示/隐藏密码）
- ✅ 图形验证码输入（包含验证码图片和刷新按钮）
- ✅ 忘记密码链接

### 📝 注册模块
- ✅ 用户名、邮箱、密码、确认密码
- ✅ 安全问题及答案选择
- ✅ 密码强度提示
- ✅ 图形验证码

### 🔄 忘记密码模块
- ✅ 输入邮箱
- ✅ 确认邮箱回答安全问题
- ✅ 重置密码表单

### 🎨 界面设计
- ✅ 简洁干净的现代设计风格
- ✅ 柔和的配色方案（主色：#3498db，辅助色：#2ecc71）
- ✅ 适当的动画过渡效果
- ✅ 表单分组清晰，间距合理
- ✅ 错误提示明显但不突兀

### 📱 技术规格
- ✅ 使用Vue 3 Composition API
- ✅ 响应式设计，支持移动端
- ✅ 使用现代CSS（Flexbox/Grid）
- ✅ 使用Element Plus UI组件库
- ✅ 表单验证（前端基础验证）
- ✅ 状态管理（Pinia）

## 技术栈

- **前端框架**: Vue 3.5.22
- **构建工具**: Vite 7.1.7
- **UI组件库**: Element Plus 2.8.8
- **状态管理**: Pinia 2.2.6
- **路由管理**: Vue Router 4.5.1
- **图标库**: @element-plus/icons-vue 2.3.1

## 项目结构

```
前端/
├── public/
│   └── favicon.ico
├── src/
│   ├── assets/
│   │   ├── base.css
│   │   ├── logo.svg
│   │   └── main.css
│   ├── components/
│   │   ├── HelloWorld.vue
│   │   ├── TheWelcome.vue
│   │   └── icons/
│   ├── stores/
│   │   └── auth.js          # 用户认证状态管理
│   ├── views/
│   │   ├── HomeView.vue     # 首页（需要登录）
│   │   ├── LoginView.vue    # 登录页面
│   │   ├── RegisterView.vue # 注册页面
│   │   └── ForgotPasswordView.vue # 忘记密码页面
│   ├── router/
│   │   └── index.js         # 路由配置
│   ├── App.vue
│   └── main.js
├── package.json
├── vite.config.js
└── README.md
```

## 快速开始

### 安装依赖

```bash
cd 前端
npm install
```

### 开发模式

```bash
npm run dev
```

或者双击 `start.bat` 文件

访问 http://localhost:5173 查看应用

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 使用说明

### 登录功能
1. 访问 `/login` 页面
2. 输入用户名（admin）和密码（123456）
3. 输入图形验证码
4. 点击"登录"按钮

### 注册功能
1. 访问 `/register` 页面
2. 填写用户名、邮箱、密码等信息
3. 选择安全问题并填写答案
4. 查看密码强度提示
5. 输入图形验证码
6. 点击"立即注册"

### 忘记密码功能
1. 访问 `/forgot-password` 页面
2. 输入注册时使用的邮箱
3. 回答安全问题
4. 设置新密码
5. 完成密码重置

## 路由说明

- `/` - 首页（需要登录）
- `/login` - 登录页面
- `/register` - 注册页面
- `/forgot-password` - 忘记密码页面

## 状态管理

使用Pinia进行状态管理，主要包含：

- `user` - 用户信息
- `token` - 认证令牌
- `isLoggedIn` - 登录状态

## 表单验证

所有表单都包含完整的前端验证：

- **用户名**: 3-20个字符，只能包含字母、数字和下划线
- **邮箱**: 标准邮箱格式验证
- **密码**: 8-20个字符，包含强度检查
- **确认密码**: 与密码一致性验证
- **安全问题**: 必选验证
- **验证码**: 长度验证

## 响应式设计

- 支持桌面端和移动端
- 使用Flexbox和Grid布局
- 移动端优化的表单布局
- 响应式字体和间距

## 浏览器支持

- Chrome (推荐)
- Firefox
- Safari
- Edge

## 开发说明

### 模拟数据
当前使用模拟API，实际项目中需要替换为真实的后端接口：

- 登录API: `authStore.login()`
- 注册API: `authStore.register()`
- 获取安全问题API: `authStore.getSecurityQuestion()`
- 验证安全问题API: `authStore.verifySecurityAnswer()`
- 重置密码API: `authStore.resetPassword()`

### 自定义样式
所有组件都使用scoped样式，可以通过CSS变量或深度选择器进行自定义。

### 图标使用
使用Element Plus图标库，可以通过以下方式使用：

```vue
<el-icon><User /></el-icon>
```

## 许可证

MIT License