package by.bsuir.supplier.controller;

import by.bsuir.supplier.dto.response.ErrorBody;
import by.bsuir.supplier.security.AuthenticationFailure;
import by.bsuir.supplier.service.common.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

/** Returns sanitized API errors while retaining diagnostic details in server logs. */
@RestControllerAdvice
public class ApiExceptions {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptions.class);

    private static ResponseEntity<ErrorBody> fail(HttpStatus status, String code, String message) {

        return ResponseEntity.status(status).body(new ErrorBody(code, message, Instant.now()));
    }

    @ExceptionHandler(AuthenticationFailure.class)
    ResponseEntity<ErrorBody> auth(AuthenticationFailure e) {
        return fail(HttpStatus.UNAUTHORIZED, "AUTH_FAILED", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorBody> forbidden(AccessDeniedException e) {
        return fail(HttpStatus.FORBIDDEN, "FORBIDDEN", "Недостаточно полномочий");
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorBody> business(BusinessException e) {
        return fail(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE", e.getMessage());
    }

    @ExceptionHandler({
        DataIntegrityViolationException.class,
        OptimisticLockingFailureException.class
    })
    ResponseEntity<ErrorBody> conflict(Exception e) {
        return fail(HttpStatus.CONFLICT, "CONFLICT", "Нарушено ограничение целостности данных");
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        ConstraintViolationException.class,
        IllegalArgumentException.class
    })
    ResponseEntity<ErrorBody> invalid(Exception e) {
        return fail(HttpStatus.BAD_REQUEST, "VALIDATION", "Некорректные параметры запроса");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<Void> notAcceptable(HttpMediaTypeNotAcceptableException e) {

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> unexpected(Exception e, HttpServletRequest request) {

        log.error(
                "Unhandled exception while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                e);

        return fail(
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Внутренняя ошибка сервера");
    }
}
