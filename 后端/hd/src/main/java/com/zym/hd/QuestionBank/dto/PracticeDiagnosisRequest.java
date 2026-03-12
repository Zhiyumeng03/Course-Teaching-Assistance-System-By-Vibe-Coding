package com.zym.hd.QuestionBank.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class PracticeDiagnosisRequest {

    private Long recordId;

    private Long courseId;

    private String courseName;

    private Long paperId;

    private String paperTitle;

    private Long studentId;

    private String studentName;

    private Integer objectiveScore;

    private Integer totalScore;

    private List<PracticeDiagnosisAnswerItem> answers = new ArrayList<>();
}
