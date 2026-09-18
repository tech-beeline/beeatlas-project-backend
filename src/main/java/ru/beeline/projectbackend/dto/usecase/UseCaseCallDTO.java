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
@Schema(description = "Шаг use case: вызов операции relatedOperationGuid со стороны operationGuid")
public class UseCaseCallDTO {

    @Schema(description = "guid вызывающей операции из reqOperations[]; не задан у стартового шага")
    private String operationGuid;

    @Schema(description = "guid вызываемой операции из reqOperations[] (обязательное)")
    private String relatedOperationGuid;

    @Schema(description = "Порядковый номер шага (обязательное)")
    private Integer order;
}
