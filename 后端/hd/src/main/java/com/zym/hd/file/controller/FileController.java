package com.zym.hd.file.controller;

import com.zym.hd.file.entity.FileEntity;
import com.zym.hd.file.service.FileService;
import com.zym.hd.file.service.OssService;
import com.zym.hd.security.SecurityContextUtil;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;
    private final OssService ossService;

    public FileController(FileService fileService, OssService ossService) {
        this.fileService = fileService;
        this.ossService = ossService;
    }

    @PostMapping("/upload")
    public FileEntity upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "bizType", required = false) String bizType,
                             @RequestParam(value = "bizId", required = false) Long bizId,
                             @RequestParam(value = "uploaderId", required = false) Long uploaderId) {
        return fileService.upload(file, bizType, bizId, uploaderId);
    }

    @PostMapping("/uploadAvatar")
    public Map<String, String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long currentUserId = SecurityContextUtil.currentUserId();
        String url = ossService.upload(file, "avatar");
        FileEntity fileEntity = fileService.createOssAsset(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename(),
                url,
                file.getContentType(),
                file.getSize(),
                "AVATAR",
                currentUserId,
                currentUserId);
        return Map.of(
                "id", String.valueOf(fileEntity.getId()),
                "url", fileEntity.getStoragePath() == null ? "" : fileEntity.getStoragePath(),
                "name", fileEntity.getOriginalName() == null ? "" : fileEntity.getOriginalName());
    }

    @GetMapping("/getById")
    public FileEntity getById(@RequestParam("id") Long id) {
        return fileService.getById(id);
    }

    @GetMapping("/listByIds")
    public List<FileEntity> listByIds(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return fileService.listByIdsPreserveOrder(ids);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return fileService.removeById(id);
    }
}

