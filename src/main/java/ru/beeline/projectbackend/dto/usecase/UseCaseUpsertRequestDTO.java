/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto.usecase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Тело запроса на создание/обновление use case")
public class UseCaseUpsertRequestDTO {

    @Schema(description = "Карточка use case (обязательное)")
    private UseCaseInfoDTO useCase;

    @Schema(description = "Шаги use case; ссылаются на reqOperations[] по guid (обязательное)")
    private List<UseCaseCallDTO> useCaseCall;

    @Schema(description = "Справочник операций, используемых шагами (обязательное, непустой)")
    private List<ReqOperationDTO> reqOperations;
}
