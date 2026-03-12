package com.zym.hd.report.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ReportAiDetectionResponse {

    private boolean success;

    private String model;

    private Integer overallAiRatio;

    private String riskLevel;

    private Integer confidence;

    private boolean needsHumanReview;

    private String summary;

    private List<String> explanations = new ArrayList<>();

    private Map<String, Object> featureSummary = new LinkedHashMap<>();

    private List<SuspiciousChunkItem> suspiciousChunks = new ArrayList<>();

    private String generatedAt;

    private String rawText;

    @Data
    public static class SuspiciousChunkItem {
        private Integer chunkIndex;
        private String chunkText;
        private Integer aiScore;
        private String riskLevel;
        private Integer confidence;
        private List<String> reasons = new ArrayList<>();
        private List<EvidenceReference> matchedRefs = new ArrayList<>();
    }

    @Data
    public static class EvidenceReference {
        private String sourceType;
        private String title;
        private Double similarity;
        private String note;
        private String excerpt;
    }
}
