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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.projectbackend.dto.usecase.OperationUseCasesResponseDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertRequestDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertResponseDTO;
import ru.beeline.projectbackend.service.UseCaseService;

import java.util.List;


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
            description = "Принимает карточку useCase (включая необязательный biStepCode), шаги useCaseCall и "
                    + "справочник операций reqOperations (включая необязательные productPlanned и isPlanned). "
                    + "Идентичность use case — пара (ветка проекта, useCase.uid): повторный вызов обновляет "
                    + "карточку и полностью пересоздаёт состав шагов. Карточка сохраняется до вызова fdm-products, "
                    + "затем состав методов публикуется через PUT /api/v1/use-case/operations?use-case-id={id} "
                    + "(массив интерфейсов: группы по interfaceCode+product, без кода — CRC32), и шаги ссылаются "
                    + "на возвращённые discovered_operation.id. Старые шаги удаляются только после успешного "
                    + "ответа. Вызов идёт внутри транзакции: ошибка смежного сервиса откатывает изменения use case.")
    public ResponseEntity<UseCaseUpsertResponseDTO> upsertUseCase(
            @Parameter(description = "Идентификатор проекта") @PathVariable @Positive Integer projectId,
            @Parameter(description = "Ветка проекта (main / design / develop / user), по умолчанию main")
            @RequestParam(required = false) String branch,
            @RequestBody(required = false) UseCaseUpsertRequestDTO request) {
        return ResponseEntity.ok(useCaseService.upsert(projectId, branch, request));
    }

    @PostMapping("/use-cases/by-operations")
    @Operation(summary = "Получить use case по идентификаторам методов",
            description = "Принимает JSON-массив целых discovered_operation.id без обёртки. Дубликаты id "
                    + "дедублицируются с сохранением порядка первого вхождения. Для каждого метода возвращаются "
                    + "use case, у которых метод стоит хотя бы на одной стороне шага us_operation_relation. "
                    + "Несуществующий id не является ошибкой: useCases = [].")
    public ResponseEntity<List<OperationUseCasesResponseDTO>> getUseCasesByOperations(
            @RequestBody(required = false) JsonNode request) {
        return ResponseEntity.ok(useCaseService.findUseCasesByOperations(request));
    }
}
