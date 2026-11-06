package com.qldq.qldqapi.controller;

import com.qldq.qldqapi.exception.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class PageRequests {
    private PageRequests() {
    }

    static Pageable of(int page, int size, Sort sort) {
        if (page < 0) {
            throw new BusinessException("Số trang không được âm");
        }
        if (size < 1 || size > 100) {
            throw new BusinessException("Kích thước trang phải từ 1 đến 100");
        }
        return PageRequest.of(page, size, sort);
    }
}
