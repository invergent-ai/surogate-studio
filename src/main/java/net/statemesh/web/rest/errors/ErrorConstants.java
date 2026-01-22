package net.statemesh.web.rest.errors;

import java.net.URI;

public final class ErrorConstants {

    public static final String ERR_CONCURRENCY_FAILURE = "error.concurrencyFailure";
    public static final String ERR_PROJECT_HAS_APPLICATION = "error.project.hasApplications";
    public static final String ERR_VALIDATION = "error.validation";
    public static final String ERR_BAD_CREDENTIALS = "error.bad.credentials";
    public static final String ERR_NOT_ACTIVATED = "error.not.activated";
    public static final String PROBLEM_BASE_URL = "https://statemesh.net/problem";
    public static final URI DEFAULT_TYPE = URI.create(PROBLEM_BASE_URL + "/problem-with-message");
    public static final URI CONSTRAINT_VIOLATION_TYPE = URI.create(PROBLEM_BASE_URL + "/constraint-violation");
    public static final URI INVALID_PASSWORD_TYPE = URI.create(PROBLEM_BASE_URL + "/invalid-password");
    public static final URI LOGIN_ALREADY_USED_TYPE = URI.create(PROBLEM_BASE_URL + "/login-already-used");

    private ErrorConstants() {}
}
