package com.zym.hd.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zym.hd.report.entity.ReportEntity;
import com.zym.hd.report.vo.StudentReportItemVO;
import com.zym.hd.report.vo.TeacherReportTaskVO;
import com.zym.hd.report.vo.ReportVersionDetailVO;
import java.util.List;
import java.util.Map;

public interface ReportService extends IService<ReportEntity> {

    ReportEntity createReport(ReportEntity entity, Long studentId);

    ReportEntity updateReportByStudent(ReportEntity entity, Long studentId);

    ReportEntity submitReport(Long reportId, Long studentId);

    boolean deleteReport(Long reportId, Long operatorId, String operatorRole);

    ReportEntity rollbackReport(Long reportId, Long teacherId);

    ReportEntity scoreReport(Long reportId,
                             Long reportVersionId,
                             Integer score,
                             String commentText,
                             String action,
                             String revisionRequirement,
                             Long teacherId);

    boolean saveDraft(Long reportId, Long studentId, String contentHtml, String contentText, List<Long> attachmentIds);

    Map<String, Object> getLatestDraft(Long reportId, Long studentId);

    /**
     * 若学生针对该实验已有报告则返回，否则自动创建并返回
     */
    ReportEntity getOrCreateReport(Long experimentId, Long studentId);

    /**
     * 一步提交报告内容（含版本写入）；若 saveAsDraft=true 则只存草稿不改状态
     */
    ReportEntity submitWithContent(Long reportId, Long studentId,
                                   String contentHtml, String contentText,
                                   List<Long> attachmentIds,
                                   boolean saveAsDraft);

    List<StudentReportItemVO> getStudentReportDashboard(Long studentId);

    List<TeacherReportTaskVO> getTeacherReportDashboard(Long teacherId);

    ReportVersionDetailVO getReportVersionDetail(Long reportId, Long reportVersionId, Long operatorId, String operatorRole);

    ReportVersionDetailVO analyzeReportVersion(Long reportId, Long reportVersionId, Long operatorId, String operatorRole);
}


