package com.zym.hd.QuestionBank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zym.hd.QuestionBank.entity.KnowledgeEntity;

public interface KnowledgeService extends IService<KnowledgeEntity> {

    String getKnowledgeTreeJson(Long courseId);

    int seedComputerScienceTree(Long courseId);

    boolean createKnowledgePoint(KnowledgeEntity entity);
}

