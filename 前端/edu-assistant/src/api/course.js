import request from '@/utils/request'

// ==================== 课程接口 ====================

/** 获取课程列表 GET /api/courses */
export function getCourseList() {
  return request.get('/api/courses')
}

/** 获取当前登录用户的课程列表 GET /api/my-courses
 *  独立路径，避免与 /api/courses/{id} 产生路由歧义
 *  - 教师/管理员：返回 teacherNo 匹配的课程
 *  - 学生：返回已加入的课程
 */
export function getMyCourseList() {
  return request.get('/api/my-courses')
}

/** 获取当前登录用户的课程分页列表 GET /api/my-courses/page */
export function getMyCoursePage(params) {
  return request.get('/api/my-courses/page', { params })
}

/** 根据 ID 获取课程详情 GET /api/courses/{id} */
export function getCourseById(id) {
  return request.get(`/api/courses/${id}`)
}

/** 创建课程（仅教师/管理员）POST /api/courses
 * @param {object} course - { courseCode, courseName, term, description, joinCode, status }
 */
export function createCourse(course) {
  return request.post('/api/courses', course)
}

/** 更新课程 PUT /api/courses/{id} */
export function updateCourse(id, course) {
  return request.put(`/api/courses/${id}`, course)
}

/** 删除课程 DELETE /api/courses/{id} */
export function deleteCourse(id) {
  return request.delete(`/api/courses/${id}`)
}

// ==================== 课程成员接口 ====================

/** 获取所有成员 GET /api/course-members */
export function getMemberList() {
  return request.get('/api/course-members')
}

/** 根据 ID 获取成员 GET /api/course-members/{id} */
export function getMemberById(id) {
  return request.get(`/api/course-members/${id}`)
}

/**
 * 获取指定课程的正式成员列表（已审核通过的 STUDENT + TEACHER，不含 "STUDENT under review"）
 * GET /api/course-members/approved?courseId=
 * @param {number} courseId - 课程 ID
 */
export function getApprovedMembers(courseId) {
  return request.get('/api/course-members/approved', { params: { courseId } })
}

/**
 * 学生申请加入课程 POST /api/course-members
 * 后端通过 joinCode 匹配 courseId，roleInCourse 传 'student'
 * @param {object} member - { courseId, userId, roleInCourse }
 */
export function joinCourse(member) {
  return request.post('/api/course-members', member)
}

/**
 * 教师审批/更新成员信息 PUT /api/course-members/{id}
 * @param {number} id - CourseMember 记录 ID
 * @param {object} member - 更新字段（如 roleInCourse: 'approved'）
 */
export function updateMember(id, member) {
  return request.put(`/api/course-members/${id}`, member)
}

/** 移除成员 DELETE /api/course-members/{id} */
export function removeMember(id) {
  return request.delete(`/api/course-members/${id}`)
}

/**
 * 学生通过加入码申请加入课程
 * POST /api/course-members/join
 * @param {string} joinCode - 课程加入码
 * @param {string} userNo   - 学生学号
 */
export function joinCourseByCode(joinCode, userNo) {
  return request.post('/api/course-members/join', { joinCode, userNo })
}

/**
 * 查询指定课程待审核的学生列表
 * GET /api/course-members/pending?courseId=
 * @param {number} courseId - 课程 ID
 */
export function getPendingMembers(courseId) {
  return request.get('/api/course-members/pending', { params: { courseId } })
}

/**
 * 教师批量审核通过
 * PUT /api/course-members/approve
 * @param {number[]} memberIds - 审核通过的成员 ID 列表
 */
export function approveMembers(memberIds) {
  return request.put('/api/course-members/approve', memberIds)
}
