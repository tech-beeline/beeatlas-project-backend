/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssessmentBcItemDTO {

    private Integer id;
    private Integer bcId;
    private String bcCode;
    private Integer relevance;
    private String reason;
}
