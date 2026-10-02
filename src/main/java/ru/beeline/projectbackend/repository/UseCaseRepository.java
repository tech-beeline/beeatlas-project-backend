/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.projectbackend.domain.UseCase;

import java.util.Optional;

public interface UseCaseRepository extends JpaRepository<UseCase, Integer> {

    @Query("SELECT u FROM UseCase u WHERE u.projectId = :projectId AND LOWER(u.code) = LOWER(:code)")
    Optional<UseCase> findByProjectIdAndCodeIgnoreCase(@Param("projectId") Integer projectId,
                                                       @Param("code") String code);

    Optional<UseCase> findByIdAndProjectIdAndDeletedDateIsNull(Integer id, Integer projectId);
}
