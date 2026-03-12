package com.zym.hd.QuestionBank.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zym.hd.QuestionBank.client.PracticeDiagnosisClient;
import com.zym.hd.QuestionBank.dto.PracticeAnswerFeedbackItem;
import com.zym.hd.QuestionBank.dto.PracticeDiagnosisAnswerItem;
import com.zym.hd.QuestionBank.dto.PracticeDiagnosisRequest;
import com.zym.hd.QuestionBank.dto.PracticeDiagnosisResponse;
import com.zym.hd.QuestionBank.dto.PracticeRecordPageResponse;
import com.zym.hd.QuestionBank.entity.PaperEntity;
import com.zym.hd.QuestionBank.entity.PaperQuestionEntity;
import com.zym.hd.QuestionBank.entity.PracticeAnswerEntity;
import com.zym.hd.QuestionBank.entity.PracticeEntity;
import com.zym.hd.QuestionBank.entity.QuestionEntity;
import com.zym.hd.QuestionBank.mapper.PaperMapper;
import com.zym.hd.QuestionBank.mapper.PaperQuestionMapper;
import com.zym.hd.QuestionBank.mapper.PracticeMapper;
import com.zym.hd.QuestionBank.mapper.QuestionMapper;
import com.zym.hd.QuestionBank.service.PracticeService;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PracticeServiceImpl extends ServiceImpl<PracticeMapper, PracticeEntity> implements PracticeService {

    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_GRADED = "GRADED";
    private static final String DIAGNOSIS_PENDING = "PENDING";
    private static final String DIAGNOSIS_DONE = "DONE";
    private static final Set<String> OBJECTIVE_TYPE_SET = Set.of("SINGLE", "MULTI", "JUDGE");
    private static final Set<String> PRACTICE_STATUS_FILTER_SET = Set.of(STATUS_PENDING_REVIEW, STATUS_GRADED);

    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final CourseService courseService;
    private final CourseMemberService courseMemberService;
    private final UserService userService;
    private final PracticeDiagnosisClient practiceDiagnosisClient;

    public PracticeServiceImpl(
            PaperMapper paperMapper,
            PaperQuestionMapper paperQuestionMapper,
            QuestionMapper questionMapper,
            CourseService courseService,
            CourseMemberService courseMemberService,
            UserService userService,
            PracticeDiagnosisClient practiceDiagnosisClient) {
        this.paperMapper = paperMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.questionMapper = questionMapper;
        this.courseService = courseService;
        this.courseMemberService = courseMemberService;
        this.userService = userService;
        this.practiceDiagnosisClient = practiceDiagnosisClient;
    }

    @Override
    public PracticeEntity createRecord(Map<String, Object> payload, Long studentId) {
        PracticeEntity entity = new PracticeEntity();
        entity.setPaperId(longVal(payload.get("paperId")));
        entity.setCourseId(longVal(payload.get("courseId")));
        entity.setStudentId(studentId);
        entity.setStatus(StringUtils.hasText(stringVal(payload.get("status"))) ? normalizeUpper(stringVal(payload.get("status"))) : STATUS_PENDING_REVIEW);
        entity.setStartedAt(timeVal(payload.get("startedAt")));
        entity.setSubmittedAt(timeVal(payload.get("submittedAt")));
        entity.setObjectiveScore(intVal(payload.get("objectiveScore")));
        entity.setTotalScore(intVal(payload.get("totalScore")));
        entity.setDiagnosisStatus(stringVal(payload.get("diagnosisStatus")));
        entity.setDiagnosisJson(stringVal(payload.get("diagnosisJson")));
        entity.setDiagnosisText(stringVal(payload.get("diagnosisText")));
        entity.setCreatedAt(LocalDateTime.now());
        if (entity.getPaperId() == null || entity.getCourseId() == null) {
            throw new IllegalArgumentException("paperId and courseId are required");
        }
        assertStudentCourseAccess(entity.getCourseId(), studentId);
        save(entity);
        return enrichRecord(entity);
    }

    @Override
    public PracticeEntity updateRecord(Map<String, Object> payload, Long studentId) {
        Long id = longVal(payload.get("id"));
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        PracticeEntity db = getById(id);
        if (db == null) {
            throw new IllegalArgumentException("record not found");
        }
        if (!Objects.equals(studentId, db.getStudentId())) {
            throw new IllegalArgumentException("forbidden");
        }
        if (payload.containsKey("status")) {
            db.setStatus(normalizeUpper(stringVal(payload.get("status"))));
        }
        if (payload.containsKey("startedAt")) {
            db.setStartedAt(timeVal(payload.get("startedAt")));
        }
        if (payload.containsKey("submittedAt")) {
            db.setSubmittedAt(timeVal(payload.get("submittedAt")));
        }
        if (payload.containsKey("objectiveScore")) {
            db.setObjectiveScore(intVal(payload.get("objectiveScore")));
        }
        if (payload.containsKey("totalScore")) {
            db.setTotalScore(intVal(payload.get("totalScore")));
        }
        if (payload.containsKey("diagnosisStatus")) {
            db.setDiagnosisStatus(stringVal(payload.get("diagnosisStatus")));
        }
        if (payload.containsKey("diagnosisJson")) {
            db.setDiagnosisJson(stringVal(payload.get("diagnosisJson")));
        }
        if (payload.containsKey("diagnosisText")) {
            db.setDiagnosisText(stringVal(payload.get("diagnosisText")));
        }
        updateById(db);
        return enrichRecord(db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRecord(Long id, Long studentId) {
        PracticeEntity db = getById(id);
        if (db == null) {
            return false;
        }
        if (!Objects.equals(studentId, db.getStudentId())) {
            throw new IllegalArgumentException("forbidden");
        }
        baseMapper.deleteAnswersByRecordId(id);
        return removeById(id);
    }

    @Override
    public PracticeEntity getRecordById(Long id, Long studentId, String role) {
        PracticeEntity db = getById(id);
        if (db == null) {
            return null;
        }
        assertRecordAccess(db, studentId, role);
        return enrichRecord(db);
    }

    @Override
    public List<PracticeEntity> listByStudent(Long studentId) {
        List<PracticeEntity> records = lambdaQuery()
                .eq(PracticeEntity::getStudentId, studentId)
                .orderByDesc(PracticeEntity::getSubmittedAt)
                .orderByDesc(PracticeEntity::getId)
                .list();
        records.forEach(this::enrichRecord);
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PracticeAnswerEntity saveAnswer(Map<String, Object> payload, Long studentId) {
        Long recordId = longVal(payload.get("recordId"));
        if (recordId == null) {
            throw new IllegalArgumentException("recordId is required");
        }
        PracticeEntity record = getById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("record not found");
        }
        if (!Objects.equals(studentId, record.getStudentId())) {
            throw new IllegalArgumentException("forbidden");
        }

        Long paperQuestionId = longVal(payload.get("paperQuestionId"));
        Long questionId = longVal(payload.get("questionId"));
        if (paperQuestionId == null || questionId == null) {
            throw new IllegalArgumentException("paperQuestionId and questionId are required");
        }
        PaperQuestionEntity paperQuestion = paperQuestionMapper.selectById(paperQuestionId);
        if (paperQuestion == null) {
            throw new IllegalArgumentException("paper question not found");
        }
        if (!Objects.equals(record.getPaperId(), paperQuestion.getPaperId())) {
            throw new IllegalArgumentException("paper question does not belong to the record");
        }
        if (!Objects.equals(questionId, paperQuestion.getQuestionId())) {
            throw new IllegalArgumentException("questionId does not match paperQuestionId");
        }

        PracticeAnswerEntity existing = baseMapper.selectAnswerByRecordIdAndPaperQuestionId(recordId, paperQuestionId);
        PracticeAnswerEntity answer = existing == null ? new PracticeAnswerEntity() : existing;
        answer.setRecordId(recordId);
        answer.setPaperQuestionId(paperQuestionId);
        answer.setQuestionId(questionId);
        answer.setAnswerJson(JsonUtils.toJson(normalizeAnswerObject(payload.get("answer"))));
        if (payload.containsKey("isCorrect")) {
            answer.setIsCorrect(intVal(payload.get("isCorrect")));
        }
        if (payload.containsKey("score")) {
            answer.setScore(intVal(payload.get("score")));
        }
        if (payload.containsKey("aiFeedbackJson")) {
            answer.setAiFeedbackJson(stringVal(payload.get("aiFeedbackJson")));
        }
        if (existing == null) {
            baseMapper.insertAnswer(answer);
        } else {
            baseMapper.updateAnswer(answer);
        }
        return answer;
    }

    @Override
    public List<PracticeAnswerEntity> listAnswers(Long recordId, Long studentId, String role) {
        PracticeEntity record = getById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("record not found");
        }
        assertRecordAccess(record, studentId, role);
        return baseMapper.selectAnswersByRecordId(recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitPractice(Map<String, Object> payload, Long studentId) {
        Long paperId = longVal(payload.get("paperId"));
        if (paperId == null) {
            throw new IllegalArgumentException("paperId is required");
        }
        PaperEntity paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new IllegalArgumentException("paper not found");
        }
        if (!"PUBLISHED".equalsIgnoreCase(paper.getStatus())) {
            throw new IllegalArgumentException("paper is not available for practice");
        }
        assertStudentCourseAccess(paper.getCourseId(), studentId);

        List<PaperQuestionEntity> paperQuestions = paperQuestionMapper.selectByPaperId(paperId);
        if (paperQuestions.isEmpty()) {
            throw new IllegalArgumentException("paper has no questions");
        }
        List<Map<String, Object>> answerItems = parseAnswerItems(payload.get("answers"));

        PracticeEntity record = new PracticeEntity();
        record.setPaperId(paperId);
        record.setCourseId(paper.getCourseId());
        record.setStudentId(studentId);
        record.setStatus(STATUS_PENDING_REVIEW);
        LocalDateTime startedAt = timeVal(payload.get("startedAt"));
        record.setStartedAt(startedAt == null ? LocalDateTime.now() : startedAt);
        record.setSubmittedAt(LocalDateTime.now());
        record.setObjectiveScore(0);
        record.setTotalScore(0);
        record.setDiagnosisStatus(DIAGNOSIS_PENDING);
        record.setDiagnosisJson(null);
        record.setDiagnosisText(null);
        record.setCreatedAt(LocalDateTime.now());
        save(record);

        Map<Long, Map<String, Object>> answerByPaperQuestionId = new LinkedHashMap<>();
        Map<Long, Map<String, Object>> answerByQuestionId = new LinkedHashMap<>();
        for (Map<String, Object> item : answerItems) {
            Long itemPaperQuestionId = longVal(item.get("paperQuestionId"));
            Long itemQuestionId = longVal(item.get("questionId"));
            Map<String, Object> normalizedAnswer = normalizeAnswerObject(item.get("answer"));
            if (itemPaperQuestionId != null) {
                answerByPaperQuestionId.put(itemPaperQuestionId, normalizedAnswer);
            }
            if (itemQuestionId != null) {
                answerByQuestionId.put(itemQuestionId, normalizedAnswer);
            }
        }

        boolean hasSubjectiveQuestion = false;
        for (PaperQuestionEntity paperQuestion : paperQuestions) {
            Map<String, Object> snapshot = JsonUtils.parseObject(paperQuestion.getQuestionSnapshotJson());
            String questionType = normalizeUpper(stringVal(snapshot.get("type")));
            if (!OBJECTIVE_TYPE_SET.contains(questionType)) {
                hasSubjectiveQuestion = true;
            }

            Map<String, Object> submittedAnswer = answerByPaperQuestionId.get(paperQuestion.getId());
            if (submittedAnswer == null) {
                submittedAnswer = answerByQuestionId.get(paperQuestion.getQuestionId());
            }
            if (submittedAnswer == null) {
                submittedAnswer = new LinkedHashMap<>();
            }

            PracticeAnswerEntity answer = new PracticeAnswerEntity();
            answer.setRecordId(record.getId());
            answer.setPaperQuestionId(paperQuestion.getId());
            answer.setQuestionId(paperQuestion.getQuestionId());
            answer.setAnswerJson(JsonUtils.toJson(submittedAnswer));
            answer.setIsCorrect(null);
            answer.setScore(null);
            answer.setAiFeedbackJson(null);
            baseMapper.insertAnswer(answer);
        }

        baseMapper.autoGradeObjectiveAnswers(record.getId());
        record.setObjectiveScore(safeInt(baseMapper.sumObjectiveScoreByRecordId(record.getId())));
        record.setTotalScore(safeInt(baseMapper.sumTotalScoreByRecordId(record.getId())));
        record.setStatus(hasSubjectiveQuestion ? STATUS_PENDING_REVIEW : STATUS_GRADED);
        updateById(record);
        return getRecordDetail(record.getId(), studentId, "STUDENT");
    }

    @Override
    public List<Map<String, Object>> listStudentRecords(Long courseId, Long studentId) {
        assertStudentCourseAccess(courseId, studentId);
        List<PracticeEntity> records = lambdaQuery()
                .eq(PracticeEntity::getCourseId, courseId)
                .eq(PracticeEntity::getStudentId, studentId)
                .orderByDesc(PracticeEntity::getSubmittedAt)
                .orderByDesc(PracticeEntity::getId)
                .list();
        return toStudentRecordSummaries(records);
    }

    @Override
    public List<Map<String, Object>> listTeacherRecords(Long courseId, Long teacherId, String teacherRole) {
        assertTeacherCourseAccess(courseId, teacherId, teacherRole);
        List<PracticeEntity> records = lambdaQuery()
                .eq(PracticeEntity::getCourseId, courseId)
                .orderByDesc(PracticeEntity::getSubmittedAt)
                .orderByDesc(PracticeEntity::getId)
                .list();
        return toTeacherRecordSummaries(records);
    }

    @Override
    public PracticeRecordPageResponse pageTeacherRecords(Long courseId,
                                                         Long teacherId,
                                                         String teacherRole,
                                                         long current,
                                                         long size,
                                                         String status,
                                                         String keyword) {
        assertTeacherCourseAccess(courseId, teacherId, teacherRole);
        String normalizedStatus = normalizePracticeStatusFilter(status);
        String normalizedKeyword = normalizeKeyword(keyword);

        List<PracticeEntity> records = lambdaQuery()
                .eq(PracticeEntity::getCourseId, courseId)
                .orderByDesc(PracticeEntity::getSubmittedAt)
                .orderByDesc(PracticeEntity::getId)
                .list();
        List<Map<String, Object>> summaries = toTeacherRecordSummaries(records);

        long pendingCount = summaries.stream()
                .filter(item -> STATUS_PENDING_REVIEW.equalsIgnoreCase(stringVal(item.get("status"))))
                .count();
        long gradedCount = summaries.stream()
                .filter(item -> STATUS_GRADED.equalsIgnoreCase(stringVal(item.get("status"))))
                .count();
        long diagnosisDoneCount = summaries.stream()
                .filter(item -> DIAGNOSIS_DONE.equalsIgnoreCase(stringVal(item.get("diagnosisStatus"))))
                .count();

        List<Map<String, Object>> filtered = summaries.stream()
                .filter(item -> matchesTeacherRecordFilter(item, normalizedStatus, normalizedKeyword))
                .toList();
        PageResponse<Map<String, Object>> pageResponse = PageResponse.of(filtered, current, size);
        return new PracticeRecordPageResponse(
                pageResponse.records(),
                pageResponse.total(),
                pageResponse.current(),
                pageResponse.size(),
                pageResponse.pages(),
                pendingCount,
                gradedCount,
                diagnosisDoneCount);
    }

    @Override
    public Map<String, Object> getRecordDetail(Long recordId, Long viewerId, String viewerRole) {
        PracticeEntity record = getById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("record not found");
        }
        assertRecordAccess(record, viewerId, viewerRole);

        PaperEntity paper = paperMapper.selectById(record.getPaperId());
        UserEntity student = userService.getById(record.getStudentId());
        List<PracticeAnswerEntity> answers = baseMapper.selectAnswersByRecordId(recordId);

        boolean includeReference = !isStudentRole(viewerRole) || STATUS_GRADED.equalsIgnoreCase(record.getStatus());
        List<Map<String, Object>> answerViews = new ArrayList<>();
        for (PracticeAnswerEntity answer : answers) {
            Map<String, Object> snapshot = JsonUtils.parseObject(answer.getQuestionSnapshotJson());
            if (!includeReference) {
                snapshot.remove("answer");
                snapshot.remove("analysisText");
            } else {
                snapshot = enrichSnapshotReference(snapshot, answer.getQuestionId());
            }

            Map<String, Object> answerView = new LinkedHashMap<>();
            answerView.put("id", answer.getId());
            answerView.put("recordId", answer.getRecordId());
            answerView.put("paperQuestionId", answer.getPaperQuestionId());
            answerView.put("questionId", answer.getQuestionId());
            answerView.put("sortNo", answer.getSortNo());
            answerView.put("fullScore", answer.getPaperQuestionScore());
            answerView.put("score", answer.getScore());
            answerView.put("isCorrect", answer.getIsCorrect());
            answerView.put("studentAnswer", JsonUtils.parseObject(answer.getAnswerJson()));
            answerView.put("snapshot", snapshot);
            answerView.put("aiFeedback", StringUtils.hasText(answer.getAiFeedbackJson()) ? JsonUtils.parseObject(answer.getAiFeedbackJson()) : null);
            answerViews.add(answerView);
        }

        Map<String, Object> recordView = new LinkedHashMap<>();
        recordView.put("id", record.getId());
        recordView.put("paperId", record.getPaperId());
        recordView.put("courseId", record.getCourseId());
        recordView.put("paperTitle", paper == null ? "" : paper.getTitle());
        recordView.put("studentId", record.getStudentId());
        recordView.put("studentName", student == null ? "" : student.getRealName());
        recordView.put("studentNo", student == null ? "" : student.getStudentNo());
        recordView.put("status", record.getStatus());
        recordView.put("startedAt", record.getStartedAt());
        recordView.put("submittedAt", record.getSubmittedAt());
        recordView.put("objectiveScore", record.getObjectiveScore());
        recordView.put("totalScore", record.getTotalScore());
        recordView.put("diagnosisStatus", record.getDiagnosisStatus());
        recordView.put("diagnosisJson", record.getDiagnosisJson());
        recordView.put("diagnosisText", record.getDiagnosisText());
        recordView.put("durationMinutes", paper == null ? null : paper.getDurationMinutes());
        recordView.put("questionCount", answers.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("record", recordView);
        result.put("answers", answerViews);
        return result;
    }

    private Map<String, Object> enrichSnapshotReference(Map<String, Object> snapshot, Long questionId) {
        Map<String, Object> result = snapshot == null ? new LinkedHashMap<>() : new LinkedHashMap<>(snapshot);
        boolean missingAnswer = !result.containsKey("answer") || result.get("answer") == null;
        boolean missingAnalysis = !StringUtils.hasText(stringVal(result.get("analysisText")));
        if (!missingAnswer && !missingAnalysis) {
            return result;
        }
        if (questionId == null) {
            return result;
        }

        QuestionEntity question = questionMapper.selectById(questionId);
        if (question == null) {
            return result;
        }
        if (missingAnswer) {
            result.put("answer", JsonUtils.parseObject(question.getAnswerJson()));
        }
        if (missingAnalysis) {
            result.put("analysisText", question.getAnalysisText());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reviewPractice(Map<String, Object> payload, Long teacherId, String teacherRole) {
        Long recordId = longVal(payload.get("recordId"));
        if (recordId == null) {
            throw new IllegalArgumentException("recordId is required");
        }
        PracticeEntity record = getById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("record not found");
        }
        assertTeacherCourseAccess(record.getCourseId(), teacherId, teacherRole);

        List<Map<String, Object>> reviewItems = parseAnswerItems(payload.get("answers"));
        Map<Long, Map<String, Object>> reviewByAnswerId = new LinkedHashMap<>();
        Map<Long, Map<String, Object>> reviewByPaperQuestionId = new LinkedHashMap<>();
        for (Map<String, Object> item : reviewItems) {
            Long answerId = longVal(item.get("answerId"));
            Long paperQuestionId = longVal(item.get("paperQuestionId"));
            if (answerId != null) {
                reviewByAnswerId.put(answerId, item);
            }
            if (paperQuestionId != null) {
                reviewByPaperQuestionId.put(paperQuestionId, item);
            }
        }

        List<PracticeAnswerEntity> answers = baseMapper.selectAnswersByRecordId(recordId);
        for (PracticeAnswerEntity answer : answers) {
            Map<String, Object> snapshot = JsonUtils.parseObject(answer.getQuestionSnapshotJson());
            String questionType = normalizeUpper(stringVal(snapshot.get("type")));
            if (OBJECTIVE_TYPE_SET.contains(questionType)) {
                continue;
            }
            Map<String, Object> reviewItem = reviewByAnswerId.get(answer.getId());
            if (reviewItem == null) {
                reviewItem = reviewByPaperQuestionId.get(answer.getPaperQuestionId());
            }
            if (reviewItem == null || reviewItem.get("score") == null) {
                throw new IllegalArgumentException("all subjective answers must be graded");
            }
            Integer fullScore = answer.getPaperQuestionScore() == null ? 0 : answer.getPaperQuestionScore();
            Integer reviewScore = intVal(reviewItem.get("score"));
            if (reviewScore == null || reviewScore < 0 || reviewScore > fullScore) {
                throw new IllegalArgumentException("review score is invalid");
            }
            answer.setScore(reviewScore);
            answer.setIsCorrect(reviewScore.equals(fullScore) ? 1 : 0);
            baseMapper.updateReviewResult(answer);
        }

        record.setObjectiveScore(safeInt(baseMapper.sumObjectiveScoreByRecordId(recordId)));
        record.setTotalScore(safeInt(baseMapper.sumTotalScoreByRecordId(recordId)));
        record.setStatus(STATUS_GRADED);
        record.setDiagnosisStatus(DIAGNOSIS_PENDING);
        record.setDiagnosisJson(null);
        record.setDiagnosisText(null);
        updateById(record);
        return getRecordDetail(recordId, teacherId, teacherRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateDiagnosis(Long recordId, Long operatorId, String operatorRole) {
        PracticeEntity record = getById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("record not found");
        }
        assertTeacherCourseAccess(record.getCourseId(), operatorId, operatorRole);
        if (!STATUS_GRADED.equalsIgnoreCase(record.getStatus())) {
            throw new IllegalArgumentException("practice must be graded before AI diagnosis");
        }

        PracticeDiagnosisRequest request = buildDiagnosisRequest(record);
        PracticeDiagnosisResponse response = practiceDiagnosisClient.diagnose(request);
        if (response == null || !response.isSuccess()) {
            throw new IllegalArgumentException("practice diagnosis failed");
        }

        if (response.getAnswerFeedbacks() != null) {
            for (PracticeAnswerFeedbackItem item : response.getAnswerFeedbacks()) {
                if (item.getAnswerId() == null) {
                    continue;
                }
                baseMapper.updateAiFeedback(item.getAnswerId(), JsonUtils.toJson(item));
            }
        }

        record.setDiagnosisStatus(DIAGNOSIS_DONE);
        record.setDiagnosisJson(JsonUtils.toJson(response.getDiagnosisJson()));
        record.setDiagnosisText(response.getDiagnosisText());
        updateById(record);
        return getRecordDetail(recordId, operatorId, operatorRole);
    }

    private PracticeDiagnosisRequest buildDiagnosisRequest(PracticeEntity record) {
        Course course = courseService.getById(record.getCourseId());
        PaperEntity paper = paperMapper.selectById(record.getPaperId());
        UserEntity student = userService.getById(record.getStudentId());
        List<PracticeAnswerEntity> answers = baseMapper.selectAnswersByRecordId(record.getId());

        PracticeDiagnosisRequest request = new PracticeDiagnosisRequest();
        request.setRecordId(record.getId());
        request.setCourseId(record.getCourseId());
        request.setCourseName(course == null ? null : course.getCourseName());
        request.setPaperId(record.getPaperId());
        request.setPaperTitle(paper == null ? null : paper.getTitle());
        request.setStudentId(record.getStudentId());
        request.setStudentName(student == null ? null : student.getRealName());
        request.setObjectiveScore(record.getObjectiveScore());
        request.setTotalScore(record.getTotalScore());

        for (PracticeAnswerEntity answer : answers) {
            Map<String, Object> snapshot = JsonUtils.parseObject(answer.getQuestionSnapshotJson());
            PracticeDiagnosisAnswerItem item = new PracticeDiagnosisAnswerItem();
            item.setAnswerId(answer.getId());
            item.setPaperQuestionId(answer.getPaperQuestionId());
            item.setQuestionId(answer.getQuestionId());
            item.setSortNo(answer.getSortNo());
            item.setQuestionType(stringVal(snapshot.get("type")));
            item.setStem(stringVal(snapshot.get("stem")));
            item.setContent(extractObject(snapshot.get("content")));
            item.setReferenceAnswer(extractObject(snapshot.get("answer")));
            item.setAnalysisText(stringVal(snapshot.get("analysisText")));
            item.setKnowledgePointNames(extractStringList(snapshot.get("knowledgePointNames")));
            item.setStudentAnswer(JsonUtils.parseObject(answer.getAnswerJson()));
            item.setScore(answer.getScore());
            item.setFullScore(answer.getPaperQuestionScore());
            item.setIsCorrect(answer.getIsCorrect());
            request.getAnswers().add(item);
        }
        return request;
    }

    private List<String> extractStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private Map<String, Object> extractObject(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private Map<String, Object> toStudentRecordSummary(PracticeEntity record) {
        return toStudentRecordSummary(record, new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    private Map<String, Object> toStudentRecordSummary(PracticeEntity record,
                                                       Map<Long, PaperEntity> paperCache,
                                                       Map<Long, UserEntity> studentCache,
                                                       Map<Long, Integer> questionCountCache) {
        PracticeEntity enriched = enrichRecord(record, paperCache, studentCache, questionCountCache);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", enriched.getId());
        result.put("paperId", enriched.getPaperId());
        result.put("paperTitle", enriched.getPaperTitle());
        result.put("status", enriched.getStatus());
        result.put("submittedAt", enriched.getSubmittedAt());
        result.put("objectiveScore", enriched.getObjectiveScore());
        result.put("totalScore", enriched.getTotalScore());
        result.put("diagnosisStatus", enriched.getDiagnosisStatus());
        result.put("diagnosisJson", enriched.getDiagnosisJson());
        result.put("diagnosisText", enriched.getDiagnosisText());
        result.put("questionCount", enriched.getQuestionCount());
        result.put("durationMinutes", enriched.getDurationMinutes());
        return result;
    }

    private Map<String, Object> toTeacherRecordSummary(PracticeEntity record) {
        return toTeacherRecordSummary(record, new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    private Map<String, Object> toTeacherRecordSummary(PracticeEntity record,
                                                       Map<Long, PaperEntity> paperCache,
                                                       Map<Long, UserEntity> studentCache,
                                                       Map<Long, Integer> questionCountCache) {
        PracticeEntity enriched = enrichRecord(record, paperCache, studentCache, questionCountCache);
        Map<String, Object> result = toStudentRecordSummary(enriched, paperCache, studentCache, questionCountCache);
        result.put("studentId", enriched.getStudentId());
        result.put("studentName", enriched.getStudentName());
        result.put("studentNo", enriched.getStudentNo());
        return result;
    }

    private PracticeEntity enrichRecord(PracticeEntity record) {
        return enrichRecord(record, new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    private PracticeEntity enrichRecord(PracticeEntity record,
                                        Map<Long, PaperEntity> paperCache,
                                        Map<Long, UserEntity> studentCache,
                                        Map<Long, Integer> questionCountCache) {
        if (record == null) {
            return null;
        }
        PaperEntity paper = record.getPaperId() == null
                ? null
                : paperCache.computeIfAbsent(record.getPaperId(), paperMapper::selectById);
        UserEntity student = record.getStudentId() == null
                ? null
                : studentCache.computeIfAbsent(record.getStudentId(), userService::getById);
        record.setPaperTitle(paper == null ? null : paper.getTitle());
        record.setDurationMinutes(paper == null ? null : paper.getDurationMinutes());
        record.setQuestionCount(paper == null
                ? 0
                : questionCountCache.computeIfAbsent(paper.getId(), paperQuestionMapper::countByPaperId));
        record.setStudentName(student == null ? null : student.getRealName());
        record.setStudentNo(student == null ? null : student.getStudentNo());
        return record;
    }

    private List<Map<String, Object>> toStudentRecordSummaries(List<PracticeEntity> records) {
        Map<Long, PaperEntity> paperCache = new HashMap<>();
        Map<Long, UserEntity> studentCache = new HashMap<>();
        Map<Long, Integer> questionCountCache = new HashMap<>();
        return records.stream()
                .map(record -> toStudentRecordSummary(record, paperCache, studentCache, questionCountCache))
                .toList();
    }

    private List<Map<String, Object>> toTeacherRecordSummaries(List<PracticeEntity> records) {
        Map<Long, PaperEntity> paperCache = new HashMap<>();
        Map<Long, UserEntity> studentCache = new HashMap<>();
        Map<Long, Integer> questionCountCache = new HashMap<>();
        return records.stream()
                .map(record -> toTeacherRecordSummary(record, paperCache, studentCache, questionCountCache))
                .toList();
    }

    private boolean matchesTeacherRecordFilter(Map<String, Object> record,
                                               String normalizedStatus,
                                               String normalizedKeyword) {
        String recordStatus = normalizeUpper(stringVal(record.get("status")));
        if (StringUtils.hasText(normalizedStatus) && !Objects.equals(normalizedStatus, recordStatus)) {
            return false;
        }
        if (!StringUtils.hasText(normalizedKeyword)) {
            return true;
        }
        String target = String.join(" ",
                stringOrEmpty(record.get("studentName")),
                stringOrEmpty(record.get("studentNo")),
                stringOrEmpty(record.get("paperTitle")))
                .toLowerCase(Locale.ROOT);
        return target.contains(normalizedKeyword);
    }

    private List<Map<String, Object>> parseAnswerItems(Object rawValue) {
        if (rawValue == null) {
            return List.of();
        }
        if (!(rawValue instanceof List<?> list)) {
            throw new IllegalArgumentException("answers must be array");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("answer item must be object");
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            result.add(normalized);
        }
        return result;
    }

    private Map<String, Object> normalizeAnswerObject(Object answer) {
        if (answer == null) {
            return new LinkedHashMap<>();
        }
        if (!(answer instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("answer must be object");
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            normalized.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return normalized;
    }

    private void assertRecordAccess(PracticeEntity record, Long viewerId, String viewerRole) {
        if (record == null) {
            throw new IllegalArgumentException("record not found");
        }
        if (isAdminRole(viewerRole)) {
            return;
        }
        if (isStudentRole(viewerRole)) {
            if (!Objects.equals(record.getStudentId(), viewerId)) {
                throw new IllegalArgumentException("forbidden");
            }
            return;
        }
        assertTeacherCourseAccess(record.getCourseId(), viewerId, viewerRole);
    }

    private void assertStudentCourseAccess(Long courseId, Long studentId) {
        Course course = courseService.getById(courseId);
        if (course == null || course.getDeleted() != null && course.getDeleted() == 1) {
            throw new IllegalArgumentException("course not found");
        }
        UserEntity student = userService.getById(studentId);
        if (student == null || !StringUtils.hasText(student.getStudentNo())) {
            throw new IllegalArgumentException("student number is missing");
        }
        CourseMember membership = courseMemberService.lambdaQuery()
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getUserNo, student.getStudentNo())
                .eq(CourseMember::getRoleInCourse, "STUDENT")
                .one();
        if (membership == null) {
            throw new IllegalArgumentException("forbidden");
        }
    }

    private void assertTeacherCourseAccess(Long courseId, Long teacherId, String teacherRole) {
        if (isAdminRole(teacherRole)) {
            return;
        }
        if (!isTeacherRole(teacherRole)) {
            throw new IllegalArgumentException("forbidden");
        }
        Course course = courseService.getById(courseId);
        if (course == null || course.getDeleted() != null && course.getDeleted() == 1) {
            throw new IllegalArgumentException("course not found");
        }
        UserEntity teacher = userService.getById(teacherId);
        if (teacher == null || !StringUtils.hasText(teacher.getTeacherNo())) {
            throw new IllegalArgumentException("teacher number is missing");
        }
        if (!Objects.equals(teacher.getTeacherNo(), course.getTeacherNo())) {
            throw new IllegalArgumentException("forbidden");
        }
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

    private Integer safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePracticeStatusFilter(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = normalizeUpper(status);
        if (!PRACTICE_STATUS_FILTER_SET.contains(normalized)) {
            throw new IllegalArgumentException("unsupported practice status");
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String stringOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
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

    private LocalDateTime timeVal(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        return LocalDateTime.parse(String.valueOf(value));
    }
}
