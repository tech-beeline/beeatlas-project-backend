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

    @Query("SELECT u FROM UseCase u WHERE u.projectBranchId = :projectBranchId AND LOWER(u.code) = LOWER(:code)")
    Optional<UseCase> findByProjectBranchIdAndCodeIgnoreCase(@Param("projectBranchId") Integer projectBranchId,
                                                             @Param("code") String code);
}
