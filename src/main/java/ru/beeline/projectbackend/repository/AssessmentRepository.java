/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.projectbackend.domain.Assessment;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Integer> {

    @Query("""
            SELECT a FROM Assessment a
            WHERE a.deleteDate IS NULL
              AND (:projectId IS NULL OR a.projectId = :projectId)
              AND (:ownerId IS NULL OR a.ownerId = :ownerId)
              AND (:statusId IS NULL OR a.statusId = :statusId)
            ORDER BY a.updateDate DESC
            """)
    List<Assessment> findAllFiltered(
            @Param("projectId") Integer projectId,
            @Param("ownerId") Integer ownerId,
            @Param("statusId") Integer statusId);

    Optional<Assessment> findByIdAndDeleteDateIsNull(Integer id);

    Optional<Assessment> findFirstByProjectIdAndStatusIdAndDeleteDateIsNullOrderByCreatedDateDescUpdateDateDesc(
            Integer projectId, Integer statusId);

    List<Assessment> findByProjectIdInAndStatusIdAndDeleteDateIsNullOrderByCreatedDateDescUpdateDateDesc(
            List<Integer> projectIds, Integer statusId);
}
