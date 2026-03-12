package com.zym.hd.QuestionBank.service.impl;

import com.zym.hd.QuestionBank.client.AiQuestionClient;
import com.zym.hd.QuestionBank.config.AiQuestionProperties;
import com.zym.hd.QuestionBank.dto.AiGenerateQuestionRequest;
import com.zym.hd.QuestionBank.dto.AiGenerateQuestionResponse;
import com.zym.hd.QuestionBank.dto.AiGeneratedQuestionItem;
import com.zym.hd.QuestionBank.dto.PythonGenerateRequest;
import com.zym.hd.QuestionBank.dto.PythonGenerateResponse;
import com.zym.hd.QuestionBank.dto.PythonGeneratedQuestionItem;
import com.zym.hd.QuestionBank.entity.KnowledgeEntity;
import com.zym.hd.QuestionBank.service.AiQuestionService;
import com.zym.hd.QuestionBank.service.KnowledgeService;
import com.zym.hd.QuestionBank.service.QuestionService;
import com.zym.hd.course.entity.Course;
import com.zym.hd.course.service.CourseService;
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
import org.springframework.util.StringUtils;

@Service
public class AiQuestionServiceImpl implements AiQuestionService {

    private static final Set<String> TYPE_SET = Set.of("SINGLE", "MULTI", "JUDGE", "BLANK", "SHORT", "MIXED");

    private final AiQuestionClient aiQuestionClient;
    private final AiQuestionProperties properties;
    private final QuestionService questionService;
    private final CourseService courseService;
    private final KnowledgeService knowledgeService;

    public AiQuestionServiceImpl(
            AiQuestionClient aiQuestionClient,
            AiQuestionProperties properties,
            QuestionService questionService,
            CourseService courseService,
            KnowledgeService knowledgeService) {
        this.aiQuestionClient = aiQuestionClient;
        this.properties = properties;
        this.questionService = questionService;
        this.courseService = courseService;
        this.knowledgeService = knowledgeService;
    }

    @Override
    public AiGenerateQuestionResponse generateQuestions(AiGenerateQuestionRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("AI question generation is disabled");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        Long courseId = request.getCourseId();
        if (courseId == null) {
            throw new IllegalArgumentException("courseId is required");
        }
        Course course = courseService.getById(courseId);
        if (course == null || course.getDeleted() != null && course.getDeleted() == 1) {
            throw new IllegalArgumentException("course not found");
        }

        String questionType = normalizeType(request.getQuestionType());
        if (!TYPE_SET.contains(questionType)) {
            throw new IllegalArgumentException("unsupported questionType");
        }

        Integer difficulty = request.getDifficulty() == null ? 3 : request.getDifficulty();
        if (difficulty < 1 || difficulty > 5) {
            throw new IllegalArgumentException("difficulty must be between 1 and 5");
        }

        int count = request.getCount() == null ? 1 : request.getCount();
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than 0");
        }
        count = Math.min(count, Math.max(properties.getMaxCount(), 1));

        List<Long> requestedKnowledgeIds = request.getKnowledgePointIds() == null
                ? List.of()
                : request.getKnowledgePointIds();
        List<KnowledgeEntity> selectedKnowledge = resolveKnowledgePoints(courseId, requestedKnowledgeIds);
        Map<String, Long> knowledgeNameIdMap = selectedKnowledge.stream()
                .collect(Collectors.toMap(
                        item -> normalizeKnowledgeName(item.getName()),
                        KnowledgeEntity::getId,
                        (left, right) -> left,
                        LinkedHashMap::new));

        PythonGenerateRequest pythonRequest = new PythonGenerateRequest();
        pythonRequest.setCourseId(courseId);
        pythonRequest.setCourseName(course.getCourseName());
        pythonRequest.setQuestionType(questionType);
        pythonRequest.setDifficulty(difficulty);
        pythonRequest.setCount(count);
        pythonRequest.setLanguage("zh-CN");
        pythonRequest.setExtraRequirements(request.getExtraRequirements());
        pythonRequest.setKnowledgePoints(selectedKnowledge.stream()
                .map(KnowledgeEntity::getName)
                .filter(StringUtils::hasText)
                .toList());

        PythonGenerateResponse pythonResponse = aiQuestionClient.generateQuestions(pythonRequest);
        if (pythonResponse == null || !pythonResponse.isSuccess()) {
            throw new IllegalArgumentException("AI service failed to generate questions");
        }
        if (pythonResponse.getQuestions() == null || pythonResponse.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("AI service returned no valid questions");
        }

        AiGenerateQuestionResponse response = new AiGenerateQuestionResponse();
        response.setSuccess(true);
        response.setModel(pythonResponse.getModel());

        for (PythonGeneratedQuestionItem item : pythonResponse.getQuestions()) {
            AiGeneratedQuestionItem normalized = normalizeQuestion(item, difficulty, requestedKnowledgeIds, knowledgeNameIdMap);
            response.getQuestions().add(normalized);
        }
        if (response.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("AI service returned no valid questions");
        }
        return response;
    }

    private AiGeneratedQuestionItem normalizeQuestion(
            PythonGeneratedQuestionItem item,
            Integer fallbackDifficulty,
            List<Long> fallbackKnowledgeIds,
            Map<String, Long> knowledgeNameIdMap) {
        if (item == null) {
            throw new IllegalArgumentException("AI question item is empty");
        }
        String type = normalizeType(item.getType());
        if ("MIXED".equals(type) || !TYPE_SET.contains(type)) {
            throw new IllegalArgumentException("AI question type is invalid");
        }
        if (!StringUtils.hasText(item.getStem())) {
            throw new IllegalArgumentException("AI question stem is required");
        }
        Map<String, Object> content = item.getContent() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(item.getContent());
        Map<String, Object> answer = item.getAnswer() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(item.getAnswer());
        questionService.convertQuestionJson(type, content, answer);

        AiGeneratedQuestionItem normalized = new AiGeneratedQuestionItem();
        normalized.setType(type);
        normalized.setStem(item.getStem().trim());
        normalized.setContent(content);
        normalized.setAnswer(answer);
        normalized.setAnalysisText(StringUtils.hasText(item.getAnalysisText()) ? item.getAnalysisText().trim() : null);
        normalized.setDifficulty(normalizeDifficulty(item.getDifficulty(), fallbackDifficulty));
        normalized.setKnowledgePointIds(resolveKnowledgeIds(item.getKnowledgePointNames(), fallbackKnowledgeIds, knowledgeNameIdMap));
        normalized.setSourceType("AI");
        normalized.setReviewStatus("APPROVED");
        normalized.setVisibility("true");
        return normalized;
    }

    private List<KnowledgeEntity> resolveKnowledgePoints(Long courseId, List<Long> knowledgePointIds) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return List.of();
        }
        List<KnowledgeEntity> entities = knowledgeService.listByIds(knowledgePointIds);
        if (entities.size() != new LinkedHashSet<>(knowledgePointIds).size()) {
            throw new IllegalArgumentException("knowledgePointIds contain invalid item");
        }
        boolean invalid = entities.stream().anyMatch(item -> !Objects.equals(item.getCourseId(), courseId));
        if (invalid) {
            throw new IllegalArgumentException("knowledge point does not belong to current course");
        }
        return entities.stream()
                .sorted((left, right) -> {
                    int leftIndex = knowledgePointIds.indexOf(left.getId());
                    int rightIndex = knowledgePointIds.indexOf(right.getId());
                    return Integer.compare(leftIndex, rightIndex);
                })
                .toList();
    }

    private List<KnowledgeEntity> resolveCourseKnowledgePoints(Long courseId) {
        return knowledgeService.list().stream()
                .filter(item -> Objects.equals(item.getCourseId(), courseId))
                .sorted((left, right) -> {
                    int sortDiff = Integer.compare(left.getSortNo() == null ? 0 : left.getSortNo(), right.getSortNo() == null ? 0 : right.getSortNo());
                    if (sortDiff != 0) {
                        return sortDiff;
                    }
                    return Long.compare(left.getId() == null ? 0L : left.getId(), right.getId() == null ? 0L : right.getId());
                })
                .toList();
    }
    private List<Long> resolveKnowledgeIds(
            List<String> knowledgePointNames,
            List<Long> fallbackKnowledgeIds,
            Map<String, Long> knowledgeNameIdMap) {
        if (knowledgePointNames == null || knowledgePointNames.isEmpty()) {
            return new ArrayList<>(fallbackKnowledgeIds);
        }
        LinkedHashSet<Long> matched = new LinkedHashSet<>();
        for (String name : knowledgePointNames) {
            Long id = knowledgeNameIdMap.get(normalizeKnowledgeName(name));
            if (id != null) {
                matched.add(id);
            }
        }
        if (matched.isEmpty()) {
            matched.addAll(fallbackKnowledgeIds);
        }
        return new ArrayList<>(matched);
    }

    private Integer normalizeDifficulty(Integer itemDifficulty, Integer fallbackDifficulty) {
        int value = itemDifficulty == null ? (fallbackDifficulty == null ? 3 : fallbackDifficulty) : itemDifficulty;
        if (value < 1) {
            return 1;
        }
        if (value > 5) {
            return 5;
        }
        return value;
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeKnowledgeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}