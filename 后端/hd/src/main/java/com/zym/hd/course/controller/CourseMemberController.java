package com.zym.hd.course.controller;

import com.zym.hd.course.entity.Course;
import com.zym.hd.course.entity.CourseMember;
import com.zym.hd.course.service.CourseMemberService;
import com.zym.hd.course.service.CourseService;
import com.zym.hd.security.LoginUser;
import com.zym.hd.security.SecurityContextUtil;
import com.zym.hd.user.entity.UserEntity;
import com.zym.hd.user.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course-members")
public class CourseMemberController {

    private static final String ROLE_PENDING = "STUDENT under review";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ENROLL_MODE_REVIEW = "REVIEW";

    private final CourseMemberService courseMemberService;
    private final CourseService courseService;
    private final UserService userService;

    public CourseMemberController(CourseMemberService courseMemberService,
                                  CourseService courseService,
                                  UserService userService) {
        this.courseMemberService = courseMemberService;
        this.courseService = courseService;
        this.userService = userService;
    }

    @GetMapping
    public List<CourseMember> list() {
        return courseMemberService.list();
    }

    @GetMapping("/User/{id}")
    public CourseMember getById(@PathVariable Long id) {
        return courseMemberService.getById(id);
    }

    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    public String joinByCode(@RequestBody Map<String, String> body) {
        String joinCode = body.get("joinCode");
        if (joinCode == null || joinCode.isBlank()) {
            throw new IllegalArgumentException("joinCode is required");
        }

        LoginUser loginUser = SecurityContextUtil.currentUser();
        UserEntity user = userService.getById(loginUser.getUserId());
        String userNo = user == null ? null : user.getStudentNo();
        if (userNo == null || userNo.isBlank()) {
            throw new IllegalArgumentException("studentNo is required");
        }

        Course course = courseService.lambdaQuery()
                .eq(Course::getJoinCode, joinCode.trim())
                .eq(Course::getDeleted, 0)
                .one();
        if (course == null) {
            throw new IllegalArgumentException("course not found");
        }

        String enrollMode = course.getEnrollMode() == null
                ? ENROLL_MODE_REVIEW
                : course.getEnrollMode().trim().toUpperCase();
        if (!ENROLL_MODE_REVIEW.equals(enrollMode)) {
            throw new IllegalArgumentException("this course uses seckill mode");
        }

        CourseMember existing = courseMemberService.lambdaQuery()
                .eq(CourseMember::getCourseId, course.getId())
                .eq(CourseMember::getUserNo, userNo)
                .one();
        if (existing != null) {
            throw new IllegalArgumentException("already applied or joined");
        }

        CourseMember member = new CourseMember();
        member.setCourseId(course.getId());
        member.setUserNo(userNo);
        member.setRoleInCourse(ROLE_PENDING);
        member.setJoinedAt(LocalDateTime.now());
        courseMemberService.save(member);
        return "applied successfully";
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<CourseMember> getPendingMembers(@RequestParam("courseId") Long courseId) {
        return courseMemberService.lambdaQuery()
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getRoleInCourse, ROLE_PENDING)
                .list();
    }

    @GetMapping("/approved")
    public List<CourseMember> getApprovedMembers(@RequestParam("courseId") Long courseId) {
        return courseMemberService.lambdaQuery()
                .eq(CourseMember::getCourseId, courseId)
                .in(CourseMember::getRoleInCourse, ROLE_STUDENT, ROLE_TEACHER)
                .list();
    }

    @PutMapping("/approve")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public boolean approveMembers(@RequestBody List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return false;
        }
        return courseMemberService.lambdaUpdate()
                .in(CourseMember::getId, memberIds)
                .eq(CourseMember::getRoleInCourse, ROLE_PENDING)
                .set(CourseMember::getRoleInCourse, ROLE_STUDENT)
                .update();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','STUDENT')")
    public boolean create(@RequestBody CourseMember courseMember) {
        return courseMemberService.save(courseMember);
    }

    @PutMapping("/{id}")
    public boolean update(@PathVariable Long id, @RequestBody CourseMember courseMember) {
        courseMember.setId(id);
        return courseMemberService.updateById(courseMember);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public boolean delete(@PathVariable Long id) {
        return courseMemberService.removeById(id);
    }
}

