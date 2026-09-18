/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AssessmentResponseDTO {

    private Integer id;
    private Integer projectId;
    private Integer ownerId;
    private String ownerName;
    private Integer statusId;
    private String statusName;
    private String impactLevel;
    private String taskDescription;
    private Integer reqFuncCount;
    private Integer reqNonFuncCount;
    private Integer tcCount;
    private Integer productCount;
    private Integer oqCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedDate;
}
