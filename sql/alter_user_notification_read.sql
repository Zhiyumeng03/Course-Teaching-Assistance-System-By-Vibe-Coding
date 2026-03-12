CREATE TABLE IF NOT EXISTS t_user_notification_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    notification_id VARCHAR(128) NOT NULL,
    read_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_notification(user_id, notification_id),
    INDEX idx_user_notification_user(user_id)
);
