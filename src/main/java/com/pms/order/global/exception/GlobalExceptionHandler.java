package com.pms.order.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        ProblemDetail problem = ProblemDetail.forStatus(errorCode.getHttpStatus());
        problem.setType(URI.create("/errors/" + errorCode.getCode()));
        problem.setTitle(errorCode.getCode());
        problem.setDetail(e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        problem.setProperty("code", errorCode.getCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ProblemDetail problem = ProblemDetail.forStatus(errorCode.getHttpStatus());
        problem.setType(URI.create("/errors/" + errorCode.getCode()));
        problem.setTitle(errorCode.getCode());
        problem.setDetail(e.getMessage());
        problem.setProperty("code", errorCode.getCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception e) {
        log.error("Unhandled exception", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        ProblemDetail problem = ProblemDetail.forStatus(errorCode.getHttpStatus());
        problem.setType(URI.create("/errors/" + errorCode.getCode()));
        problem.setTitle(errorCode.getCode());
        problem.setDetail(errorCode.getMessage());
        problem.setProperty("code", errorCode.getCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
