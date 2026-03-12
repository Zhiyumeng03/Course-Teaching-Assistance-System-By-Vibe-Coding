package com.zym.hd.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_report")
public class ReportEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("experiment_id")
    private Long experimentId;

    @TableField("student_id")
    private Long studentId;

    @TableField("status")
    private String status;

    @TableField("latest_version_no")
    private Integer latestVersionNo;

    @TableField("final_score")
    private Integer finalScore;

    @TableField("last_submitted_at")
    private LocalDateTime lastSubmittedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

