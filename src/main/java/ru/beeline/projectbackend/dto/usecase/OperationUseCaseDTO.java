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
@Schema(description = "Краткая карточка use case")
public class OperationUseCaseDTO {

    @Schema(description = "Идентификатор use case")
    private Integer id;

    @Schema(description = "Название use case")
    private String name;
}
