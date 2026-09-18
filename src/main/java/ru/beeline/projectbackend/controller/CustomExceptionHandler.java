/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import ru.beeline.projectbackend.dto.CreateAssessmentRequestDTO;
import ru.beeline.projectbackend.dto.CreateProjectRequestDTO;
import ru.beeline.projectbackend.dto.error.ErrorResponseDTO;
import ru.beeline.projectbackend.exception.AuthServiceException;
import ru.beeline.projectbackend.exception.ConflictException;
import ru.beeline.projectbackend.exception.ExternalServiceException;
import ru.beeline.projectbackend.exception.ForbiddenException;
import ru.beeline.projectbackend.exception.NotFoundException;
import ru.beeline.projectbackend.exception.S3Exception;
import ru.beeline.projectbackend.exception.ValidationException;


@ControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    private static ResponseEntity<Object> errorResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponseDTO(message));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleException(ConflictException e) {
        log.error(e.getMessage());
        return errorResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleException(MethodArgumentNotValidException e) {
        Object target = e.getBindingResult().getTarget();
        String message;
        if (target instanceof CreateProjectRequestDTO) {
            message = "name/source обязательные параметры";
        } else if (target instanceof CreateAssessmentRequestDTO) {
            message = "Не указаны все обязательные параметры";
        } else {
            message = e.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .findFirst()
                    .orElse("Ошибка валидации");
        }
        log.error(message);
        return errorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse(e.getMessage());
        log.error(message);
        return errorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleException(ForbiddenException e) {
        log.error(e.getMessage());
        return errorResponse(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleException(NotFoundException e) {
        log.error(e.getMessage());
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<Object> handleException(S3Exception e) {
        log.error(e.getMessage());
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<Object> handleException(AuthServiceException e) {
        log.error(e.getMessage());
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleException(ValidationException e) {
        log.error(e.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Object> handleException(ExternalServiceException e) {
        log.error(e.getMessage());
        return errorResponse(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Object> handleMultipartException(MultipartException e) {
        String errorMessage = "Ошибка при загрузке файла, Заголовок: Content-Type должен быть: multipart/form-data; " +
                "boundary=<calculated when request is sent>";
        return errorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Object> handleContentTypeException(MissingServletRequestPartException e) {
        String errorMessage = "Заголовок: Content-Type должен быть: multipart/form-data; " +
                "boundary=<calculated when request is sent> " +
                "Проверьте обязательную часть запроса Key : " + e.getRequestPartName();
        return errorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return errorResponse(HttpStatus.BAD_REQUEST, "Ошибка при загрузке файла: превышен максимальный размер файла.");
    }
}