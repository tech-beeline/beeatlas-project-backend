/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.projectbackend.dto.usecase.OperationUseCasesResponseDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseOperationNodeDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertRequestDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertResponseDTO;
import ru.beeline.projectbackend.service.UseCaseService;

import java.util.List;


@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
@Validated
@Tag(name = "project-use-case",
        description = "Публикация use case (карточка принадлежит проекту; шаги — на ветке use case).")
public class ProjectUseCaseController {

    private final UseCaseService useCaseService;

    @PutMapping("/{projectId}/use-case")
    @Operation(summary = "Создать или обновить use case проекта",
            description = "Карточка принадлежит проекту (идентичность — projectId + useCase.uid). "
                    + "Шаги пишутся на ветку use case (query branch, по умолчанию main); отсутствующая ветка создаётся.")
    public ResponseEntity<UseCaseUpsertResponseDTO> upsertUseCase(
            @Parameter(description = "Идентификатор проекта") @PathVariable @Positive Integer projectId,
            @Parameter(description = "Ветка use case, по умолчанию main")
            @RequestParam(required = false) String branch,
            @RequestBody(required = false) UseCaseUpsertRequestDTO request) {
        return ResponseEntity.ok(useCaseService.upsert(projectId, branch, request));
    }

    @GetMapping("/{projectId}/use-case/{useCaseId}/operations")
    @Operation(summary = "Получить дерево вызовов методов ветки use case",
            description = "Возвращает методы выбранной ветки use case в виде дерева вызовов: "
                    + "корни — стартовые методы, дочерние узлы — методы, которые они вызывают. "
                    + "Каждый узел содержит информацию о методе, его интерфейсе и технической возможности. "
                    + "Ветка по умолчанию — main. Если в ветке нет методов, возвращается пустой список.")
    public ResponseEntity<List<UseCaseOperationNodeDTO>> getUseCaseOperations(
            @Parameter(description = "Идентификатор проекта") @PathVariable @Positive Integer projectId,
            @Parameter(description = "Идентификатор use case") @PathVariable @Positive Integer useCaseId,
            @Parameter(description = "Ветка use case, по умолчанию main")
            @RequestParam(required = false) String branch) {
        return ResponseEntity.ok(useCaseService.getOperationsTree(projectId, useCaseId, branch));
    }

    @PostMapping("/use-cases/by-operations")
    @Operation(summary = "Получить use case по идентификаторам методов",
            description = "Принимает JSON-массив целых discovered_operation.id без обёртки. Дубликаты id "
                    + "дедублицируются с сохранением порядка первого вхождения. Для каждого метода возвращаются "
                    + "карточки use case, у которых метод стоит хотя бы на одной стороне шага "
                    + "us_operation_relation (шаги привязаны к ветке use case). Одна карточка не дублируется, "
                    + "если метод есть в нескольких её ветках. Несуществующий id не является ошибкой: "
                    + "useCases = [].")
    public ResponseEntity<List<OperationUseCasesResponseDTO>> getUseCasesByOperations(
            @RequestBody(required = false) JsonNode request) {
        return ResponseEntity.ok(useCaseService.findUseCasesByOperations(request));
    }
}
