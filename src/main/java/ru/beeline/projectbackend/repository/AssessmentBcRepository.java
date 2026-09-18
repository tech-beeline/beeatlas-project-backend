/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.projectbackend.domain.AssessmentBc;

import java.util.Collection;
import java.util.List;

public interface AssessmentBcRepository extends JpaRepository<AssessmentBc, Integer> {

    @Query("""
            SELECT b.assessmentId, COUNT(b.id)
            FROM AssessmentBc b
            WHERE b.assessmentId IN :ids
            GROUP BY b.assessmentId
            """)
    List<Object[]> countGroupedByAssessmentId(@Param("ids") Collection<Integer> ids);

    List<AssessmentBc> findByAssessmentId(Integer assessmentId);
}
