package com.zym.hd.course.controller;

import com.zym.hd.course.entity.Course;
import com.zym.hd.course.service.CourseService;

import java.util.List;

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
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** 全量列表（管理用途） */
    @GetMapping
    public List<Course> list() {
        return courseService.list();
    }

    /** 按加入码查询课程（学生申请加入时使用） */
    @GetMapping("/by-join-code")
    public Course getByJoinCode(@RequestParam("joinCode") String joinCode) {
        return courseService.lambdaQuery()
                .eq(Course::getJoinCode, joinCode)
                .eq(Course::getDeleted, 0)
                .one();
    }

    @GetMapping("/{id}")
    public Course getById(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public boolean create(@RequestBody Course course) {
        return courseService.save(course);
    }

    @PutMapping("/{id}")
    public boolean update(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        return courseService.updateById(course);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return courseService.removeById(id);
    }
}
