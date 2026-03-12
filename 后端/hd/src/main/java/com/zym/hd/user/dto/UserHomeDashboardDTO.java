package com.zym.hd.user.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class UserHomeDashboardDTO {

    private String role;
    private long courseCount;
    private Long pendingExperimentCount;
    private Long pendingReportReviewCount;
    private long questionCount;
    private long notificationCount;
    private long experimentBadgeCount;
    private long reportBadgeCount;
    private long questionBadgeCount;
    private long practiceBadgeCount;
    private List<UserHomeNotificationDTO> notifications = new ArrayList<>();
}
