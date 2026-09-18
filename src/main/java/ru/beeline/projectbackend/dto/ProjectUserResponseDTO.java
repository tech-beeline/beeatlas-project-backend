/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectUserResponseDTO {

    private Integer id;
    private Integer projectId;
    private Integer userId;
}
