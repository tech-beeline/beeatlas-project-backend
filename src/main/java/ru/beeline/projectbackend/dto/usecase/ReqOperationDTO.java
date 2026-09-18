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
@Schema(description = "Элемент справочника операций, на которые ссылаются шаги useCaseCall")
public class ReqOperationDTO {

    @Schema(description = "Уникальный в рамках запроса идентификатор операции (обязательное)")
    private String guid;

    @Schema(description = "Alias (cmdb) продукта, которому принадлежит операция (обязательное)")
    private String product;

    @Schema(description = "Код интерфейса; если не задан — интерфейс получает код CRC32 "
            + "от остальных атрибутов операции, и она публикуется отдельным интерфейсом")
    private String interfaceCode;

    @Schema(description = "Имя операции (обязательное)")
    private String name;

    @Schema(description = "Тип операции (обязательное)")
    private String type;

    @Schema(description = "Описание ТС")
    private String descriptionTc;

    @Schema(description = "Код ТС в TeamCenter")
    private String tcCode;

    @Schema(description = "Описание самой операции")
    private String descriptionOperation;
}
