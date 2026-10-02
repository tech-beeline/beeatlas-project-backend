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
@Schema(description = "Строка product.discovered_interface по interface_id")
public class DiscoveredInterfaceByIdsDTO {

    private Integer id;
    private String name;
    private String externalId;
    private Integer apiId;
    private Integer connectionInterfaceId;
    private String apiLink;
    private String version;
    private String description;
    private String status;
    private String context;
    private String source;
    private Integer productId;
    private String productPlanned;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime deletedDate;
}
