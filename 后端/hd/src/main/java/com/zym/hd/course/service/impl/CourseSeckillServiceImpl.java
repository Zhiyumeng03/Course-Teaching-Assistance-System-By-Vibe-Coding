package com.zym.hd.course.service.impl;

import com.zym.hd.course.dto.CourseEnrollConfigRequest;
import com.zym.hd.course.dto.CourseSeckillEnrollResponse;
import com.zym.hd.course.dto.CourseSeckillListItemResponse;
import com.zym.hd.course.dto.CourseSeckillMyStatusResponse;
import com.zym.hd.course.dto.CourseSeckillStatsResponse;
import com.zym.hd.course.dto.PageResponse;
import com.zym.hd.course.entity.Course;
import com.zym.hd.course.entity.CourseMember;
import com.zym.hd.course.service.CourseMemberService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zym.hd.course.service.CourseSeckillService;
import com.zym.hd.course.service.CourseService;
import com.zym.hd.user.entity.UserEntity;
import com.zym.hd.user.service.UserService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CourseSeckillServiceImpl implements CourseSeckillService {

    private static final Logger log = LoggerFactory.getLogger(CourseSeckillServiceImpl.class);
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ENROLL_MODE_REVIEW = "REVIEW";
    private static final String ENROLL_MODE_SECKILL = "SECKILL";
    private static final String ACTIVITY_NOT_STARTED = "NOT_STARTED";
    private static final String ACTIVITY_ONGOING = "ONGOING";
    private static final String ACTIVITY_ENDED = "ENDED";
    private static final String PAGE_CACHE_KEY_PREFIX = "course:seckill:page:";
    private static final long PAGE_CACHE_TTL_SECONDS = 30L;
    private static final ObjectMapper PAGE_CACHE_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final RedisScript<Long> SECKILL_LUA_SCRIPT;

    static {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local stockKey = KEYS[1]
                local usersKey = KEYS[2]
                local windowKey = KEYS[3]
                local userNo = ARGV[1]
                local nowTs = tonumber(ARGV[2])

                local startAt = tonumber(redis.call('HGET', windowKey, 'startAt') or '0')
                local endAt = tonumber(redis.call('HGET', windowKey, 'endAt') or '0')

                if startAt > 0 and nowTs < startAt then
                    return 3
                end
                if endAt > 0 and nowTs > endAt then
                    return 4
                end

                if redis.call('SISMEMBER', usersKey, userNo) == 1 then
                    return 1
                end

                local stock = tonumber(redis.call('GET', stockKey) or '-1')
                if stock <= 0 then
                    return 2
                end

                redis.call('DECR', stockKey)
                redis.call('SADD', usersKey, userNo)
                return 0
                """);
        SECKILL_LUA_SCRIPT = script;
    }

    private final CourseService courseService;
    private final CourseMemberService courseMemberService;
    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    public CourseSeckillServiceImpl(CourseService courseService,
                                    CourseMemberService courseMemberService,
                                    UserService userService,
                                    StringRedisTemplate stringRedisTemplate) {
        this.courseService = courseService;
        this.courseMemberService = courseMemberService;
        this.userService = userService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean updateEnrollConfig(Long courseId, Long operatorUserId, String operatorRole, CourseEnrollConfigRequest request) {
        Course course = getCourseRequired(courseId);
        assertCanManage(course, operatorUserId, operatorRole);
        String enrollMode = normalizeEnrollMode(request == null ? null : request.getEnrollMode());
        Integer enrollCapacity = request == null ? null : request.getEnrollCapacity();
        LocalDateTime enrollStartAt = request == null ? null : request.getEnrollStartAt();
        LocalDateTime enrollEndAt = request == null ? null : request.getEnrollEndAt();

        if (ENROLL_MODE_SECKILL.equals(enrollMode)) {
            validateSeckillConfig(enrollCapacity, enrollStartAt, enrollEndAt);
        } else {
            enrollCapacity = null;
            enrollStartAt = null;
            enrollEndAt = null;
        }

        boolean updated = courseService.lambdaUpdate()
                .eq(Course::getId, courseId)
                .set(Course::getEnrollMode, enrollMode)
                .set(Course::getEnrollCapacity, enrollCapacity)
                .set(Course::getEnrollStartAt, enrollStartAt)
                .set(Course::getEnrollEndAt, enrollEndAt)
                .set(Course::getEnrollPreheatedAt, null)
                .update();
        if (updated) {
            clearSeckillCache(courseId);
            evictSeckillPageCache();
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseSeckillStatsResponse preheat(Long courseId, Long operatorUserId, String operatorRole) {
        Course course = getCourseRequired(courseId);
        assertCanManage(course, operatorUserId, operatorRole);
        ensureSeckillMode(course);
        validateSeckillConfig(course.getEnrollCapacity(), course.getEnrollStartAt(), course.getEnrollEndAt());
        CourseSeckillStatsResponse response = syncRedisSnapshot(course, true);
        evictSeckillPageCache();
        return response;
    }

    @Override
    public CourseSeckillStatsResponse getStats(Long courseId, Long operatorUserId, String operatorRole) {
        Course course = getCourseRequired(courseId);
        assertCanManage(course, operatorUserId, operatorRole);
        int enrolledCount = countEnrolled(courseId);
        int remainingStock = resolveRemainingStock(course, enrolledCount);
        boolean preheated = Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey(courseId)))
                && Boolean.TRUE.equals(stringRedisTemplate.hasKey(windowKey(courseId)));
        return toStats(course, enrolledCount, remainingStock, preheated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseSeckillEnrollResponse enroll(Long courseId, Long studentUserId) {
        switchExpiredSeckillToReview();
        Course course = getCourseRequired(courseId);
        ensureSeckillMode(course);
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey(courseId)))
                || !Boolean.TRUE.equals(stringRedisTemplate.hasKey(windowKey(courseId)))) {
            syncRedisSnapshot(course, true);
        }
        UserEntity student = userService.getById(studentUserId);
        if (student == null || !StringUtils.hasText(student.getStudentNo())) {
            throw new IllegalArgumentException("studentNo is required");
        }
        String studentNo = student.getStudentNo().trim();

        List<String> keys = List.of(stockKey(courseId), usersKey(courseId), windowKey(courseId));
        Long code = stringRedisTemplate.execute(
                SECKILL_LUA_SCRIPT,
                keys,
                studentNo,
                String.valueOf(nowEpochSeconds()));
        int resultCode = code == null ? 2 : code.intValue();

        if (resultCode != 0) {
            return buildEnrollResponse(resultCode, resolveRemainingStock(course, countEnrolled(courseId)));
        }

        try {
            CourseMember member = new CourseMember();
            member.setCourseId(courseId);
            member.setUserNo(studentNo);
            member.setRoleInCourse(ROLE_STUDENT);
            member.setJoinedAt(LocalDateTime.now());
            courseMemberService.save(member);
        } catch (DuplicateKeyException ex) {
            compensateRedisAfterWriteFailed(courseId, studentNo);
            return buildEnrollResponse(1, resolveRemainingStock(course, countEnrolled(courseId)));
        } catch (Exception ex) {
            compensateRedisAfterWriteFailed(courseId, studentNo);
            throw new IllegalStateException("enroll failed, please retry", ex);
        }
        evictSeckillPageCache();
        return buildEnrollResponse(0, resolveRemainingStock(course, countEnrolled(courseId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseSeckillEnrollResponse withdraw(Long courseId, Long studentUserId) {
        switchExpiredSeckillToReview();
        Course course = getCourseRequired(courseId);
        ensureSeckillMode(course);
        if (!ACTIVITY_ONGOING.equals(resolveActivityStatus(course))) {
            return CourseSeckillEnrollResponse.builder()
                    .code("NOT_IN_PERIOD")
                    .message("withdraw is only allowed during seckill period")
                    .success(false)
                    .remainingStock(resolveRemainingStock(course, countEnrolled(courseId)))
                    .build();
        }

        UserEntity student = userService.getById(studentUserId);
        if (student == null || !StringUtils.hasText(student.getStudentNo())) {
            throw new IllegalArgumentException("studentNo is required");
        }
        String studentNo = student.getStudentNo().trim();

        CourseMember member = courseMemberService.lambdaQuery()
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getUserNo, studentNo)
                .eq(CourseMember::getRoleInCourse, ROLE_STUDENT)
                .one();
        if (member == null) {
            return CourseSeckillEnrollResponse.builder()
                    .code("NOT_SELECTED")
                    .message("not selected")
                    .success(false)
                    .remainingStock(resolveRemainingStock(course, countEnrolled(courseId)))
                    .build();
        }

        boolean removed = courseMemberService.removeById(member.getId());
        if (!removed) {
            throw new IllegalStateException("withdraw failed");
        }

        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey(courseId)))
                || Boolean.TRUE.equals(stringRedisTemplate.hasKey(windowKey(courseId)))) {
            syncRedisSnapshot(course, false);
        }
        evictSeckillPageCache();
        return CourseSeckillEnrollResponse.builder()
                .code("WITHDRAW_SUCCESS")
                .message("withdraw success")
                .success(true)
                .remainingStock(resolveRemainingStock(course, countEnrolled(courseId)))
                .build();
    }

    @Override
    public CourseSeckillMyStatusResponse getMyStatus(Long courseId, Long studentUserId) {
        Course course = getCourseRequired(courseId);
        ensureSeckillMode(course);
        UserEntity student = userService.getById(studentUserId);
        if (student == null || !StringUtils.hasText(student.getStudentNo())) {
            throw new IllegalArgumentException("studentNo is required");
        }
        String studentNo = student.getStudentNo().trim();
        boolean selected = courseMemberService.lambdaQuery()
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getUserNo, studentNo)
                .eq(CourseMember::getRoleInCourse, ROLE_STUDENT)
                .count() > 0;
        int enrolledCount = countEnrolled(courseId);
        int remainingStock = resolveRemainingStock(course, enrolledCount);
        return CourseSeckillMyStatusResponse.builder()
                .courseId(courseId)
                .selected(selected)
                .remainingStock(remainingStock)
                .activityStatus(resolveActivityStatus(course))
                .build();
    }

    @Override
    public PageResponse<CourseSeckillListItemResponse> pageSeckillCourses(Long studentUserId, long current, long size) {
        switchExpiredSeckillToReview();
        UserEntity student = userService.getById(studentUserId);
        if (student == null || !StringUtils.hasText(student.getStudentNo())) {
            throw new IllegalArgumentException("studentNo is required");
        }
        String studentNo = student.getStudentNo().trim();
        long safeCurrent = Math.max(1, current);
        long safeSize = Math.max(1, size);
        String cacheKey = PAGE_CACHE_KEY_PREFIX + safeCurrent + ":" + safeSize;

        CachedPage cachedPage = readPageCache(cacheKey);
        if (cachedPage == null) {
            cachedPage = buildCachedPage(safeCurrent, safeSize);
            writePageCache(cacheKey, cachedPage);
        }

        List<Long> pageCourseIds = cachedPage.records.stream()
                .map(CourseSeckillListItemResponse::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Set<Long> selectedCourseIds = querySelectedCourseIds(studentNo, pageCourseIds);

        List<CourseSeckillListItemResponse> records = cachedPage.records.stream()
                .map(item -> CourseSeckillListItemResponse.builder()
                        .courseId(item.getCourseId())
                        .courseCode(item.getCourseCode())
                        .courseName(item.getCourseName())
                        .term(item.getTerm())
                        .description(item.getDescription())
                        .teacherNo(item.getTeacherNo())
                        .enrollCapacity(item.getEnrollCapacity())
                        .remainingStock(item.getRemainingStock())
                        .activityStatus(item.getActivityStatus())
                        .enrollStartAt(item.getEnrollStartAt())
                        .enrollEndAt(item.getEnrollEndAt())
                        .selected(selectedCourseIds.contains(item.getCourseId()))
                        .build())
                .collect(Collectors.toList());

        return new PageResponse<>(
                records,
                cachedPage.total,
                cachedPage.current,
                cachedPage.size,
                cachedPage.pages
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcileAllSeckillCourses() {
        switchExpiredSeckillToReview();
        List<Course> seckillCourses = courseService.lambdaQuery()
                .eq(Course::getDeleted, 0)
                .eq(Course::getEnrollMode, ENROLL_MODE_SECKILL)
                .list();
        for (Course course : seckillCourses) {
            if (course.getEnrollCapacity() == null || course.getEnrollCapacity() <= 0) {
                continue;
            }
            List<String> dbUsers = listEnrolledUserNos(course.getId());
            int dbEnrolledCount = dbUsers.size();
            int dbRemaining = Math.max(course.getEnrollCapacity() - dbEnrolledCount, 0);
            int redisStock = readInt(stringRedisTemplate.opsForValue().get(stockKey(course.getId())), -1);
            long redisSetSize = safeLong(stringRedisTemplate.opsForSet().size(usersKey(course.getId())));
            boolean redisWindowExists = Boolean.TRUE.equals(stringRedisTemplate.hasKey(windowKey(course.getId())));
            if (redisStock != dbRemaining || redisSetSize != dbEnrolledCount || !redisWindowExists) {
                syncRedisSnapshot(course, true);
                log.warn("course seckill reconciled, courseId={}, dbRemaining={}, redisStockBefore={}, dbEnrolled={}, redisSetBefore={}",
                        course.getId(), dbRemaining, redisStock, dbEnrolledCount, redisSetSize);
                evictSeckillPageCache();
            }
        }
    }

    private void switchExpiredSeckillToReview() {
        LocalDateTime now = LocalDateTime.now();
        List<Course> expiredCourses = courseService.lambdaQuery()
                .eq(Course::getDeleted, 0)
                .eq(Course::getEnrollMode, ENROLL_MODE_SECKILL)
                .isNotNull(Course::getEnrollEndAt)
                .le(Course::getEnrollEndAt, now)
                .list();
        if (expiredCourses.isEmpty()) {
            return;
        }

        for (Course course : expiredCourses) {
            boolean updated = courseService.lambdaUpdate()
                    .eq(Course::getId, course.getId())
                    .eq(Course::getEnrollMode, ENROLL_MODE_SECKILL)
                    .set(Course::getEnrollMode, ENROLL_MODE_REVIEW)
                    .set(Course::getEnrollPreheatedAt, null)
                    .update();
            if (updated) {
                clearSeckillCache(course.getId());
                log.info("seckill course switched to review mode, courseId={}", course.getId());
            }
        }
        evictSeckillPageCache();
    }

    private Course getCourseRequired(Long courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId is required");
        }
        Course course = courseService.getById(courseId);
        if (course == null || Integer.valueOf(1).equals(course.getDeleted())) {
            throw new IllegalArgumentException("course not found");
        }
        return course;
    }

    private void assertCanManage(Course course, Long operatorUserId, String operatorRole) {
        String role = normalizeRole(operatorRole);
        if (ROLE_ADMIN.equals(role)) {
            return;
        }
        if (!ROLE_TEACHER.equals(role)) {
            throw new IllegalArgumentException("permission denied");
        }
        UserEntity operator = userService.getById(operatorUserId);
        String teacherNo = operator == null ? null : operator.getTeacherNo();
        if (!StringUtils.hasText(teacherNo) || !Objects.equals(teacherNo, course.getTeacherNo())) {
            throw new IllegalArgumentException("you can only manage your own courses");
        }
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEnrollMode(String enrollMode) {
        String normalized = enrollMode == null ? "" : enrollMode.trim().toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            return ENROLL_MODE_REVIEW;
        }
        if (!ENROLL_MODE_REVIEW.equals(normalized) && !ENROLL_MODE_SECKILL.equals(normalized)) {
            throw new IllegalArgumentException("invalid enrollMode");
        }
        return normalized;
    }

    private void ensureSeckillMode(Course course) {
        if (!ENROLL_MODE_SECKILL.equals(normalizeEnrollMode(course.getEnrollMode()))) {
            throw new IllegalArgumentException("course is not in seckill mode");
        }
    }

    private void validateSeckillConfig(Integer capacity, LocalDateTime startAt, LocalDateTime endAt) {
        if (capacity == null || capacity <= 0) {
            throw new IllegalArgumentException("enrollCapacity must be greater than 0");
        }
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("enrollStartAt and enrollEndAt are required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("enrollStartAt must be earlier than enrollEndAt");
        }
    }

    private CourseSeckillStatsResponse syncRedisSnapshot(Course course, boolean updatePreheatedAt) {
        List<String> enrolledUsers = listEnrolledUserNos(course.getId());
        int enrolledCount = enrolledUsers.size();
        int remaining = Math.max(course.getEnrollCapacity() - enrolledCount, 0);

        String stockKey = stockKey(course.getId());
        String usersKey = usersKey(course.getId());
        String windowKey = windowKey(course.getId());

        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(remaining));
        stringRedisTemplate.delete(usersKey);
        if (!enrolledUsers.isEmpty()) {
            stringRedisTemplate.opsForSet().add(usersKey, enrolledUsers.toArray(new String[0]));
        }
        Map<String, String> window = new HashMap<>();
        window.put("startAt", String.valueOf(toEpochSeconds(course.getEnrollStartAt())));
        window.put("endAt", String.valueOf(toEpochSeconds(course.getEnrollEndAt())));
        window.put("capacity", String.valueOf(course.getEnrollCapacity()));
        window.put("mode", ENROLL_MODE_SECKILL);
        stringRedisTemplate.opsForHash().putAll(windowKey, window);

        expireSeckillKeys(course.getId(), course.getEnrollEndAt());
        if (updatePreheatedAt) {
            course.setEnrollPreheatedAt(LocalDateTime.now());
            courseService.updateById(course);
        }
        return toStats(course, enrolledCount, remaining, true);
    }

    private void expireSeckillKeys(Long courseId, LocalDateTime endAt) {
        long ttlSeconds = 24 * 60 * 60;
        if (endAt != null) {
            long candidate = toEpochSeconds(endAt.plusDays(1)) - nowEpochSeconds();
            if (candidate > 0) {
                ttlSeconds = candidate;
            }
        }
        stringRedisTemplate.expire(stockKey(courseId), ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(usersKey(courseId), ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(windowKey(courseId), ttlSeconds, TimeUnit.SECONDS);
    }

    private void clearSeckillCache(Long courseId) {
        stringRedisTemplate.delete(stockKey(courseId));
        stringRedisTemplate.delete(usersKey(courseId));
        stringRedisTemplate.delete(windowKey(courseId));
    }

    private List<String> listEnrolledUserNos(Long courseId) {
        return courseMemberService.lambdaQuery()
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getRoleInCourse, ROLE_STUDENT)
                .list()
                .stream()
                .map(CourseMember::getUserNo)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private int countEnrolled(Long courseId) {
        return listEnrolledUserNos(courseId).size();
    }

    private int resolveRemainingStock(Course course, int enrolledCount) {
        Integer capacity = course.getEnrollCapacity();
        if (capacity == null || capacity <= 0) {
            return 0;
        }
        String redisValue = stringRedisTemplate.opsForValue().get(stockKey(course.getId()));
        if (redisValue != null) {
            return Math.max(readInt(redisValue, 0), 0);
        }
        return Math.max(capacity - enrolledCount, 0);
    }

    private CourseSeckillStatsResponse toStats(Course course, int enrolledCount, int remainingStock, boolean preheated) {
        return CourseSeckillStatsResponse.builder()
                .courseId(course.getId())
                .enrollMode(normalizeEnrollMode(course.getEnrollMode()))
                .enrollCapacity(course.getEnrollCapacity())
                .enrolledCount(enrolledCount)
                .remainingStock(remainingStock)
                .preheated(preheated)
                .activityStatus(resolveActivityStatus(course))
                .enrollStartAt(course.getEnrollStartAt())
                .enrollEndAt(course.getEnrollEndAt())
                .enrollPreheatedAt(course.getEnrollPreheatedAt())
                .build();
    }

    private String resolveActivityStatus(Course course) {
        LocalDateTime now = LocalDateTime.now();
        if (course.getEnrollStartAt() != null && now.isBefore(course.getEnrollStartAt())) {
            return ACTIVITY_NOT_STARTED;
        }
        if (course.getEnrollEndAt() != null && now.isAfter(course.getEnrollEndAt())) {
            return ACTIVITY_ENDED;
        }
        return ACTIVITY_ONGOING;
    }

    private CourseSeckillEnrollResponse buildEnrollResponse(int code, int remainingStock) {
        return switch (code) {
            case 0 -> CourseSeckillEnrollResponse.builder()
                    .code("SUCCESS")
                    .message("enroll success")
                    .success(true)
                    .remainingStock(remainingStock)
                    .build();
            case 1 -> CourseSeckillEnrollResponse.builder()
                    .code("ALREADY_SELECTED")
                    .message("already selected")
                    .success(false)
                    .remainingStock(remainingStock)
                    .build();
            case 2 -> CourseSeckillEnrollResponse.builder()
                    .code("SOLD_OUT")
                    .message("sold out")
                    .success(false)
                    .remainingStock(remainingStock)
                    .build();
            case 3 -> CourseSeckillEnrollResponse.builder()
                    .code("NOT_STARTED")
                    .message("not started")
                    .success(false)
                    .remainingStock(remainingStock)
                    .build();
            case 4 -> CourseSeckillEnrollResponse.builder()
                    .code("ENDED")
                    .message("ended")
                    .success(false)
                    .remainingStock(remainingStock)
                    .build();
            default -> CourseSeckillEnrollResponse.builder()
                    .code("FAILED")
                    .message("failed")
                    .success(false)
                    .remainingStock(remainingStock)
                    .build();
        };
    }

    private void compensateRedisAfterWriteFailed(Long courseId, String studentNo) {
        stringRedisTemplate.opsForSet().remove(usersKey(courseId), studentNo);
        stringRedisTemplate.opsForValue().increment(stockKey(courseId));
    }

    private CachedPage readPageCache(String cacheKey) {
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return PAGE_CACHE_MAPPER.readValue(json, new TypeReference<CachedPage>() {});
        } catch (Exception ex) {
            log.warn("failed to parse seckill page cache, key={}", cacheKey, ex);
            stringRedisTemplate.delete(cacheKey);
            return null;
        }
    }

    private void writePageCache(String cacheKey, CachedPage page) {
        try {
            String json = PAGE_CACHE_MAPPER.writeValueAsString(page);
            stringRedisTemplate.opsForValue().set(cacheKey, json, PAGE_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("failed to write seckill page cache, key={}", cacheKey, ex);
        }
    }

    private CachedPage buildCachedPage(long current, long size) {
        List<Course> seckillCourses = courseService.lambdaQuery()
                .eq(Course::getDeleted, 0)
                .eq(Course::getEnrollMode, ENROLL_MODE_SECKILL)
                .orderByAsc(Course::getEnrollStartAt)
                .orderByAsc(Course::getId)
                .list();
        if (seckillCourses.isEmpty()) {
            return CachedPage.empty(current, size);
        }

        List<Long> courseIds = seckillCourses.stream()
                .map(Course::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Integer> enrolledCountMap = buildEnrolledCountMap(courseIds);

        List<CourseSeckillListItemResponse> allItems = new ArrayList<>(seckillCourses.size());
        for (Course course : seckillCourses) {
            int enrolledCount = enrolledCountMap.getOrDefault(course.getId(), 0);
            int remaining = resolveRemainingStock(course, enrolledCount);
            allItems.add(CourseSeckillListItemResponse.builder()
                    .courseId(course.getId())
                    .courseCode(course.getCourseCode())
                    .courseName(course.getCourseName())
                    .term(course.getTerm())
                    .description(course.getDescription())
                    .teacherNo(course.getTeacherNo())
                    .enrollCapacity(course.getEnrollCapacity())
                    .remainingStock(remaining)
                    .activityStatus(resolveActivityStatus(course))
                    .enrollStartAt(course.getEnrollStartAt())
                    .enrollEndAt(course.getEnrollEndAt())
                    .selected(false)
                    .build());
        }

        PageResponse<CourseSeckillListItemResponse> page = PageResponse.of(allItems, current, size);
        return new CachedPage(page.records(), page.total(), page.current(), page.size(), page.pages());
    }

    private Map<Long, Integer> buildEnrolledCountMap(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CourseMember> members = courseMemberService.lambdaQuery()
                .in(CourseMember::getCourseId, courseIds)
                .eq(CourseMember::getRoleInCourse, ROLE_STUDENT)
                .list();
        Map<Long, Integer> countMap = new HashMap<>();
        for (CourseMember member : members) {
            if (member.getCourseId() == null) {
                continue;
            }
            countMap.merge(member.getCourseId(), 1, Integer::sum);
        }
        return countMap;
    }

    private Set<Long> querySelectedCourseIds(String studentNo, List<Long> pageCourseIds) {
        if (!StringUtils.hasText(studentNo) || pageCourseIds == null || pageCourseIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<CourseMember> members = courseMemberService.lambdaQuery()
                .in(CourseMember::getCourseId, pageCourseIds)
                .eq(CourseMember::getUserNo, studentNo)
                .eq(CourseMember::getRoleInCourse, ROLE_STUDENT)
                .list();
        Set<Long> selected = new HashSet<>();
        for (CourseMember member : members) {
            if (member.getCourseId() != null) {
                selected.add(member.getCourseId());
            }
        }
        return selected;
    }

    private void evictSeckillPageCache() {
        Set<String> keys = stringRedisTemplate.keys(PAGE_CACHE_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        stringRedisTemplate.delete(keys);
    }

    private int readInt(String value, int fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private long safeLong(Long value) {
        return value == null ? -1L : value;
    }

    private String stockKey(Long courseId) {
        return "course:enroll:stock:" + courseId;
    }

    private String usersKey(Long courseId) {
        return "course:enroll:users:" + courseId;
    }

    private String windowKey(Long courseId) {
        return "course:enroll:window:" + courseId;
    }

    private long nowEpochSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    private long toEpochSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private static class CachedPage {
        public List<CourseSeckillListItemResponse> records;
        public long total;
        public long current;
        public long size;
        public long pages;

        public CachedPage() {
            this.records = new ArrayList<>();
            this.total = 0L;
            this.current = 1L;
            this.size = 10L;
            this.pages = 0L;
        }

        public CachedPage(List<CourseSeckillListItemResponse> records, long total, long current, long size, long pages) {
            this.records = records == null ? new ArrayList<>() : records;
            this.total = total;
            this.current = current;
            this.size = size;
            this.pages = pages;
        }

        private static CachedPage empty(long current, long size) {
            long safeCurrent = Math.max(1, current);
            long safeSize = Math.max(1, size);
            return new CachedPage(new ArrayList<>(), 0L, safeCurrent, safeSize, 0L);
        }
    }
}
