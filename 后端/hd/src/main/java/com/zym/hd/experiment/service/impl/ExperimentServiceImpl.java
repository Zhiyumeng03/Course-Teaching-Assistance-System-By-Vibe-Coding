package com.zym.hd.experiment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zym.hd.experiment.entity.ExperimentEntity;
import com.zym.hd.experiment.mapper.ExperimentMapper;
import com.zym.hd.experiment.service.ExperimentService;
import com.zym.hd.user.entity.UserEntity;
import com.zym.hd.user.service.UserService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExperimentServiceImpl extends ServiceImpl<ExperimentMapper, ExperimentEntity> implements ExperimentService {

    private final UserService userService;

    public ExperimentServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ExperimentEntity createExperiment(ExperimentEntity entity, Long teacherId) {
        if (entity == null || !StringUtils.hasText(entity.getTitle()) || entity.getCourseId() == null) {
            throw new IllegalArgumentException("courseId and title are required");
        }
        // 通过 userId 查询教师工号并设置 creatorNo
        UserEntity teacher = userService.getById(teacherId);
        String teacherNo = (teacher != null && StringUtils.hasText(teacher.getTeacherNo()))
                ? teacher.getTeacherNo() : String.valueOf(teacherId);
        entity.setCreatorNo(teacherNo);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("DRAFT");
        }
        if (entity.getAllowResubmit() == null) {
            entity.setAllowResubmit(1);
        }
        if (entity.getAttachmentIds() != null) {
            entity.setAttachmentIds(entity.getAttachmentIds().trim());
            if (entity.getAttachmentIds().isEmpty()) {
                entity.setAttachmentIds(null);
            }
        }
        if (entity.getMaxScore() == null) {
            entity.setMaxScore(100);
        }
        if (entity.getDeleted() == null) {
            entity.setDeleted(0);
        }
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        save(entity);
        return entity;
    }

    @Override
    public ExperimentEntity updateExperiment(ExperimentEntity entity) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalArgumentException("id is required");
        }
        ExperimentEntity db = getById(entity.getId());
        if (db == null || (db.getDeleted() != null && db.getDeleted() == 1)) {
            throw new IllegalArgumentException("experiment not found");
        }
        if (StringUtils.hasText(entity.getTitle())) {
            db.setTitle(entity.getTitle());
        }
        if (entity.getCourseId() != null) {
            db.setCourseId(entity.getCourseId());
        }
        if (entity.getObjective() != null) {
            db.setObjective(entity.getObjective());
        }
        if (entity.getContentHtml() != null) {
            db.setContentHtml(entity.getContentHtml());
        }
        if (entity.getContentText() != null) {
            db.setContentText(entity.getContentText());
        }
        if (entity.getStartTime() != null) {
            db.setStartTime(entity.getStartTime());
        }
        if (entity.getDeadline() != null) {
            db.setDeadline(entity.getDeadline());
        }
        if (entity.getMaxScore() != null) {
            db.setMaxScore(entity.getMaxScore());
        }
        if (entity.getAllowResubmit() != null) {
            db.setAllowResubmit(entity.getAllowResubmit());
        }
        if (entity.getAttachmentIds() != null) {
            String attachmentIds = entity.getAttachmentIds().trim();
            db.setAttachmentIds(attachmentIds.isEmpty() ? null : attachmentIds);
        }
        if (StringUtils.hasText(entity.getStatus())) {
            db.setStatus(entity.getStatus());
        }
        db.setUpdatedAt(LocalDateTime.now());
        updateById(db);
        return db;
    }

    @Override
    public boolean deleteExperiment(Long id) {
        ExperimentEntity db = getById(id);
        if (db == null) {
            return false;
        }
        db.setDeleted(1);
        db.setUpdatedAt(LocalDateTime.now());
        return updateById(db);
    }

    @Override
    public ExperimentEntity submitExperiment(Long id, Long studentId) {
        ExperimentEntity db = getById(id);
        if (db == null || (db.getDeleted() != null && db.getDeleted() == 1)) {
            throw new IllegalArgumentException("experiment not found");
        }
        db.setStatus("SUBMITTED");
        db.setUpdatedAt(LocalDateTime.now());
        updateById(db);
        return db;
    }
}

