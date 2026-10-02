/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto.usecase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.projectbackend.dto.product.DiscoveredOperationByIdsDTO;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Узел дерева вызовов ветки use case (шаг us_operation_relation)")
public class UseCaseOperationNodeDTO {

    private Integer order;
    private Integer discoveredOperationId;
    private DiscoveredOperationByIdsDTO discoveredOperation;

    @Builder.Default
    private List<UseCaseOperationNodeDTO> children = new ArrayList<>();
}
