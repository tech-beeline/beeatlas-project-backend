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
import org.springframework.web.bind.annotation.*;
import ru.beeline.projectbackend.dto.CreateProjectRequestDTO;
import ru.beeline.projectbackend.dto.ProjectDetailsResponseDTO;
import ru.beeline.projectbackend.dto.ProjectListResponseDTO;
import ru.beeline.projectbackend.dto.ProjectResponseDTO;
import ru.beeline.projectbackend.dto.ProjectUserResponseDTO;
import ru.beeline.projectbackend.service.ProjectService;

import java.util.List;

import static ru.beeline.projectbackend.utils.Constant.ID_MUST_BE_POSITIVE;
import static ru.beeline.projectbackend.utils.Constant.USER_ID_HEADER;

@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
@Validated
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(
            @Valid @RequestBody CreateProjectRequestDTO request,
            @RequestHeader(value = USER_ID_HEADER, required = false) Integer userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request, userId));
    }

    @PostMapping("/{projectId}/user/{userId}")
    public ResponseEntity<ProjectUserResponseDTO> addUserToProject(
            @PathVariable @Positive(message = ID_MUST_BE_POSITIVE) Integer projectId,
            @PathVariable @Positive(message = ID_MUST_BE_POSITIVE) Integer userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addUserToProject(projectId, userId));
    }

    @GetMapping
    public List<ProjectListResponseDTO> getProjects(
            @RequestParam(name = "status-id", required = false) @Positive(message = ID_MUST_BE_POSITIVE) Integer statusId,
            @RequestParam(name = "owner-id", required = false) @Positive(message = ID_MUST_BE_POSITIVE) Integer ownerId) {
        return projectService.getProjects(statusId, ownerId);
    }

    @GetMapping("/{id}")
    public ProjectDetailsResponseDTO getProjectById(
            @PathVariable @Positive(message = ID_MUST_BE_POSITIVE) Integer id) {
        return projectService.getProjectById(id);
    }
}
