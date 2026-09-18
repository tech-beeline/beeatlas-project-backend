/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssessmentReqFuncDTO {

    private Integer id;
    private String uniqueIdent;
    private String title;
    private String description;
}
