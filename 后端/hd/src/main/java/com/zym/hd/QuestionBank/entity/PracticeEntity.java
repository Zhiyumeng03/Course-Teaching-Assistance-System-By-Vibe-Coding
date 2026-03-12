package com.zym.hd.QuestionBank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_practice_record")
public class PracticeEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("paper_id")
    private Long paperId;

    @TableField("student_id")
    private Long studentId;

    @TableField("course_id")
    private Long courseId;

    @TableField("status")
    private String status;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("objective_score")
    private Integer objectiveScore;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("diagnosis_status")
    private String diagnosisStatus;

    @TableField("diagnosis_json")
    private String diagnosisJson;

    @TableField("diagnosis_text")
    private String diagnosisText;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String paperTitle;

    @TableField(exist = false)
    private Integer questionCount;

    @TableField(exist = false)
    private Integer durationMinutes;

    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String studentNo;
}

