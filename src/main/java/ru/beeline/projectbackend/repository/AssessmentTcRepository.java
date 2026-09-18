/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.projectbackend.domain.AssessmentTc;

import java.util.Collection;
import java.util.List;

public interface AssessmentTcRepository extends JpaRepository<AssessmentTc, Integer> {

    @Query("""
            SELECT t.assessmentId, COUNT(t.id)
            FROM AssessmentTc t
            WHERE t.assessmentId IN :ids
            GROUP BY t.assessmentId
            """)
    List<Object[]> countGroupedByAssessmentId(@Param("ids") Collection<Integer> ids);

    @Query("""
            SELECT t.assessmentId, t.productAlias
            FROM AssessmentTc t
            WHERE t.assessmentId IN :ids
              AND t.productAlias IS NOT NULL
              AND t.productAlias <> ''
            """)
    List<Object[]> findProductAliasesByAssessmentIds(@Param("ids") Collection<Integer> ids);

    List<AssessmentTc> findByAssessmentId(Integer assessmentId);
}
