package com.zym.hd.QuestionBank.dto;

import com.zym.hd.QuestionBank.entity.QuestionEntity;
import java.util.List;

public record QuestionPageResponse(
        List<QuestionEntity> records,
        long total,
        long current,
        long size,
        long pages,
        long approvedCount,
        long pendingCount,
        long pendingReviewCount,
        List<QuestionEntity> pendingReviewQuestions
) {
}
