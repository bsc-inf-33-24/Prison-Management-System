package com.pms.api.cases.service;

import com.pms.api.cases.dto.CrimeRequest;
import com.pms.api.cases.dto.CrimeResponse;
import com.pms.api.cases.entity.Crime;
import com.pms.api.cases.repository.CrimeRepository;
import com.pms.api.common.audit.Audited;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrimeService {

    private final CrimeRepository crimeRepository;

    public CrimeService(CrimeRepository crimeRepository) {
        this.crimeRepository = crimeRepository;
    }

    @Audited
    @Transactional
    public CrimeResponse create(CrimeRequest request) {
        validate(request);
        Crime crime = new Crime(request.offenceName().trim(), request.description() == null ? "" : request.description().trim(), request.category().trim());
        return CrimeResponse.from(crimeRepository.saveAndFlush(crime));
    }

    @Transactional(readOnly = true)
    public CrimeResponse get(Long crimeId) {
        return CrimeResponse.from(findCrime(crimeId));
    }

    @Transactional(readOnly = true)
    public List<CrimeResponse> list() {
        return crimeRepository.findAll().stream().map(CrimeResponse::from).toList();
    }

    @Audited
    @Transactional
    public CrimeResponse update(Long crimeId, CrimeRequest request) {
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

    private Crime findCrime(Long crimeId) {
        return crimeRepository.findById(crimeId).orElseThrow(CrimeNotFoundException::new);
    }

    public static class CrimeNotFoundException extends RuntimeException {
    }
}
