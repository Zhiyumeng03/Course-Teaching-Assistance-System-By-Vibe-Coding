package com.zym.hd.experiment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_experiment")
public class ExperimentEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("course_id")
    private Long courseId;

    @TableField("title")
    private String title;

    @TableField("objective")
    private String objective;

    @TableField("content_html")
    private String contentHtml;

    @TableField("content_text")
    private String contentText;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("deadline")
    private LocalDateTime deadline;

    @TableField("max_score")
    private Integer maxScore;

    @TableField("allow_resubmit")
    private Integer allowResubmit;

    @TableField("attachment_ids")
    private String attachmentIds;

    @TableField("status")
    private String status;

    @TableField("creator_no")
    private String creatorNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted")
    private Integer deleted;
}

