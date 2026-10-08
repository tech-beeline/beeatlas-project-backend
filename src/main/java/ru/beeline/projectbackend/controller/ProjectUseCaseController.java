/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import ru.beeline.projectbackend.dto.error.ErrorResponseDTO;
import ru.beeline.projectbackend.dto.usecase.OperationUseCasesResponseDTO;
import ru.beeline.projectbackend.dto.usecase.ProjectUseCaseResponseDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseByBiStepCodesGroupDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseOperationNodeDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseOperationsGroupedDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertRequestDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertResponseDTO;
import ru.beeline.projectbackend.service.UseCaseService;

import java.util.List;

import static ru.beeline.projectbackend.utils.Constant.ID_MUST_BE_POSITIVE;


@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
@Validated
@Tag(name = "project-use-case",
        description = "Публикация use case (карточка принадлежит проекту; шаги — на ветке use case).")
public class ProjectUseCaseController {

    private final UseCaseService useCaseService;

    @GetMapping("/{projectId}/use-cases")
    @Operation(summary = "Получить живые use case проекта",
            description = "Возвращает карточки сценариев проекта, у которых deleted_date пустой. "
                    + "Карточка привязана к проекту напрямую (use_case.project_id). "
                    + "Шаги us_operation_relation и ветки use case не возвращаются. "
                    + "Проект без живых use case — пустой массив.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Живые use case проекта",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProjectUseCaseResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "projectId не является целым положительным числом",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Проект не найден или удален",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<ProjectUseCaseResponseDTO>> getLiveUseCases(
            @Parameter(description = "Идентификатор проекта, целое положительное")
            @PathVariable @Positive(message = ID_MUST_BE_POSITIVE) Integer projectId) {
        return ResponseEntity.ok(useCaseService.getLiveUseCases(projectId));
    }

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
                    + "Атрибуты метода — элемент POST /api/v1/discovered-operation/by-ids целиком: "
                    + "у operation только id, name, type, tcId, techCapability, sla со всеми колонками, "
                    + "interface с id и code и container с id и code. "
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

    @PostMapping("/use-cases/operations/grouped-by-use-case")
    @Operation(summary = "Найти use case по списку методов",
            description = "По списку discovered_operation.id возвращает карточки use case, в шагах которых "
                    + "есть хотя бы один переданный метод. Для каждой карточки — id, name, code и список "
                    + "operationIds: только те id из запроса, которые реально участвуют в её шагах. "
                    + "Тело — JSON-массив целых чисел без обёртки. Дубликаты схлопываются, порядок — "
                    + "первое вхождение. Карточки отсортированы по id. Методов без совпадений в ответе нет; "
                    + "если совпадений нет совсем — пустой массив.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карточки use case с operationIds из тела",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = UseCaseOperationsGroupedDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Неверное тело запроса",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<UseCaseOperationsGroupedDTO>> getOperationsGroupedByUseCase(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "JSON-массив целых discovered_operation.id",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Integer.class,
                            example = "20"))))
            @RequestBody(required = false) JsonNode request) {
        return ResponseEntity.ok(useCaseService.findOperationsGroupedByUseCase(request));
    }

    @PostMapping("/use-cases/by-bi-step-codes")
    @Operation(summary = "Получить use case по кодам шагов BI",
            description = "Возвращает use case, сгруппированные по кодам шагов BI.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Группы в порядке уникальных кодов из тела",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = UseCaseByBiStepCodesGroupDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Неверное тело запроса",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<UseCaseByBiStepCodesGroupDTO>> getUseCasesByBiStepCodes(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "JSON-массив кодов шагов BI",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class,
                            example = "Step.00.00.02.77"))))
            @RequestBody(required = false) JsonNode request) {
        return ResponseEntity.ok(useCaseService.findUseCasesByBiStepCodes(request));
    }
}
