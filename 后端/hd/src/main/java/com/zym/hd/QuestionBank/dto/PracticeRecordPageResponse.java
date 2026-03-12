package com.zym.hd.QuestionBank.dto;

import java.util.List;
import java.util.Map;

public record PracticeRecordPageResponse(
        List<Map<String, Object>> records,
        long total,
        long current,
        long size,
        long pages,
        long pendingCount,
        long gradedCount,
        long diagnosisDoneCount
) {
}
