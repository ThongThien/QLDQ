package com.qldq.qldqapi.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PagedResponse<T> from(Page<?> source, List<T> content) {
        return new PagedResponse<>(content, source.getNumber(), source.getSize(), source.getTotalElements(),
                source.getTotalPages(), source.isFirst(), source.isLast());
    }
}
