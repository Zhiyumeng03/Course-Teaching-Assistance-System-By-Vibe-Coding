package com.zym.hd.course.dto;

import java.util.Collections;
import java.util.List;

public record PageResponse<T>(
        List<T> records,
        long total,
        long current,
        long size,
        long pages
) {

    public static <T> PageResponse<T> of(List<T> source, long current, long size) {
        List<T> safeSource = source == null ? Collections.emptyList() : source;
        long safeSize = Math.max(1, size);
        long total = safeSource.size();
        long pages = total == 0 ? 0 : (total + safeSize - 1) / safeSize;
        long safeCurrent = Math.max(1, current);

        if (pages > 0 && safeCurrent > pages) {
            safeCurrent = pages;
        }

        int fromIndex = (int) Math.min((safeCurrent - 1) * safeSize, total);
        int toIndex = (int) Math.min(fromIndex + safeSize, total);

        return new PageResponse<>(
                safeSource.subList(fromIndex, toIndex),
                total,
                safeCurrent,
                safeSize,
                pages
        );
    }
}
