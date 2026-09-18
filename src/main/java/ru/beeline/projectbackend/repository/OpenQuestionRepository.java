/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.projectbackend.domain.OpenQuestion;

import java.util.Collection;
import java.util.List;

public interface OpenQuestionRepository extends JpaRepository<OpenQuestion, Integer> {

    @Query("""
            SELECT o.assessmentId, COUNT(o.id)
            FROM OpenQuestion o
            WHERE o.assessmentId IN :ids
            GROUP BY o.assessmentId
            """)
    List<Object[]> countGroupedByAssessmentId(@Param("ids") Collection<Integer> ids);

    List<OpenQuestion> findByAssessmentId(Integer assessmentId);
}
