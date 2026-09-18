/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.exception;

public class AssessmentNotFoundException extends NotFoundException {
    public AssessmentNotFoundException() {
        super("Оценка не найдена");
    }
}
