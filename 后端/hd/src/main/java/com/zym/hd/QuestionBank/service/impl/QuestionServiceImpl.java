package com.zym.hd.QuestionBank.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zym.hd.QuestionBank.dto.QuestionPageResponse;
import com.zym.hd.QuestionBank.dto.QuestionStatsDTO;
import com.zym.hd.QuestionBank.entity.QuestionEntity;
import com.zym.hd.QuestionBank.mapper.QuestionMapper;
import com.zym.hd.QuestionBank.service.QuestionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, QuestionEntity> implements QuestionService {

    private static final Set<String> TYPE_SET = Set.of("SINGLE", "MULTI", "JUDGE", "BLANK", "SHORT");
    private static final Set<String> REVIEW_STATUS_SET = Set.of("PENDING", "APPROVED", "REJECTED");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int PENDING_REVIEW_PREVIEW_LIMIT = 20;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionEntity createQuestion(Map<String, Object> payload, Long creatorId, String creatorRole) {
        String type = normalizeType(stringVal(payload.get("type")));
        String stem = stringVal(payload.get("stem"));
        Long courseId = longVal(payload.get("courseId"));
        if (!TYPE_SET.contains(type)) {
            throw new IllegalArgumentException("unsupported type");
        }
        if (!StringUtils.hasText(stem) || courseId == null) {
            throw new IllegalArgumentException("courseId/type/stem is required");
        }

        Object content = payload.get("content");
        Object answer = payload.get("answer");
        validateTypePayload(type, content, answer);

        QuestionEntity entity = new QuestionEntity();
        entity.setCourseId(courseId);
        entity.setCreatorId(creatorId);
        entity.setCreatorRole(creatorRole);
        entity.setType(type);
        entity.setStem(stem);
        entity.setContentJson(toJson(content));
        entity.setAnswerJson(toJson(answer));
        entity.setAnalysisText(stringVal(payload.get("analysisText")));
        entity.setDifficulty(intVal(payload.get("difficulty")));
        entity.setSourceType(resolveSourceType(payload.get("sourceType"), creatorRole));
        entity.setVisibility(resolveVisibility(payload.get("visibility")));
        entity.setReviewStatus(resolveInitialReviewStatus(payload.get("reviewStatus"), creatorRole));
        entity.setUsageCount(0);
        entity.setDeleted(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        save(entity);

        syncKnowledgePointRelations(entity.getId(), parseKnowledgePointIds(payload.get("knowledgePointIds")));
        return attachKnowledgePointIds(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionEntity updateQuestion(Map<String, Object> payload, Long operatorId, String operatorRole) {
        Long id = longVal(payload.get("id"));
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        QuestionEntity db = getQuestionById(id);
        if (db == null) {
            throw new IllegalArgumentException("question not found");
        }
        boolean teacherOrAdmin = "TEACHER".equalsIgnoreCase(operatorRole) || "ADMIN".equalsIgnoreCase(operatorRole);
        if (!teacherOrAdmin && !operatorId.equals(db.getCreatorId())) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!teacherOrAdmin && "APPROVED".equalsIgnoreCase(db.getReviewStatus())) {
            throw new IllegalArgumentException("approved question cannot be edited by student");
        }

        boolean hasType = payload.containsKey("type");
        boolean hasContent = payload.containsKey("content");
        boolean hasAnswer = payload.containsKey("answer");
        String type = hasType ? normalizeType(stringVal(payload.get("type"))) : db.getType();
        Object content = hasContent ? payload.get("content") : null;
        Object answer = hasAnswer ? payload.get("answer") : null;
        if (hasType || hasContent || hasAnswer) {
            if (!hasContent || !hasAnswer) {
                throw new IllegalArgumentException("content and answer must be provided together when updating type structure");
            }
            validateTypePayload(type, content, answer);
            db.setType(type);
            db.setContentJson(toJson(content));
            db.setAnswerJson(toJson(answer));
        }

        if (payload.containsKey("courseId")) {
            db.setCourseId(longVal(payload.get("courseId")));
        }
        if (payload.containsKey("stem")) {
            db.setStem(stringVal(payload.get("stem")));
        }
        if (payload.containsKey("analysisText")) {
            db.setAnalysisText(stringVal(payload.get("analysisText")));
        }
        if (payload.containsKey("difficulty")) {
            db.setDifficulty(intVal(payload.get("difficulty")));
        }
        if (payload.containsKey("sourceType")) {
            db.setSourceType(resolveSourceType(payload.get("sourceType"), db.getCreatorRole()));
        }
        if (payload.containsKey("visibility")) {
            db.setVisibility(resolveVisibility(payload.get("visibility")));
        }
        if (payload.containsKey("reviewStatus")) {
            if (!teacherOrAdmin) {
                throw new IllegalArgumentException("student cannot update review status");
            }
            db.setReviewStatus(normalizeReviewStatus(stringVal(payload.get("reviewStatus"))));
        }
        if (!teacherOrAdmin) {
            db.setReviewStatus("PENDING");
        }
        db.setUpdatedAt(LocalDateTime.now());
        updateById(db);
        if (payload.containsKey("knowledgePointIds")) {
            syncKnowledgePointRelations(db.getId(), parseKnowledgePointIds(payload.get("knowledgePointIds")));
        }
        return attachKnowledgePointIds(db);
    }

    @Override
    public boolean deleteQuestion(Long id, Long operatorId, String operatorRole) {
        QuestionEntity db = getQuestionById(id);
        if (db == null) {
            return false;
        }
        boolean teacherOrAdmin = "TEACHER".equalsIgnoreCase(operatorRole) || "ADMIN".equalsIgnoreCase(operatorRole);
        if (!teacherOrAdmin && !operatorId.equals(db.getCreatorId())) {
            throw new IllegalArgumentException("forbidden");
        }
        db.setDeleted(1);
        db.setUpdatedAt(LocalDateTime.now());
        return updateById(db);
    }

    @Override
    public QuestionEntity getQuestionById(Long id) {
        QuestionEntity entity = lambdaQuery()
                .eq(QuestionEntity::getId, id)
                .eq(QuestionEntity::getDeleted, 0)
                .one();
        return attachKnowledgePointIds(entity);
    }

    @Override
    public List<QuestionEntity> listByCourseId(Long courseId, Long viewerId, String viewerRole) {
        List<QuestionEntity> list = lambdaQuery()
                .eq(QuestionEntity::getCourseId, courseId)
                .eq(QuestionEntity::getDeleted, 0)
                .and(
                        !"TEACHER".equalsIgnoreCase(viewerRole) && !"ADMIN".equalsIgnoreCase(viewerRole),
                        wrapper -> wrapper
                                .and(inner -> inner.eq(QuestionEntity::getReviewStatus, "APPROVED")
                                        .and(visible -> visible.ne(QuestionEntity::getVisibility, "false")
                                                .or()
                                                .isNull(QuestionEntity::getVisibility)))
                                .or()
                                .eq(QuestionEntity::getCreatorId, viewerId))
                .orderByAsc(QuestionEntity::getId)
                .list();
        list.forEach(this::attachKnowledgePointIds);
        return list;
    }

    @Override
    public QuestionPageResponse pageByCourseId(Long courseId,
                                               Long viewerId,
                                               String viewerRole,
                                               long current,
                                               long size,
                                               String type,
                                               String reviewStatus,
                                               String keyword) {
        long safeCurrent = Math.max(1, current);
        long safeSize = Math.max(1, size);
        String normalizedType = normalizeFilterType(type);
        String normalizedReviewStatus = normalizeFilterReviewStatus(reviewStatus);
        String normalizedKeyword = normalizeKeyword(keyword);

        long total = baseMapper.countPageRecords(
                courseId,
                viewerId,
                viewerRole,
                normalizedType,
                normalizedReviewStatus,
                normalizedKeyword);
        long pages = total == 0 ? 0 : (total + safeSize - 1) / safeSize;
        if (pages > 0 && safeCurrent > pages) {
            safeCurrent = pages;
        }
        long offset = (safeCurrent - 1) * safeSize;

        List<QuestionEntity> records = baseMapper.selectPageRecords(
                courseId,
                viewerId,
                viewerRole,
                normalizedType,
                normalizedReviewStatus,
                normalizedKeyword,
                offset,
                safeSize);
        records.forEach(this::attachKnowledgePointIds);

        QuestionStatsDTO stats = baseMapper.selectQuestionStats(courseId, viewerId, viewerRole);
        if (stats == null) {
            stats = new QuestionStatsDTO();
        }

        List<QuestionEntity> pendingReviewQuestions =
                ("TEACHER".equalsIgnoreCase(viewerRole) || "ADMIN".equalsIgnoreCase(viewerRole))
                        ? baseMapper.selectPendingReviewQuestions(courseId, PENDING_REVIEW_PREVIEW_LIMIT)
                        : List.of();
        pendingReviewQuestions.forEach(this::attachKnowledgePointIds);

        return new QuestionPageResponse(
                records,
                total,
                safeCurrent,
                safeSize,
                pages,
                stats.getApprovedCount(),
                stats.getPendingCount(),
                stats.getPendingReviewCount(),
                pendingReviewQuestions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionEntity reviewQuestion(Long id, String reviewStatus, Long operatorId, String operatorRole) {
        if (!"TEACHER".equalsIgnoreCase(operatorRole) && !"ADMIN".equalsIgnoreCase(operatorRole)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        QuestionEntity db = getQuestionById(id);
        if (db == null) {
            throw new IllegalArgumentException("question not found");
        }
        db.setReviewStatus(normalizeReviewStatus(reviewStatus));
        db.setUpdatedAt(LocalDateTime.now());
        updateById(db);
        return attachKnowledgePointIds(db);
    }

    @Override
    public Map<String, String> convertQuestionJson(String type, Object content, Object answer) {
        String normalizedType = normalizeType(type);
        validateTypePayload(normalizedType, content, answer);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("contentJson", toJson(content));
        result.put("answerJson", toJson(answer));
        return result;
    }

    private void validateTypePayload(String type, Object content, Object answer) {
        if (!TYPE_SET.contains(type)) {
            throw new IllegalArgumentException("unsupported type");
        }
        if (!(content instanceof Map<?, ?> contentMap)) {
            throw new IllegalArgumentException("content must be object");
        }
        if (!(answer instanceof Map<?, ?> answerMap)) {
            throw new IllegalArgumentException("answer must be object");
        }

        if ("SINGLE".equals(type)) {
            validateOptionMap(contentMap, 2);
            validateOptionAnswerMap(contentMap, answerMap, false);
            return;
        }
        if ("MULTI".equals(type)) {
            validateOptionMap(contentMap, 2);
            validateOptionAnswerMap(contentMap, answerMap, true);
            return;
        }
        if ("JUDGE".equals(type)) {
            validateOptionMap(contentMap, 2);
            if (!contentMap.containsKey("T") || !contentMap.containsKey("F")) {
                throw new IllegalArgumentException("JUDGE content must contain T/F");
            }
            validateOptionAnswerMap(contentMap, answerMap, false);
            return;
        }
        if ("BLANK".equals(type)) {
            validateBlankMap(contentMap, "content");
            validateBlankMap(answerMap, "answer");
            return;
        }
        if ("SHORT".equals(type)) {
            validateShortPayload(contentMap, answerMap);
        }
    }

    private void validateOptionMap(Map<?, ?> contentMap, int minSize) {
        if (contentMap.size() < minSize) {
            throw new IllegalArgumentException("content options are required");
        }
        for (Map.Entry<?, ?> entry : contentMap.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !StringUtils.hasText(key)) {
                throw new IllegalArgumentException("option key is required");
            }
            if (!(entry.getValue() instanceof String text) || !StringUtils.hasText(text)) {
                throw new IllegalArgumentException("option text is required");
            }
        }
    }

    private void validateOptionAnswerMap(Map<?, ?> contentMap, Map<?, ?> answerMap, boolean allowMultiple) {
        if (answerMap.isEmpty()) {
            throw new IllegalArgumentException("answer is required");
        }
        if (!allowMultiple && answerMap.size() != 1) {
            throw new IllegalArgumentException("single answer must contain one option");
        }
        for (Map.Entry<?, ?> entry : answerMap.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !StringUtils.hasText(key)) {
                throw new IllegalArgumentException("answer key is required");
            }
            if (!contentMap.containsKey(key)) {
                throw new IllegalArgumentException("answer key not found in content");
            }
            Object expected = contentMap.get(key);
            if (!(entry.getValue() instanceof String text) || !Objects.equals(expected, text)) {
                throw new IllegalArgumentException("answer value must match content");
            }
        }
    }

    private void validateBlankMap(Map<?, ?> payload, String fieldName) {
        if (payload.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        for (Map.Entry<?, ?> entry : payload.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !StringUtils.hasText(key)) {
                throw new IllegalArgumentException(fieldName + " key is required");
            }
            if (!(entry.getValue() instanceof String text) || !StringUtils.hasText(text)) {
                throw new IllegalArgumentException(fieldName + " value is required");
            }
        }
    }

    private void validateShortPayload(Map<?, ?> contentMap, Map<?, ?> answerMap) {
        if (!contentMap.isEmpty()) {
            Object guide = contentMap.get("guide");
            if (guide != null && (!(guide instanceof String) || !StringUtils.hasText((String) guide))) {
                throw new IllegalArgumentException("SHORT content.guide must be text");
            }
        }
        Object answerText = answerMap.get("text");
        if (!(answerText instanceof String) || !StringUtils.hasText((String) answerText)) {
            throw new IllegalArgumentException("SHORT answer.text is required");
        }
    }

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return "";
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String resolveSourceType(Object value, String creatorRole) {
        String sourceType = stringVal(value);
        if (StringUtils.hasText(sourceType)) {
            return sourceType;
        }
        return "STUDENT".equalsIgnoreCase(creatorRole) ? "STUDENT_SUBMITTED" : "MANUAL";
    }

    private String resolveVisibility(Object value) {
        String visibility = stringVal(value);
        if (!StringUtils.hasText(visibility)) {
            return "true";
        }
        String normalized = visibility.trim().toLowerCase(Locale.ROOT);
        if (!"true".equals(normalized) && !"false".equals(normalized)) {
            throw new IllegalArgumentException("visibility must be true/false");
        }
        return normalized;
    }

    private String resolveInitialReviewStatus(Object value, String creatorRole) {
        if ("STUDENT".equalsIgnoreCase(creatorRole)) {
            return "PENDING";
        }
        return StringUtils.hasText(stringVal(value)) ? normalizeReviewStatus(stringVal(value)) : "APPROVED";
    }

    private String normalizeReviewStatus(String reviewStatus) {
        String normalized = normalizeType(reviewStatus);
        if (!REVIEW_STATUS_SET.contains(normalized)) {
            throw new IllegalArgumentException("unsupported review status");
        }
        return normalized;
    }

    private String normalizeFilterType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return normalizeType(type);
    }

    private String normalizeFilterReviewStatus(String reviewStatus) {
        if (!StringUtils.hasText(reviewStatus)) {
            return null;
        }
        return normalizeReviewStatus(reviewStatus);
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
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

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("json serialize failed", e);
        }
    }

    private List<Long> parseKnowledgePointIds(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("knowledgePointIds must be array");
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (Object item : list) {
            Long id = longVal(item);
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("knowledgePointIds item invalid");
            }
            ids.add(id);
        }
        return new ArrayList<>(ids);
    }
    
    private void syncKnowledgePointRelations(Long questionId, List<Long> knowledgePointIds) {
        baseMapper.deleteQuestionKnowledgeRel(questionId);
        for (Long knowledgePointId : knowledgePointIds) {
            baseMapper.insertQuestionKnowledgeRel(questionId, knowledgePointId);
        }
    }

    private QuestionEntity attachKnowledgePointIds(QuestionEntity entity) {
        if (entity == null) {
            return null;
        }
        entity.setKnowledgePointIds(baseMapper.selectKnowledgePointIds(entity.getId()));
        return entity;
    }
}

