package com.pms.api.common.pagination;

public class InvalidSortException extends RuntimeException {
    public InvalidSortException(String message) {
        super(message);
    }
}
