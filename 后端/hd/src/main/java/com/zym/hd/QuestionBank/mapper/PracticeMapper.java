package com.zym.hd.QuestionBank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zym.hd.QuestionBank.entity.PracticeAnswerEntity;
import com.zym.hd.QuestionBank.entity.PracticeEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PracticeMapper extends BaseMapper<PracticeEntity> {

    @Insert("""
            INSERT INTO t_practice_answer(
                record_id, paper_question_id, question_id, answer_json, is_correct, score, ai_feedback_json
            ) VALUES(
                #{recordId}, #{paperQuestionId}, #{questionId}, #{answerJson}, #{isCorrect}, #{score}, #{aiFeedbackJson}
            )
            """)
    int insertAnswer(PracticeAnswerEntity entity);

    @Update("""
            UPDATE t_practice_answer
            SET answer_json = #{answerJson},
                is_correct = #{isCorrect},
                score = #{score},
                ai_feedback_json = #{aiFeedbackJson}
            WHERE id = #{id}
            """)
    int updateAnswer(PracticeAnswerEntity entity);

    @Select("""
            SELECT id AS id,
                   record_id AS recordId,
                   paper_question_id AS paperQuestionId,
                   question_id AS questionId,
                   answer_json AS answerJson,
                   is_correct AS isCorrect,
                   score AS score,
                   ai_feedback_json AS aiFeedbackJson
            FROM t_practice_answer
            WHERE record_id = #{recordId}
              AND paper_question_id = #{paperQuestionId}
            """)
    PracticeAnswerEntity selectAnswerByRecordIdAndPaperQuestionId(@Param("recordId") Long recordId,
                                                                  @Param("paperQuestionId") Long paperQuestionId);

    @Select("""
            SELECT pa.id AS id,
                   pa.record_id AS recordId,
                   pa.paper_question_id AS paperQuestionId,
                   pa.question_id AS questionId,
                   pa.answer_json AS answerJson,
                   pa.is_correct AS isCorrect,
                   pa.score AS score,
                   pa.ai_feedback_json AS aiFeedbackJson,
                   pq.sort_no AS sortNo,
                   pq.score AS paperQuestionScore,
                   pq.question_snapshot_json AS questionSnapshotJson
            FROM t_practice_answer pa
            LEFT JOIN t_paper_question pq
              ON pq.id = pa.paper_question_id
            WHERE pa.record_id = #{recordId}
            ORDER BY CASE WHEN pq.sort_no IS NULL THEN 1 ELSE 0 END,
                     pq.sort_no ASC,
                     pa.id ASC
            """)
    List<PracticeAnswerEntity> selectAnswersByRecordId(@Param("recordId") Long recordId);

    @Delete("DELETE FROM t_practice_answer WHERE record_id = #{recordId}")
    int deleteAnswersByRecordId(@Param("recordId") Long recordId);

    @Update("""
            UPDATE t_practice_answer pa
            JOIN t_paper_question pq
              ON pq.id = pa.paper_question_id
            SET pa.is_correct = CASE
                    WHEN JSON_UNQUOTE(JSON_EXTRACT(pq.question_snapshot_json, '$.type')) IN ('SINGLE', 'MULTI', 'JUDGE')
                     AND JSON_CONTAINS(COALESCE(pa.answer_json, JSON_OBJECT()), JSON_EXTRACT(pq.question_snapshot_json, '$.answer'))
                     AND JSON_CONTAINS(JSON_EXTRACT(pq.question_snapshot_json, '$.answer'), COALESCE(pa.answer_json, JSON_OBJECT()))
                     AND JSON_LENGTH(COALESCE(pa.answer_json, JSON_OBJECT())) = JSON_LENGTH(JSON_EXTRACT(pq.question_snapshot_json, '$.answer'))
                    THEN 1 ELSE 0
                END,
                pa.score = CASE
                    WHEN JSON_UNQUOTE(JSON_EXTRACT(pq.question_snapshot_json, '$.type')) IN ('SINGLE', 'MULTI', 'JUDGE')
                     AND JSON_CONTAINS(COALESCE(pa.answer_json, JSON_OBJECT()), JSON_EXTRACT(pq.question_snapshot_json, '$.answer'))
                     AND JSON_CONTAINS(JSON_EXTRACT(pq.question_snapshot_json, '$.answer'), COALESCE(pa.answer_json, JSON_OBJECT()))
                     AND JSON_LENGTH(COALESCE(pa.answer_json, JSON_OBJECT())) = JSON_LENGTH(JSON_EXTRACT(pq.question_snapshot_json, '$.answer'))
                    THEN pq.score ELSE 0
                END
            WHERE pa.record_id = #{recordId}
              AND JSON_UNQUOTE(JSON_EXTRACT(pq.question_snapshot_json, '$.type')) IN ('SINGLE', 'MULTI', 'JUDGE')
            """)
    int autoGradeObjectiveAnswers(@Param("recordId") Long recordId);

    @Select("""
            SELECT COALESCE(SUM(pa.score), 0)
            FROM t_practice_answer pa
            JOIN t_paper_question pq
              ON pq.id = pa.paper_question_id
            WHERE pa.record_id = #{recordId}
              AND JSON_UNQUOTE(JSON_EXTRACT(pq.question_snapshot_json, '$.type')) IN ('SINGLE', 'MULTI', 'JUDGE')
            """)
    Integer sumObjectiveScoreByRecordId(@Param("recordId") Long recordId);

    @Select("""
            SELECT COALESCE(SUM(score), 0)
            FROM t_practice_answer
            WHERE record_id = #{recordId}
            """)
    Integer sumTotalScoreByRecordId(@Param("recordId") Long recordId);

    @Update("""
            UPDATE t_practice_answer
            SET score = #{score},
                is_correct = #{isCorrect},
                ai_feedback_json = #{aiFeedbackJson}
            WHERE id = #{id}
            """)
    int updateReviewResult(PracticeAnswerEntity entity);

    @Update("""
            UPDATE t_practice_answer
            SET ai_feedback_json = #{aiFeedbackJson}
            WHERE id = #{answerId}
            """)
    int updateAiFeedback(@Param("answerId") Long answerId, @Param("aiFeedbackJson") String aiFeedbackJson);
}
