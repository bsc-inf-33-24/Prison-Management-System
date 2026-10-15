package com.pms.api.cases.service;

import com.pms.api.cases.dto.CrimeRequest;
import com.pms.api.cases.dto.CrimeResponse;
import com.pms.api.cases.entity.Crime;
import com.pms.api.cases.repository.CrimeRepository;
import com.pms.api.common.audit.Audited;
import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrimeService {

    private final CrimeRepository crimeRepository;
    private final PaginationProperties paginationProperties;

    public CrimeService(CrimeRepository crimeRepository, PaginationProperties paginationProperties) {
        this.crimeRepository = crimeRepository;
        this.paginationProperties = paginationProperties;
    }

    @Audited
    @Transactional
    public CrimeResponse create(CrimeRequest request) {
        validate(request);
        Crime crime = new Crime(request.offenceName().trim(), request.description() == null ? "" : request.description().trim(), request.category().trim());
        return CrimeResponse.from(crimeRepository.saveAndFlush(crime));
    }

    @Transactional(readOnly = true)
    public CrimeResponse get(UUID crimeId) {
        return CrimeResponse.from(findCrime(crimeId));
    }

    @Transactional(readOnly = true)
    public PageResponse<CrimeResponse> list(String category, String search, Integer page, Integer size,
                                            List<String> sort) {
        Pageable pageable = PageableValidator.create(page, size, sort,
                Set.of("id", "offenceName", "category", "createdAt"),
                Sort.by(Sort.Order.asc("id")), paginationProperties);
        Specification<Crime> specification = Specification.where((Specification<Crime>) null);
        if (category != null && !category.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("category"), category.trim()));
        }
        if (search != null && !search.isBlank()) {
            String value = "%" + escapeLike(search.trim()) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("offenceName")), value.toLowerCase(Locale.ROOT), '!'),
                    cb.like(cb.lower(root.get("description")), value.toLowerCase(Locale.ROOT), '!')));
        }
        return PageResponse.from(crimeRepository.findAll(specification, pageable).map(CrimeResponse::from));
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    @Audited
    @Transactional
    public CrimeResponse update(UUID crimeId, CrimeRequest request) {
        validate(request);
        Crime crime = findCrime(crimeId);
        crime.update(request.offenceName().trim(), request.description() == null ? "" : request.description().trim(), request.category().trim());
        return CrimeResponse.from(crimeRepository.saveAndFlush(crime));
    }

    private static void validate(CrimeRequest request) {
        if (request == null || request.offenceName() == null || request.offenceName().isBlank()
                || request.category() == null || request.category().isBlank()) {
            throw new IllegalArgumentException("One or more request fields are invalid.");
        }
        if (request.offenceName().trim().length() > 150 || request.category().trim().length() > 100
                || (request.description() != null && request.description().trim().length() > 500)) {
            throw new IllegalArgumentException("One or more request fields are invalid.");
        }
    }

    private Crime findCrime(UUID crimeId) {
        return crimeRepository.findByPublicUuid(crimeId).orElseThrow(CrimeNotFoundException::new);
    }

    public static class CrimeNotFoundException extends RuntimeException {
    }
}
