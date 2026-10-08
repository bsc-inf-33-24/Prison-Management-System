package com.pms.api.inmate.service;

import com.pms.api.common.audit.Audited;
import com.pms.api.inmate.dto.InmateRequest;
import com.pms.api.inmate.dto.InmateResponse;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InmateService {

    private static final String INMATE_NUMBER_CONSTRAINT = "uk_inmates_inmate_number";

    private final InmateRepository inmateRepository;
    private final String facilityName;

    public InmateService(InmateRepository inmateRepository,
                         @Value("${app.facility-name}") String facilityName) {
        this.inmateRepository = inmateRepository;
        this.facilityName = facilityName;
    }

    @Audited
    @Transactional
    public InmateResponse enroll(InmateRequest request) {
        Inmate inmate = new Inmate(
                request.inmateNumber().trim(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.dateOfBirth(),
                request.gender().trim(),
                request.admissionDate(),
                request.classification().trim(),
                facilityName
        );
        return save(inmate);
    }

    @Transactional(readOnly = true)
    public List<InmateResponse> list(String search) {
        List<Inmate> inmates;
        if (search == null || search.isBlank()) {
            inmates = inmateRepository.findAllByOrderByIdAsc();
        } else {
            inmates = inmateRepository.search(escapeLikePattern(search.trim()));
        }
        return inmates.stream().map(InmateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InmateResponse get(Long inmateId) {
        return InmateResponse.from(findInmate(inmateId));
    }

    @Audited
    @Transactional
    public InmateResponse update(Long inmateId, InmateRequest request) {
        Inmate inmate = findInmate(inmateId);
        inmate.updateDetails(
                request.inmateNumber().trim(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.dateOfBirth(),
                request.gender().trim(),
                request.admissionDate(),
                request.classification().trim()
        );
        return save(inmate);
    }

    private InmateResponse save(Inmate inmate) {
        try {
            return InmateResponse.from(inmateRepository.saveAndFlush(inmate));
        } catch (DataIntegrityViolationException exception) {
            if (violatesInmateNumberConstraint(exception)) {
                throw new InmateNumberAlreadyExistsException();
            }
            throw exception;
        }
    }

    private Inmate findInmate(Long inmateId) {
        return inmateRepository.findById(inmateId).orElseThrow(InmateNotFoundException::new);
    }

    private static boolean violatesInmateNumberConstraint(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && INMATE_NUMBER_CONSTRAINT.equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    private static String escapeLikePattern(String search) {
        return search.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    public static class InmateNotFoundException extends RuntimeException {
    }

    public static class InmateNumberAlreadyExistsException extends RuntimeException {
    }
}
