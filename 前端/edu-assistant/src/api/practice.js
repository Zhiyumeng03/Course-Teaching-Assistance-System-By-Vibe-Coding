import request from '@/utils/request'

export function submitPractice(data) {
  return request.post('/api/practice/submit', data)
}

export function getStudentPracticeList(courseId) {
  return request.get('/api/practice/student/list', { params: { courseId } })
}

export function getTeacherPracticeList(courseId) {
  return request.get('/api/practice/teacher/list', { params: { courseId } })
}

export function getTeacherPracticePage(params) {
  return request.get('/api/practice/teacher/page', { params })
}

export function getPracticeDetail(recordId) {
  return request.get('/api/practice/detail', { params: { recordId } })
}

export function reviewPractice(data) {
  return request.post('/api/practice/review', data)
}

export function generatePracticeDiagnosis(recordId) {
  return request.post(`/api/practice/${recordId}/diagnosis`)
}
