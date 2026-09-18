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
@Schema(description = "Результат создания/обновления use case")
public class UseCaseUpsertResponseDTO {

    @Schema(description = "Идентификатор use case (projects.use_case.id)")
    private Integer id;

    @Schema(description = "Код use case")
    private String code;

    @Schema(description = "Идентификатор ветки проекта, в которой сохранён use case")
    private Integer projectBranchId;
}
