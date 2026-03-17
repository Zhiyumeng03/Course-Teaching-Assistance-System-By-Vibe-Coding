package com.zym.hd.course.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseSeckillEnrollResponse {
    private String code;
    private String message;
    private boolean success;
    private Integer remainingStock;
}

