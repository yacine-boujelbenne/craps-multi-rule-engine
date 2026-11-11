package com.exportdocs.project.api;

import com.exportdocs.project.application.ProjectProblem;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProjectErrorHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectErrorHandler.class);

    @ExceptionHandler(ProjectProblem.class)
    ResponseEntity<ProblemDetail> project(ProjectProblem exception, HttpServletRequest request) {
        return response(exception.status(), exception.getMessage(), exception.errors(), request.getRequestURI());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> stale(OptimisticLockingFailureException exception, HttpServletRequest request) {
        return response(409, "Project changed; reload it before updating.",
                List.of(new ProjectProblem.FieldError("expectedVersion", "stale_version", "The supplied version is stale.")), request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException constraint &&
                    "project_reference_key".equals(constraint.getConstraintName())) {
                return response(409, "Project reference already exists.",
                        List.of(new ProjectProblem.FieldError("reference", "duplicate_reference", "Project reference must be unique.")), request.getRequestURI());
            }
        }
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && "23503".equals(sql.getSQLState())) {
                return response(422, "Referenced data changed; reload and retry.",
                        List.of(new ProjectProblem.FieldError("body", "invalid_reference", "Referenced data is no longer available.")), request.getRequestURI());
            }
        }
        return unexpected(exception, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception, HttpServletRequest request) {
        LOG.error("Project request failed: {} {}", request.getMethod(), request.getRequestURI(), exception);
        return response(500, "The request could not be completed.", List.of(), request.getRequestURI());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers,HttpStatusCode status,WebRequest request) {
        var errors=exception.getBindingResult().getFieldErrors().stream().map(error->
                new ProjectProblem.FieldError(error.getField(),"invalid",error.getDefaultMessage())).toList();
        return new ResponseEntity<>(problem(400,"Request validation failed.",errors,
                request.getDescription(false).replaceFirst("^uri=","")),headers,status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(status.value(), status.is4xxClientError()
                ? "Invalid request. Check the request body, path and parameters." : "The request could not be completed.",
                List.of(), request.getDescription(false).replaceFirst("^uri=", ""));
        return super.handleExceptionInternal(exception, problem, headers, status, request);
    }

    private ResponseEntity<ProblemDetail> response(int status, String detail, List<ProjectProblem.FieldError> errors, String path) {
        return ResponseEntity.status(status).body(problem(status, detail, errors, path));
    }

    public static ProblemDetail problem(int status, String detail, List<ProjectProblem.FieldError> errors, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(path));
        problem.setProperty("errors", errors);
        return problem;
    }
}
