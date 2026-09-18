/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AssessmentTcItemDTO {

    private Integer id;
    private String tcCode;
    private String productAlias;
    private String productName;
    private String parentBcCode;
    private List<String> frIds;
}
