package com.zym.hd.file.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 阿里云 OSS 上传服务
 * <p>
 * 配置读取顺序：
 * 1. Spring 配置属性，例如 oss.access-key-id
 * 2. 系统环境变量，例如 OSS_ACCESS_KEY_ID
 * 3. JVM 启动参数，例如 -DOSS_ACCESS_KEY_ID=xxx
 */
@Service
public class OssService {

    private final Environment environment;

    public OssService(Environment environment) {
        this.environment = environment;
    }

    /**
     * 上传文件到 OSS
     *
     * @param file    上传的文件
     * @param bizType 业务类型，例如 report
     * @return 文件访问 URL
     */
    public String upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }

        String bucket = getRequiredConfig("oss.bucket", "OSS_BUCKET");
        String region = getRequiredConfig("oss.region", "OSS_REGION");
        String endpoint = getOptionalConfig("oss.endpoint", "OSS_ENDPOINT");
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "https://oss-" + region + ".aliyuncs.com";
        }

        String accessKeyId = getRequiredConfig("oss.access-key-id", "OSS_ACCESS_KEY_ID");
        String accessKeySecret = getRequiredConfig("oss.access-key-secret", "OSS_ACCESS_KEY_SECRET");

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String objectKey = String.format("%s/%s/%s%s",
                bizType,
                LocalDate.now(),
                UUID.randomUUID(),
                ext);

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getSize());
            if (file.getContentType() != null) {
                meta.setContentType(file.getContentType());
            }
            ossClient.putObject(bucket, objectKey, file.getInputStream(), meta);

            return String.format("https://%s.%s/%s", bucket, endpoint.replace("https://", ""), objectKey);
        } catch (IOException e) {
            throw new RuntimeException("OSS upload failed", e);
        } finally {
            ossClient.shutdown();
        }
    }

    private String getRequiredConfig(String propertyKey, String envKey) {
        String value = getOptionalConfig(propertyKey, envKey);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "OSS config missing: set property '" + propertyKey + "' or environment variable '" + envKey + "'");
        }
        return value;
    }

    private String getOptionalConfig(String propertyKey, String envKey) {
        String value = environment.getProperty(propertyKey);
        if (value == null || value.isBlank()) {
            value = environment.getProperty(envKey);
        }
        if (value == null || value.isBlank()) {
            value = System.getProperty(envKey);
        }
        return value == null ? null : value.trim();
    }
}
