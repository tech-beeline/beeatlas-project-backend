/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "requirements_func", schema = "projects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementFunc {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "requirements_func_id_seq")
    @SequenceGenerator(name = "requirements_func_id_seq", sequenceName = "projects.requirements_func_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "assessment_id", nullable = false)
    private Integer assessmentId;

    @Column(name = "unique_ident", nullable = false)
    private String uniqueIdent;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;
}
