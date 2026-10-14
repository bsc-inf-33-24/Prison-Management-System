package com.pms.api.common.pagination;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pagination")
public record PaginationProperties(int defaultPage, int defaultSize, int maxSize) {
    public PaginationProperties {
        if (defaultPage < 0 || defaultSize < 1 || maxSize < defaultSize) {
            throw new IllegalArgumentException("Invalid pagination configuration.");
        }
    }
}
