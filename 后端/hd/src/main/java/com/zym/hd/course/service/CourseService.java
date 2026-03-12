package com.zym.hd.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zym.hd.course.entity.Course;
import java.util.List;

public interface CourseService extends IService<Course> {

    /** 按教师工号查询该教师的课程 */
    List<Course> listByTeacherNo(String teacherNo);

    /** 按学生学号 (user_no) 查询该学生加入的课程（通过成员表关联） */
    List<Course> listByStudentUserNo(String userNo);
}

