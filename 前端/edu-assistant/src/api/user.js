import request from '@/utils/request'

/**
 * 用户登录
 * POST /api/user/login
 * @param {string} username - 用户名
 * @param {string} password - 密码
 */
export function login(data) {
  return request.post('/api/user/login', data)
}

export function loginBySms(data) {
  return request.post('/api/user/login/sms', data)
}

export function sendSmsCode(data) {
  return request.post('/api/user/sms/send-code', data)
}

export function logout() {
  return request.post('/api/user/logout')
}

/**
 * 用户注册
 * POST /api/user/register
 * @param {object} data - 注册信息
 */
export function register(data) {
  return request.post('/api/user/register', data)
}

/**
 * 获取当前登录用户信息
 * GET /api/user/profile
 */
export function getProfile() {
  return request.get('/api/user/profile')
}

export function getHomeDashboard() {
  return request.get('/api/user/dashboard')
}

export function markDashboardNotificationRead(notificationId) {
  return request.post('/api/user/dashboard/notifications/read', { notificationId })
}

/**
 * 更新用户信息
 * PUT /api/user/update
 * @param {object} data - 更新信息
 */
export function updateUser(data) {
  return request.put('/api/user/update', data)
}

/**
 * 按 ID 查询用户信息（供成员列表等场景使用）
 * GET /api/user/getById?id=
 * @param {number} id - 用户 ID
 */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/file/uploadAvatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getUserById(id) {
  return request.get('/api/user/getById', { params: { id } })
}

/**
 * 按教师工号查询教师信息
 * GET /api/user/getByTeacherNo?teacherNo=
 * @param {string} teacherNo - 教师工号
 */
export function getUserByTeacherNo(teacherNo) {
  return request.get('/api/user/getByTeacherNo', { params: { teacherNo } })
}

/**
 * 按学生学号查询学生信息
 * GET /api/user/getByStudentNo?studentNo=
 * @param {string} studentNo - 学生学号
 */
export function getUserByStudentNo(studentNo) {
  return request.get('/api/user/getByStudentNo', { params: { studentNo } })
}
