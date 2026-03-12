import request from '@/utils/request'

export function getQuestionList(courseId) {
  return request.get('/api/question/list', { params: { courseId } })
}

export function getQuestionPage(params) {
  return request.get('/api/question/page', { params })
}

export function createQuestion(data) {
  return request.post('/api/question/create', data)
}

export function updateQuestion(data) {
  return request.put('/api/question/update', data)
}

export function reviewQuestion(id, reviewStatus) {
  return request.put('/api/question/review', { id, reviewStatus })
}

export function deleteQuestion(id) {
  return request.delete(`/api/question/${id}`)
}

export function generateAiQuestions(data) {
  return request.post('/api/question/ai/generate', data)
}
