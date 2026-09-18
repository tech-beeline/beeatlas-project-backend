/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "us_operation_relation", schema = "projects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsOperationRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "us_operation_relation_id_seq")
    @SequenceGenerator(name = "us_operation_relation_id_seq",
            sequenceName = "projects.us_operation_relation_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "uc_id", nullable = false)
    private Integer ucId;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;

    /** Вызывающая операция; NULL у стартового шага. */
    @Column(name = "req_operation_id")
    private Integer reqOperationId;

    @Column(name = "related_req_operation_id", nullable = false)
    private Integer relatedReqOperationId;
}
