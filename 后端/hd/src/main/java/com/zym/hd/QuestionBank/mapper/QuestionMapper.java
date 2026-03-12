package com.zym.hd.QuestionBank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zym.hd.QuestionBank.dto.QuestionStatsDTO;
import com.zym.hd.QuestionBank.entity.QuestionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuestionMapper extends BaseMapper<QuestionEntity> {

    @Insert("""
            INSERT INTO t_question_kp_rel(question_id, knowledge_point_id)
            VALUES(#{questionId}, #{knowledgePointId})
            """)
    int insertQuestionKnowledgeRel(@Param("questionId") Long questionId,
                                   @Param("knowledgePointId") Long knowledgePointId);

    @Delete("""
            DELETE FROM t_question_kp_rel
            WHERE question_id = #{questionId}
            """)
    int deleteQuestionKnowledgeRel(@Param("questionId") Long questionId);

    @Select("""
            SELECT knowledge_point_id
            FROM t_question_kp_rel
            WHERE question_id = #{questionId}
            ORDER BY id ASC
            """)
    List<Long> selectKnowledgePointIds(@Param("questionId") Long questionId);

    List<QuestionEntity> selectPageRecords(@Param("courseId") Long courseId,
                                           @Param("viewerId") Long viewerId,
                                           @Param("viewerRole") String viewerRole,
                                           @Param("type") String type,
                                           @Param("reviewStatus") String reviewStatus,
                                           @Param("keyword") String keyword,
                                           @Param("offset") long offset,
                                           @Param("size") long size);

    long countPageRecords(@Param("courseId") Long courseId,
                          @Param("viewerId") Long viewerId,
                          @Param("viewerRole") String viewerRole,
                          @Param("type") String type,
                          @Param("reviewStatus") String reviewStatus,
                          @Param("keyword") String keyword);

    QuestionStatsDTO selectQuestionStats(@Param("courseId") Long courseId,
                                         @Param("viewerId") Long viewerId,
                                         @Param("viewerRole") String viewerRole);

    List<QuestionEntity> selectPendingReviewQuestions(@Param("courseId") Long courseId,
                                                      @Param("limit") int limit);
}

