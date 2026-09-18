/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.projectbackend.dto.AssessmentDetailsResponseDTO;
import ru.beeline.projectbackend.dto.AssessmentResponseDTO;
import ru.beeline.projectbackend.dto.CreateAssessmentRequestDTO;
import ru.beeline.projectbackend.dto.CreateAssessmentResponseDTO;
import ru.beeline.projectbackend.service.AssessmentService;

import java.util.List;

import static ru.beeline.projectbackend.utils.Constant.ID_MUST_BE_POSITIVE;
import static ru.beeline.projectbackend.utils.Constant.USER_ID_HEADER;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
@Validated
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping
    public List<AssessmentResponseDTO> getAssessments(
            @RequestParam(name = "project-id", required = false) @Positive(message = ID_MUST_BE_POSITIVE) Integer projectId,
            @RequestParam(name = "status-id", required = false) @Positive(message = ID_MUST_BE_POSITIVE) Integer statusId,
            @RequestParam(name = "owner-id", required = false) @Positive(message = ID_MUST_BE_POSITIVE) Integer ownerId) {
        return assessmentService.getAssessments(projectId, statusId, ownerId);
    }

    @GetMapping("/{id}")
    public AssessmentDetailsResponseDTO getAssessmentById(@PathVariable @Positive(message = ID_MUST_BE_POSITIVE) Integer id) {
        return assessmentService.getAssessmentById(id);
    }

    @PostMapping
    public ResponseEntity<CreateAssessmentResponseDTO> createAssessment(
            @Valid @RequestBody CreateAssessmentRequestDTO request,
            @RequestHeader(value = USER_ID_HEADER, required = false) Integer userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.createAssessment(request, userId));
    }
}
