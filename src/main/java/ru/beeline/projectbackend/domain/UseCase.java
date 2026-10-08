/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "bi_step_code")
    private String biStepCode;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;
}
