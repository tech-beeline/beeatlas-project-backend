/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.*;


@Data
@Entity
@Table(name = "artifact_branch", schema = "projects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactBranch {

    public static final String TYPE_PROJECT = "project";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "artifact_branch_id_seq")
    @SequenceGenerator(name = "artifact_branch_id_seq", sequenceName = "projects.artifact_branch_id_seq",
            allocationSize = 1)
    private Integer id;

    @Column(name = "artifact_type", nullable = false)
    private String artifactType;

    @Column(name = "artifact_id", nullable = false)
    private Integer artifactId;

    @Column(name = "name", nullable = false)
    private String name;
}
