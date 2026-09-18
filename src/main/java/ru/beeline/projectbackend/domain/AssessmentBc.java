/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "assessment_bc", schema = "projects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentBc {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assessment_bc_id_seq")
    @SequenceGenerator(name = "assessment_bc_id_seq", sequenceName = "projects.assessment_bc_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "assessment_id", nullable = false)
    private Integer assessmentId;

    @Column(name = "bc_id", nullable = false)
    private Integer bcId;

    @Column(name = "relevance")
    private Integer relevance;

    @Column(name = "reason")
    private String reason;
}
