package com.zym.hd.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zym.hd.file.entity.FileEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper extends BaseMapper<FileEntity> {
}

