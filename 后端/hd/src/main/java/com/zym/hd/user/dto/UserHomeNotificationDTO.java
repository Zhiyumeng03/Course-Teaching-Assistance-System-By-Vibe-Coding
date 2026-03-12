package com.zym.hd.user.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserHomeNotificationDTO {

    private String id;
    private boolean unread;
    private String category;
    private String level;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private String path;
}
