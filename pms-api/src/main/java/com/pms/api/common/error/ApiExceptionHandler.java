package com.pms.api.common.error;

import com.pms.api.auth.service.AuthService;
import com.pms.api.cases.service.CaseService;
import com.pms.api.cell.service.CellService;
import com.pms.api.inmate.service.InmateService;
import com.pms.api.incident.service.IncidentService;
import com.pms.api.sentence.service.SentenceService;
import com.pms.api.user.service.UserService;
import com.pms.api.visitor.service.VisitService;
import com.pms.api.visitor.service.VisitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.pms.api.auth.service.PasswordPolicy;
import com.pms.api.auth.service.UnavailableRecoveryTokenDelivery;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationFailure() {
        return problem(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
    }

    @ExceptionHandler(InmateService.InmateNotFoundException.class)
    public ResponseEntity<ApiError> handleInmateNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Inmate not found.");
    }

    @ExceptionHandler(InmateService.InmateNumberAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleInmateNumberConflict() {
        return problem(HttpStatus.CONFLICT, "An inmate with that inmate number already exists.");
    }

    @ExceptionHandler(UserService.UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUsernameConflict() {
        return problem(HttpStatus.CONFLICT, "Username already exists.");
    }

    @ExceptionHandler(UserService.UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound() {
        return problem(HttpStatus.NOT_FOUND, "User not found.");
    }

    @ExceptionHandler(UserService.InvalidRoleException.class)
    public ResponseEntity<ApiError> handleInvalidRole() {
        return problem(HttpStatus.BAD_REQUEST, "Role is not allowed.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials() {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationFailure() {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied() {
        return problem(HttpStatus.FORBIDDEN, "You are not authorized to access this resource.");
    }

    @ExceptionHandler(AuthService.AccountLockedException.class)
    public ResponseEntity<ApiError> handleAccountLocked() {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Account temporarily locked after too many failed login attempts.");
    }

    @ExceptionHandler(AuthService.InvalidCurrentPasswordException.class)
    public ResponseEntity<ApiError> handleInvalidCurrentPassword() {
        return problem(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
    }

    @ExceptionHandler(AuthService.PasswordReuseException.class)
    public ResponseEntity<ApiError> handlePasswordReuse() { return problem(HttpStatus.BAD_REQUEST, "The new password must differ from the current password."); }

    @ExceptionHandler(AuthService.InvalidResetTokenException.class)
    public ResponseEntity<ApiError> handleInvalidResetToken() { return problem(HttpStatus.BAD_REQUEST, "The password reset request is invalid or expired."); }

    @ExceptionHandler(PasswordPolicy.WeakPasswordException.class)
    public ResponseEntity<ApiError> handleWeakPassword() {
        return problem(HttpStatus.BAD_REQUEST,
                "Password must be 8 to 16 characters and contain at least one digit and one punctuation mark.");
    }

    @ExceptionHandler(UnavailableRecoveryTokenDelivery.RecoveryDeliveryUnavailableException.class)
    public ResponseEntity<ApiError> handleRecoveryDeliveryUnavailable() { return problem(HttpStatus.SERVICE_UNAVAILABLE, "Password recovery is temporarily unavailable."); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation() {
        return problem(HttpStatus.CONFLICT, "The request conflicts with existing data.");
    }

    @ExceptionHandler(IncidentService.IncidentNotFoundException.class)
    public ResponseEntity<ApiError> handleIncidentNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Incident not found.");
    }

    @ExceptionHandler(IncidentService.IncidentInmateNotFoundException.class)
    public ResponseEntity<ApiError> handleIncidentInmateNotFound() {
        return problem(HttpStatus.NOT_FOUND, "One or more inmates were not found.");
    }

    @ExceptionHandler(IncidentService.DuplicateInmateIdsException.class)
    public ResponseEntity<ApiError> handleDuplicateInmateIds() {
        return problem(HttpStatus.BAD_REQUEST, "The same inmate ID was listed more than once.");
    }

    @ExceptionHandler(IncidentService.InvalidParticipantException.class)
    public ResponseEntity<ApiError> handleInvalidParticipant() {
        return problem(HttpStatus.BAD_REQUEST, "Inmate entries cannot be null.");
    }

    @ExceptionHandler(IncidentService.OutcomeAlreadyRecordedException.class)
    public ResponseEntity<ApiError> handleOutcomeAlreadyRecorded() {
        return problem(HttpStatus.CONFLICT, "This incident already has a recorded outcome.");
    }

    @ExceptionHandler(CellService.CellNotFoundException.class)
    public ResponseEntity<ApiError> handleCellNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Cell not found.");
    }

    @ExceptionHandler(CellService.InmateNotFoundException.class)
    public ResponseEntity<ApiError> handleCellInmateNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Inmate not found.");
    }

    @ExceptionHandler(CellService.CellAllocationNotFoundException.class)
    public ResponseEntity<ApiError> handleCellAllocationNotFound() {
        return problem(HttpStatus.NOT_FOUND, "No cell allocation exists for this inmate.");
    }

    @ExceptionHandler(CellService.CellCapacityExceededException.class)
    public ResponseEntity<ApiError> handleCellCapacityExceeded() {
        return problem(HttpStatus.CONFLICT, "This cell is at full capacity.");
    }

    @ExceptionHandler(CellService.CellClassificationMismatchException.class)
    public ResponseEntity<ApiError> handleCellClassificationMismatch() {
        return problem(HttpStatus.CONFLICT, "The inmate classification does not match the cell classification.");
    }

    @ExceptionHandler(CellService.CellAlreadyAllocatedException.class)
    public ResponseEntity<ApiError> handleCellAlreadyAllocated() {
        return problem(HttpStatus.CONFLICT, "This inmate is already assigned to a cell.");
    }

    @ExceptionHandler(CellService.CellAlreadyAssignedToSameCellException.class)
    public ResponseEntity<ApiError> handleCellAlreadyAssignedToSameCell() {
        return problem(HttpStatus.CONFLICT, "The inmate is already assigned to this cell.");
    }

    @ExceptionHandler(VisitorService.VisitorNotFoundException.class)
    public ResponseEntity<ApiError> handleVisitorNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Visitor not found.");
    }

    @ExceptionHandler(VisitorService.DuplicateVisitorIdentityException.class)
    public ResponseEntity<ApiError> handleDuplicateVisitorIdentity() {
        return problem(HttpStatus.CONFLICT, "A visitor with that identity number already exists.");
    }

    @ExceptionHandler(VisitService.VisitNotFoundException.class)
    public ResponseEntity<ApiError> handleVisitNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Visit not found.");
    }

    @ExceptionHandler(VisitService.InmateNotFoundException.class)
    public ResponseEntity<ApiError> handleVisitInmateNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Inmate not found.");
    }

    @ExceptionHandler(VisitService.UserNotFoundException.class)
    public ResponseEntity<ApiError> handleVisitUserNotFound() {
        return problem(HttpStatus.NOT_FOUND, "User not found.");
    }

    @ExceptionHandler(CaseService.CaseNotFoundException.class)
    public ResponseEntity<ApiError> handleCaseNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Case not found.");
    }

    @ExceptionHandler(CaseService.InmateNotFoundException.class)
    public ResponseEntity<ApiError> handleCaseInmateNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Inmate not found.");
    }

    @ExceptionHandler(CaseService.CrimeNotFoundException.class)
    public ResponseEntity<ApiError> handleCrimeNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Crime not found.");
    }

    @ExceptionHandler(CaseService.InvalidCaseRequestException.class)
    public ResponseEntity<ApiError> handleInvalidCaseRequest() {
        return problem(HttpStatus.BAD_REQUEST, "One or more case fields are invalid.");
    }

    @ExceptionHandler(SentenceService.SentenceNotFoundException.class)
    public ResponseEntity<ApiError> handleSentenceNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Sentence not found.");
    }

    @ExceptionHandler(SentenceService.InmateNotFoundException.class)
    public ResponseEntity<ApiError> handleSentenceInmateNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Inmate not found.");
    }

    @ExceptionHandler(SentenceService.CaseNotFoundException.class)
    public ResponseEntity<ApiError> handleSentenceCaseNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Case not found.");
    }

    @ExceptionHandler(SentenceService.InvalidSentenceRequestException.class)
    public ResponseEntity<ApiError> handleInvalidSentenceRequest() {
        return problem(HttpStatus.BAD_REQUEST, "One or more sentence fields are invalid.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument() {
        return problem(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception) {
        LOGGER.error("Unexpected API error ({})", exception.getClass().getName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private ResponseEntity<ApiError> problem(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status, message));
    }
}
