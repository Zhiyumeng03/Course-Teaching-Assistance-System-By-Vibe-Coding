package com.zym.hd.QuestionBank.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class PracticeDiagnosisAnswerItem {

    private Long answerId;

    private Long paperQuestionId;

    private Long questionId;

    private Integer sortNo;

    private String questionType;

    private String stem;

    private Map<String, Object> content = new LinkedHashMap<>();

    private Map<String, Object> referenceAnswer = new LinkedHashMap<>();

    private String analysisText;

    private List<String> knowledgePointNames = new ArrayList<>();

    private Map<String, Object> studentAnswer = new LinkedHashMap<>();

    private Integer score;

    private Integer fullScore;

    private Integer isCorrect;
}
