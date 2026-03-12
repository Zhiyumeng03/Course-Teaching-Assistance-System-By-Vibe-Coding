package com.zym.hd.QuestionBank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zym.hd.QuestionBank.entity.KnowledgeEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeMapper extends BaseMapper<KnowledgeEntity> {

    @Select("""
            SELECT id, course_id, parent_id, name, level, sort_no, path
            FROM t_knowledge_point
            WHERE course_id = #{courseId}
            ORDER BY sort_no ASC, id ASC
            """)
    List<KnowledgeEntity> selectByCourseId(Long courseId);

    @Select("SELECT COALESCE(MAX(course_id), 0) FROM t_knowledge_point")
    Long selectMaxCourseId();
}

