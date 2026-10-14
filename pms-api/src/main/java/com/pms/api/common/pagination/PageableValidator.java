package com.pms.api.common.pagination;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableValidator {

    private PageableValidator() {
    }

    public static Pageable create(Integer page, Integer size, List<String> requestedSort,
                                  Set<String> allowedProperties, Sort defaultSort,
                                  PaginationProperties properties) {
        int actualPage = page == null ? properties.defaultPage() : page;
        int actualSize = size == null ? properties.defaultSize() : size;
        if (actualPage < 0) {
            throw new InvalidPaginationException("page must be greater than or equal to 0.");
        }
        if (actualSize < 1 || actualSize > properties.maxSize()) {
            throw new InvalidPaginationException("size must be between 1 and " + properties.maxSize() + ".");
        }

        Sort sort = requestedSort == null || requestedSort.isEmpty()
                ? defaultSort
                : parseSort(requestedSort, allowedProperties);
        return PageRequest.of(actualPage, actualSize, sort);
    }

    private static Sort parseSort(List<String> requestedSort, Set<String> allowedProperties) {
        List<Sort.Order> orders = requestedSort.stream()
                .map(value -> value.split(",", -1))
                .map(parts -> {
                    if (parts.length > 2 || parts[0].isBlank()) {
                        throw new InvalidSortException("Invalid sort parameter.");
                    }
                    String property = parts[0].trim();
                    if (!allowedProperties.contains(property)) {
                        throw new InvalidSortException("Sorting by this field is not supported.");
                    }
                    String direction = parts.length == 2 ? parts[1].trim() : "asc";
                    if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
                        throw new InvalidSortException("Sort direction must be asc or desc.");
                    }
                    return new Sort.Order(Sort.Direction.fromString(direction), property);
                })
                .toList();
        if (orders.isEmpty()) {
            throw new InvalidSortException("Invalid sort parameter.");
        }
        return Sort.by(orders);
    }
}
