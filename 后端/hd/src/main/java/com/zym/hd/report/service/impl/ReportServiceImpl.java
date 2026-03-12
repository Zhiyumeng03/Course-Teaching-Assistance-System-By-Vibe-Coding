package com.zym.hd.report.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zym.hd.QuestionBank.util.JsonUtils;
import com.zym.hd.file.entity.FileEntity;
import com.zym.hd.file.service.FileService;
import com.zym.hd.report.client.ReportAiDetectionClient;
import com.zym.hd.report.dto.ReportAiDetectionRequest;
import com.zym.hd.report.dto.ReportAiDetectionResponse;
import com.zym.hd.report.entity.ReportEntity;
import com.zym.hd.report.mapper.ReportMapper;
import com.zym.hd.report.service.ReportService;
import com.zym.hd.report.vo.ReportAnalysisPeerVersionVO;
import com.zym.hd.report.vo.ReportVersionDetailVO;
import com.zym.hd.report.vo.ReportVersionItemVO;
import com.zym.hd.report.vo.StudentReportItemVO;
import com.zym.hd.report.vo.TeacherReportStudentVO;
import com.zym.hd.report.vo.TeacherReportTaskVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, ReportEntity> implements ReportService {

    private final FileService fileService;
    private final ReportAiDetectionClient reportAiDetectionClient;

    public ReportServiceImpl(FileService fileService, ReportAiDetectionClient reportAiDetectionClient) {
        this.fileService = fileService;
        this.reportAiDetectionClient = reportAiDetectionClient;
    }

    @Override
    public ReportEntity createReport(ReportEntity entity, Long studentId) {
        if (entity == null || entity.getExperimentId() == null) {
            throw new IllegalArgumentException("experimentId is required");
        }
        entity.setStudentId(studentId);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("DRAFT");
        }
        if (entity.getLatestVersionNo() == null) {
            entity.setLatestVersionNo(0);
        }
        entity.setFinalScore(null);
        entity.setLastSubmittedAt(null);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        save(entity);
        return entity;
    }

    @Override
    public ReportEntity updateReportByStudent(ReportEntity entity, Long studentId) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalArgumentException("report id is required");
        }
        ReportEntity db = getById(entity.getId());
        if (db == null) {
            throw new IllegalArgumentException("report not found");
        }
        if (!studentId.equals(db.getStudentId())) {
            throw new IllegalArgumentException("forbidden");
        }
        if (entity.getExperimentId() != null) {
            db.setExperimentId(entity.getExperimentId());
        }
        if (StringUtils.hasText(entity.getStatus())) {
            db.setStatus(entity.getStatus());
        }
        db.setUpdatedAt(LocalDateTime.now());
        updateById(db);
        return db;
    }

    @Override
    public ReportEntity submitReport(Long reportId, Long studentId) {
        ReportEntity db = getById(reportId);
        if (db == null) {
            throw new IllegalArgumentException("report not found");
        }
        if (!studentId.equals(db.getStudentId())) {
            throw new IllegalArgumentException("forbidden");
        }
        int latest = db.getLatestVersionNo() == null ? 0 : db.getLatestVersionNo();
        db.setLatestVersionNo(latest + 1);
        db.setStatus("SUBMITTED");
        db.setLastSubmittedAt(LocalDateTime.now());
        db.setUpdatedAt(LocalDateTime.now());
        updateById(db);
        return db;
    }

    @Override
    public boolean deleteReport(Long reportId, Long operatorId, String operatorRole) {
        ReportEntity db = getById(reportId);
        if (db == null) {
            return false;
        }
        if ("STUDENT".equalsIgnoreCase(operatorRole) && !operatorId.equals(db.getStudentId())) {
            throw new IllegalArgumentException("forbidden");
        }
        return removeById(reportId);
    }

    @Override
    public ReportEntity rollbackReport(Long reportId, Long teacherId) {
        ensureTeacherResponsible(reportId, teacherId);
        ReportEntity db = getById(reportId);
        if (db == null) {
            throw new IllegalArgumentException("report not found");
        }
        db.setStatus("DRAFT");
        db.setUpdatedAt(LocalDateTime.now());
        updateById(db);
        return db;
    }

    @Override
    public ReportEntity scoreReport(Long reportId,
                                    Long reportVersionId,
                                    Integer score,
                                    String commentText,
                                    String action,
                                    String revisionRequirement,
                                    Long teacherId) {
        ensureTeacherResponsible(reportId, teacherId);
        ReportEntity db = getById(reportId);
        if (db == null) {
            throw new IllegalArgumentException("report not found");
        }

        int inserted = baseMapper.insertReview(
                reportId,

                reportVersionId,
                teacherId,
                score,
                commentText,
                action,
                revisionRequirement,
                LocalDateTime.now());
        if (inserted <= 0) {
            throw new IllegalStateException("insert review failed");
        }

        db.setFinalScore(score);
        db.setStatus(resolveReviewStatus(action));
        db.setUpdatedAt(LocalDateTime.now());
        updateById(db);
        return db;
    }

    @Override
    public boolean saveDraft(Long reportId, Long studentId, String contentHtml, String contentText, List<Long> attachmentIds) {
        ReportEntity db = getById(reportId);
        if (db == null) {
            throw new IllegalArgumentException("report not found");
        }
        if (!studentId.equals(db.getStudentId())) {
            throw new IllegalArgumentException("forbidden");
        }
        String normalizedContentText = resolveReportText(contentText, contentHtml);
        return baseMapper.upsertDraft(
                reportId,
                contentHtml,
                normalizedContentText,
                toAttachmentIdsString(attachmentIds),
                LocalDateTime.now()) > 0;
    }

    @Override
    public Map<String, Object> getLatestDraft(Long reportId, Long studentId) {
        ReportEntity db = getById(reportId);
        if (db == null) {
            throw new IllegalArgumentException("report not found");
        }
        if (!studentId.equals(db.getStudentId())) {
            throw new IllegalArgumentException("forbidden");
        }

        Map<String, Object> draft = baseMapper.getDraftByReportId(reportId);
        if (draft == null || draft.isEmpty()) {
            return draft;
        }

        List<Long> attachmentIds = parseAttachmentIds((String) draft.get("attachmentIds"));
        draft.put("attachmentIds", attachmentIds);
        draft.put("attachments", attachmentIds.isEmpty()
                ? new ArrayList<FileEntity>()
                : fileService.listByIdsPreserveOrder(attachmentIds));
        return draft;
    }

    @Override
    public ReportEntity getOrCreateReport(Long experimentId, Long studentId) {
        ReportEntity existing = baseMapper.getByExperimentAndStudent(experimentId, studentId);
        if (existing != null) {
            return existing;
        }

        ReportEntity entity = new ReportEntity();
        entity.setExperimentId(experimentId);
        entity.setStudentId(studentId);
        entity.setStatus("DRAFT");
        entity.setLatestVersionNo(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        save(entity);
        return entity;
    }

    @Override
    public ReportEntity submitWithContent(Long reportId, Long studentId,
                                          String contentHtml, String contentText,
                                          List<Long> attachmentIds,
                                          boolean saveAsDraft) {
        ReportEntity db = getById(reportId);
        if (db == null) {
            throw new IllegalArgumentException("report not found");
        }
        if (!studentId.equals(db.getStudentId())) {
            throw new IllegalArgumentException("forbidden");
        }

        String normalizedContentText = resolveReportText(contentText, contentHtml);
        int wordCount = normalizedContentText == null ? 0 : normalizedContentText.trim().length();
        LocalDateTime now = LocalDateTime.now();
        String attachmentIdsValue = toAttachmentIdsString(attachmentIds);

        if (saveAsDraft) {
            baseMapper.upsertDraft(reportId, contentHtml, normalizedContentText, attachmentIdsValue, now);
        } else {
            int newVersionNo = (db.getLatestVersionNo() == null ? 0 : db.getLatestVersionNo()) + 1;
            baseMapper.insertVersion(reportId, newVersionNo, contentHtml, normalizedContentText, attachmentIdsValue, wordCount, now, now);
            db.setLatestVersionNo(newVersionNo);
            db.setStatus("SUBMITTED");
            db.setLastSubmittedAt(now);
            db.setUpdatedAt(now);
            updateById(db);

            Long versionId = baseMapper.getLatestVersionId(reportId);
            if (versionId != null) {
                try {
                    runAnalysisAndPersist(loadVersionDetail(reportId, versionId));
                } catch (Exception ignored) {
                    // AI 检测失败不阻塞学生正常提交，教师可在详情页手动重试。
                }
            }
        }
        return db;
    }

    @Override
    public List<StudentReportItemVO> getStudentReportDashboard(Long studentId) {
        List<StudentReportItemVO> items = baseMapper.listStudentReportItems(studentId);
        for (StudentReportItemVO item : items) {
            item.setVersions(fillVersionAttachments(baseMapper.listReportVersions(item.getReportId())));
        }
        return items;
    }

    @Override
    public List<TeacherReportTaskVO> getTeacherReportDashboard(Long teacherId) {
        List<TeacherReportTaskVO> tasks = baseMapper.listTeacherReportTasks(teacherId);
        for (TeacherReportTaskVO task : tasks) {
            long total = safeLong(task.getTotalStudents());
            long submitted = safeLong(task.getSubmittedCount());
            task.setUnsubmittedCount(Math.max(total - submitted, 0));

            List<TeacherReportStudentVO> students = baseMapper.listTeacherReportStudents(task.getExperimentId());
            for (TeacherReportStudentVO student : students) {
                if (student.getReportId() != null) {
                    student.setVersions(fillVersionAttachments(baseMapper.listReportVersions(student.getReportId())));
                }
            }
            task.setStudents(students);
        }
        return tasks;
    }

    @Override
    public ReportVersionDetailVO getReportVersionDetail(Long reportId, Long reportVersionId, Long operatorId, String operatorRole) {
        ensureReadableAccess(reportId, operatorId, operatorRole);
        return loadVersionDetail(reportId, reportVersionId);
    }

    @Override
    public ReportVersionDetailVO analyzeReportVersion(Long reportId, Long reportVersionId, Long operatorId, String operatorRole) {
        ensureReadableAccess(reportId, operatorId, operatorRole);
        ReportVersionDetailVO detail = loadVersionDetail(reportId, reportVersionId);
        return runAnalysisAndPersist(detail);
    }

    private String toAttachmentIdsString(List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Long attachmentId : attachmentIds) {
            if (attachmentId == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(attachmentId);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private List<Long> parseAttachmentIds(String value) {
        List<Long> result = new ArrayList<>();
        if (!StringUtils.hasText(value)) {
            return result;
        }
        for (String item : value.split(",")) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            try {
                result.add(Long.parseLong(item.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed ids to avoid blocking draft rendering.
            }
        }
        return result;
    }

    private List<ReportVersionItemVO> fillVersionAttachments(List<ReportVersionItemVO> versions) {
        for (ReportVersionItemVO version : versions) {
            version.setAttachmentIds(parseAttachmentIds(version.getAttachmentIdsRaw()));
            version.setContentText(resolveReportText(version.getContentText(), version.getContentHtml()));
            version.setAiRiskLevel(extractRiskLevel(version.getAiResultJson()));
        }
        return versions;
    }

    private ReportVersionDetailVO loadVersionDetail(Long reportId, Long reportVersionId) {
        ReportVersionDetailVO detail = baseMapper.getReportVersionDetail(reportId, reportVersionId);
        if (detail == null) {
            throw new IllegalArgumentException("report version not found");
        }
        List<Long> attachmentIds = parseAttachmentIds(detail.getAttachmentIdsRaw());
        detail.setAttachmentIds(attachmentIds);
        detail.setContentText(resolveReportText(detail.getContentText(), detail.getContentHtml()));
        detail.setDraftContentText(resolveReportText(detail.getDraftContentText(), null));
        detail.setAttachments(attachmentIds.isEmpty()
                ? new ArrayList<>()
                : fileService.listByIdsPreserveOrder(attachmentIds));
        detail.setAiResult(parseAiResult(detail.getAiResultJson()));
        return detail;
    }

    private ReportVersionDetailVO runAnalysisAndPersist(ReportVersionDetailVO detail) {
        ReportAiDetectionRequest request = buildDetectionRequest(detail);
        ReportAiDetectionResponse response = reportAiDetectionClient.detect(request);
        String resultJson = JsonUtils.toJson(response);
        baseMapper.updateVersionAiResult(
                detail.getReportVersionId(),
                response.getOverallAiRatio(),
                resultJson);
        detail.setAiRiskScore(response.getOverallAiRatio());
        detail.setAiResultJson(resultJson);
        detail.setAiResult(parseAiResult(resultJson));
        return detail;
    }

    private ReportAiDetectionRequest buildDetectionRequest(ReportVersionDetailVO detail) {
        ReportAiDetectionRequest request = new ReportAiDetectionRequest();
        request.setReportId(detail.getReportId());
        request.setReportVersionId(detail.getReportVersionId());
        request.setExperimentId(detail.getExperimentId());
        request.setExperimentTitle(detail.getExperimentTitle());
        request.setExperimentObjective(detail.getExperimentObjective());
        request.setExperimentContentText(detail.getExperimentContentText());
        request.setStudentId(detail.getStudentId());
        request.setStudentName(detail.getStudentName());
        request.setVersionNo(detail.getVersionNo());
        request.setWordCount(detail.getWordCount());
        request.setDraftSaveCount(detail.getDraftSaveCount());
        request.setReportContentHtml(detail.getContentHtml());
        request.setReportContentText(resolveReportText(detail.getContentText(), detail.getContentHtml()));
        request.setDraftContentText(resolveReportText(detail.getDraftContentText(), null));

        List<ReportVersionItemVO> previousVersions = baseMapper.listPreviousVersions(detail.getReportId(), detail.getReportVersionId());
        for (ReportVersionItemVO item : previousVersions) {
            ReportAiDetectionRequest.PreviousVersionItem previousVersion = new ReportAiDetectionRequest.PreviousVersionItem();
            previousVersion.setReportVersionId(item.getReportVersionId());
            previousVersion.setVersionNo(item.getVersionNo());
            previousVersion.setSubmittedAt(item.getSubmittedAt() == null ? null : item.getSubmittedAt().toString());
            previousVersion.setContentText(shorten(resolveReportText(item.getContentText(), item.getContentHtml()), 3000));
            request.getPreviousVersions().add(previousVersion);
        }

        List<ReportAnalysisPeerVersionVO> peerVersions = baseMapper.listPeerReportVersions(detail.getExperimentId(), detail.getReportId());
        for (ReportAnalysisPeerVersionVO item : peerVersions) {
            ReportAiDetectionRequest.PeerReportItem peerReport = new ReportAiDetectionRequest.PeerReportItem();
            peerReport.setReportId(item.getReportId());
            peerReport.setReportVersionId(item.getReportVersionId());
            peerReport.setStudentName(item.getStudentName());
            peerReport.setVersionNo(item.getVersionNo());
            peerReport.setWordCount(item.getWordCount());
            peerReport.setContentText(shorten(resolveReportText(item.getContentText(), item.getContentHtml()), 2400));
            request.getPeerReports().add(peerReport);
        }

        for (FileEntity attachment : detail.getAttachments()) {
            ReportAiDetectionRequest.AttachmentItem attachmentItem = new ReportAiDetectionRequest.AttachmentItem();
            attachmentItem.setFileId(attachment.getId());
            attachmentItem.setName(attachment.getOriginalName());
            attachmentItem.setMimeType(attachment.getMimeType());
            attachmentItem.setFileSize(attachment.getFileSize());
            attachmentItem.setStoragePath(attachment.getStoragePath());
            request.getAttachments().add(attachmentItem);
        }
        return request;
    }

    private Map<String, Object> parseAiResult(String aiResultJson) {
        if (!StringUtils.hasText(aiResultJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return JsonUtils.parse(aiResultJson, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String extractRiskLevel(String aiResultJson) {
        Map<String, Object> result = parseAiResult(aiResultJson);
        Object value = result.get("riskLevel");
        return value == null ? null : String.valueOf(value);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private void ensureTeacherResponsible(Long reportId, Long teacherId) {
        Integer count = baseMapper.countTeacherResponsible(reportId, teacherId);
        if (count == null || count <= 0) {
            throw new IllegalArgumentException("forbidden");
        }
    }

    private void ensureReadableAccess(Long reportId, Long operatorId, String operatorRole) {
        ReportEntity report = getById(reportId);
        if (report == null) {
            throw new IllegalArgumentException("report not found");
        }
        if ("ADMIN".equalsIgnoreCase(operatorRole)) {
            return;
        }
        if ("STUDENT".equalsIgnoreCase(operatorRole)) {
            if (!operatorId.equals(report.getStudentId())) {
                throw new IllegalArgumentException("forbidden");
            }
            return;
        }
        if ("TEACHER".equalsIgnoreCase(operatorRole)) {
            ensureTeacherResponsible(reportId, operatorId);
            return;
        }
        throw new IllegalArgumentException("forbidden");
    }

    private String resolveReviewStatus(String action) {
        if (!StringUtils.hasText(action)) {
            return "REVIEWED";
        }
        String normalized = action.trim().toUpperCase();
        if ("REVISION_REQUIRED".equals(normalized) || "REVISE".equals(normalized)) {
            return "REVISION_REQUIRED";
        }
        return "REVIEWED";
    }

    private String shorten(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String normalized = text.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(maxLength - 3, 1)) + "...";
    }

    private String resolveReportText(String contentText, String contentHtml) {
        if (StringUtils.hasText(contentText)) {
            return contentText.trim();
        }
        if (!StringUtils.hasText(contentHtml)) {
            return contentText;
        }
        String text = contentHtml
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|section|article|li|tr|td|th|h[1-6]|blockquote|pre|ul|ol)>", "\n")
                .replaceAll("(?i)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
        text = text.replaceAll("\\r\\n?", "\n")
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        return StringUtils.hasText(text) ? text : contentText;
    }
}
