/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "open_questions", schema = "projects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "open_questions_id_seq")
    @SequenceGenerator(name = "open_questions_id_seq", sequenceName = "projects.open_questions_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "assessment_id", nullable = false)
    private Integer assessmentId;

    @Column(name = "unique_ident", nullable = false)
    private String uniqueIdent;

    @Column(name = "question_text", nullable = false)
    private String questionText;
}
