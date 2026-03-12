CREATE DATABASE course_assist
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE course_assist;

-- �û���
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    role VARCHAR(20) NOT NULL COMMENT 'STUDENT/TEACHER/ADMIN',

    real_name VARCHAR(50),
    student_no VARCHAR(50),
    teacher_no VARCHAR(50),

    email VARCHAR(100),
    phone VARCHAR(30),
    avatar_url VARCHAR(255),

    status TINYINT DEFAULT 1 COMMENT '1���� 0����',

    last_login_at DATETIME,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- �û�֪ͨ�Ѷ���¼
CREATE TABLE t_user_notification_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    notification_id VARCHAR(128) NOT NULL,
    read_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_notification(user_id, notification_id),
    INDEX idx_user_notification_user(user_id)
);

-- 用户�?


-- 课程�?
CREATE TABLE t_course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(200) NOT NULL,

    term VARCHAR(50),
    description TEXT,

    teacher_id BIGINT NOT NULL,

    join_code VARCHAR(20),

    status VARCHAR(20) DEFAULT 'ACTIVE',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 课程成员
CREATE TABLE t_course_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    role_in_course VARCHAR(20) COMMENT 'TEACHER/STUDENT',

    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_course_user(course_id,user_id)
);

-- 文件管理
CREATE TABLE t_file_asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    biz_type VARCHAR(50),
    biz_id BIGINT,

    original_name VARCHAR(255),
    storage_path VARCHAR(255),

    mime_type VARCHAR(100),
    file_size BIGINT,

    sha256 VARCHAR(64),

    uploader_id BIGINT,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 实验任务
CREATE TABLE t_experiment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    course_id BIGINT NOT NULL,

    title VARCHAR(255) NOT NULL,

    objective TEXT,
    content_html LONGTEXT,
    content_text LONGTEXT,

    start_time DATETIME,
    deadline DATETIME,

    max_score INT DEFAULT 100,

    allow_resubmit TINYINT DEFAULT 1,

    status VARCHAR(20) DEFAULT 'DRAFT',

    creator_id BIGINT,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 报告主表
CREATE TABLE t_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    experiment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,

    status VARCHAR(20) DEFAULT 'DRAFT',

    latest_version_no INT DEFAULT 0,

    final_score INT,

    last_submitted_at DATETIME,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_exp_student(experiment_id,student_id)
);

-- 报告草稿
CREATE TABLE t_report_draft (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    report_id BIGINT NOT NULL,

    content_html LONGTEXT,
    content_text LONGTEXT,

    draft_save_count INT DEFAULT 0,

    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_report_draft(report_id)
);

-- 报告版本
CREATE TABLE t_report_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    report_id BIGINT NOT NULL,

    version_no INT NOT NULL,

    content_html LONGTEXT,
    content_text LONGTEXT,

    word_count INT,

    submitted_at DATETIME,

    ai_task_id BIGINT,

    ai_risk_score INT,

    ai_result_json JSON,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 教师批改记录
CREATE TABLE t_report_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    report_id BIGINT,
    report_version_id BIGINT,

    teacher_id BIGINT,

    score INT,

    comment_text TEXT,

    annotation_json JSON,

    action VARCHAR(20),

    revision_requirement TEXT,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 题目知识�?
CREATE TABLE t_knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    course_id BIGINT,

    parent_id BIGINT,

    name VARCHAR(200),

    description TEXT,

    sort_no INT
);

-- 题目
CREATE TABLE t_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    course_id BIGINT,

    creator_id BIGINT,

    creator_role VARCHAR(20),

    type VARCHAR(20),

    stem TEXT,

    content_json JSON,

    answer_json JSON,

    analysis_text TEXT,

    difficulty INT,

    source_type VARCHAR(20),

    visibility VARCHAR(20),

    review_status VARCHAR(20),

    usage_count INT DEFAULT 0,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 题目知识点关�?
CREATE TABLE t_question_kp_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    question_id BIGINT,
    knowledge_point_id BIGINT
);

-- 试卷
CREATE TABLE t_paper (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    course_id BIGINT,

    creator_id BIGINT,

    title VARCHAR(255),

    paper_type VARCHAR(20),

    generation_mode VARCHAR(20),

    config_json JSON,

    total_score INT,

    duration_minutes INT,

    status VARCHAR(20),

    startt_time DATETIME,
    end_time DATETIME,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 试卷题目
CREATE TABLE t_paper_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    paper_id BIGINT,

    question_id BIGINT,

    sort_no INT,

    score INT,

    question_snapshot_json JSON
);

-- 学生练习记录
CREATE TABLE t_practice_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    paper_id BIGINT,

    student_id BIGINT,

    course_id BIGINT,

    status VARCHAR(20),

    started_at DATETIME,
    submitted_at DATETIME,

    objective_score INT,
    total_score INT,

    diagnosis_status VARCHAR(20),

    diagnosis_json JSON,

    diagnosis_text TEXT,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 学生答案
CREATE TABLE t_practice_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    record_id BIGINT,

    paper_question_id BIGINT,

    question_id BIGINT,

    answer_json JSON,

    is_correct TINYINT,

    score INT,

    ai_feedback_json JSON
);

-- ai任务�?
CREATE TABLE t_ai_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    task_type VARCHAR(50),

    biz_type VARCHAR(50),

    biz_id BIGINT,

    request_json JSON,

    status VARCHAR(20),

    retry_count INT DEFAULT 0,

    next_retry_at DATETIME,

    result_json JSON,

    error_msg TEXT,

    model_name VARCHAR(100),

    prompt_version VARCHAR(50),

    started_at DATETIME,

    finished_at DATETIME,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 建立索引
CREATE INDEX idx_course_teacher ON t_course(teacher_id);

CREATE INDEX idx_experiment_course ON t_experiment(course_id);

CREATE INDEX idx_report_student ON t_report(student_id);

CREATE INDEX idx_question_course ON t_question(course_id);

CREATE INDEX idx_paper_course ON t_paper(course_id);

CREATE INDEX idx_practice_student ON t_practice_record(student_id);

CREATE INDEX idx_ai_task_status ON t_ai_task(status);





