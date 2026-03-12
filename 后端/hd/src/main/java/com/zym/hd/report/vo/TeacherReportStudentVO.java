package com.zym.hd.report.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class TeacherReportStudentVO {

    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long reportId;
    private String status;
    private Integer latestVersionNo;
    private Integer finalScore;
    private LocalDateTime lastSubmittedAt;
    private List<ReportVersionItemVO> versions = new ArrayList<>();
}
