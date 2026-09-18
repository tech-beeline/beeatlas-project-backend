/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AssessmentDetailsResponseDTO {

    private Integer id;
    private Integer projectId;
    private String projectName;
    private Integer ownerId;
    private String ownerName;
    private Integer statusId;
    private String statusName;
    private String source;
    private String sourceUrl;
    private String rawText;
    private String taskDescription;
    private String impactLevel;
    private List<AssessmentReqFuncDTO> reqFunc;
    private List<AssessmentReqNonFuncDTO> reqNonFunc;
    private List<AssessmentOpenQuestionDTO> openQuestions;
    private List<AssessmentTcItemDTO> tc;
    private List<AssessmentDesignTcDTO> designTc;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedDate;
}
