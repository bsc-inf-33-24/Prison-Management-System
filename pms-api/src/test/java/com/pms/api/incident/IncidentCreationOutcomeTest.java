package com.pms.api.incident;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pms.api.incident.dto.LogIncidentRequest;
import com.pms.api.incident.entity.Incident;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IncidentCreationOutcomeTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsMissingNullAnd255CharacterOutcomes() {
        assertTrue(validator.validate(request(null)).isEmpty());
        assertTrue(validator.validate(request("x".repeat(255))).isEmpty());
    }

    @Test
    void rejectsBlankOutcomes() {
        assertFalse(validator.validate(request("")).isEmpty());
        assertFalse(validator.validate(request(" \t\n ")).isEmpty());
    }

    @Test
    void rejectsOutcomesLongerThan255Characters() {
        assertFalse(validator.validate(request("x".repeat(256))).isEmpty());
    }

    @Test
    void derivesIncidentStatusFromOutcome() {
        Incident open = Incident.logNew(LocalDate.now(), "Incident", "Officer", null);
        Incident resolved = Incident.logNew(LocalDate.now(), "Incident", "Officer", "Warning issued");

        assertEquals(Incident.STATUS_OPEN, open.getStatus());
        assertNull(open.getOutcome());
        assertEquals(Incident.STATUS_RESOLVED, resolved.getStatus());
        assertEquals("Warning issued", resolved.getOutcome());
    }

    private LogIncidentRequest request(String outcome) {
        return new LogIncidentRequest(
                List.of(new LogIncidentRequest.Participant(UUID.randomUUID(), null)),
                LocalDate.now(),
                "Incident",
                outcome);
    }
}
