package com.zym.hd.QuestionBank.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class AiGeneratedQuestionItem {

    private String type;

    private String stem;

    private Map<String, Object> content = new LinkedHashMap<>();

    private Map<String, Object> answer = new LinkedHashMap<>();

    private String analysisText;

    private Integer difficulty;

    private List<Long> knowledgePointIds = new ArrayList<>();

    private String sourceType;

    private String reviewStatus;

    private String visibility;
}