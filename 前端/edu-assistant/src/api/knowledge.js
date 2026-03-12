import request from '@/utils/request'

export function getKnowledgeList(courseId) {
  return request.get('/api/knowledge/list', { params: { courseId } })
}

export function getKnowledgeTree(courseId) {
  return request.get('/api/knowledge/tree', { params: { courseId } })
}

export function createKnowledgePoint(data) {
  return request.post('/api/knowledge/create', data)
}
