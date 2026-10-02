/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.projectbackend.domain.UsOperationRelation;

import java.util.Collection;
import java.util.List;

public interface UsOperationRelationRepository extends JpaRepository<UsOperationRelation, Integer> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM UsOperationRelation r WHERE r.useCaseBranchId = :useCaseBranchId")
    int deleteAllByUseCaseBranchId(@Param("useCaseBranchId") Integer useCaseBranchId);

    List<UsOperationRelation> findAllByUseCaseBranchId(Integer useCaseBranchId);

    @Query("SELECT r.reqOperationId AS reqOperationId, r.relatedReqOperationId AS relatedReqOperationId, "
            + "u.id AS ucId, u.name AS ucName "
            + "FROM UsOperationRelation r, ArtifactBranch b, UseCase u "
            + "WHERE r.useCaseBranchId = b.id "
            + "AND b.artifactType = 'use_case' "
            + "AND b.artifactId = u.id "
            + "AND (r.reqOperationId IN :operationIds OR r.relatedReqOperationId IN :operationIds)")
    List<UseCaseByOperationProjection> findUseCasesByOperationIds(
            @Param("operationIds") Collection<Integer> operationIds);
}
