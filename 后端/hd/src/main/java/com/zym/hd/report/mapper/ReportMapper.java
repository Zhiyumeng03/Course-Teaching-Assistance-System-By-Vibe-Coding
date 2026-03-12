package com.zym.hd.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zym.hd.report.entity.ReportEntity;
import com.zym.hd.report.vo.ReportAnalysisPeerVersionVO;
import com.zym.hd.report.vo.ReportVersionItemVO;
import com.zym.hd.report.vo.ReportVersionDetailVO;
import com.zym.hd.report.vo.StudentReportItemVO;
import com.zym.hd.report.vo.TeacherReportStudentVO;
import com.zym.hd.report.vo.TeacherReportTaskVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReportMapper extends BaseMapper<ReportEntity> {

    @Insert("""
            INSERT INTO t_report_draft(report_id, content_html, content_text, draft_save_count, updated_at, attachment_ids)
            VALUES(#{reportId}, #{contentHtml}, #{contentText}, 1, #{updatedAt}, #{attachmentIds})
            ON DUPLICATE KEY UPDATE
            content_html = VALUES(content_html),
            content_text = VALUES(content_text),
            attachment_ids = VALUES(attachment_ids),
            draft_save_count = draft_save_count + 1,
            updated_at = VALUES(updated_at)
            """)
    int upsertDraft(@Param("reportId") Long reportId,
                    @Param("contentHtml") String contentHtml,
                    @Param("contentText") String contentText,
                    @Param("attachmentIds") String attachmentIds,
                    @Param("updatedAt") LocalDateTime updatedAt);

    @Select("""
            SELECT id,
                   report_id AS reportId,
                   content_html AS contentHtml,
                   content_text AS contentText,
                   draft_save_count AS draftSaveCount,
                   updated_at AS updatedAt,
                   attachment_ids AS attachmentIds
            FROM t_report_draft
            WHERE report_id = #{reportId}
            LIMIT 1
            """)
    Map<String, Object> getDraftByReportId(@Param("reportId") Long reportId);

    @Insert("""
            INSERT INTO t_report_review(
                report_id,
                report_version_id,
                teacher_id,
                score,
                comment_text,
                action,
                revision_requirement,
                created_at
            ) VALUES(
                #{reportId},
                #{reportVersionId},
                #{teacherId},
                #{score},
                #{commentText},
                #{action},
                #{revisionRequirement},
                #{createdAt}
            )
            """)
    int insertReview(@Param("reportId") Long reportId,
                     @Param("reportVersionId") Long reportVersionId,
                     @Param("teacherId") Long teacherId,
                     @Param("score") Integer score,
                     @Param("commentText") String commentText,
                     @Param("action") String action,
                     @Param("revisionRequirement") String revisionRequirement,
                     @Param("createdAt") LocalDateTime createdAt);

    /**
     * 插入一条报告版本记录（t_report_version）
     */
    @Insert("""
            INSERT INTO t_report_version(
                report_id, version_no, content_html, content_text,
                attachment_ids, word_count, submitted_at, created_at
            ) VALUES(
                #{reportId}, #{versionNo}, #{contentHtml}, #{contentText}, #{attachmentIds},
                #{wordCount}, #{submittedAt}, #{createdAt}
            )
            """)
    int insertVersion(@Param("reportId") Long reportId,
                      @Param("versionNo") Integer versionNo,
                      @Param("contentHtml") String contentHtml,
                      @Param("contentText") String contentText,
                      @Param("attachmentIds") String attachmentIds,
                      @Param("wordCount") Integer wordCount,
                      @Param("submittedAt") LocalDateTime submittedAt,
                      @Param("createdAt") LocalDateTime createdAt);

    /**
     * 按 experimentId + studentId 查报告（用于学生进入提交页时判断是否已有报告）
     */
    @Select("""
            SELECT id, experiment_id AS experimentId, student_id AS studentId,
                   status, latest_version_no AS latestVersionNo,
                   final_score AS finalScore,
                   last_submitted_at AS lastSubmittedAt,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM t_report
            WHERE experiment_id = #{experimentId} AND student_id = #{studentId}
            LIMIT 1
            """)
    ReportEntity getByExperimentAndStudent(@Param("experimentId") Long experimentId,
                                           @Param("studentId") Long studentId);

    @Select("""
            SELECT r.id AS reportId,
                   r.experiment_id AS experimentId,
                   e.title AS experimentTitle,
                   c.id AS courseId,
                   c.course_name AS courseName,
                   COALESCE(t.real_name, t.username) AS teacherName,
                   r.status AS status,
                   r.latest_version_no AS latestVersionNo,
                   r.final_score AS finalScore,
                   r.last_submitted_at AS lastSubmittedAt
            FROM t_report r
            INNER JOIN t_experiment e ON e.id = r.experiment_id
            INNER JOIN t_course c ON c.id = e.course_id
            LEFT JOIN t_user t ON t.teacher_no = c.teacher_no AND t.deleted = 0
            WHERE r.student_id = #{studentId}
            ORDER BY COALESCE(r.last_submitted_at, r.updated_at, r.created_at) DESC, r.id DESC
            """)
    List<StudentReportItemVO> listStudentReportItems(@Param("studentId") Long studentId);

    @Select("""
            SELECT v.id AS reportVersionId,
                   v.version_no AS versionNo,
                   v.content_html AS contentHtml,
                   v.content_text AS contentText,
                   v.attachment_ids AS attachmentIdsRaw,
                   v.word_count AS wordCount,
                   v.submitted_at AS submittedAt,
                   v.ai_task_id AS aiTaskId,
                   v.ai_risk_score AS aiRiskScore,
                   CAST(v.ai_result_json AS CHAR) AS aiResultJson,
                   rv.score AS score,
                   rv.comment_text AS commentText,
                   rv.action AS action,
                   rv.revision_requirement AS revisionRequirement,
                   rv.created_at AS reviewedAt,
                   COALESCE(t.real_name, t.username) AS teacherName
            FROM t_report_version v
            LEFT JOIN t_report_review rv
                ON rv.report_version_id = v.id
               AND rv.id = (
                   SELECT MAX(r2.id)
                   FROM t_report_review r2
                   WHERE r2.report_version_id = v.id
               )
            LEFT JOIN t_user t ON t.id = rv.teacher_id
            WHERE v.report_id = #{reportId}
            ORDER BY v.version_no DESC, v.id DESC
            """)
    List<ReportVersionItemVO> listReportVersions(@Param("reportId") Long reportId);

    @Select("""
            SELECT r.id AS reportId,
                   r.experiment_id AS experimentId,
                   e.title AS experimentTitle,
                   e.objective AS experimentObjective,
                   e.content_text AS experimentContentText,
                   c.id AS courseId,
                   c.course_name AS courseName,
                   COALESCE(rt.real_name, rt.username, ct.real_name, ct.username) AS teacherName,
                   r.student_id AS studentId,
                   su.student_no AS studentNo,
                   COALESCE(su.real_name, su.username) AS studentName,
                   r.status AS status,
                   v.id AS reportVersionId,
                   v.version_no AS versionNo,
                   v.content_html AS contentHtml,
                   v.content_text AS contentText,
                   v.attachment_ids AS attachmentIdsRaw,
                   v.word_count AS wordCount,
                   v.submitted_at AS submittedAt,
                   rv.score AS score,
                   rv.comment_text AS commentText,
                   rv.action AS action,
                   rv.revision_requirement AS revisionRequirement,
                   rv.created_at AS reviewedAt,
                   v.ai_task_id AS aiTaskId,
                   v.ai_risk_score AS aiRiskScore,
                   CAST(v.ai_result_json AS CHAR) AS aiResultJson,
                   d.draft_save_count AS draftSaveCount,
                   d.content_text AS draftContentText
            FROM t_report r
            INNER JOIN t_report_version v ON v.report_id = r.id
            INNER JOIN t_experiment e ON e.id = r.experiment_id
            INNER JOIN t_course c ON c.id = e.course_id
            LEFT JOIN t_report_draft d ON d.report_id = r.id
            LEFT JOIN t_report_review rv
                ON rv.report_version_id = v.id
               AND rv.id = (
                   SELECT MAX(r2.id)
                   FROM t_report_review r2
                   WHERE r2.report_version_id = v.id
               )
            LEFT JOIN t_user rt ON rt.id = rv.teacher_id
            LEFT JOIN t_user ct ON ct.teacher_no = c.teacher_no AND ct.deleted = 0
            LEFT JOIN t_user su ON su.id = r.student_id
            WHERE r.id = #{reportId} AND v.id = #{reportVersionId}
            LIMIT 1
            """)
    ReportVersionDetailVO getReportVersionDetail(@Param("reportId") Long reportId,
                                                 @Param("reportVersionId") Long reportVersionId);

    @Select("""
            SELECT v.id AS reportVersionId,
                   v.version_no AS versionNo,
                   v.content_html AS contentHtml,
                   v.content_text AS contentText,
                   v.submitted_at AS submittedAt
            FROM t_report_version v
            WHERE v.report_id = #{reportId}
              AND v.id <> #{reportVersionId}
            ORDER BY v.version_no DESC, v.id DESC
            LIMIT 5
            """)
    List<ReportVersionItemVO> listPreviousVersions(@Param("reportId") Long reportId,
                                                   @Param("reportVersionId") Long reportVersionId);

    @Select("""
            SELECT r.id AS reportId,
                   v.id AS reportVersionId,
                   COALESCE(su.real_name, su.username) AS studentName,
                   v.version_no AS versionNo,
                   v.word_count AS wordCount,
                   v.content_html AS contentHtml,
                   v.content_text AS contentText,
                   v.submitted_at AS submittedAt
            FROM t_report r
            INNER JOIN t_user su ON su.id = r.student_id
            INNER JOIN t_report_version v ON v.report_id = r.id
            WHERE r.experiment_id = #{experimentId}
              AND r.id <> #{currentReportId}
              AND v.id = (
                  SELECT MAX(v2.id)
                  FROM t_report_version v2
                  WHERE v2.report_id = r.id
              )
            ORDER BY v.submitted_at DESC, v.id DESC
            LIMIT 8
            """)
    List<ReportAnalysisPeerVersionVO> listPeerReportVersions(@Param("experimentId") Long experimentId,
                                                             @Param("currentReportId") Long currentReportId);

    @Select("""
            SELECT e.id AS experimentId,
                   e.title AS experimentTitle,
                   c.id AS courseId,
                   c.course_name AS courseName,
                   COUNT(DISTINCT su.id) AS totalStudents,
                   COUNT(DISTINCT CASE WHEN r.last_submitted_at IS NOT NULL THEN su.id END) AS submittedCount,
                   COUNT(DISTINCT CASE WHEN r.status = 'REVIEWED' THEN su.id END) AS reviewedCount,
                   COUNT(DISTINCT CASE WHEN r.status = 'SUBMITTED' THEN su.id END) AS pendingReviewCount,
                   COUNT(DISTINCT CASE WHEN r.status = 'REVISION_REQUIRED' THEN su.id END) AS revisionRequiredCount
            FROM t_experiment e
            INNER JOIN t_user teacher ON teacher.id = #{teacherId} AND teacher.teacher_no = e.creator_no
            INNER JOIN t_course c ON c.id = e.course_id
            LEFT JOIN t_course_member cm ON cm.course_id = c.id AND cm.role_in_course = 'STUDENT'
            LEFT JOIN t_user su ON su.student_no = cm.user_no AND su.deleted = 0
            LEFT JOIN t_report r ON r.experiment_id = e.id AND r.student_id = su.id
            WHERE e.deleted = 0
            GROUP BY e.id, e.title, c.id, c.course_name
            ORDER BY e.created_at DESC, e.id DESC
            """)
    List<TeacherReportTaskVO> listTeacherReportTasks(@Param("teacherId") Long teacherId);

    @Select("""
            SELECT su.id AS studentId,
                   su.student_no AS studentNo,
                   COALESCE(su.real_name, su.username) AS studentName,
                   r.id AS reportId,
                   COALESCE(r.status, 'UNSUBMITTED') AS status,
                   COALESCE(r.latest_version_no, 0) AS latestVersionNo,
                   r.final_score AS finalScore,
                   r.last_submitted_at AS lastSubmittedAt
            FROM t_experiment e
            INNER JOIN t_course_member cm ON cm.course_id = e.course_id AND cm.role_in_course = 'STUDENT'
            INNER JOIN t_user su ON su.student_no = cm.user_no AND su.deleted = 0
            LEFT JOIN t_report r ON r.experiment_id = e.id AND r.student_id = su.id
            WHERE e.id = #{experimentId}
            ORDER BY CASE WHEN r.last_submitted_at IS NULL THEN 1 ELSE 0 END,
                     r.last_submitted_at DESC,
                     su.student_no ASC
            """)
    List<TeacherReportStudentVO> listTeacherReportStudents(@Param("experimentId") Long experimentId);

    @Select("""
            SELECT COUNT(1)
            FROM t_report r
            INNER JOIN t_experiment e ON e.id = r.experiment_id
            INNER JOIN t_user t ON t.id = #{teacherId} AND t.teacher_no = e.creator_no
            WHERE r.id = #{reportId}
            """)
    Integer countTeacherResponsible(@Param("reportId") Long reportId, @Param("teacherId") Long teacherId);

    @Select("""
            SELECT id
            FROM t_report_version
            WHERE report_id = #{reportId}
            ORDER BY version_no DESC, id DESC
            LIMIT 1
            """)
    Long getLatestVersionId(@Param("reportId") Long reportId);

    @Update("""
            UPDATE t_report_version
            SET ai_risk_score = #{aiRiskScore},
                ai_result_json = #{aiResultJson}
            WHERE id = #{reportVersionId}
            """)
    int updateVersionAiResult(@Param("reportVersionId") Long reportVersionId,
                              @Param("aiRiskScore") Integer aiRiskScore,
                              @Param("aiResultJson") String aiResultJson);

}
