ALTER TABLE t_course
ADD COLUMN enroll_mode VARCHAR(20) NOT NULL DEFAULT 'REVIEW' COMMENT 'REVIEW/SECKILL' AFTER join_code,
ADD COLUMN enroll_capacity INT NULL COMMENT 'max enrollment seats for seckill' AFTER enroll_mode,
ADD COLUMN enroll_start_at DATETIME NULL COMMENT 'seckill start time' AFTER enroll_capacity,
ADD COLUMN enroll_end_at DATETIME NULL COMMENT 'seckill end time' AFTER enroll_start_at,
ADD COLUMN enroll_preheated_at DATETIME NULL COMMENT 'last redis preheat time' AFTER enroll_end_at;

