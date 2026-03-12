import request from '@/utils/request'

/**
 * 创建实验（教师）
 * POST /api/experiment/create
 * @param {object} data - ExperimentEntity 字段
 */
export function createExperiment(data) {
  return request.post('/api/experiment/create', data)
}

/**
 * 更新实验（教师）
 * PUT /api/experiment/update
 * @param {object} data - ExperimentEntity 字段（必须含 id）
 */
export function updateExperiment(data) {
  return request.put('/api/experiment/update', data)
}

/**
 * 删除实验（软删除）
 * DELETE /api/experiment/{id}
 * @param {number} id - 实验 ID
 */
export function deleteExperiment(id) {
  return request.delete(`/api/experiment/${id}`)
}

/**
 * 根据 ID 获取实验详情
 * GET /api/experiment/getById?id=
 * @param {number} id - 实验 ID
 */
export function getExperimentById(id) {
  return request.get('/api/experiment/getById', { params: { id } })
}

export function uploadExperimentAttachment(file, bizId) {
  const formData = new FormData()
  formData.append('file', file)
  if (bizId) formData.append('experimentId', bizId)
  return request.post('/api/experiment/uploadAttachment', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getFileAssetsByIds(ids) {
  return request.get('/api/file/listByIds', { params: { ids } })
}

/**
 * 获取当前用户的实验列表（按角色过滤）
 * - TEACHER：只返回自己创建的实验
 * - STUDENT：返回所加入课程的实验
 * GET /api/experiment/list
 */
export function getMyExperimentList() {
  return request.get('/api/experiment/list')
}

/**
 * 按 courseId 获取实验列表
 * GET /api/experiment/listByCourse?courseId=
 * @param {number} courseId - 课程 ID
 */
export function getExperimentsByCourse(courseId) {
  return request.get('/api/experiment/listByCourse', { params: { courseId } })
}

/**
 * 学生提交实验
 * POST /api/experiment/submit?id=
 * @param {number} id - 实验 ID
 */
export function submitExperiment(id) {
  return request.post('/api/experiment/submit', null, { params: { id } })
}
