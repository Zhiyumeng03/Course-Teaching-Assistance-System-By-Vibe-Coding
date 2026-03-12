package com.zym.hd.experiment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zym.hd.experiment.entity.ExperimentEntity;

public interface ExperimentService extends IService<ExperimentEntity> {

    ExperimentEntity createExperiment(ExperimentEntity entity, Long teacherId);

    ExperimentEntity updateExperiment(ExperimentEntity entity);

    boolean deleteExperiment(Long id);

    ExperimentEntity submitExperiment(Long id, Long studentId);
}

