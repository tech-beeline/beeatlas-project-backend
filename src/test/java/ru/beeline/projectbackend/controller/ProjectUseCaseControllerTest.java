/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.beeline.projectbackend.client.ProductClient;
import ru.beeline.projectbackend.domain.ArtifactBranch;
import ru.beeline.projectbackend.domain.Project;
import ru.beeline.projectbackend.domain.UsOperationRelation;
import ru.beeline.projectbackend.domain.UseCase;
import ru.beeline.projectbackend.dto.product.DiscoveredOperationByIdsDTO;
import ru.beeline.projectbackend.repository.ArtifactBranchRepository;
import ru.beeline.projectbackend.repository.ProjectRepository;
import ru.beeline.projectbackend.repository.UseCaseByOperationProjection;
import ru.beeline.projectbackend.repository.UsOperationRelationRepository;
import ru.beeline.projectbackend.repository.UseCaseRepository;
import ru.beeline.projectbackend.service.UseCaseService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.beeline.projectbackend.utils.Constant.CONNECTION_OPERATION_ID_MUST_BE_INTEGER;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectUseCaseControllerTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ArtifactBranchRepository artifactBranchRepository;
    @Mock
    private UseCaseRepository useCaseRepository;
    @Mock
    private UsOperationRelationRepository usOperationRelationRepository;
    @Mock
    private ProductClient productClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UseCaseService useCaseService = new UseCaseService(projectRepository, artifactBranchRepository,
                useCaseRepository, usOperationRelationRepository, productClient);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectUseCaseController(useCaseService))
                .setControllerAdvice(new CustomExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        new ObjectMapper().registerModule(new JavaTimeModule())))
                .build();
    }

    @Test
    void getLiveUseCases_returnsCardsWithoutDeletedDateAndBranches() throws Exception {
        when(projectRepository.findByIdAndDeleteDateIsNull(1))
                .thenReturn(Optional.of(Project.builder().id(1).build()));
        when(useCaseRepository.findByProjectIdAndDeletedDateIsNullOrderByIdAsc(1)).thenReturn(List.of(
                UseCase.builder().id(8).projectId(1).code("UC-RETURN").name("Возврат")
                        .createdDate(LocalDateTime.of(2026, 9, 30, 12, 5, 0)).build(),
                UseCase.builder().id(7).projectId(1).code("UC-ORDER").name("Оформление заказа")
                        .description("Клиент оформляет заказ")
                        .createdDate(LocalDateTime.of(2026, 9, 30, 12, 0, 0))
                        .updatedDate(LocalDateTime.of(2026, 9, 30, 15, 0, 0)).build()));

        mockMvc.perform(get("/api/v1/project/1/use-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].code").value("UC-ORDER"))
                .andExpect(jsonPath("$[0].name").value("Оформление заказа"))
                .andExpect(jsonPath("$[0].description").value("Клиент оформляет заказ"))
                .andExpect(jsonPath("$[0].projectId").value(1))
                .andExpect(jsonPath("$[0].createdDate").value("2026-09-30T12:00:00"))
                .andExpect(jsonPath("$[0].updatedDate").value("2026-09-30T15:00:00"))
                .andExpect(jsonPath("$[0].deletedDate").doesNotExist())
                .andExpect(jsonPath("$[0].branch").doesNotExist())
                .andExpect(jsonPath("$[0].projectBranchId").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(8))
                .andExpect(jsonPath("$[1].code").value("UC-RETURN"))
                .andExpect(jsonPath("$[1].description").value(nullValue()))
                .andExpect(jsonPath("$[1].updatedDate").value(nullValue()))
                .andExpect(jsonPath("$[1].createdDate").value("2026-09-30T12:05:00"));
        verifyNoInteractions(artifactBranchRepository, productClient, usOperationRelationRepository);
    }

    @Test
    void getLiveUseCases_empty_returnsEmptyArray() throws Exception {
        when(projectRepository.findByIdAndDeleteDateIsNull(1))
                .thenReturn(Optional.of(Project.builder().id(1).build()));
        when(useCaseRepository.findByProjectIdAndDeletedDateIsNullOrderByIdAsc(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/project/1/use-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getLiveUseCases_deletedProject_returns404() throws Exception {
        when(projectRepository.findByIdAndDeleteDateIsNull(1)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/project/1/use-cases"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Проект не найден или удален"));
        verifyNoInteractions(useCaseRepository, artifactBranchRepository, productClient, usOperationRelationRepository);
    }

    @Test
    void getLiveUseCases_nonNumericProjectId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/project/abc/use-cases"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(projectRepository, useCaseRepository, artifactBranchRepository, productClient,
                usOperationRelationRepository);
    }

    @Test
    void upsertUseCase_connectionOperationIdNotInteger_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/project/1/use-case")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reqOperations\":[{\"connectionOperationId\":\"15\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value(CONNECTION_OPERATION_ID_MUST_BE_INTEGER));

        mockMvc.perform(put("/api/v1/project/1/use-case")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reqOperations\":[{\"connectionOperationId\":1.5}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value(CONNECTION_OPERATION_ID_MUST_BE_INTEGER));

        verifyNoInteractions(productClient);
    }

    @Test
    void getUseCasesByOperations_returnsGroupedResponse() throws Exception {
        List<UseCaseByOperationProjection> rows = List.of(
                row(null, 20, 7, "Оформление заказа"),
                row(20, 30, 8, "Возврат"));
        when(usOperationRelationRepository.findUseCasesByOperationIds(any()))
                .thenReturn(rows);

        mockMvc.perform(post("/api/v1/project/use-cases/by-operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[20, 99, 20]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].operationId").value(20))
                .andExpect(jsonPath("$[0].useCases[0].id").value(7))
                .andExpect(jsonPath("$[0].useCases[0].name").value("Оформление заказа"))
                .andExpect(jsonPath("$[0].useCases[1].id").value(8))
                .andExpect(jsonPath("$[1].operationId").value(99))
                .andExpect(jsonPath("$[1].useCases").isEmpty());
    }

    @Test
    void getUseCasesByOperations_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project/use-cases/by-operations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Отсутствует тело запроса"));
    }

    @Test
    void getUseCasesByOperations_notArray_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project/use-cases/by-operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operationIds\": [20]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Отсутствует тело запроса"));
    }

    @Test
    void getUseCasesByOperations_emptyArray_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project/use-cases/by-operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Список идентификаторов методов не может быть пустым"));
    }

    @Test
    void getUseCasesByOperations_nullElement_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project/use-cases/by-operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[20, null]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Идентификатор метода не может быть пустым"));
    }

    @Test
    void getUseCasesByOperations_nonNumericElement_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project/use-cases/by-operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"abc\"]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Идентификатор метода не может быть пустым"));
    }

    @Test
    void getUseCasesByOperations_numericString_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project/use-cases/by-operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"5\"]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Идентификатор метода не может быть пустым"));
    }

    @Test
    void getUseCasesByOperations_floatElement_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project/use-cases/by-operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[5.7]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Идентификатор метода не может быть пустым"));
    }

    @Test
    void getUseCasesByOperations_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project/use-cases/by-operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[20,"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Отсутствует тело запроса"));
    }

    @Test
    void getUseCasesByBiStepCodes_returnsGroups() throws Exception {
        when(useCaseRepository.findLiveByBiStepCodeInIgnoreCase(any())).thenReturn(List.of(
                UseCase.builder().id(12).projectId(4).code("UC-ORDER")
                        .name("Оформление заказа в другом проекте")
                        .biStepCode("Step.00.00.02.77").build(),
                UseCase.builder().id(7).projectId(1).code("UC-ORDER")
                        .name("Оформление заказа")
                        .biStepCode("Step.00.00.02.77").build()));

        mockMvc.perform(post("/api/v1/project/use-cases/by-bi-step-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"Step.00.00.02.77\",\"Step.00.00.02.84\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].biStepCode").value("Step.00.00.02.77"))
                .andExpect(jsonPath("$[0].useCases[0].id").value(7))
                .andExpect(jsonPath("$[0].useCases[0].name").value("Оформление заказа"))
                .andExpect(jsonPath("$[0].useCases[0].code").value("UC-ORDER"))
                .andExpect(jsonPath("$[0].useCases[0].projectId").value(1))
                .andExpect(jsonPath("$[0].useCases[1].id").value(12))
                .andExpect(jsonPath("$[0].useCases[1].projectId").value(4))
                .andExpect(jsonPath("$[1].biStepCode").value("Step.00.00.02.84"))
                .andExpect(jsonPath("$[1].useCases").isEmpty());
    }

    @Test
    void getUseCasesByBiStepCodes_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project/use-cases/by-bi-step-codes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Отсутствует тело запроса"));
        verifyNoInteractions(useCaseRepository);
    }

    @Test
    void getUseCasesByBiStepCodes_emptyElement_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project/use-cases/by-bi-step-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"Step.A\", \"\"]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Код bi-step не может быть пустым"));
        verifyNoInteractions(useCaseRepository);
    }

    @Test
    void getUseCasesByBiStepCodes_numberElement_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project/use-cases/by-bi-step-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Код bi-step не может быть пустым"));
        verifyNoInteractions(useCaseRepository);
    }

    @Test
    void getUseCaseOperations_returnsTree() throws Exception {
        when(projectRepository.findByIdAndDeleteDateIsNull(1))
                .thenReturn(Optional.of(Project.builder().id(1).build()));
        when(useCaseRepository.findByIdAndProjectIdAndDeletedDateIsNull(7, 1))
                .thenReturn(Optional.of(UseCase.builder().id(7).projectId(1).build()));
        when(artifactBranchRepository.findByArtifactTypeAndArtifactIdAndNameIgnoreCase("use_case", 7, "main"))
                .thenReturn(Optional.of(ArtifactBranch.builder().id(42).artifactType("use_case")
                        .artifactId(7).name("main").build()));
        when(usOperationRelationRepository.findAllByUseCaseBranchId(42)).thenReturn(List.of(
                UsOperationRelation.builder().id(1).useCaseBranchId(42).order(0).relatedReqOperationId(20).build(),
                UsOperationRelation.builder().id(2).useCaseBranchId(42).order(1)
                        .reqOperationId(20).relatedReqOperationId(21).build()));
        when(productClient.getDiscoveredOperationsByIds(List.of(20, 21)))
                .thenReturn(List.of(DiscoveredOperationByIdsDTO.builder().id(20).name("createOrder").build()));

        mockMvc.perform(get("/api/v1/project/1/use-case/7/operations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order").value(0))
                .andExpect(jsonPath("$[0].discoveredOperationId").value(20))
                .andExpect(jsonPath("$[0].discoveredOperation.name").value("createOrder"))
                .andExpect(jsonPath("$[0].children[0].discoveredOperationId").value(21))
                .andExpect(jsonPath("$[0].children[0].discoveredOperation").isEmpty())
                .andExpect(jsonPath("$[0].children[0].children").isEmpty());
    }

    @Test
    void getUseCaseOperations_emptyBranch_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/project/1/use-case/7/operations").param("branch", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Имя ветки не может быть пустым"));
    }

    @Test
    void getUseCaseOperations_useCaseNotFound_returns404() throws Exception {
        when(projectRepository.findByIdAndDeleteDateIsNull(1))
                .thenReturn(Optional.of(Project.builder().id(1).build()));
        when(useCaseRepository.findByIdAndProjectIdAndDeletedDateIsNull(7, 1)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/project/1/use-case/7/operations"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Use case не найден"));
    }

    private UseCaseByOperationProjection row(Integer reqOperationId, Integer relatedOperationId,
                                             Integer ucId, String ucName) {
        UseCaseByOperationProjection row = mock(UseCaseByOperationProjection.class);
        when(row.getReqOperationId()).thenReturn(reqOperationId);
        when(row.getRelatedReqOperationId()).thenReturn(relatedOperationId);
        when(row.getUcId()).thenReturn(ucId);
        when(row.getUcName()).thenReturn(ucName);
        return row;
    }
}
