/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto.usecase;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Элемент справочника операций, на которые ссылаются шаги useCaseCall")
public class ReqOperationDTO {

    private String guid;
    private String product;
    private String productPlanned;
    private String interfaceCode;
    private String name;
    private String type;
    private String descriptionTc;
    private String tcCode;
    private String descriptionOperation;
    private Boolean isPlanned;
    @JsonDeserialize(using = ConnectionOperationIdDeserializer.class)
    private Integer connectionOperationId;
}
