package com.zym.hd.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zym.hd.file.entity.FileEntity;
import com.zym.hd.file.mapper.FileMapper;
import com.zym.hd.file.service.FileService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, FileEntity> implements FileService {

    @Override
    public FileEntity upload(MultipartFile file, String bizType, Long bizId, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalName) && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        Path uploadDir = Paths.get("uploads");
        try {
            Files.createDirectories(uploadDir);
            String fileName = UUID.randomUUID() + ext;
            Path target = uploadDir.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            FileEntity entity = new FileEntity();
            entity.setBizType(bizType);
            entity.setBizId(bizId);
            entity.setOriginalName(originalName);
            entity.setStoragePath(target.toString().replace("\\", "/"));
            entity.setMimeType(file.getContentType());
            entity.setFileSize(file.getSize());
            entity.setSha256(calculateSha256(target));
            entity.setUploaderId(uploaderId);
            entity.setCreatedAt(LocalDateTime.now());
            save(entity);
            return entity;
        } catch (IOException e) {
            throw new RuntimeException("upload failed", e);
        }
    }

    @Override
    public FileEntity createOssAsset(String originalName,
                                     String storagePath,
                                     String mimeType,
                                     Long fileSize,
                                     String bizType,
                                     Long bizId,
                                     Long uploaderId) {
        FileEntity entity = new FileEntity();
        entity.setBizType(bizType);
        entity.setBizId(bizId);
        entity.setOriginalName(originalName);
        entity.setStoragePath(storagePath);
        entity.setMimeType(mimeType);
        entity.setFileSize(fileSize);
        entity.setUploaderId(uploaderId);
        entity.setCreatedAt(LocalDateTime.now());
        save(entity);
        return entity;
    }

    @Override
    public List<FileEntity> listByIdsPreserveOrder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, FileEntity> fileMap = listByIds(ids).stream()
                .collect(Collectors.toMap(
                        FileEntity::getId,
                        entity -> entity,
                        (left, right) -> left,
                        LinkedHashMap::new));
        return ids.stream()
                .map(fileMap::get)
                .filter(entity -> entity != null)
                .collect(Collectors.toList());
    }

    private String calculateSha256(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("calculate sha256 failed", e);
        }
    }
}

