/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.exception;

public class AuthServiceException extends RuntimeException {
    public AuthServiceException(String message) {
        super(message);
    }
}
