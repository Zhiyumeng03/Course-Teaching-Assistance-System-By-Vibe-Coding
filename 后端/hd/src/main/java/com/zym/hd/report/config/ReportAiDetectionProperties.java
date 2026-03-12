package com.zym.hd.report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.report-detection")
public class ReportAiDetectionProperties {

    private boolean enabled = true;

    private String pythonBaseUrl = "http://localhost:8003";

    private int connectTimeoutMs = 3000;

    private int readTimeoutMs = 120000;
}
