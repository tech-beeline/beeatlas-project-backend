/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertRequestDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertResponseDTO;
import ru.beeline.projectbackend.service.UseCaseService;

@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
@Validated
@Tag(name = "project-use-case",
        description = "Публикация use case (карточка + шаги вызовов операций) в ветку проекта.")
public class ProjectUseCaseController {

    private final UseCaseService useCaseService;

    @PutMapping("/{projectId}/use-case")
    @Operation(summary = "Создать или обновить use case проекта",
            description = "Принимает карточку useCase, шаги useCaseCall и справочник операций reqOperations. "
                    + "Идентичность use case — пара (ветка проекта, useCase.uid): повторный вызов обновляет "
                    + "карточку и полностью пересоздаёт состав шагов. Операции метод хранит не у себя: "
                    + "справочник публикуется в fdm-products (PUT /api/v1/discovered-interface/project/{projectId}"
                    + "?source=ProjectTask), и шаги ссылаются на возвращённые discovered_operation.id. "
                    + "Вызов идёт внутри транзакции: ошибка смежного сервиса откатывает изменения use case.")
    public ResponseEntity<UseCaseUpsertResponseDTO> upsertUseCase(
            @Parameter(description = "Идентификатор проекта") @PathVariable @Positive Integer projectId,
            @Parameter(description = "Ветка проекта (main / design / develop / user), по умолчанию main")
            @RequestParam(required = false) String branch,
            @RequestBody(required = false) UseCaseUpsertRequestDTO request) {
        return ResponseEntity.ok(useCaseService.upsert(projectId, branch, request));
    }
}
