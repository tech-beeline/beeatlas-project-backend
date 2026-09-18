/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "project", schema = "projects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_id_seq")
    @SequenceGenerator(name = "project_id_seq", sequenceName = "projects.project_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "doc_link")
    private String docLink;

    @Column(name = "unique_ident", nullable = false, unique = true)
    private String uniqueIdent;

    @Column(name = "owner_id", nullable = false)
    private Integer ownerId;

    @Column(name = "status_id", nullable = false)
    private Integer statusId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "delete_date")
    private LocalDateTime deleteDate;
}
