package com.zym.hd.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_file_asset")
public class FileEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private Long bizId;

    @TableField("original_name")
    private String originalName;

    @TableField("storage_path")
    private String storagePath;

    @TableField("mime_type")
    private String mimeType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("sha256")
    private String sha256;

    @TableField("uploader_id")
    private Long uploaderId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

