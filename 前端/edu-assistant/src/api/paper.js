import request from '@/utils/request'

export function getPaperList(courseId) {
  return request.get('/api/paper/list', { params: { courseId } })
}

export function getPaperPage(params) {
  return request.get('/api/paper/page', { params })
}

export function getPaperDetail(id) {
  return request.get('/api/paper/detail', { params: { id } })
}

export function createPaper(data) {
  return request.post('/api/paper/create', data)
}

export function updatePaper(data) {
  return request.put('/api/paper/update', data)
}

export function deletePaper(id) {
  return request.delete(`/api/paper/${id}`)
}

export function publishPaper(id) {
  return request.post(`/api/paper/${id}/publish`)
}

export function closePaper(id) {
  return request.post(`/api/paper/${id}/close`)
}
