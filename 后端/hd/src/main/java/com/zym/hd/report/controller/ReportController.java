package com.zym.hd.report.controller;

import com.zym.hd.file.entity.FileEntity;
import com.zym.hd.file.service.FileService;
import com.zym.hd.file.service.OssService;
import com.zym.hd.report.entity.ReportEntity;
import com.zym.hd.report.service.ReportService;
import com.zym.hd.report.vo.ReportVersionDetailVO;
import com.zym.hd.report.vo.StudentReportItemVO;
import com.zym.hd.report.vo.TeacherReportTaskVO;
import com.zym.hd.security.LoginUser;
import com.zym.hd.security.SecurityContextUtil;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;
    private final OssService ossService;
    private final FileService fileService;

    public ReportController(ReportService reportService, OssService ossService, FileService fileService) {
        this.reportService = reportService;
        this.ossService = ossService;
        this.fileService = fileService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/create")
    public ReportEntity create(@RequestBody ReportEntity entity) {
        return reportService.createReport(entity, SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
    @GetMapping("/getById")
    public ReportEntity getById(@RequestParam("id") Long id) {
        return reportService.getById(id);
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
    @GetMapping("/list")
    public List<ReportEntity> list() {
        return reportService.list();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/update")
    public ReportEntity update(@RequestBody ReportEntity entity) {
        return reportService.updateReportByStudent(entity, SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        LoginUser user = SecurityContextUtil.currentUser();
        return reportService.deleteReport(id, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/submit")
    public ReportEntity submit(@RequestParam("id") Long id) {
        return reportService.submitReport(id, SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/rollback")
    public ReportEntity rollback(@RequestBody RollbackRequest request) {
        return reportService.rollbackReport(request.getReportId(), SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/score")
    public ReportEntity score(@RequestBody ScoreRequest request) {
        return reportService.scoreReport(
                request.getReportId(),
                request.getReportVersionId(),
                request.getScore(),
                request.getCommentText(),
                request.getAction(),
                request.getRevisionRequirement(),
                SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/draft/save")
    public boolean saveDraft(@RequestBody DraftRequest request) {
        return reportService.saveDraft(
                request.getReportId(),
                SecurityContextUtil.currentUserId(),
                request.getContentHtml(),
                request.getContentText(),
                request.getAttachmentIds());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/draft")
    public Map<String, Object> getLatestDraft(@RequestParam("reportId") Long reportId) {
        return reportService.getLatestDraft(reportId, SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/getOrCreate")
    public ReportEntity getOrCreate(@RequestParam("experimentId") Long experimentId) {
        return reportService.getOrCreateReport(experimentId, SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/student/dashboard")
    public List<StudentReportItemVO> studentDashboard() {
        return reportService.getStudentReportDashboard(SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/teacher/dashboard")
    public List<TeacherReportTaskVO> teacherDashboard() {
        return reportService.getTeacherReportDashboard(SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/version/detail")
    public ReportVersionDetailVO getVersionDetail(@RequestParam("reportId") Long reportId,
                                                  @RequestParam("reportVersionId") Long reportVersionId) {
        LoginUser user = SecurityContextUtil.currentUser();
        return reportService.getReportVersionDetail(reportId, reportVersionId, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/version/analyze")
    public ReportVersionDetailVO analyzeVersion(@RequestBody VersionAnalyzeRequest request) {
        LoginUser user = SecurityContextUtil.currentUser();
        return reportService.analyzeReportVersion(
                request.getReportId(),
                request.getReportVersionId(),
                user.getUserId(),
                user.getRole());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/submitWithContent")
    public ReportEntity submitWithContent(@RequestBody SubmitContentRequest request) {
        return reportService.submitWithContent(
                request.getReportId(),
                SecurityContextUtil.currentUserId(),
                request.getContentHtml(),
                request.getContentText(),
                request.getAttachmentIds(),
                Boolean.TRUE.equals(request.getSaveAsDraft()));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/uploadAttachment")
    public Map<String, String> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "reportId", required = false) Long reportId) {
        String url = ossService.upload(file, "report");
        FileEntity fileEntity = fileService.createOssAsset(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename(),
                url,
                file.getContentType(),
                file.getSize(),
                "REPORT_ATTACHMENT",
                reportId,
                SecurityContextUtil.currentUserId());
        return Map.of(
                "id", String.valueOf(fileEntity.getId()),
                "url", url,
                "name", fileEntity.getOriginalName() == null ? "" : fileEntity.getOriginalName());
    }

    @Data
    public static class RollbackRequest {
        private Long reportId;
    }

    @Data
    public static class ScoreRequest {
        private Long reportId;
        private Long reportVersionId;
        private Integer score;
        private String commentText;
        private String action;
        private String revisionRequirement;
    }

    @Data
    public static class DraftRequest {
        private Long reportId;
        private String contentHtml;
        private String contentText;
        private List<Long> attachmentIds;
    }

    @Data
    public static class SubmitContentRequest {
        private Long reportId;
        private String contentHtml;
        private String contentText;
        private List<Long> attachmentIds;
        private Boolean saveAsDraft;
    }

    @Data
    public static class VersionAnalyzeRequest {
        private Long reportId;
        private Long reportVersionId;
    }
}
