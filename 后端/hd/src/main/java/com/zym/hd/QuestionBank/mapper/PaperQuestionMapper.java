package com.zym.hd.QuestionBank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zym.hd.QuestionBank.entity.PaperQuestionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaperQuestionMapper extends BaseMapper<PaperQuestionEntity> {

    @Delete("DELETE FROM t_paper_question WHERE paper_id = #{paperId}")
    int deleteByPaperId(@Param("paperId") Long paperId);

    @Select("""
            SELECT id, paper_id, question_id, sort_no, score, question_snapshot_json
            FROM t_paper_question
            WHERE paper_id = #{paperId}
            ORDER BY sort_no ASC, id ASC
            """)
    List<PaperQuestionEntity> selectByPaperId(@Param("paperId") Long paperId);

    @Select("""
            SELECT COUNT(1)
            FROM t_paper_question
            WHERE paper_id = #{paperId}
            """)
    int countByPaperId(@Param("paperId") Long paperId);
}

