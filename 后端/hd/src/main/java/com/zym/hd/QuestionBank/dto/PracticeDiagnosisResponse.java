package com.zym.hd.QuestionBank.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class PracticeDiagnosisResponse {

    private boolean success;

    private String model;

    private List<PracticeAnswerFeedbackItem> answerFeedbacks = new ArrayList<>();

    private Map<String, Object> diagnosisJson = new LinkedHashMap<>();

    private String diagnosisText;
}
