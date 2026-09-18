/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.projectbackend.domain.RequirementNonFunc;

import java.util.Collection;
import java.util.List;

public interface RequirementNonFuncRepository extends JpaRepository<RequirementNonFunc, Integer> {

    @Query("""
            SELECT r.assessmentId, COUNT(r.id)
            FROM RequirementNonFunc r
            WHERE r.assessmentId IN :ids
            GROUP BY r.assessmentId
            """)
    List<Object[]> countGroupedByAssessmentId(@Param("ids") Collection<Integer> ids);

    List<RequirementNonFunc> findByAssessmentId(Integer assessmentId);
}
