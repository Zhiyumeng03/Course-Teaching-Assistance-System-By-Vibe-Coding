package com.zym.hd.course.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseSeckillListItemResponse {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String term;
    private String description;
    private String teacherNo;
    private Integer enrollCapacity;
    private Integer remainingStock;
    private String activityStatus;
    private LocalDateTime enrollStartAt;
    private LocalDateTime enrollEndAt;
    private boolean selected;
}

