/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProjectRequestDTO {

    @NotBlank
    private String name;

    private String description;

    private String docLink;

    @NotBlank
    private String source;
}
