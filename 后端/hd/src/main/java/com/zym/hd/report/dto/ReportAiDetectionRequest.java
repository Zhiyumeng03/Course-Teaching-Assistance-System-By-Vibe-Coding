package com.zym.hd.report.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ReportAiDetectionRequest {

    private Long reportId;

    private Long reportVersionId;

    private Long experimentId;

    private String experimentTitle;

    private String experimentObjective;

    private String experimentContentText;

    private Long studentId;

    private String studentName;

    private Integer versionNo;

    private Integer wordCount;

    private Integer draftSaveCount;

    private String reportContentHtml;

    private String reportContentText;

    private String draftContentText;

    private List<PreviousVersionItem> previousVersions = new ArrayList<>();

    private List<PeerReportItem> peerReports = new ArrayList<>();

    private List<AttachmentItem> attachments = new ArrayList<>();

    @Data
    public static class PreviousVersionItem {
        private Long reportVersionId;
        private Integer versionNo;
        private String submittedAt;
        private String contentText;
    }

    @Data
    public static class PeerReportItem {
        private Long reportId;
        private Long reportVersionId;
        private String studentName;
        private Integer versionNo;
        private Integer wordCount;
        private String contentText;
    }

    @Data
    public static class AttachmentItem {
        private Long fileId;
        private String name;
        private String mimeType;
        private Long fileSize;
        private String storagePath;
    }
}
