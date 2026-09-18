/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.projectbackend.domain.UsOperationRelation;

public interface UsOperationRelationRepository extends JpaRepository<UsOperationRelation, Integer> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM UsOperationRelation r WHERE r.ucId = :ucId")
    int deleteAllByUcId(@Param("ucId") Integer ucId);
}
