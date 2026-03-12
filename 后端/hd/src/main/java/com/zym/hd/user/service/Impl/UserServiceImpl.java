package com.zym.hd.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zym.hd.QuestionBank.entity.PracticeEntity;
import com.zym.hd.QuestionBank.entity.QuestionEntity;
import com.zym.hd.QuestionBank.service.PracticeService;
import com.zym.hd.QuestionBank.service.QuestionService;
import com.zym.hd.course.entity.Course;
import com.zym.hd.course.entity.CourseMember;
import com.zym.hd.course.mapper.CourseMapper;
import com.zym.hd.course.service.CourseMemberService;
import com.zym.hd.course.service.CourseService;
import com.zym.hd.experiment.entity.ExperimentEntity;
import com.zym.hd.experiment.mapper.ExperimentMapper;
import com.zym.hd.experiment.service.ExperimentService;
import com.zym.hd.report.service.ReportService;
import com.zym.hd.report.vo.ReportVersionItemVO;
import com.zym.hd.report.vo.StudentReportItemVO;
import com.zym.hd.report.vo.TeacherReportStudentVO;
import com.zym.hd.report.vo.TeacherReportTaskVO;
import com.zym.hd.user.dto.UserHomeDashboardDTO;
import com.zym.hd.user.dto.UserHomeNotificationDTO;
import com.zym.hd.user.entity.UserNotificationReadEntity;
import com.zym.hd.user.entity.UserEntity;
import com.zym.hd.user.mapper.UserMapper;
import com.zym.hd.user.service.UserNotificationReadService;
import com.zym.hd.user.service.UserService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String MEMBER_ROLE_STUDENT = "STUDENT";
    private static final String MEMBER_ROLE_PENDING = "STUDENT under review";
    private static final String REPORT_STATUS_SUBMITTED = "SUBMITTED";
    private static final String REPORT_STATUS_REVIEWED = "REVIEWED";
    private static final String REPORT_STATUS_REVISION_REQUIRED = "REVISION_REQUIRED";
    private static final String QUESTION_STATUS_PENDING = "PENDING";
    private static final String QUESTION_STATUS_APPROVED = "APPROVED";
    private static final String PRACTICE_STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String PRACTICE_STATUS_GRADED = "GRADED";
    private static final int NOTIFICATION_LIMIT = 12;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final CourseService courseService;
    private final CourseMemberService courseMemberService;
    private final ExperimentService experimentService;
    private final ReportService reportService;
    private final QuestionService questionService;
    private final PracticeService practiceService;
    private final UserNotificationReadService userNotificationReadService;
    private final CourseMapper courseMapper;
    private final ExperimentMapper experimentMapper;

    public UserServiceImpl(CourseService courseService,
                           CourseMemberService courseMemberService,
                           @Lazy ExperimentService experimentService,
                           ReportService reportService,
                           QuestionService questionService,
                           @Lazy PracticeService practiceService,
                           UserNotificationReadService userNotificationReadService,
                           CourseMapper courseMapper,
                           ExperimentMapper experimentMapper) {
        this.courseService = courseService;
        this.courseMemberService = courseMemberService;
        this.experimentService = experimentService;
        this.reportService = reportService;
        this.questionService = questionService;
        this.practiceService = practiceService;
        this.userNotificationReadService = userNotificationReadService;
        this.courseMapper = courseMapper;
        this.experimentMapper = experimentMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserEntity register(UserEntity user, String rawPassword) {
        if (user == null) {
            throw new IllegalArgumentException("user is required");
        }
        String username = normalizeRequired(user.getUsername(), "username");
        String role = normalizeRole(user.getRole());
        if (!StringUtils.hasText(rawPassword) || rawPassword.trim().length() < 6) {
            throw new IllegalArgumentException("password must be at least 6 characters");
        }
        if (lambdaQuery().eq(UserEntity::getUsername, username).eq(UserEntity::getDeleted, 0).count() > 0) {
            throw new IllegalArgumentException("username already exists");
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setRole(role);
        entity.setRealName(normalizeRequired(user.getRealName(), "realName"));
        entity.setEmail(normalizeNullable(user.getEmail()));
        entity.setPhone(normalizeNullable(user.getPhone()));
        entity.setAvatarUrl(normalizeNullable(user.getAvatarUrl()));
        entity.setStatus(1);
        entity.setDeleted(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setPasswordHash(passwordEncoder.encode(rawPassword.trim()));

        if (ROLE_STUDENT.equals(role)) {
            String studentNo = normalizeRequired(user.getStudentNo(), "studentNo");
            ensureUniqueStudentNo(studentNo, null);
            entity.setStudentNo(studentNo);
            entity.setTeacherNo(null);
        } else if (ROLE_TEACHER.equals(role)) {
            String teacherNo = normalizeRequired(user.getTeacherNo(), "teacherNo");
            ensureUniqueTeacherNo(teacherNo, null);
            entity.setTeacherNo(teacherNo);
            entity.setStudentNo(null);
        } else {
            entity.setStudentNo(normalizeNullable(user.getStudentNo()));
            entity.setTeacherNo(normalizeNullable(user.getTeacherNo()));
        }

        save(entity);
        return sanitizeUser(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserEntity login(String username, String rawPassword) {
        String normalizedUsername = normalizeRequired(username, "username");
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("password is required");
        }
        UserEntity entity = lambdaQuery()
                .eq(UserEntity::getUsername, normalizedUsername)
                .eq(UserEntity::getDeleted, 0)
                .one();
        if (entity == null || !passwordEncoder.matches(rawPassword, entity.getPasswordHash())) {
            throw new IllegalArgumentException("username or password is incorrect");
        }
        if (entity.getStatus() != null && entity.getStatus() == 0) {
            throw new IllegalArgumentException("account is disabled");
        }

        entity.setRole(normalizeRole(entity.getRole()));
        entity.setLastLoginAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        updateById(entity);
        return sanitizeUser(entity);
    }

    @Override
    public UserEntity getProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        UserEntity entity = lambdaQuery()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0)
                .one();
        if (entity == null) {
            throw new IllegalArgumentException("user not found");
        }
        return sanitizeUser(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserEntity updateUser(UserEntity user, String newPassword) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("user id is required");
        }
        UserEntity db = getProfile(user.getId());
        String role = normalizeRole(db.getRole());
        String oldStudentNo = db.getStudentNo();
        String oldTeacherNo = db.getTeacherNo();

        if (user.getRealName() != null) {
            db.setRealName(normalizeRequired(user.getRealName(), "realName"));
        }
        if (user.getEmail() != null) {
            db.setEmail(normalizeNullable(user.getEmail()));
        }
        if (user.getPhone() != null) {
            db.setPhone(normalizeNullable(user.getPhone()));
        }
        if (user.getAvatarUrl() != null) {
            db.setAvatarUrl(normalizeNullable(user.getAvatarUrl()));
        }

        if (ROLE_STUDENT.equals(role) && user.getStudentNo() != null) {
            String studentNo = normalizeRequired(user.getStudentNo(), "studentNo");
            ensureUniqueStudentNo(studentNo, db.getId());
            db.setStudentNo(studentNo);
        }
        if (ROLE_TEACHER.equals(role) && user.getTeacherNo() != null) {
            String teacherNo = normalizeRequired(user.getTeacherNo(), "teacherNo");
            ensureUniqueTeacherNo(teacherNo, db.getId());
            db.setTeacherNo(teacherNo);
        }

        if (StringUtils.hasText(newPassword)) {
            String normalizedPassword = newPassword.trim();
            if (normalizedPassword.length() < 6) {
                throw new IllegalArgumentException("password must be at least 6 characters");
            }
            db.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        }

        db.setUpdatedAt(LocalDateTime.now());
        updateById(db);

        if (ROLE_STUDENT.equals(role)
                && StringUtils.hasText(oldStudentNo)
                && StringUtils.hasText(db.getStudentNo())
                && !Objects.equals(oldStudentNo, db.getStudentNo())) {
            courseMemberService.lambdaUpdate()
                    .eq(CourseMember::getUserNo, oldStudentNo)
                    .set(CourseMember::getUserNo, db.getStudentNo())
                    .update();
        }

        if (ROLE_TEACHER.equals(role)
                && StringUtils.hasText(oldTeacherNo)
                && StringUtils.hasText(db.getTeacherNo())
                && !Objects.equals(oldTeacherNo, db.getTeacherNo())) {
            courseMapper.updateTeacherNo(oldTeacherNo, db.getTeacherNo());
            experimentMapper.updateCreatorNo(oldTeacherNo, db.getTeacherNo());
        }

        return sanitizeUser(db);
    }

    @Override
    public UserEntity getByTeacherNo(String teacherNo) {
        String normalizedTeacherNo = normalizeNullable(teacherNo);
        if (!StringUtils.hasText(normalizedTeacherNo)) {
            return null;
        }
        return sanitizeUser(lambdaQuery()
                .eq(UserEntity::getTeacherNo, normalizedTeacherNo)
                .eq(UserEntity::getDeleted, 0)
                .one());
    }

    @Override
    public UserEntity getByStudentNo(String studentNo) {
        String normalizedStudentNo = normalizeNullable(studentNo);
        if (!StringUtils.hasText(normalizedStudentNo)) {
            return null;
        }
        return sanitizeUser(lambdaQuery()
                .eq(UserEntity::getStudentNo, normalizedStudentNo)
                .eq(UserEntity::getDeleted, 0)
                .one());
    }

    @Override
    public UserHomeDashboardDTO getHomeDashboard(Long userId) {
        UserEntity currentUser = getProfile(userId);
        String role = normalizeRole(currentUser.getRole());
        List<Course> courses = resolveCourses(currentUser, role);
        List<Long> courseIds = courses.stream()
                .map(Course::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        UserHomeDashboardDTO dashboard = new UserHomeDashboardDTO();
        dashboard.setRole(role);
        dashboard.setCourseCount(courses.size());

        if (ROLE_STUDENT.equals(role)) {
            long pendingExperimentCount = countPendingStudentExperiments(currentUser.getId(), courseIds);
            dashboard.setPendingExperimentCount(pendingExperimentCount);
            dashboard.setPendingReportReviewCount(null);
            dashboard.setQuestionCount(countStudentAccessibleQuestions(courseIds, currentUser.getId()));
            dashboard.setExperimentBadgeCount(pendingExperimentCount);
            dashboard.setReportBadgeCount(0L);
            dashboard.setQuestionBadgeCount(0L);
            dashboard.setPracticeBadgeCount(0L);
            return fillNotifications(currentUser.getId(), dashboard, buildStudentNotifications(currentUser, courses));
        }

        long pendingReportReviewCount = countTeacherPendingReportReviews(currentUser.getId());
        long pendingQuestionReviewCount = countTeacherPendingQuestionReviews(courseIds);
        long pendingPracticeReviewCount = countTeacherPendingPracticeReviews(courseIds);
        dashboard.setPendingExperimentCount(null);
        dashboard.setPendingReportReviewCount(pendingReportReviewCount);
        dashboard.setQuestionCount(countTeacherQuestionBankSize(courseIds));
        dashboard.setExperimentBadgeCount(0L);
        dashboard.setReportBadgeCount(pendingReportReviewCount);
        dashboard.setQuestionBadgeCount(pendingQuestionReviewCount);
        dashboard.setPracticeBadgeCount(pendingPracticeReviewCount);
        return fillNotifications(currentUser.getId(), dashboard, buildTeacherNotifications(currentUser, role, courses));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markNotificationRead(Long userId, String notificationId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        String normalizedNotificationId = normalizeNullable(notificationId);
        if (!StringUtils.hasText(normalizedNotificationId)) {
            throw new IllegalArgumentException("notificationId is required");
        }
        long existingCount = userNotificationReadService.lambdaQuery()
                .eq(UserNotificationReadEntity::getUserId, userId)
                .eq(UserNotificationReadEntity::getNotificationId, normalizedNotificationId)
                .count();
        if (existingCount > 0) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        UserNotificationReadEntity entity = new UserNotificationReadEntity();
        entity.setUserId(userId);
        entity.setNotificationId(normalizedNotificationId);
        entity.setReadAt(now);
        entity.setCreatedAt(now);
        try {
            return userNotificationReadService.save(entity);
        } catch (DuplicateKeyException ignored) {
            return true;
        }
    }

    private UserHomeDashboardDTO fillNotifications(Long userId,
                                                   UserHomeDashboardDTO dashboard,
                                                   List<UserHomeNotificationDTO> notifications) {
        List<UserHomeNotificationDTO> sortedNotifications = notifications.stream()
                .sorted(notificationComparator())
                .collect(Collectors.toList());
        List<String> notificationIds = sortedNotifications.stream()
                .map(UserHomeNotificationDTO::getId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        Set<String> readIds = notificationIds.isEmpty()
                ? Collections.emptySet()
                : userNotificationReadService.lambdaQuery()
                .eq(UserNotificationReadEntity::getUserId, userId)
                .in(UserNotificationReadEntity::getNotificationId, notificationIds)
                .list()
                .stream()
                .map(UserNotificationReadEntity::getNotificationId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        long unreadCount = 0L;
        for (UserHomeNotificationDTO notification : sortedNotifications) {
            boolean unread = !StringUtils.hasText(notification.getId()) || !readIds.contains(notification.getId());
            notification.setUnread(unread);
            if (unread) {
                unreadCount++;
            }
        }
        dashboard.setNotificationCount(unreadCount);
        dashboard.setNotifications(sortedNotifications.stream().limit(NOTIFICATION_LIMIT).collect(Collectors.toList()));
        return dashboard;
    }

    private List<Course> resolveCourses(UserEntity currentUser, String role) {
        if (ROLE_STUDENT.equals(role)) {
            if (!StringUtils.hasText(currentUser.getStudentNo())) {
                return Collections.emptyList();
            }
            List<Long> courseIds = courseMemberService.lambdaQuery()
                    .eq(CourseMember::getUserNo, currentUser.getStudentNo())
                    .eq(CourseMember::getRoleInCourse, MEMBER_ROLE_STUDENT)
                    .list()
                    .stream()
                    .map(CourseMember::getCourseId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (courseIds.isEmpty()) {
                return Collections.emptyList();
            }
            return courseService.lambdaQuery()
                    .in(Course::getId, courseIds)
                    .eq(Course::getDeleted, 0)
                    .orderByDesc(Course::getCreatedAt, Course::getId)
                    .list();
        }
        if (ROLE_ADMIN.equals(role) && !StringUtils.hasText(currentUser.getTeacherNo())) {
            return courseService.lambdaQuery()
                    .eq(Course::getDeleted, 0)
                    .orderByDesc(Course::getCreatedAt, Course::getId)
                    .list();
        }
        if (!StringUtils.hasText(currentUser.getTeacherNo())) {
            return Collections.emptyList();
        }
        return courseService.listByTeacherNo(currentUser.getTeacherNo());
    }

    private long countPendingStudentExperiments(Long studentId, List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return 0L;
        }
        List<ExperimentEntity> experiments = experimentService.lambdaQuery()
                .in(ExperimentEntity::getCourseId, courseIds)
                .eq(ExperimentEntity::getDeleted, 0)
                .ne(ExperimentEntity::getStatus, "DRAFT")
                .orderByDesc(ExperimentEntity::getCreatedAt, ExperimentEntity::getId)
                .list();
        if (experiments.isEmpty()) {
            return 0L;
        }
        Map<Long, StudentReportItemVO> reportsByExperimentId = reportService.getStudentReportDashboard(studentId)
                .stream()
                .collect(Collectors.toMap(
                        StudentReportItemVO::getExperimentId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new));
        return experiments.stream()
                .filter(experiment -> requiresStudentSubmission(reportsByExperimentId.get(experiment.getId())))
                .count();
    }

    private long countTeacherPendingReportReviews(Long teacherId) {
        return reportService.getTeacherReportDashboard(teacherId)
                .stream()
                .mapToLong(task -> safeLong(task.getPendingReviewCount()))
                .sum();
    }

    private long countTeacherPendingQuestionReviews(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return 0L;
        }
        return questionService.lambdaQuery()
                .in(QuestionEntity::getCourseId, courseIds)
                .eq(QuestionEntity::getDeleted, 0)
                .eq(QuestionEntity::getReviewStatus, QUESTION_STATUS_PENDING)
                .eq(QuestionEntity::getCreatorRole, ROLE_STUDENT)
                .count();
    }

    private long countTeacherPendingPracticeReviews(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return 0L;
        }
        return practiceService.lambdaQuery()
                .in(PracticeEntity::getCourseId, courseIds)
                .eq(PracticeEntity::getStatus, PRACTICE_STATUS_PENDING_REVIEW)
                .count();
    }

    private long countTeacherQuestionBankSize(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return 0L;
        }
        return questionService.lambdaQuery()
                .in(QuestionEntity::getCourseId, courseIds)
                .eq(QuestionEntity::getDeleted, 0)
                .count();
    }

    private long countStudentAccessibleQuestions(List<Long> courseIds, Long userId) {
        if (courseIds.isEmpty()) {
            return 0L;
        }
        return questionService.lambdaQuery()
                .in(QuestionEntity::getCourseId, courseIds)
                .eq(QuestionEntity::getDeleted, 0)
                .and(wrapper -> wrapper
                        .and(inner -> inner
                                .eq(QuestionEntity::getReviewStatus, QUESTION_STATUS_APPROVED)
                                .and(visible -> visible
                                        .ne(QuestionEntity::getVisibility, "false")
                                        .or()
                                        .isNull(QuestionEntity::getVisibility)))
                        .or()
                        .eq(QuestionEntity::getCreatorId, userId))
                .count();
    }

    private List<UserHomeNotificationDTO> buildTeacherNotifications(UserEntity currentUser,
                                                                    String role,
                                                                    List<Course> courses) {
        if (courses.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Course> courseMap = courses.stream()
                .filter(course -> course.getId() != null)
                .collect(Collectors.toMap(Course::getId, course -> course, (left, right) -> left, LinkedHashMap::new));
        List<Long> courseIds = new ArrayList<>(courseMap.keySet());
        Map<Long, UserEntity> userIdCache = new HashMap<>();
        Map<String, UserEntity> studentNoCache = new HashMap<>();
        List<UserHomeNotificationDTO> notifications = new ArrayList<>();

        List<CourseMember> pendingMembers = courseMemberService.lambdaQuery()
                .in(CourseMember::getCourseId, courseIds)
                .eq(CourseMember::getRoleInCourse, MEMBER_ROLE_PENDING)
                .orderByDesc(CourseMember::getJoinedAt)
                .list();
        for (CourseMember member : pendingMembers) {
            Course course = courseMap.get(member.getCourseId());
            UserEntity student = getStudentByNoCached(studentNoCache, member.getUserNo());
            notifications.add(createNotification(
                    "course-join-" + member.getId(),
                    "COURSE",
                    "warning",
                    "学生申请加入课程",
                    displayName(student, member.getUserNo()) + " 申请加入《" + courseName(course) + "》课程，等待你审核。",
                    member.getJoinedAt(),
                    "/course/" + member.getCourseId()));
        }

        List<QuestionEntity> pendingQuestions = questionService.lambdaQuery()
                .in(QuestionEntity::getCourseId, courseIds)
                .eq(QuestionEntity::getDeleted, 0)
                .eq(QuestionEntity::getReviewStatus, QUESTION_STATUS_PENDING)
                .eq(QuestionEntity::getCreatorRole, ROLE_STUDENT)
                .orderByDesc(QuestionEntity::getCreatedAt, QuestionEntity::getId)
                .list();
        for (QuestionEntity question : pendingQuestions) {
            Course course = courseMap.get(question.getCourseId());
            UserEntity creator = getUserByIdCached(userIdCache, question.getCreatorId());
            notifications.add(createNotification(
                    "question-review-" + question.getId(),
                    "QUESTION",
                    "info",
                    "学生提交审核题目",
                    displayName(creator, "学生") + " 向《" + courseName(course) + "》提交了题目“" + shorten(question.getStem(), 18) + "”，请及时审核。",
                    firstNonNull(question.getUpdatedAt(), question.getCreatedAt()),
                    "/home?menu=question-bank"));
        }

        List<TeacherReportTaskVO> reportTasks = reportService.getTeacherReportDashboard(currentUser.getId());
        for (TeacherReportTaskVO task : reportTasks) {
            for (TeacherReportStudentVO student : task.getStudents()) {
                if (!REPORT_STATUS_SUBMITTED.equalsIgnoreCase(student.getStatus())) {
                    continue;
                }
                notifications.add(createNotification(
                        "report-review-" + student.getReportId(),
                        "REPORT",
                        "warning",
                        "学生提交实验报告",
                        valueOrDefault(student.getStudentName(), "学生")
                                + " 已提交《" + valueOrDefault(task.getExperimentTitle(), "实验") + "》报告，请尽快批阅。",
                        student.getLastSubmittedAt(),
                        reportDetailPath(student.getReportId(), findLatestVersionId(student.getVersions()), "/home?menu=report")));
            }
        }

        for (Long courseId : courseIds) {
            List<Map<String, Object>> records = practiceService.listTeacherRecords(courseId, currentUser.getId(), role);
            for (Map<String, Object> record : records) {
                if (!PRACTICE_STATUS_PENDING_REVIEW.equalsIgnoreCase(stringValue(record.get("status")))) {
                    continue;
                }
                notifications.add(createNotification(
                        "practice-review-" + record.get("id"),
                        "PRACTICE",
                        "warning",
                        "学生完成测验请批阅",
                        valueOrDefault(stringValue(record.get("studentName")), "学生")
                                + " 已完成《" + valueOrDefault(stringValue(record.get("paperTitle")), "试卷") + "》测验，请及时批阅。",
                        timeValue(record.get("submittedAt")),
                        "/home?menu=practice&courseId=" + courseId));
            }
        }

        return notifications;
    }

    private List<UserHomeNotificationDTO> buildStudentNotifications(UserEntity currentUser, List<Course> courses) {
        Map<Long, Course> courseMap = courses.stream()
                .filter(course -> course.getId() != null)
                .collect(Collectors.toMap(Course::getId, course -> course, (left, right) -> left, LinkedHashMap::new));
        List<Long> courseIds = new ArrayList<>(courseMap.keySet());
        List<UserHomeNotificationDTO> notifications = new ArrayList<>();

        if (StringUtils.hasText(currentUser.getStudentNo())) {
            List<CourseMember> approvedMembers = courseMemberService.lambdaQuery()
                    .eq(CourseMember::getUserNo, currentUser.getStudentNo())
                    .eq(CourseMember::getRoleInCourse, MEMBER_ROLE_STUDENT)
                    .orderByDesc(CourseMember::getJoinedAt)
                    .list();
            for (CourseMember member : approvedMembers) {
                Course course = courseMap.get(member.getCourseId());
                if (course == null) {
                    course = courseService.getById(member.getCourseId());
                }
                if (course == null || course.getDeleted() != null && course.getDeleted() == 1) {
                    continue;
                }
                notifications.add(createNotification(
                        "course-approved-" + member.getId(),
                        "COURSE",
                        "success",
                        "课程审核已通过",
                        "你已成功加入《" + courseName(course) + "》课程，可以开始学习了。",
                        member.getJoinedAt(),
                        "/course/" + member.getCourseId()));
            }
        }

        if (!courseIds.isEmpty()) {
            List<ExperimentEntity> experiments = experimentService.lambdaQuery()
                    .in(ExperimentEntity::getCourseId, courseIds)
                    .eq(ExperimentEntity::getDeleted, 0)
                    .ne(ExperimentEntity::getStatus, "DRAFT")
                    .orderByDesc(ExperimentEntity::getCreatedAt, ExperimentEntity::getId)
                    .list();
            for (ExperimentEntity experiment : experiments) {
                Course course = courseMap.get(experiment.getCourseId());
                notifications.add(createNotification(
                        "experiment-published-" + experiment.getId(),
                        "EXPERIMENT",
                        "info",
                        "教师新发布实验",
                        "《" + courseName(course) + "》发布了实验“" + valueOrDefault(experiment.getTitle(), "未命名实验") + "”，记得按时完成。",
                        firstNonNull(experiment.getUpdatedAt(), experiment.getCreatedAt()),
                        "/experiment/detail/" + experiment.getId()));
            }
        }

        List<QuestionEntity> approvedQuestions = questionService.lambdaQuery()
                .eq(QuestionEntity::getCreatorId, currentUser.getId())
                .eq(QuestionEntity::getDeleted, 0)
                .eq(QuestionEntity::getReviewStatus, QUESTION_STATUS_APPROVED)
                .orderByDesc(QuestionEntity::getUpdatedAt, QuestionEntity::getId)
                .list();
        for (QuestionEntity question : approvedQuestions) {
            notifications.add(createNotification(
                    "question-approved-" + question.getId(),
                    "QUESTION",
                    "success",
                    "题目审核已通过",
                    "你提交的题目“" + shorten(question.getStem(), 18) + "”已通过审核，可以在题库中查看。",
                    firstNonNull(question.getUpdatedAt(), question.getCreatedAt()),
                    "/home?menu=question-bank"));
        }

        List<StudentReportItemVO> reportItems = reportService.getStudentReportDashboard(currentUser.getId());
        for (StudentReportItemVO item : reportItems) {
            if (!REPORT_STATUS_REVIEWED.equalsIgnoreCase(item.getStatus())
                    && !REPORT_STATUS_REVISION_REQUIRED.equalsIgnoreCase(item.getStatus())) {
                continue;
            }
            ReportVersionItemVO reviewedVersion = findLatestReviewedVersion(item.getVersions());
            boolean revisionRequired = REPORT_STATUS_REVISION_REQUIRED.equalsIgnoreCase(item.getStatus());
            notifications.add(createNotification(
                    "report-reviewed-" + item.getReportId(),
                    "REPORT",
                    revisionRequired ? "warning" : "success",
                    revisionRequired ? "实验报告需要修改" : "实验报告已批阅",
                    revisionRequired
                            ? "《" + valueOrDefault(item.getExperimentTitle(), "实验") + "》报告已退回修改，请查看批注后完善内容。"
                            : "《" + valueOrDefault(item.getExperimentTitle(), "实验") + "》报告已完成批阅，点击查看详情。",
                    reviewedVersion == null ? item.getLastSubmittedAt() : reviewedVersion.getReviewedAt(),
                    reviewedVersion == null
                            ? "/home?menu=report"
                            : reportDetailPath(item.getReportId(), reviewedVersion.getReportVersionId(), "/home?menu=report")));
        }

        for (Long courseId : courseIds) {
            List<Map<String, Object>> records = practiceService.listStudentRecords(courseId, currentUser.getId());
            for (Map<String, Object> record : records) {
                if (!PRACTICE_STATUS_GRADED.equalsIgnoreCase(stringValue(record.get("status")))) {
                    continue;
                }
                notifications.add(createNotification(
                        "practice-graded-" + record.get("id"),
                        "PRACTICE",
                        "success",
                        "测验已完成批阅",
                        "《" + valueOrDefault(stringValue(record.get("paperTitle")), "试卷") + "》测验已完成批阅，点击查看成绩与诊断。",
                        timeValue(record.get("submittedAt")),
                        "/home?menu=practice&courseId=" + courseId));
            }
        }

        return notifications;
    }

    private void ensureUniqueStudentNo(String studentNo, Long selfId) {
        if (!StringUtils.hasText(studentNo)) {
            return;
        }
        UserEntity existing = lambdaQuery()
                .eq(UserEntity::getStudentNo, studentNo)
                .eq(UserEntity::getDeleted, 0)
                .one();
        if (existing != null && !Objects.equals(existing.getId(), selfId)) {
            throw new IllegalArgumentException("studentNo already exists");
        }
    }

    private void ensureUniqueTeacherNo(String teacherNo, Long selfId) {
        if (!StringUtils.hasText(teacherNo)) {
            return;
        }
        UserEntity existing = lambdaQuery()
                .eq(UserEntity::getTeacherNo, teacherNo)
                .eq(UserEntity::getDeleted, 0)
                .one();
        if (existing != null && !Objects.equals(existing.getId(), selfId)) {
            throw new IllegalArgumentException("teacherNo already exists");
        }
    }

    private UserEntity sanitizeUser(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        entity.setRole(normalizeRole(entity.getRole()));
        return entity;
    }

    private Comparator<UserHomeNotificationDTO> notificationComparator() {
        return Comparator
                .comparing(UserHomeNotificationDTO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UserHomeNotificationDTO::getId, Comparator.nullsLast(String::compareTo));
    }

    private UserHomeNotificationDTO createNotification(String id,
                                                       String category,
                                                       String level,
                                                       String title,
                                                       String content,
                                                       LocalDateTime createdAt,
                                                       String path) {
        UserHomeNotificationDTO notification = new UserHomeNotificationDTO();
        notification.setId(id);
        notification.setCategory(category);
        notification.setLevel(level);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setCreatedAt(createdAt == null ? LocalDateTime.now() : createdAt);
        notification.setPath(path);
        return notification;
    }

    private boolean requiresStudentSubmission(StudentReportItemVO reportItem) {
        if (reportItem == null) {
            return true;
        }
        String status = normalizeUpper(reportItem.getStatus());
        return !REPORT_STATUS_SUBMITTED.equals(status) && !REPORT_STATUS_REVIEWED.equals(status);
    }

    private String normalizeRole(String role) {
        String normalized = normalizeUpper(role);
        if (!StringUtils.hasText(normalized)) {
            return ROLE_STUDENT;
        }
        if ("TEACHER".equals(normalized) || "TEACHER".equals(normalized.replace("_", ""))) {
            return ROLE_TEACHER;
        }
        if (ROLE_ADMIN.equals(normalized)) {
            return ROLE_ADMIN;
        }
        return ROLE_STUDENT;
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalizeNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String courseName(Course course) {
        return course == null ? "课程" : valueOrDefault(course.getCourseName(), "课程");
    }

    private String displayName(UserEntity user, String fallback) {
        if (user == null) {
            return valueOrDefault(fallback, "用户");
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return valueOrDefault(fallback, "用户");
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String shorten(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(1, maxLength - 1)) + "...";
    }

    private UserEntity getUserByIdCached(Map<Long, UserEntity> cache, Long userId) {
        if (userId == null) {
            return null;
        }
        return cache.computeIfAbsent(userId, this::getById);
    }

    private UserEntity getStudentByNoCached(Map<String, UserEntity> cache, String studentNo) {
        if (!StringUtils.hasText(studentNo)) {
            return null;
        }
        return cache.computeIfAbsent(studentNo, this::getByStudentNo);
    }

    private Long findLatestVersionId(List<ReportVersionItemVO> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        return versions.get(0).getReportVersionId();
    }

    private ReportVersionItemVO findLatestReviewedVersion(List<ReportVersionItemVO> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        return versions.stream()
                .filter(version -> version.getReviewedAt() != null)
                .findFirst()
                .orElse(versions.get(0));
    }

    private String reportDetailPath(Long reportId, Long reportVersionId, String fallback) {
        if (reportId == null || reportVersionId == null) {
            return fallback;
        }
        return "/report/" + reportId + "/version/" + reportVersionId;
    }

    private LocalDateTime firstNonNull(LocalDateTime primary, LocalDateTime fallback) {
        return primary != null ? primary : fallback;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime timeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime time) {
            return time;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }
}
