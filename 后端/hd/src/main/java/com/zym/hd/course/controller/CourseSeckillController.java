package com.zym.hd.course.controller;

import com.zym.hd.course.dto.CourseEnrollConfigRequest;
import com.zym.hd.course.dto.CourseSeckillEnrollResponse;
import com.zym.hd.course.dto.CourseSeckillListItemResponse;
import com.zym.hd.course.dto.CourseSeckillMyStatusResponse;
import com.zym.hd.course.dto.CourseSeckillStatsResponse;
import com.zym.hd.course.dto.PageResponse;
import com.zym.hd.course.service.CourseSeckillService;
import com.zym.hd.security.LoginUser;
import com.zym.hd.security.SecurityContextUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseSeckillController {

    private final CourseSeckillService courseSeckillService;

    public CourseSeckillController(CourseSeckillService courseSeckillService) {
        this.courseSeckillService = courseSeckillService;
    }

    @PutMapping("/{courseId}/enroll-config")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public boolean updateEnrollConfig(@PathVariable Long courseId, @RequestBody CourseEnrollConfigRequest request) {
        LoginUser user = SecurityContextUtil.currentUser();
        return courseSeckillService.updateEnrollConfig(courseId, user.getUserId(), user.getRole(), request);
    }

    @PostMapping("/{courseId}/seckill/preheat")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public CourseSeckillStatsResponse preheat(@PathVariable Long courseId) {
        LoginUser user = SecurityContextUtil.currentUser();
        return courseSeckillService.preheat(courseId, user.getUserId(), user.getRole());
    }

    @GetMapping("/{courseId}/seckill/stats")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public CourseSeckillStatsResponse stats(@PathVariable Long courseId) {
        LoginUser user = SecurityContextUtil.currentUser();
        return courseSeckillService.getStats(courseId, user.getUserId(), user.getRole());
    }

    @PostMapping("/{courseId}/seckill/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public CourseSeckillEnrollResponse enroll(@PathVariable Long courseId) {
        LoginUser user = SecurityContextUtil.currentUser();
        return courseSeckillService.enroll(courseId, user.getUserId());
    }

    @PostMapping("/{courseId}/seckill/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    public CourseSeckillEnrollResponse withdraw(@PathVariable Long courseId) {
        LoginUser user = SecurityContextUtil.currentUser();
        return courseSeckillService.withdraw(courseId, user.getUserId());
    }

    @GetMapping("/seckill/page")
    @PreAuthorize("hasRole('STUDENT')")
    public PageResponse<CourseSeckillListItemResponse> pageSeckillCourses(
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "size", defaultValue = "8") long size) {
        LoginUser user = SecurityContextUtil.currentUser();
        return courseSeckillService.pageSeckillCourses(user.getUserId(), current, size);
    }

    @GetMapping("/{courseId}/seckill/me")
    @PreAuthorize("hasRole('STUDENT')")
    public CourseSeckillMyStatusResponse myStatus(@PathVariable Long courseId) {
        LoginUser user = SecurityContextUtil.currentUser();
        return courseSeckillService.getMyStatus(courseId, user.getUserId());
    }
}
