package com.zym.hd.QuestionBank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_paper_question")
public class PaperQuestionEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("paper_id")
    private Long paperId;

    @TableField("question_id")
    private Long questionId;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("score")
    private Integer score;

    @TableField("question_snapshot_json")
    private String questionSnapshotJson;
}

