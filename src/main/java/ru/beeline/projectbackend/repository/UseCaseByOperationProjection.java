/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

public interface UseCaseByOperationProjection {

    Integer getReqOperationId();

    Integer getRelatedReqOperationId();

    Integer getUcId();

    String getUcName();

    String getUcCode();
}
