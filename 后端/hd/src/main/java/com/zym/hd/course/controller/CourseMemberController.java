package com.zym.hd.course.controller;

import com.zym.hd.course.entity.Course;
import com.zym.hd.course.entity.CourseMember;
import com.zym.hd.course.service.CourseMemberService;
import com.zym.hd.course.service.CourseService;
import com.zym.hd.security.SecurityContextUtil;

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

    private static final String ROLE_PENDING  = "STUDENT under review";
    private static final String ROLE_STUDENT  = "STUDENT";
    private static final String ROLE_TEACHER  = "TEACHER";

    private final CourseMemberService courseMemberService;
    private final CourseService courseService;

    public CourseMemberController(CourseMemberService courseMemberService,
                                  CourseService courseService) {
        this.courseMemberService = courseMemberService;
        this.courseService = courseService;
    }

    /** 全量成员列表 */
    @GetMapping
    public List<CourseMember> list() {
        return courseMemberService.list();
    }

    @GetMapping("/User/{id}")
    public CourseMember getById(@PathVariable Long id) {
        return courseMemberService.getById(id);
    }

    /**
     * 学生申请加入课程（通过加入码）
     * POST /api/course-members/join
     * body: { joinCode, userNo }
     * 1. 按 joinCode 查课程
     * 2. 创建成员记录，roleInCourse = "STUDENT under review"
     */
    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    public String joinByCode(@RequestBody Map<String, String> body) {
        String joinCode = body.get("joinCode");
        String userNo   = body.get("userNo");

        if (joinCode == null || joinCode.isBlank()) {
            throw new IllegalArgumentException("加入码不能为空");
        }
        if (userNo == null || userNo.isBlank()) {
            throw new IllegalArgumentException("学号不能为空");
        }

        // 1. 按加入码查课程
        Course course = courseService.lambdaQuery()
                .eq(Course::getJoinCode, joinCode)
                .eq(Course::getDeleted, 0)
                .one();
        if (course == null) {
            throw new IllegalArgumentException("加入码无效，课程不存在");
        }

        // 2. 检查是否已经申请/加入过
        CourseMember existing = courseMemberService.lambdaQuery()
                .eq(CourseMember::getCourseId, course.getId())
                .eq(CourseMember::getUserNo, userNo)
                .one();
        if (existing != null) {
            throw new IllegalArgumentException("您已申请或加入该课程，请勿重复操作");
        }

        // 3. 创建待审核成员记录
        CourseMember member = new CourseMember();
        member.setCourseId(course.getId());
        member.setUserNo(userNo);
        member.setRoleInCourse(ROLE_PENDING);
        member.setJoinedAt(LocalDateTime.now());
        courseMemberService.save(member);

        return "申请已提交，等待教师审批";
    }

    /**
     * 查询指定课程的待审核学生列表
     * GET /api/course-members/pending?courseId=
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<CourseMember> getPendingMembers(@RequestParam("courseId") Long courseId) {
        return courseMemberService.lambdaQuery()
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getRoleInCourse, ROLE_PENDING)
                .list();
    }

    /**
     * 查询指定课程的正式成员列表（审核通过的 STUDENT + TEACHER，不含待审核）
     * GET /api/course-members/approved?courseId=
     */
    @GetMapping("/approved")
    public List<CourseMember> getApprovedMembers(@RequestParam("courseId") Long courseId) {
        return courseMemberService.lambdaQuery()
                .eq(CourseMember::getCourseId, courseId)
                .in(CourseMember::getRoleInCourse, ROLE_STUDENT, ROLE_TEACHER)
                .list();
    }

    /**
     * 教师批量审核通过（将 roleInCourse 从 "STUDENT under review" 改为 "STUDENT"）
     * PUT /api/course-members/approve
     * body: [memberId1, memberId2, ...]
     */
    @PutMapping("/approve")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public boolean approveMembers(@RequestBody List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return false;
        return courseMemberService.lambdaUpdate()
                .in(CourseMember::getId, memberIds)
                .eq(CourseMember::getRoleInCourse, ROLE_PENDING)
                .set(CourseMember::getRoleInCourse, ROLE_STUDENT)
                .update();
    }

    /** 通用创建（保留，供内部或管理使用） */
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
