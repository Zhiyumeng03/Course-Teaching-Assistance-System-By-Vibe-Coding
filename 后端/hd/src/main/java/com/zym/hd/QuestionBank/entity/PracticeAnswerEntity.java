package com.zym.hd.QuestionBank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_practice_answer")
public class PracticeAnswerEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("record_id")
    private Long recordId;

    @TableField("paper_question_id")
    private Long paperQuestionId;

    @TableField("question_id")
    private Long questionId;

    @TableField("answer_json")
    private String answerJson;

    @TableField("is_correct")
    private Integer isCorrect;

    @TableField("score")
    private Integer score;

    @TableField("ai_feedback_json")
    private String aiFeedbackJson;

    @TableField(exist = false)
    private Integer sortNo;

    @TableField(exist = false)
    private Integer paperQuestionScore;

    @TableField(exist = false)
    private String questionSnapshotJson;
}

