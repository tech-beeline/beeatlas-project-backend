/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto.usecase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Живая карточка use case каталога со связью на шаг BI")
public class UseCaseCatalogItemDTO {

    private Integer id;
    private String code;
    private String name;
    private String description;
    private Integer projectId;
    private String biStepCode;
}
