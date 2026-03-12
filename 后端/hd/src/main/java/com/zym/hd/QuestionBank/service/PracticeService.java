package com.zym.hd.QuestionBank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zym.hd.QuestionBank.dto.PracticeRecordPageResponse;
import com.zym.hd.QuestionBank.entity.PracticeAnswerEntity;
import com.zym.hd.QuestionBank.entity.PracticeEntity;
import java.util.List;
import java.util.Map;

public interface PracticeService extends IService<PracticeEntity> {

    PracticeEntity createRecord(Map<String, Object> payload, Long studentId);

    PracticeEntity updateRecord(Map<String, Object> payload, Long studentId);

    boolean deleteRecord(Long id, Long studentId);

    PracticeEntity getRecordById(Long id, Long studentId, String role);

    List<PracticeEntity> listByStudent(Long studentId);

    PracticeAnswerEntity saveAnswer(Map<String, Object> payload, Long studentId);

    List<PracticeAnswerEntity> listAnswers(Long recordId, Long studentId, String role);

    Map<String, Object> submitPractice(Map<String, Object> payload, Long studentId);

    List<Map<String, Object>> listStudentRecords(Long courseId, Long studentId);

    List<Map<String, Object>> listTeacherRecords(Long courseId, Long teacherId, String teacherRole);

    PracticeRecordPageResponse pageTeacherRecords(Long courseId,
                                                  Long teacherId,
                                                  String teacherRole,
                                                  long current,
                                                  long size,
                                                  String status,
                                                  String keyword);

    Map<String, Object> getRecordDetail(Long recordId, Long viewerId, String viewerRole);

    Map<String, Object> reviewPractice(Map<String, Object> payload, Long teacherId, String teacherRole);

    Map<String, Object> generateDiagnosis(Long recordId, Long operatorId, String operatorRole);
}

