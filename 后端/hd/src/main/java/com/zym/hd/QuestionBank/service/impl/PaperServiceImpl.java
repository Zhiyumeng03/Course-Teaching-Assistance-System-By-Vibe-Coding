package com.zym.hd.QuestionBank.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zym.hd.QuestionBank.entity.KnowledgeEntity;
import com.zym.hd.QuestionBank.entity.PaperEntity;
import com.zym.hd.QuestionBank.entity.PaperQuestionEntity;
import com.zym.hd.QuestionBank.entity.QuestionEntity;
import com.zym.hd.QuestionBank.mapper.PaperMapper;
import com.zym.hd.QuestionBank.mapper.PaperQuestionMapper;
import com.zym.hd.QuestionBank.mapper.QuestionMapper;
import com.zym.hd.QuestionBank.service.KnowledgeService;
import com.zym.hd.QuestionBank.service.PaperService;
import com.zym.hd.QuestionBank.util.JsonUtils;
import com.zym.hd.course.dto.PageResponse;
import com.zym.hd.course.entity.Course;
import com.zym.hd.course.entity.CourseMember;
import com.zym.hd.course.service.CourseMemberService;
import com.zym.hd.course.service.CourseService;
import com.zym.hd.user.entity.UserEntity;
import com.zym.hd.user.service.UserService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, PaperEntity> implements PaperService {

    private static final Set<String> PAPER_STATUS_SET = Set.of("DRAFT", "PUBLISHED", "CLOSED");
    private static final Set<String> PAPER_MODE_SET = Set.of("MANUAL", "AI");

    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final KnowledgeService knowledgeService;
    private final CourseService courseService;
    private final CourseMemberService courseMemberService;
    private final UserService userService;

    public PaperServiceImpl(
            PaperQuestionMapper paperQuestionMapper,
            QuestionMapper questionMapper,
            KnowledgeService knowledgeService,
            CourseService courseService,
            CourseMemberService courseMemberService,
            UserService userService) {
        this.paperQuestionMapper = paperQuestionMapper;
        this.questionMapper = questionMapper;
        this.knowledgeService = knowledgeService;
        this.courseService = courseService;
        this.courseMemberService = courseMemberService;
        this.userService = userService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperEntity createPaper(Map<String, Object> payload, Long creatorId, String operatorRole) {
        Long courseId = longVal(payload.get("courseId"));
        String title = stringVal(payload.get("title"));
        List<Map<String, Object>> questionItems = parseQuestionItems(payload.get("questionItems"));
        if (courseId == null || !StringUtils.hasText(title)) {
            throw new IllegalArgumentException("courseId and title are required");
        }
        if (questionItems.isEmpty()) {
            throw new IllegalArgumentException("questionItems is required");
        }
        assertTeacherCourseAccess(courseId, creatorId, operatorRole);

        PaperEntity entity = new PaperEntity();
        entity.setCourseId(courseId);
        entity.setCreatorId(creatorId);
        entity.setTitle(title.trim());
        entity.setPaperType(StringUtils.hasText(stringVal(payload.get("paperType")))
                ? stringVal(payload.get("paperType")).trim()
                : "PRACTICE");
        entity.setGenerationMode(resolveGenerationMode(payload.get("generationMode")));
        entity.setConfigJson(buildConfigJson(payload.get("config"), questionItems.size()));
        entity.setTotalScore(0);
        entity.setDurationMinutes(intVal(payload.get("durationMinutes")));
        entity.setStatus(resolveStatus(payload.get("status"), "DRAFT"));
        entity.setCreatedAt(LocalDateTime.now());
        save(entity);

        bindQuestions(entity.getId(), questionItems, creatorId, operatorRole);
        return enrichPaper(getById(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperEntity updatePaper(Map<String, Object> payload, Long operatorId, String operatorRole) {
        Long id = longVal(payload.get("id"));
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        PaperEntity db = getById(id);
        if (db == null) {
            throw new IllegalArgumentException("paper not found");
        }
        assertCanManagePaper(db, operatorId, operatorRole);

        if (payload.containsKey("courseId")) {
            Long courseId = longVal(payload.get("courseId"));
            if (courseId == null) {
                throw new IllegalArgumentException("courseId is invalid");
            }
            assertTeacherCourseAccess(courseId, operatorId, operatorRole);
            db.setCourseId(courseId);
        }
        if (payload.containsKey("title")) {
            db.setTitle(stringVal(payload.get("title")));
        }
        if (payload.containsKey("paperType")) {
            db.setPaperType(stringVal(payload.get("paperType")));
        }
        if (payload.containsKey("generationMode")) {
            db.setGenerationMode(resolveGenerationMode(payload.get("generationMode")));
        }
        if (payload.containsKey("durationMinutes")) {
            db.setDurationMinutes(intVal(payload.get("durationMinutes")));
        }
        if (payload.containsKey("status")) {
            db.setStatus(resolveStatus(payload.get("status"), db.getStatus()));
        }

        List<Map<String, Object>> questionItems = payload.containsKey("questionItems")
                ? parseQuestionItems(payload.get("questionItems"))
                : List.of();
        int questionCount = questionItems.isEmpty() ? paperQuestionMapper.countByPaperId(db.getId()) : questionItems.size();
        db.setConfigJson(buildConfigJson(payload.get("config"), questionCount));
        updateById(db);

        if (payload.containsKey("questionItems")) {
            if (questionItems.isEmpty()) {
                throw new IllegalArgumentException("questionItems cannot be empty");
            }
            bindQuestions(db.getId(), questionItems, operatorId, operatorRole);
        }
        return enrichPaper(getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePaper(Long id, Long operatorId, String operatorRole) {
        PaperEntity db = getById(id);
        if (db == null) {
            return false;
        }
        assertCanManagePaper(db, operatorId, operatorRole);
        paperQuestionMapper.deleteByPaperId(id);
        return removeById(id);
    }

    @Override
    public PaperEntity publishPaper(Long id, Long operatorId, String operatorRole) {
        PaperEntity db = getById(id);
        if (db == null) {
            throw new IllegalArgumentException("paper not found");
        }
        assertCanManagePaper(db, operatorId, operatorRole);
        if (paperQuestionMapper.countByPaperId(id) <= 0) {
            throw new IllegalArgumentException("paper must contain at least one question");
        }
        db.setStatus("PUBLISHED");
        updateById(db);
        return enrichPaper(db);
    }

    @Override
    public PaperEntity closePaper(Long id, Long operatorId, String operatorRole) {
        PaperEntity db = getById(id);
        if (db == null) {
            throw new IllegalArgumentException("paper not found");
        }
        assertCanManagePaper(db, operatorId, operatorRole);
        db.setStatus("CLOSED");
        updateById(db);
        return enrichPaper(db);
    }

    @Override
    public PaperEntity getPaperById(Long id, Long viewerId, String viewerRole) {
        PaperEntity paper = getById(id);
        if (paper == null) {
            return null;
        }
        assertCanViewPaper(paper, viewerId, viewerRole);
        return enrichPaper(paper);
    }

    @Override
    public List<PaperEntity> listByCourseId(Long courseId, Long viewerId, String viewerRole) {
        assertCourseAccess(courseId, viewerId, viewerRole);
        List<PaperEntity> papers = lambdaQuery()
                .eq(PaperEntity::getCourseId, courseId)
                .in(
                        isStudentRole(viewerRole),
                        PaperEntity::getStatus,
                        List.of("PUBLISHED", "CLOSED"))
                .orderByDesc(PaperEntity::getCreatedAt)
                .orderByDesc(PaperEntity::getId)
                .list();
        papers.forEach(this::enrichPaper);
        return papers;
    }

    @Override
    public PageResponse<PaperEntity> pageByCourseId(Long courseId,
                                                    Long viewerId,
                                                    String viewerRole,
                                                    long current,
                                                    long size,
                                                    String keyword) {
        assertCourseAccess(courseId, viewerId, viewerRole);
        String normalizedKeyword = normalizeKeyword(keyword);
        List<PaperEntity> filtered = lambdaQuery()
                .eq(PaperEntity::getCourseId, courseId)
                .in(
                        isStudentRole(viewerRole),
                        PaperEntity::getStatus,
                        List.of("PUBLISHED", "CLOSED"))
                .orderByDesc(PaperEntity::getCreatedAt)
                .orderByDesc(PaperEntity::getId)
                .list()
                .stream()
                .filter(paper -> matchesPaperKeyword(paper, normalizedKeyword))
                .toList();
        PageResponse<PaperEntity> pageResponse = PageResponse.of(filtered, current, size);
        pageResponse.records().forEach(this::enrichPaper);
        return pageResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PaperQuestionEntity> bindQuestions(Long paperId,
                                                   List<Map<String, Object>> questionItems,
                                                   Long operatorId,
                                                   String operatorRole) {
        PaperEntity paper = getById(paperId);
        if (paper == null) {
            throw new IllegalArgumentException("paper not found");
        }
        assertCanManagePaper(paper, operatorId, operatorRole);
        if (questionItems == null || questionItems.isEmpty()) {
            throw new IllegalArgumentException("questionItems is required");
        }

        paperQuestionMapper.deleteByPaperId(paperId);
        List<PaperQuestionEntity> saved = new ArrayList<>();
        int totalScore = 0;
        int sortNo = 1;
        for (Map<String, Object> item : questionItems) {
            Long questionId = longVal(item.get("questionId"));
            Integer score = intVal(item.get("score"));
            Integer requestSortNo = intVal(item.get("sortNo"));
            if (questionId == null || score == null || score <= 0) {
                throw new IllegalArgumentException("questionId and score are required");
            }
            QuestionEntity question = questionMapper.selectById(questionId);
            if (question == null || isDeleted(question)) {
                throw new IllegalArgumentException("question not found: " + questionId);
            }
            if (!Objects.equals(question.getCourseId(), paper.getCourseId())) {
                throw new IllegalArgumentException("question does not belong to current course");
            }
            if (!"APPROVED".equalsIgnoreCase(question.getReviewStatus())) {
                throw new IllegalArgumentException("question is not approved: " + questionId);
            }

            PaperQuestionEntity entity = new PaperQuestionEntity();
            entity.setPaperId(paperId);
            entity.setQuestionId(questionId);
            entity.setSortNo(requestSortNo == null || requestSortNo <= 0 ? sortNo : requestSortNo);
            entity.setScore(score);
            entity.setQuestionSnapshotJson(buildQuestionSnapshot(question));
            paperQuestionMapper.insert(entity);
            saved.add(entity);
            totalScore += score;
            sortNo++;
        }

        paper.setTotalScore(totalScore);
        paper.setConfigJson(buildConfigJson(JsonUtils.parseObject(paper.getConfigJson()), saved.size()));
        updateById(paper);
        return saved;
    }

    @Override
    public List<Map<String, Object>> listPaperQuestions(Long paperId, Long viewerId, String viewerRole) {
        PaperEntity paper = getById(paperId);
        if (paper == null) {
            throw new IllegalArgumentException("paper not found");
        }
        assertCanViewPaper(paper, viewerId, viewerRole);
        boolean includeAnswer = !isStudentRole(viewerRole);
        return paperQuestionMapper.selectByPaperId(paperId).stream()
                .map(item -> toQuestionView(item, includeAnswer))
                .toList();
    }

    @Override
    public Map<String, Object> getPaperDetail(Long paperId, Long viewerId, String viewerRole) {
        PaperEntity paper = getPaperById(paperId, viewerId, viewerRole);
        if (paper == null) {
            throw new IllegalArgumentException("paper not found");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paper", paper);
        result.put("questions", listPaperQuestions(paperId, viewerId, viewerRole));
        return result;
    }

    private PaperEntity enrichPaper(PaperEntity paper) {
        if (paper == null) {
            return null;
        }
        paper.setQuestionCount(paperQuestionMapper.countByPaperId(paper.getId()));
        return paper;
    }

    private Map<String, Object> toQuestionView(PaperQuestionEntity entity, boolean includeAnswer) {
        Map<String, Object> snapshot = JsonUtils.parseObject(entity.getQuestionSnapshotJson());
        if (!includeAnswer) {
            snapshot.remove("answer");
            snapshot.remove("analysisText");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", entity.getId());
        result.put("paperId", entity.getPaperId());
        result.put("questionId", entity.getQuestionId());
        result.put("sortNo", entity.getSortNo());
        result.put("score", entity.getScore());
        result.put("snapshot", snapshot);
        return result;
    }

    private String buildQuestionSnapshot(QuestionEntity question) {
        List<Long> knowledgePointIds = questionMapper.selectKnowledgePointIds(question.getId());
        List<String> knowledgePointNames = resolveKnowledgeNames(knowledgePointIds);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("questionId", question.getId());
        snapshot.put("courseId", question.getCourseId());
        snapshot.put("type", normalizeUpper(question.getType()));
        snapshot.put("stem", question.getStem());
        snapshot.put("content", JsonUtils.parseObject(question.getContentJson()));
        snapshot.put("answer", JsonUtils.parseObject(question.getAnswerJson()));
        snapshot.put("analysisText", question.getAnalysisText());
        snapshot.put("difficulty", question.getDifficulty());
        snapshot.put("knowledgePointIds", knowledgePointIds);
        snapshot.put("knowledgePointNames", knowledgePointNames);
        return JsonUtils.toJson(snapshot);
    }

    private List<String> resolveKnowledgeNames(List<Long> knowledgePointIds) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> nameMap = knowledgeService.listByIds(knowledgePointIds).stream()
                .collect(Collectors.toMap(KnowledgeEntity::getId, KnowledgeEntity::getName, (left, right) -> left));
        List<String> result = new ArrayList<>();
        for (Long id : knowledgePointIds) {
            String name = nameMap.get(id);
            if (StringUtils.hasText(name)) {
                result.add(name);
            }
        }
        return result;
    }

    private List<Map<String, Object>> parseQuestionItems(Object rawValue) {
        if (rawValue == null) {
            return List.of();
        }
        if (!(rawValue instanceof List<?> list)) {
            throw new IllegalArgumentException("questionItems must be array");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("questionItems item must be object");
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            result.add(normalized);
        }
        return result;
    }

    private String buildConfigJson(Object rawConfig, int questionCount) {
        Map<String, Object> config = rawConfig instanceof Map<?, ?> map
                ? normalizeMap(map)
                : new LinkedHashMap<>();
        config.put("selectedQuestionCount", questionCount);
        return JsonUtils.toJson(config);
    }

    private Map<String, Object> normalizeMap(Map<?, ?> source) {
        Map<String, Object> target = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            target.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return target;
    }

    private String resolveGenerationMode(Object value) {
        String mode = normalizeUpper(stringVal(value));
        if (!StringUtils.hasText(mode)) {
            return "MANUAL";
        }
        if (!PAPER_MODE_SET.contains(mode)) {
            throw new IllegalArgumentException("generationMode must be MANUAL or AI");
        }
        return mode;
    }

    private String resolveStatus(Object value, String fallback) {
        String status = normalizeUpper(stringVal(value));
        if (!StringUtils.hasText(status)) {
            return StringUtils.hasText(fallback) ? normalizeUpper(fallback) : "DRAFT";
        }
        if (!PAPER_STATUS_SET.contains(status)) {
            throw new IllegalArgumentException("invalid paper status");
        }
        return status;
    }

    private void assertCanManagePaper(PaperEntity paper, Long operatorId, String operatorRole) {
        if (paper == null) {
            throw new IllegalArgumentException("paper not found");
        }
        if (isAdminRole(operatorRole)) {
            return;
        }
        if (!Objects.equals(operatorId, paper.getCreatorId())) {
            throw new IllegalArgumentException("forbidden");
        }
        assertTeacherCourseAccess(paper.getCourseId(), operatorId, operatorRole);
    }

    private void assertCanViewPaper(PaperEntity paper, Long viewerId, String viewerRole) {
        if (paper == null) {
            throw new IllegalArgumentException("paper not found");
        }
        assertCourseAccess(paper.getCourseId(), viewerId, viewerRole);
        if (isStudentRole(viewerRole) && "DRAFT".equalsIgnoreCase(paper.getStatus())) {
            throw new IllegalArgumentException("paper is not available");
        }
    }

    private void assertCourseAccess(Long courseId, Long viewerId, String viewerRole) {
        Course course = courseService.getById(courseId);
        if (course == null || course.getDeleted() != null && course.getDeleted() == 1) {
            throw new IllegalArgumentException("course not found");
        }
        if (isAdminRole(viewerRole)) {
            return;
        }
        UserEntity user = userService.getById(viewerId);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        if (isTeacherRole(viewerRole)) {
            if (!StringUtils.hasText(user.getTeacherNo()) || !Objects.equals(user.getTeacherNo(), course.getTeacherNo())) {
                throw new IllegalArgumentException("forbidden");
            }
            return;
        }
        if (!isStudentRole(viewerRole)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!StringUtils.hasText(user.getStudentNo())) {
            throw new IllegalArgumentException("student number is missing");
        }
        CourseMember membership = courseMemberService.lambdaQuery()
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getUserNo, user.getStudentNo())
                .eq(CourseMember::getRoleInCourse, "STUDENT")
                .one();
        if (membership == null) {
            throw new IllegalArgumentException("forbidden");
        }
    }

    private void assertTeacherCourseAccess(Long courseId, Long operatorId, String operatorRole) {
        if (isAdminRole(operatorRole)) {
            return;
        }
        if (!isTeacherRole(operatorRole)) {
            throw new IllegalArgumentException("forbidden");
        }
        Course course = courseService.getById(courseId);
        if (course == null || course.getDeleted() != null && course.getDeleted() == 1) {
            throw new IllegalArgumentException("course not found");
        }
        UserEntity operator = userService.getById(operatorId);
        if (operator == null || !StringUtils.hasText(operator.getTeacherNo())) {
            throw new IllegalArgumentException("teacher number is missing");
        }
        if (!Objects.equals(operator.getTeacherNo(), course.getTeacherNo())) {
            throw new IllegalArgumentException("forbidden");
        }
    }

    private boolean isDeleted(QuestionEntity question) {
        return question.getDeleted() != null && question.getDeleted() == 1;
    }

    private boolean matchesPaperKeyword(PaperEntity paper, String normalizedKeyword) {
        if (!StringUtils.hasText(normalizedKeyword)) {
            return true;
        }
        String target = String.join(" ",
                stringOrEmpty(paper.getTitle()),
                stringOrEmpty(paper.getStatus()),
                stringOrEmpty(paper.getPaperType()))
                .toLowerCase(Locale.ROOT);
        return target.contains(normalizedKeyword);
    }

    private boolean isTeacherRole(String role) {
        return "TEACHER".equalsIgnoreCase(role);
    }

    private boolean isStudentRole(String role) {
        return "STUDENT".equalsIgnoreCase(role);
    }

    private boolean isAdminRole(String role) {
        return "ADMIN".equalsIgnoreCase(role);
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String stringOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
    }

    private Long longVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Integer intVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
