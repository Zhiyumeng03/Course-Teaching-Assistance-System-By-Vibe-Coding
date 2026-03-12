package com.zym.hd.QuestionBank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zym.hd.QuestionBank.entity.PaperEntity;
import com.zym.hd.QuestionBank.entity.PaperQuestionEntity;
import com.zym.hd.course.dto.PageResponse;
import java.util.List;
import java.util.Map;

public interface PaperService extends IService<PaperEntity> {

    PaperEntity createPaper(Map<String, Object> payload, Long creatorId, String operatorRole);

    PaperEntity updatePaper(Map<String, Object> payload, Long operatorId, String operatorRole);

    boolean deletePaper(Long id, Long operatorId, String operatorRole);

    PaperEntity publishPaper(Long id, Long operatorId, String operatorRole);

    PaperEntity closePaper(Long id, Long operatorId, String operatorRole);

    PaperEntity getPaperById(Long id, Long viewerId, String viewerRole);

    List<PaperEntity> listByCourseId(Long courseId, Long viewerId, String viewerRole);

    PageResponse<PaperEntity> pageByCourseId(Long courseId,
                                             Long viewerId,
                                             String viewerRole,
                                             long current,
                                             long size,
                                             String keyword);

    List<PaperQuestionEntity> bindQuestions(Long paperId, List<Map<String, Object>> questionItems, Long operatorId, String operatorRole);

    List<Map<String, Object>> listPaperQuestions(Long paperId, Long viewerId, String viewerRole);

    Map<String, Object> getPaperDetail(Long paperId, Long viewerId, String viewerRole);
}

