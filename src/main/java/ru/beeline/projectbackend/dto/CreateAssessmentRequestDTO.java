/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateAssessmentRequestDTO {

    @NotNull
    private Integer projectId;

    @NotBlank
    private String source;

    private String sourceUrl;
    @NotBlank
    private String rawText;
    private String taskDescription;

    @NotBlank
    private String impactLevel;

    @NotNull
    private List<RequirementItem> requirementsFunc;
    @NotNull
    private List<RequirementItem> requirementsNonFunc;
    @NotNull
    private List<OpenQuestionItem> openQuestions;
    @NotNull
    private List<TcItem> tc;
    @NotNull
    private List<DesignTcItem> designTc;

    @Data
    public static class RequirementItem {
        private String uniqueIdent;
        private String title;
        private String description;
    }

    @Data
    public static class OpenQuestionItem {
        private String uniqueIdent;
        private String questionText;
    }

    @Data
    public static class TcItem {
        private String tcCode;
        private String productAlias;
        private String productName;
        private String parentBcCode;
        private List<String> frIds;
    }

    @Data
    public static class DesignTcItem {
        private String name;
        private String description;
        private String productAlias;
        private String productName;
        private String parentBcCode;
        private List<String> frIds;
    }
}
