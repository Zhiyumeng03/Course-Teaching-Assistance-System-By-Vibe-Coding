package com.zym.hd.QuestionBank.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.question")
public class AiQuestionProperties {

    private boolean enabled = true;

    private String pythonBaseUrl = "http://localhost:8001";

    private int connectTimeoutMs = 3000;

    private int readTimeoutMs = 60000;

    private int maxCount = 10;
}