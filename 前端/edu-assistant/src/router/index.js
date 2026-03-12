import { createRouter, createWebHistory } from 'vue-router'
import AuthView from '@/views/AuthView.vue'
import HomeView from '@/views/HomeView.vue'
import { getToken, getUserInfo } from '@/utils/auth'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: AuthView,
    meta: { title: '登录 - 知予萌教学辅助系统', requiresAuth: false }
  },
  {
    path: '/home',
    name: 'Home',
    component: HomeView,
    meta: { title: '知予萌 - 课程教学辅助系统', requiresAuth: true }
  },
  {
    path: '/course/:id',
    name: 'CourseDetail',
    component: () => import('@/views/CourseDetailView.vue'),
    meta: { title: '课程详情 - 知予萌', requiresAuth: true }
  },
  {
    path: '/experiment/publish',
    name: 'ExperimentPublish',
    component: () => import('@/views/PublishExperimentView.vue'),
    meta: { title: '发布实验 - 知予萌', requiresAuth: true }
  },
  {
    path: '/experiment/edit/:id',
    name: 'ExperimentEdit',
    component: () => import('@/views/PublishExperimentView.vue'),
    meta: { title: '编辑实验 - 知予萌', requiresAuth: true }
  },
  {
    path: '/experiment/detail/:id',
    name: 'ExperimentDetail',
    component: () => import('@/views/ExperimentDetailView.vue'),
    meta: { title: '实验详情 - 知予萌', requiresAuth: true }
  },
  {
    path: '/experiment/:experimentId/submit',
    name: 'SubmitReport',
    component: () => import('@/views/SubmitReportView.vue'),
    meta: { title: '提交实验报告 - 知予萌', requiresAuth: true }
  },
  {
    path: '/report/:reportId/version/:reportVersionId',
    name: 'ReportReviewDetail',
    component: () => import('@/views/ReportReviewDetailView.vue'),
    meta: { title: '报告批改详情', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 全局路由守卫：处理页面标题 & 登录拦截
function resolveHomeTitle() {
  const userInfo = getUserInfo()
  const realName = userInfo?.realName?.trim()
  const username = userInfo?.username?.trim()

  if (realName) {
    return `${realName} - 课程教学辅助系统`
  }
  if (username) {
    return `${username} - 课程教学辅助系统`
  }
  return '课程教学辅助系统'
}

router.beforeEach((to, from, next) => {
  if (to.name === 'Home') {
    document.title = resolveHomeTitle()
  } else if (to.meta?.title) {
    document.title = to.meta.title
  }
  const token = getToken()
  if (to.meta?.requiresAuth && !token) {
    // 需要认证但未登录，跳转到登录页
    next({ name: 'Login' })
  } else if (to.name === 'Login' && token) {
    // 已登录时访问登录页，直接跳到主页
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
