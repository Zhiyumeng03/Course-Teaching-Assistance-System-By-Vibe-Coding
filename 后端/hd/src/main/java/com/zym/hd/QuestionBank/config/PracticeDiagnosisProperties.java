package com.zym.hd.QuestionBank.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.practice-diagnosis")
public class PracticeDiagnosisProperties {

    private boolean enabled = true;

    private String pythonBaseUrl = "http://localhost:8002";

    private int connectTimeoutMs = 3000;

    private int readTimeoutMs = 60000;
}
