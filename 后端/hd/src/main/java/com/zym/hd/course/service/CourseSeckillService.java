package com.zym.hd.course.service;

import com.zym.hd.course.dto.CourseEnrollConfigRequest;
import com.zym.hd.course.dto.CourseSeckillEnrollResponse;
import com.zym.hd.course.dto.CourseSeckillListItemResponse;
import com.zym.hd.course.dto.CourseSeckillMyStatusResponse;
import com.zym.hd.course.dto.CourseSeckillStatsResponse;
import com.zym.hd.course.dto.PageResponse;

public interface CourseSeckillService {

    boolean updateEnrollConfig(Long courseId, Long operatorUserId, String operatorRole, CourseEnrollConfigRequest request);

    CourseSeckillStatsResponse preheat(Long courseId, Long operatorUserId, String operatorRole);

    CourseSeckillStatsResponse getStats(Long courseId, Long operatorUserId, String operatorRole);

    CourseSeckillEnrollResponse enroll(Long courseId, Long studentUserId);

    CourseSeckillEnrollResponse withdraw(Long courseId, Long studentUserId);

    CourseSeckillMyStatusResponse getMyStatus(Long courseId, Long studentUserId);

    PageResponse<CourseSeckillListItemResponse> pageSeckillCourses(Long studentUserId, long current, long size);

    void reconcileAllSeckillCourses();
}
