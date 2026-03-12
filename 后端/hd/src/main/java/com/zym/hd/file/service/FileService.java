package com.zym.hd.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zym.hd.file.entity.FileEntity;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileService extends IService<FileEntity> {

    FileEntity upload(MultipartFile file, String bizType, Long bizId, Long uploaderId);

    FileEntity createOssAsset(String originalName,
                              String storagePath,
                              String mimeType,
                              Long fileSize,
                              String bizType,
                              Long bizId,
                              Long uploaderId);

    List<FileEntity> listByIdsPreserveOrder(List<Long> ids);
}

