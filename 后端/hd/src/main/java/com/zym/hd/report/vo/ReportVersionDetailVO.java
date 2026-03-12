package com.zym.hd.report.vo;

import com.zym.hd.file.entity.FileEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ReportVersionDetailVO {

    private Long reportId;
    private Long experimentId;
    private String experimentTitle;
    private String experimentObjective;
    private String experimentContentText;
    private Long courseId;
    private String courseName;
    private String teacherName;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String status;
    private Long reportVersionId;
    private Integer versionNo;
    private String contentHtml;
    private String contentText;
    private String attachmentIdsRaw;
    private List<Long> attachmentIds = new ArrayList<>();
    private List<FileEntity> attachments = new ArrayList<>();
    private Integer wordCount;
    private LocalDateTime submittedAt;
    private Integer score;
    private String commentText;
    private String action;
    private String revisionRequirement;
    private LocalDateTime reviewedAt;
    private Long aiTaskId;
    private Integer aiRiskScore;
    private String aiResultJson;
    private Map<String, Object> aiResult = new LinkedHashMap<>();
    private Integer draftSaveCount;
    private String draftContentText;
}
