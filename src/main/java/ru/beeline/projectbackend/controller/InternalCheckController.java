/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.projectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.projectbackend.exception.NotFoundException;
import ru.beeline.projectbackend.repository.ProjectRepository;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InternalCheckController {

    private final ProjectRepository projectRepository;

    @GetMapping("/api/v1/internal/check/project/{id}/owner")
    public ResponseEntity<Map<String, Boolean>> checkProjectOwner(
            @PathVariable Integer id,
            @RequestParam Integer userId) {
        return projectRepository.findByIdAndDeleteDateIsNull(id)
                .map(project -> project.getOwnerId() != null && project.getOwnerId().equals(userId))
                .map(hasAccess -> ResponseEntity.ok(Map.of("hasAccess", hasAccess)))
                .orElseThrow(() -> new NotFoundException("Проект не найден или удален: " + id));
    }
}
