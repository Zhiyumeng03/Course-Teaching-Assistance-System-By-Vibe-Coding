package com.zym.hd.report.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ReportVersionItemVO {

    private Long reportVersionId;
    private Integer versionNo;
    private String contentHtml;
    private String contentText;
    private String attachmentIdsRaw;
    private List<Long> attachmentIds;
    private Integer wordCount;
    private LocalDateTime submittedAt;
    private Integer score;
    private String commentText;
    private String action;
    private String revisionRequirement;
    private LocalDateTime reviewedAt;
    private String teacherName;
    private Long aiTaskId;
    private Integer aiRiskScore;
    private String aiResultJson;
    private String aiRiskLevel;
}
