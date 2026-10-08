/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Каноническая operation: id, name, type, tcId, techCapability, sla, interface")
public class OperationWithTechCapabilityDTO {

    private Integer id;
    private String name;
    private String type;
    private Integer tcId;
    private TcDTO techCapability;
    private ConnectedOperationSlaDTO sla;
    @JsonProperty("interface")
    private ConnectedInterfaceDTO connectedInterface;
}
