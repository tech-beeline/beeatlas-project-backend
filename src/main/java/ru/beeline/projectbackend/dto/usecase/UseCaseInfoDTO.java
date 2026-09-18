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
@Schema(description = "Карточка use case")
public class UseCaseInfoDTO {

    @Schema(description = "Стабильный код use case в ветке проекта (use_case.code) (обязательное)")
    private String uid;

    @Schema(description = "Название use case (обязательное)")
    private String name;

    @Schema(description = "Описание use case")
    private String description;
}
