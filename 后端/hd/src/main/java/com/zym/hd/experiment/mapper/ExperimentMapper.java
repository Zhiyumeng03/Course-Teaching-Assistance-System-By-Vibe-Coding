package com.zym.hd.experiment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zym.hd.experiment.entity.ExperimentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExperimentMapper extends BaseMapper<ExperimentEntity> {

    @Update("""
            UPDATE t_experiment
            SET creator_no = #{newCreatorNo},
                updated_at = NOW()
            WHERE creator_no = #{oldCreatorNo}
            """)
    int updateCreatorNo(@Param("oldCreatorNo") String oldCreatorNo,
                        @Param("newCreatorNo") String newCreatorNo);
}

