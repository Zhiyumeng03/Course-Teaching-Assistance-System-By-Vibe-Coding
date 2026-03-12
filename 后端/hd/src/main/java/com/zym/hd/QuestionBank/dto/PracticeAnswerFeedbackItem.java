package com.zym.hd.QuestionBank.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class PracticeAnswerFeedbackItem {

    private Long answerId;

    private Long paperQuestionId;

    private Long questionId;

    private String masteryLevel;

    private List<Map<String, Object>> knowledgePoints = new ArrayList<>();

    private String summary;

    private String improvement;

    private Map<String, Object> extra = new LinkedHashMap<>();
}
