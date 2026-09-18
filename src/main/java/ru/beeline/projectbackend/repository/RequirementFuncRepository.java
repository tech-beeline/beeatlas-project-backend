/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.projectbackend.domain.RequirementFunc;

import java.util.Collection;
import java.util.List;

public interface RequirementFuncRepository extends JpaRepository<RequirementFunc, Integer> {

    @Query("""
            SELECT r.assessmentId, COUNT(r.id)
            FROM RequirementFunc r
            WHERE r.assessmentId IN :ids
            GROUP BY r.assessmentId
            """)
    List<Object[]> countGroupedByAssessmentId(@Param("ids") Collection<Integer> ids);

    List<RequirementFunc> findByAssessmentId(Integer assessmentId);
}
