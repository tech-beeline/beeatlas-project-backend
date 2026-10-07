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
@Schema(description = "Карточка use case и id методов из тела запроса, участвующие в её шагах")
public class UseCaseOperationsGroupedDTO {

    private Integer id;
    private String name;
    private String code;
    private List<Integer> operationIds;
}
