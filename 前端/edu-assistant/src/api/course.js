import request from "@/utils/request";

export function getCourseList() {
  return request.get("/api/courses");
}

export function getMyCourseList() {
  return request.get("/api/my-courses");
}

export function getMyCoursePage(params) {
  return request.get("/api/my-courses/page", { params });
}

export function getCourseById(id) {
  return request.get(`/api/courses/${id}`);
}

export function getCourseByJoinCode(joinCode) {
  return request.get("/api/courses/by-join-code", { params: { joinCode } });
}

export function createCourse(course) {
  return request.post("/api/courses", course);
}

export function updateCourse(id, course) {
  return request.put(`/api/courses/${id}`, course);
}

export function deleteCourse(id) {
  return request.delete(`/api/courses/${id}`);
}

export function updateCourseEnrollConfig(courseId, payload) {
  return request.put(`/api/courses/${courseId}/enroll-config`, payload);
}

export function preheatCourseSeckill(courseId) {
  return request.post(`/api/courses/${courseId}/seckill/preheat`);
}

export function getCourseSeckillStats(courseId) {
  return request.get(`/api/courses/${courseId}/seckill/stats`);
}

export function seckillEnroll(courseId) {
  return request.post(`/api/courses/${courseId}/seckill/enroll`);
}

export function seckillWithdraw(courseId) {
  return request.post(`/api/courses/${courseId}/seckill/withdraw`);
}

export function getSeckillCoursePage(params) {
  return request.get("/api/courses/seckill/page", { params });
}

export function getMySeckillStatus(courseId) {
  return request.get(`/api/courses/${courseId}/seckill/me`);
}

export function getMemberList() {
  return request.get("/api/course-members");
}

export function getMemberById(id) {
  return request.get(`/api/course-members/${id}`);
}

export function getApprovedMembers(courseId) {
  return request.get("/api/course-members/approved", { params: { courseId } });
}

export function joinCourse(member) {
  return request.post("/api/course-members", member);
}

export function updateMember(id, member) {
  return request.put(`/api/course-members/${id}`, member);
}

export function removeMember(id) {
  return request.delete(`/api/course-members/${id}`);
}

export function joinCourseByCode(joinCode) {
  return request.post("/api/course-members/join", { joinCode });
}

export function getPendingMembers(courseId) {
  return request.get("/api/course-members/pending", { params: { courseId } });
}

export function approveMembers(memberIds) {
  return request.put("/api/course-members/approve", memberIds);
}
