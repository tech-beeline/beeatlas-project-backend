/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "assessment_status_enum", schema = "projects")
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentStatusEnum {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assessment_status_id_seq")
    @SequenceGenerator(name = "assessment_status_id_seq", sequenceName = "projects.assessment_status_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;
}
