package com.exportdocs.project.application;

import java.util.List;

public final class ProjectProblem extends RuntimeException {
    private final int status;
    private final List<FieldError> errors;

    public ProjectProblem(int status, String message, List<FieldError> errors) {
        super(message);
        this.status = status;
        this.errors = List.copyOf(errors);
    }

    public int status() { return status; }
    public List<FieldError> errors() { return errors; }

    public static ProjectProblem field(int status, String field, String code, String message) {
        return new ProjectProblem(status, message, List.of(new FieldError(field, code, message)));
    }

    public static ProjectProblem notFound() {
        return new ProjectProblem(404, "Project not found.", List.of());
    }

    public record FieldError(String field, String code, String message) {}
}
