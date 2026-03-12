package com.zym.hd.report.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReportAnalysisPeerVersionVO {

    private Long reportId;
    private Long reportVersionId;
    private String studentName;
    private Integer versionNo;
    private Integer wordCount;
    private String contentHtml;
    private String contentText;
    private LocalDateTime submittedAt;
}
