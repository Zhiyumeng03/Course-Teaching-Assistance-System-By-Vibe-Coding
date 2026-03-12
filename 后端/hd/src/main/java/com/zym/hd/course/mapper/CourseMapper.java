package com.zym.hd.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zym.hd.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    @Update("""
            UPDATE t_course
            SET teacher_no = #{newTeacherNo},
                updated_at = NOW()
            WHERE teacher_no = #{oldTeacherNo}
            """)
    int updateTeacherNo(@Param("oldTeacherNo") String oldTeacherNo,
                        @Param("newTeacherNo") String newTeacherNo);
}

