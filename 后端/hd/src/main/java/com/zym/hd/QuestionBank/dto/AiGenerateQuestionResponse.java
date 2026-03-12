package com.zym.hd.QuestionBank.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AiGenerateQuestionResponse {

    private boolean success;

    private String model;

    private List<AiGeneratedQuestionItem> questions = new ArrayList<>();
}