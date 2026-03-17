package com.zym.hd.course.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseSeckillStatsResponse {
    private Long courseId;
    private String enrollMode;
    private Integer enrollCapacity;
    private Integer enrolledCount;
    private Integer remainingStock;
    private boolean preheated;
    private String activityStatus;
    private LocalDateTime enrollStartAt;
    private LocalDateTime enrollEndAt;
    private LocalDateTime enrollPreheatedAt;
}

