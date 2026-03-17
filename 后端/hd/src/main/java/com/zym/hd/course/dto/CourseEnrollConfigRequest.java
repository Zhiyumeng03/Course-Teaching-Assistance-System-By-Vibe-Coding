package com.zym.hd.course.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CourseEnrollConfigRequest {
    private String enrollMode;
    private Integer enrollCapacity;
    private LocalDateTime enrollStartAt;
    private LocalDateTime enrollEndAt;
}

