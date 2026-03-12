package com.zym.hd.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_course_member")
public class CourseMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("course_id")
    private Long courseId;

    @TableField("user_no")
    private String userNo;

    @TableField("role_in_course")
    private String roleInCourse;

    @TableField("joined_at")
    private LocalDateTime joinedAt;
}

