package com.zym.hd.course.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseSeckillMyStatusResponse {
    private Long courseId;
    private boolean selected;
    private Integer remainingStock;
    private String activityStatus;
}

