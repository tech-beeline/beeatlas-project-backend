/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "assessments", schema = "projects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assessments_id_seq")
    @SequenceGenerator(name = "assessments_id_seq", sequenceName = "projects.assessments_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "owner_id", nullable = false)
    private Integer ownerId;

    @Column(name = "status_id", nullable = false)
    private Integer statusId;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "raw_text", nullable = false)
    private String rawText;

    @Column(name = "task_description")
    private String taskDescription;

    @Column(name = "impact_level")
    private String impactLevel;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "delete_date")
    private LocalDateTime deleteDate;
}
