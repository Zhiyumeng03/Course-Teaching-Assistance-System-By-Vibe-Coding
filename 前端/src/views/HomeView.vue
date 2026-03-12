<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// 用户信息
const user = computed(() => authStore.user)
const isLoggedIn = computed(() => authStore.isLoggedIn)

// 登出功能
const handleLogout = () => {
  authStore.logout()
  ElMessage.success('已成功登出')
  router.push('/login')
}

const handleClose = () => {
  console.log("关闭了")
}

const handleOpen = () => {
  console.log("选择了")
}
</script>

<template>
  <div class="home-container">
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="logo">课程教学辅助系统</h1>
          <div class="user-info" v-if="isLoggedIn">
            <el-dropdown>
              <div class="user-dropdown">
                <el-icon>
                  <User />
                </el-icon>
                <span>{{ user?.username || '用户' }}</span>
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleLogout">
                    <el-icon>
                      <Logout />
                    </el-icon>
                    登出
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <el-container>
        <el-aside width="200px"><el-menu default-active="2" class="el-menu-vertical-demo" @open="handleOpen"
            @close="handleClose">
            <el-sub-menu index="1">
              <template #title>
                <el-icon>
                  <Document />
                </el-icon>
                <span>实验模块</span>
              </template>
              <el-sub-menu index="1-1">
                <template #title>教师端</template>
                <el-menu-item index="1-1-1">发布实验</el-menu-item>
                <el-menu-item index="1-1-2">查看实验</el-menu-item>
              </el-sub-menu>

              <el-sub-menu index="1-2">
                <template #title>学生端</template>
                <el-menu-item index="1-2-1">未提交实验</el-menu-item>
                <el-menu-item index="1-2-2">已提交实验</el-menu-item>
              </el-sub-menu>

            </el-sub-menu>

            <el-sub-menu index="2">
              <template #title>
                <el-icon>
                  <EditPen />
                </el-icon>
                <span>测试练习模块</span>
              </template>
            </el-sub-menu>
          </el-menu></el-aside>
        <el-main>Main</el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.home-container {
  min-height: 100vh;
  background: #f8f9fa;
}

.header {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 20;
}

.header-content {
  padding: 0 20px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  background: linear-gradient(135deg, #3498db, #2ecc71);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #606266;
}

.user-dropdown:hover {
  background: #f5f7fa;
  color: #3498db;
}

.main-content {
  padding: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .logo {
    font-size: 20px;
  }

  .main-content {
    padding: 16px;
  }
}

/* Element Plus 样式覆盖 */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
