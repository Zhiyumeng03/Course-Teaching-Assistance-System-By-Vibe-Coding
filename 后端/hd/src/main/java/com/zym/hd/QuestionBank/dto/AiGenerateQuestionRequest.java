package com.zym.hd.QuestionBank.dto;

import java.util.List;
import lombok.Data;

@Data
public class AiGenerateQuestionRequest {

    private Long courseId;

    private String questionType;

    private Integer difficulty;

    private Integer count;

    private List<Long> knowledgePointIds;

    private String extraRequirements;
}