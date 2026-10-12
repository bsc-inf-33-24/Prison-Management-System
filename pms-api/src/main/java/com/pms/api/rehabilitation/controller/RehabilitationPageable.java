package com.pms.api.rehabilitation.controller;

import org.springframework.data.domain.Pageable;

import java.util.Set;

final class RehabilitationPageable {

    private RehabilitationPageable() {
    }

    static void validate(Pageable pageable, Set<String> allowedSorts) {
        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("Page size must not exceed 100.");
        }
        pageable.getSort().forEach(order -> {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new IllegalArgumentException("Sorting by this field is not supported.");
            }
        });
    }
}
