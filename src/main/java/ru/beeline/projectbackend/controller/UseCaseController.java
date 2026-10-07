/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.projectbackend.dto.error.ErrorResponseDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseCardResponseDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseCatalogItemDTO;
import ru.beeline.projectbackend.service.UseCaseService;

import java.util.List;

import static ru.beeline.projectbackend.utils.Constant.ID_MUST_BE_POSITIVE;

@RestController
@RequestMapping("/api/v1/use-case")
@RequiredArgsConstructor
@Validated
@Tag(name = "project-use-case")
public class UseCaseController {

    private final UseCaseService useCaseService;

    @GetMapping
    @Operation(summary = "Получить все живые use case",
            description = "Возвращает карточки сценариев с пустым deleted_date и кодом шага BI (biStepCode). "
                    + "Один код может быть у нескольких use case — без группировки. "
                    + "Ветки и шаги не возвращаются. Если живых карточек нет — пустой массив.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Живые use case",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = UseCaseCatalogItemDTO.class))))
    })
    public ResponseEntity<List<UseCaseCatalogItemDTO>> getAllLiveUseCases() {
        return ResponseEntity.ok(useCaseService.getAllLiveUseCases());
    }

    @GetMapping("/{useCaseId}")
    @Operation(summary = "Получить карточку use case и список её веток",
            description = "Возвращает карточку сценария и ветки, в которых он есть. Шаги вызовов не возвращаются.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карточка use case и её ветки",
                    content = @Content(schema = @Schema(implementation = UseCaseCardResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "useCaseId не является целым положительным числом",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Use case не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<UseCaseCardResponseDTO> getUseCase(
            @Parameter(description = "Идентификатор use case, целое положительное")
            @PathVariable @Positive(message = ID_MUST_BE_POSITIVE) Integer useCaseId) {
        return ResponseEntity.ok(useCaseService.getCard(useCaseId));
    }
}
