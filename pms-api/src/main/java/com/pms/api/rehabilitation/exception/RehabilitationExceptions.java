package com.pms.api.rehabilitation.exception;

public final class RehabilitationExceptions {

    private RehabilitationExceptions() {
    }

    public static class ProgramNotFoundException extends RuntimeException {
    }

    public static class EnrollmentNotFoundException extends RuntimeException {
    }

    public static class AttendanceNotFoundException extends RuntimeException {
    }

    public static class ProgramNotActiveException extends RuntimeException {
    }

    public static class DuplicateEnrollmentException extends RuntimeException {
    }

    public static class DuplicateAttendanceException extends RuntimeException {
    }

    public static class InvalidEnrollmentTransitionException extends RuntimeException {
    }

    public static class AttendanceNotAllowedException extends RuntimeException {
    }

    public static class UnsupportedAttendanceStatusException extends RuntimeException {
    }
}
