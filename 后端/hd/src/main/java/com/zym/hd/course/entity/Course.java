package com.zym.hd.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_course")
public class Course {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("course_code")
    private String courseCode;

    @TableField("course_name")
    private String courseName;

    @TableField("term")
    private String term;

    @TableField("description")
    private String description;

    @TableField("teacher_no")
    private String teacherNo;

    @TableField("join_code")
    private String joinCode;

    @TableField("enroll_mode")
    private String enrollMode;

    @TableField("enroll_capacity")
    private Integer enrollCapacity;

    @TableField("enroll_start_at")
    private LocalDateTime enrollStartAt;

    @TableField("enroll_end_at")
    private LocalDateTime enrollEndAt;

    @TableField("enroll_preheated_at")
    private LocalDateTime enrollPreheatedAt;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted")
    private Integer deleted;
}
