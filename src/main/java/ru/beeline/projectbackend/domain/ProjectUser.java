/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "project_user", schema = "projects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_user_id_seq")
    @SequenceGenerator(name = "project_user_id_seq", sequenceName = "projects.project_user_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;
}
