SET NAMES utf8mb4;

create table t_ai_task
(
    id             bigint auto_increment
        primary key,
    task_type      varchar(50)                        null,
    biz_type       varchar(50)                        null,
    biz_id         bigint                             null,
    request_json   json                               null,
    status         varchar(20)                        null,
    retry_count    int      default 0                 null,
    next_retry_at  datetime                           null,
    result_json    json                               null,
    error_msg      text                               null,
    model_name     varchar(100)                       null,
    prompt_version varchar(50)                        null,
    started_at     datetime                           null,
    finished_at    datetime                           null,
    created_at     datetime default CURRENT_TIMESTAMP null
);

create index idx_ai_task_status
    on t_ai_task (status);

create table t_course
(
    id          bigint auto_increment
        primary key,
    course_code varchar(50)                           not null,
    course_name varchar(200)                          not null,
    term        varchar(50)                           null,
    description text                                  null,
    teacher_no  varchar(50)                           not null comment 'teacher number',
    join_code   varchar(20)                           null,
    status      varchar(20) default 'ACTIVE'          null,
    created_at  datetime    default CURRENT_TIMESTAMP null,
    updated_at  datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    deleted     tinyint     default 0                 null
);

create index idx_course_teacher
    on t_course (teacher_no);

create table t_course_member
(
    id             bigint auto_increment
        primary key,
    course_id      bigint                             not null,
    user_no        varchar(20)                        not null comment 'user number, T for teacher and S for student',
    role_in_course varchar(20)                        null comment 'TEACHER/STUDENT',
    joined_at      datetime default CURRENT_TIMESTAMP null,
    constraint uk_course_user
        unique (course_id, user_no)
);

create table t_experiment
(
    id             bigint auto_increment
        primary key,
    course_id      bigint                                not null,
    title          varchar(255)                          not null comment 'experiment title',
    objective      text                                  null comment 'experiment objective',
    content_html   longtext                              null comment 'rich text content with embedded assets',
    content_text   longtext                              null comment 'plain text content',
    attachment_ids varchar(255)                          null comment 'attachment id list',
    start_time     datetime                              null,
    deadline       datetime                              null,
    max_score      int         default 100               null,
    allow_resubmit tinyint     default 1                 null comment 'allow resubmission',
    status         varchar(20) default 'DRAFT'           null comment 'status: DRAFT, ACTIVE or FINISHED',
    creator_no     varchar(20)                           null comment 'creator number',
    created_at     datetime    default CURRENT_TIMESTAMP null,
    updated_at     datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    deleted        tinyint     default 0                 null
);

create index idx_experiment_course
    on t_experiment (course_id);

create table t_file_asset
(
    id            bigint auto_increment
        primary key,
    biz_type      varchar(50)                        null,
    biz_id        bigint                             null,
    original_name varchar(255)                       null,
    storage_path  varchar(255)                       null,
    mime_type     varchar(100)                       null,
    file_size     bigint                             null,
    sha256        varchar(64)                        null,
    uploader_id   bigint                             null,
    created_at    datetime default CURRENT_TIMESTAMP null
);

create table t_knowledge_point
(
    id          bigint auto_increment
        primary key,
    course_id   bigint       null,
    parent_id   bigint       null,
    name        varchar(200) null,
    description text         null,
    sort_no     int          null comment 'display order within the same level',
    level       int          null comment 'tree level',
    path        varchar(50)  null comment 'knowledge point path'
);

create table t_paper
(
    id               bigint auto_increment
        primary key,
    course_id        bigint                             null,
    creator_id       bigint                             null,
    title            varchar(255)                       null,
    paper_type       varchar(20)                        null,
    generation_mode  varchar(20)                        null,
    config_json      json                               null,
    total_score      int                                null,
    duration_minutes int                                null,
    status           varchar(20)                        null,
    created_at       datetime default CURRENT_TIMESTAMP null
);

create index idx_paper_course
    on t_paper (course_id);

create table t_paper_question
(
    id                     bigint auto_increment
        primary key,
    paper_id               bigint null,
    question_id            bigint null,
    sort_no                int    null,
    score                  int    null,
    question_snapshot_json json   null
);

create index idx_paper_question_paper
    on t_paper_question (paper_id);

create table t_practice_answer
(
    id                bigint auto_increment
        primary key,
    record_id         bigint  null,
    paper_question_id bigint  null,
    question_id       bigint  null,
    answer_json       json    null,
    is_correct        tinyint null,
    score             int     null,
    ai_feedback_json  json    null,
    constraint uk_practice_answer_record_question
        unique (record_id, paper_question_id)
);

create table t_practice_record
(
    id               bigint auto_increment
        primary key,
    paper_id         bigint                             null,
    student_id       bigint                             null,
    course_id        bigint                             null,
    status           varchar(20)                        null,
    started_at       datetime                           null,
    submitted_at     datetime                           null,
    objective_score  int                                null,
    total_score      int                                null,
    diagnosis_status varchar(20)                        null,
    diagnosis_json   json                               null,
    diagnosis_text   text                               null,
    created_at       datetime default CURRENT_TIMESTAMP null
);

create index idx_practice_course
    on t_practice_record (course_id);

create index idx_practice_student
    on t_practice_record (student_id);

create table t_question
(
    id            bigint auto_increment
        primary key,
    course_id     bigint                             null,
    creator_id    bigint                             null,
    creator_role  varchar(20)                        null,
    type          varchar(20)                        null,
    stem          text                               null,
    content_json  json                               null,
    answer_json   json                               null,
    analysis_text text                               null,
    difficulty    int                                null,
    source_type   varchar(20)                        null,
    visibility    varchar(20)                        null,
    review_status varchar(20)                        null,
    usage_count   int      default 0                 null,
    created_at    datetime default CURRENT_TIMESTAMP null,
    updated_at    datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    deleted       tinyint  default 0                 null
);

create index idx_question_course
    on t_question (course_id);

create table t_question_kp_rel
(
    id                 bigint auto_increment
        primary key,
    question_id        bigint null,
    knowledge_point_id bigint null
);

create table t_report
(
    id                bigint auto_increment
        primary key,
    experiment_id     bigint                                not null,
    student_id        bigint                                not null,
    status            varchar(20) default 'DRAFT'           null,
    latest_version_no int         default 0                 null,
    final_score       int                                   null,
    last_submitted_at datetime                              null,
    created_at        datetime    default CURRENT_TIMESTAMP null,
    updated_at        datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_exp_student
        unique (experiment_id, student_id)
);

create index idx_report_student
    on t_report (student_id);

create table t_report_draft
(
    id               bigint auto_increment
        primary key,
    report_id        bigint                             not null,
    content_html     longtext                           null,
    content_text     longtext                           null,
    draft_save_count int      default 0                 null,
    updated_at       datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    attachment_ids   varchar(255)                       null comment 'attachment id list',
    constraint uk_report_draft
        unique (report_id)
);

create table t_report_review
(
    id                   bigint auto_increment
        primary key,
    report_id            bigint                             null,
    report_version_id    bigint                             null,
    teacher_id           bigint                             null,
    score                int                                null,
    comment_text         text                               null,
    action               varchar(20)                        null,
    revision_requirement text                               null,
    created_at           datetime default CURRENT_TIMESTAMP null
);

create table t_report_version
(
    id             bigint auto_increment
        primary key,
    report_id      bigint                             not null,
    version_no     int                                not null,
    content_html   longtext                           null,
    content_text   longtext                           null,
    attachment_ids varchar(255)                       null comment 'attachment id list',
    word_count     int                                null,
    submitted_at   datetime                           null,
    ai_task_id     bigint                             null,
    ai_risk_score  int                                null,
    ai_result_json json                               null,
    created_at     datetime default CURRENT_TIMESTAMP null
);

create table t_user
(
    id            bigint auto_increment
        primary key,
    username      varchar(50)                        not null,
    password_hash varchar(255)                       not null,
    role          varchar(20)                        not null comment 'STUDENT, TEACHER or ADMIN',
    real_name     varchar(50)                        null,
    student_no    varchar(50)                        null,
    teacher_no    varchar(50)                        null,
    email         varchar(100)                       null,
    phone         varchar(30)                        null,
    avatar_url    varchar(255)                       null,
    status        tinyint  default 1                 null comment '1 active, 0 disabled',
    last_login_at datetime                           null,
    created_at    datetime default CURRENT_TIMESTAMP null,
    updated_at    datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    deleted       tinyint  default 0                 null,
    constraint username
        unique (username)
);
