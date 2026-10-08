package com.pms.api.common.error;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.pms.api.user.service.UserService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import com.pms.api.auth.service.AuthService;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationFailure(MethodArgumentNotValidException exception) {
        List<FieldViolation> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldViolation(
                        error.getField(),
                        error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage()
                ))
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more request fields are invalid."
        );
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    public record FieldViolation(String field, String message) {
    }

    @ExceptionHandler(UserService.UsernameAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleUsernameConflict() {
        return problem(HttpStatus.CONFLICT, "Username already exists.");
    }

    @ExceptionHandler(UserService.UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound() {
        return problem(HttpStatus.NOT_FOUND, "User not found.");
    }

    @ExceptionHandler(UserService.InvalidRoleException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRole() {
        return problem(HttpStatus.BAD_REQUEST, "Role is not allowed.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials() {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationFailure() {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
    }

    @ExceptionHandler(AuthService.AccountLockedException.class)
    public ResponseEntity<ProblemDetail> handleAccountLocked() {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Account temporarily locked after too many failed login attempts.");
    }

    @ExceptionHandler(AuthService.InvalidCurrentPasswordException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCurrentPassword() {
        return problem(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        return ResponseEntity.status(status).body(problem);
    }
}
