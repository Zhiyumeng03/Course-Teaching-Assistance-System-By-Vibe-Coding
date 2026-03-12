package com.zym.hd.course.controller;

import com.zym.hd.course.dto.PageResponse;
import com.zym.hd.course.entity.Course;
import com.zym.hd.course.service.CourseService;
import com.zym.hd.security.LoginUser;
import com.zym.hd.security.SecurityContextUtil;
import com.zym.hd.user.entity.UserEntity;
import com.zym.hd.user.service.UserService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "我的课程"接口 —— 独立 Controller 避免与 /api/courses/{id} 路由歧义
 * 接口路径：GET /api/my-courses
 */
@RestController
@RequestMapping("/api/my-courses")
public class MyCourseController {

    private final CourseService courseService;
    private final UserService userService;

    public MyCourseController(CourseService courseService, UserService userService) {
        this.courseService = courseService;
        this.userService = userService;
    }

    /**
     * 获取当前登录用户的课程列表
     * - TEACHER / ADMIN：通过 teacherNo 匹配 t_course.teacher_no
     * - STUDENT：通过 t_course_member.user_no 关联查课程
     */
    @GetMapping
    public List<Course> myCourses() {
        return resolveMyCourses();
    }

    @GetMapping("/page")
    public PageResponse<Course> myCoursePage(
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "size", defaultValue = "8") long size
    ) {
        return PageResponse.of(resolveMyCourses(), current, size);
    }

    private List<Course> resolveMyCourses() {
        LoginUser loginUser = SecurityContextUtil.currentUser();
        String role = loginUser.getRole();
        // 获取当前用户详情（教师取 teacherNo，学生取 studentNo）
        UserEntity me = userService.getById(loginUser.getUserId());

        if ("TEACHER".equals(role) || "ADMIN".equals(role)) {
            return courseService.listByTeacherNo(me != null ? me.getTeacherNo() : null);
        } else {
            // 学生：用 studentNo 匹配 t_course_member.user_no
            return courseService.listByStudentUserNo(me != null ? me.getStudentNo() : null);
        }
    }
}
