package com.zym.hd.QuestionBank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zym.hd.QuestionBank.dto.QuestionPageResponse;
import com.zym.hd.QuestionBank.entity.QuestionEntity;
import java.util.List;
import java.util.Map;

public interface QuestionService extends IService<QuestionEntity> {

    QuestionEntity createQuestion(Map<String, Object> payload, Long creatorId, String creatorRole);

    QuestionEntity updateQuestion(Map<String, Object> payload, Long operatorId, String operatorRole);

    boolean deleteQuestion(Long id, Long operatorId, String operatorRole);

    QuestionEntity getQuestionById(Long id);

    List<QuestionEntity> listByCourseId(Long courseId, Long viewerId, String viewerRole);

    QuestionPageResponse pageByCourseId(Long courseId,
                                        Long viewerId,
                                        String viewerRole,
                                        long current,
                                        long size,
                                        String type,
                                        String reviewStatus,
                                        String keyword);

    QuestionEntity reviewQuestion(Long id, String reviewStatus, Long operatorId, String operatorRole);

    Map<String, String> convertQuestionJson(String type, Object content, Object answer);
}

