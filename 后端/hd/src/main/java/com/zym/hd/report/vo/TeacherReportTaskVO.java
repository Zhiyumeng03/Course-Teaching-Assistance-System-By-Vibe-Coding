package com.zym.hd.report.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class TeacherReportTaskVO {

    private Long experimentId;
    private String experimentTitle;
    private Long courseId;
    private String courseName;
    private Long totalStudents;
    private Long submittedCount;
    private Long unsubmittedCount;
    private Long reviewedCount;
    private Long pendingReviewCount;
    private Long revisionRequiredCount;
    private List<TeacherReportStudentVO> students = new ArrayList<>();
}
