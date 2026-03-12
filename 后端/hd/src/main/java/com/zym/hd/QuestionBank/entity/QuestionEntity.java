package com.zym.hd.QuestionBank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@TableName("t_question")
public class QuestionEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("course_id")
    private Long courseId;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("creator_role")
    private String creatorRole;

    @TableField("type")
    private String type;

    @TableField("stem")
    private String stem;

    @TableField("content_json")
    private String contentJson;

    @TableField("answer_json")
    private String answerJson;

    @TableField("analysis_text")
    private String analysisText;

    @TableField("difficulty")
    private Integer difficulty;

    @TableField("source_type")
    private String sourceType;

    @TableField("visibility")
    private String visibility;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("usage_count")
    private Integer usageCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted")
    private Integer deleted;

    @TableField(exist = false)
    private List<Long> knowledgePointIds;
}

