package com.zym.hd.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zym.hd.course.entity.Course;
import com.zym.hd.course.entity.CourseMember;
import com.zym.hd.course.mapper.CourseMapper;
import com.zym.hd.course.service.CourseMemberService;
import com.zym.hd.course.service.CourseService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMemberService courseMemberService;

    public CourseServiceImpl(@Lazy CourseMemberService courseMemberService) {
        this.courseMemberService = courseMemberService;
    }

    @Override
    public List<Course> listByTeacherNo(String teacherNo) {
        if (!StringUtils.hasText(teacherNo)) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .eq(Course::getTeacherNo, teacherNo)
                .eq(Course::getDeleted, 0)
                .orderByDesc(Course::getCreatedAt, Course::getId)
                .list();
    }

    @Override
    public List<Course> listByStudentUserNo(String userNo) {
        if (!StringUtils.hasText(userNo)) {
            return Collections.emptyList();
        }
        // 1. 通过学号 (user_no) 查该学生加入的所有课程成员记录
        List<Long> courseIds = courseMemberService.lambdaQuery()
                .eq(CourseMember::getUserNo, userNo)
                .list()
                .stream()
                .map(CourseMember::getCourseId)
                .collect(Collectors.toList());

        if (courseIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 2. 批量查课程详情
        return lambdaQuery()
                .in(Course::getId, courseIds)
                .eq(Course::getDeleted, 0)
                .orderByDesc(Course::getCreatedAt, Course::getId)
                .list();
    }
}
