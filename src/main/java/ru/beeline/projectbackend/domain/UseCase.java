/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "use_case", schema = "projects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UseCase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "use_case_id_seq")
    @SequenceGenerator(name = "use_case_id_seq", sequenceName = "projects.use_case_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "project_branch_id", nullable = false)
    private Integer projectBranchId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;
}
