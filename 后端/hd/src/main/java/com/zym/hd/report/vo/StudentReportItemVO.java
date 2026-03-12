package com.zym.hd.report.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class StudentReportItemVO {

    private Long reportId;
    private Long experimentId;
    private String experimentTitle;
    private Long courseId;
    private String courseName;
    private String teacherName;
    private String status;
    private Integer latestVersionNo;
    private Integer finalScore;
    private LocalDateTime lastSubmittedAt;
    private List<ReportVersionItemVO> versions = new ArrayList<>();
}
