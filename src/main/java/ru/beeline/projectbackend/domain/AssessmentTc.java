/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Data
@Entity
@Table(name = "assessment_tc", schema = "projects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentTc {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assessment_tc_id_seq")
    @SequenceGenerator(name = "assessment_tc_id_seq", sequenceName = "projects.assessment_tc_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "assessment_id", nullable = false)
    private Integer assessmentId;

    @Column(name = "tc_code", nullable = false)
    private String tcCode;

    @Column(name = "product_alias")
    private String productAlias;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "parent_bc_code")
    private String parentBcCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fr_ids", columnDefinition = "jsonb")
    private List<String> frIds;
}
