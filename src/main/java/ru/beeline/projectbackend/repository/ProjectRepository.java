/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.projectbackend.domain.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

    boolean existsByName(String name);

    Optional<Project> findByIdAndDeleteDateIsNull(Integer id);

    @Query("""
            SELECT p FROM Project p
            WHERE p.deleteDate IS NULL
              AND (:ownerId IS NULL OR p.ownerId = :ownerId)
              AND (:statusId IS NULL OR p.statusId = :statusId)
            ORDER BY p.updateDate DESC
            """)
    List<Project> findAllFiltered(@Param("ownerId") Integer ownerId, @Param("statusId") Integer statusId);
}
