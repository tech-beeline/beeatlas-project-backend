/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Строка product.discovered_operation со вложенным интерфейсом и канонической операцией")
public class DiscoveredOperationByIdsDTO {

    private Integer id;
    private Integer interfaceId;
    private Integer connectionOperationId;
    private String name;
    private String context;
    private String description;
    private String tcCode;
    private String tcDescription;
    private Boolean isPlanned;
    private String type;
    private String returnType;
    private Double rps;
    private Double latency;
    private Double errorRate;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime deletedDate;
    private DiscoveredInterfaceByIdsDTO discoveredInterface;
    private OperationWithTechCapabilityDTO operation;
}
