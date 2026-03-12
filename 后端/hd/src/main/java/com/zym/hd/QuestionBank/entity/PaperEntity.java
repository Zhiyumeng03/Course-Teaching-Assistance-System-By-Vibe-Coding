package com.zym.hd.QuestionBank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_paper")
public class PaperEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("course_id")
    private Long courseId;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("title")
    private String title;

    @TableField("paper_type")
    private String paperType;

    @TableField("generation_mode")
    private String generationMode;

    @TableField("config_json")
    private String configJson;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("duration_minutes")
    private Integer durationMinutes;

    @TableField("status")
    private String status;

    @TableField(exist = false)
    private LocalDateTime starttTime;

    @TableField(exist = false)
    private LocalDateTime endTime;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private Integer questionCount;
}

