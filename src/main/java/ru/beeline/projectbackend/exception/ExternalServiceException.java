/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ExternalServiceException extends RuntimeException {

    private final HttpStatus status;

    public ExternalServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
