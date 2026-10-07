/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

import static ru.beeline.projectbackend.utils.Constant.BLANK_BI_STEP_CODE;
import static ru.beeline.projectbackend.utils.Constant.BLANK_OPERATION_ID;
import static ru.beeline.projectbackend.utils.Constant.CONNECTION_OPERATION_ID_MUST_BE_INTEGER;
import static ru.beeline.projectbackend.utils.Constant.MISSING_REQUEST_BODY;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    private static final String USE_CASES_BY_OPERATIONS_PATH = "/use-cases/by-operations";
    private static final String USE_CASES_OPERATIONS_GROUPED_BY_USE_CASE_PATH =
            "/use-cases/operations/grouped-by-use-case";
    private static final String USE_CASES_BY_BI_STEP_CODES_PATH = "/use-cases/by-bi-step-codes";

    private static ResponseEntity<Object> errorResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponseDTO(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleUnreadableBody(HttpMessageNotReadableException e,
                                                       HttpServletRequest request) {
        String message = connectionOperationIdError(e);
        if (message == null) {
            message = MISSING_REQUEST_BODY;
            String uri = request.getRequestURI();
            if (uri != null && isElementError(e)) {
                if (uri.endsWith(USE_CASES_BY_OPERATIONS_PATH)
                        || uri.endsWith(USE_CASES_OPERATIONS_GROUPED_BY_USE_CASE_PATH)) {
                    message = BLANK_OPERATION_ID;
                } else if (uri.endsWith(USE_CASES_BY_BI_STEP_CODES_PATH)) {
                    message = BLANK_BI_STEP_CODE;
                }
            }
        }
        log.error(message);
        return errorResponse(HttpStatus.BAD_REQUEST, message);
    }

    private String connectionOperationIdError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof JsonMappingException mapping
                    && CONNECTION_OPERATION_ID_MUST_BE_INTEGER.equals(mapping.getOriginalMessage())) {
                return CONNECTION_OPERATION_ID_MUST_BE_INTEGER;
            }
            current = current.getCause();
        }
        return null;
    }

    private boolean isElementError(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        return cause instanceof JsonMappingException mappingException && !mappingException.getPath().isEmpty();
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