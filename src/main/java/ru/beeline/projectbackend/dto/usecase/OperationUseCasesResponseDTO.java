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
@Schema(description = "Use case, связанные с методом (discovered_operation.id)")
public class OperationUseCasesResponseDTO {

    @Schema(description = "Идентификатор метода")
    private Integer operationId;

    @Schema(description = "Use case, в шагах которых встречается метод")
    private List<OperationUseCaseDTO> useCases;
}
