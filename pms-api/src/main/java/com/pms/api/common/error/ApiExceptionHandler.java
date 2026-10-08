package com.pms.api.common.error;

import com.pms.api.auth.service.AuthService;
import com.pms.api.inmate.service.InmateService;
import com.pms.api.user.service.UserService;
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation() {
        return problem(HttpStatus.CONFLICT, "The request conflicts with existing data.");
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
