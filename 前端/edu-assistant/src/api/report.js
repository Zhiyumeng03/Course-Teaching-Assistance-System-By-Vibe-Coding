import request from '@/utils/request'

/** 幂等获取或创建报告（学生进入提交页时调用）*/
export function getOrCreateReport(experimentId) {
  return request.get('/api/report/getOrCreate', { params: { experimentId } })
}

/** 获取报告草稿内容 */
export function getLatestDraft(reportId) {
  return request.get('/api/report/draft', { params: { reportId } })
}

/**
 * 保存草稿 或 正式提交（含富文本内容）
 * @param {object} data - { reportId, contentHtml, contentText, saveAsDraft }
 */
export function submitWithContent(data) {
  return request.post('/api/report/submitWithContent', data)
}

/**
 * 上传实验报告附件到 OSS
 * @param {File} file
 * @param {number|null} reportId
 */
export function uploadReportAttachment(file, reportId) {
  const formData = new FormData()
  formData.append('file', file)
  if (reportId) formData.append('reportId', reportId)
  return request.post('/api/report/uploadAttachment', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 按 ID 获取报告 */
export function getReportById(id) {
  return request.get('/api/report/getById', { params: { id } })
}

export function getStudentReportDashboard() {
  return request.get('/api/report/student/dashboard')
}

export function getTeacherReportDashboard() {
  return request.get('/api/report/teacher/dashboard')
}

export function scoreReport(data) {
  return request.post('/api/report/score', data)
}

export function getReportVersionDetail(reportId, reportVersionId) {
  return request.get('/api/report/version/detail', {
    params: { reportId, reportVersionId },
  })
}

export function analyzeReportVersion(reportId, reportVersionId) {
  return request.post('/api/report/version/analyze', {
    reportId,
    reportVersionId,
  })
}
