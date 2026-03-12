package com.zym.hd.QuestionBank.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class PythonGenerateRequest {

    private Long courseId;

    private String courseName;

    private List<String> knowledgePoints = new ArrayList<>();

    private String questionType;

    private Integer difficulty;

    private Integer count;

    private String language;

    private String extraRequirements;
}