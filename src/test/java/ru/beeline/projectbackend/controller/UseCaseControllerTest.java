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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.beeline.projectbackend.client.ProductClient;
import ru.beeline.projectbackend.domain.ArtifactBranch;
import ru.beeline.projectbackend.domain.UseCase;
import ru.beeline.projectbackend.repository.ArtifactBranchRepository;
import ru.beeline.projectbackend.repository.ProjectRepository;
import ru.beeline.projectbackend.repository.UsOperationRelationRepository;
import ru.beeline.projectbackend.repository.UseCaseRepository;
import ru.beeline.projectbackend.service.UseCaseService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UseCaseControllerTest {

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
        mockMvc = MockMvcBuilders.standaloneSetup(new UseCaseController(useCaseService))
                .setControllerAdvice(new CustomExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        new ObjectMapper().registerModule(new JavaTimeModule())))
                .build();
    }

    @Test
    void getAllLiveUseCases_returnsFlatCatalogWithBiStepCode() throws Exception {
        when(useCaseRepository.findByDeletedDateIsNullOrderByIdAsc()).thenReturn(List.of(
                UseCase.builder().id(12).projectId(4).code("UC-ORDER")
                        .name("Оформление заказа в другом проекте")
                        .biStepCode("Step.00.00.02.77").build(),
                UseCase.builder().id(8).projectId(1).code("UC-DRAFT")
                        .name("Черновик").description(null).biStepCode(null).build(),
                UseCase.builder().id(7).projectId(1).code("UC-ORDER")
                        .name("Оформление заказа").description("Клиент оформляет заказ")
                        .biStepCode("Step.00.00.02.77").build()));

        mockMvc.perform(get("/api/v1/use-case"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].code").value("UC-ORDER"))
                .andExpect(jsonPath("$[0].name").value("Оформление заказа"))
                .andExpect(jsonPath("$[0].description").value("Клиент оформляет заказ"))
                .andExpect(jsonPath("$[0].projectId").value(1))
                .andExpect(jsonPath("$[0].biStepCode").value("Step.00.00.02.77"))
                .andExpect(jsonPath("$[0].createdDate").doesNotExist())
                .andExpect(jsonPath("$[0].updatedDate").doesNotExist())
                .andExpect(jsonPath("$[0].deletedDate").doesNotExist())
                .andExpect(jsonPath("$[0].branches").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(8))
                .andExpect(jsonPath("$[1].description").value(nullValue()))
                .andExpect(jsonPath("$[1].biStepCode").value(nullValue()))
                .andExpect(jsonPath("$[2].id").value(12))
                .andExpect(jsonPath("$[2].projectId").value(4))
                .andExpect(jsonPath("$[2].biStepCode").value("Step.00.00.02.77"));
        verifyNoInteractions(projectRepository, artifactBranchRepository, productClient, usOperationRelationRepository);
    }

    @Test
    void getAllLiveUseCases_empty_returnsEmptyArray() throws Exception {
        when(useCaseRepository.findByDeletedDateIsNullOrderByIdAsc()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/use-case"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        verifyNoInteractions(projectRepository, artifactBranchRepository, productClient, usOperationRelationRepository);
    }

    @Test
    void getUseCase_returnsCardWithBranches() throws Exception {
        when(useCaseRepository.findById(7)).thenReturn(Optional.of(UseCase.builder()
                .id(7)
                .projectId(1)
                .code("UC-ORDER")
                .name("Оформление заказа")
                .description("Клиент оформляет заказ")
                .biStepCode("BI-STEP-1")
                .createdDate(LocalDateTime.of(2026, 9, 30, 12, 0, 0))
                .updatedDate(LocalDateTime.of(2026, 9, 30, 15, 0, 0))
                .deletedDate(null)
                .build()));
        when(artifactBranchRepository.findByArtifactTypeAndArtifactId("use_case", 7)).thenReturn(List.of(
                ArtifactBranch.builder().id(4).artifactType("use_case").artifactId(7).name("main").build(),
                ArtifactBranch.builder().id(5).artifactType("use_case").artifactId(7).name("design").build()));

        mockMvc.perform(get("/api/v1/use-case/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.code").value("UC-ORDER"))
                .andExpect(jsonPath("$.name").value("Оформление заказа"))
                .andExpect(jsonPath("$.description").value("Клиент оформляет заказ"))
                .andExpect(jsonPath("$.biStepCode").value("BI-STEP-1"))
                .andExpect(jsonPath("$.createdDate").value("2026-09-30T12:00:00"))
                .andExpect(jsonPath("$.updatedDate").value("2026-09-30T15:00:00"))
                .andExpect(jsonPath("$.deletedDate").value(nullValue()))
                .andExpect(jsonPath("$.branches[0].id").value(5))
                .andExpect(jsonPath("$.branches[0].name").value("design"))
                .andExpect(jsonPath("$.branches[1].id").value(4))
                .andExpect(jsonPath("$.branches[1].name").value("main"))
                .andExpect(jsonPath("$.branches.length()").value(2));
        verifyNoInteractions(productClient, usOperationRelationRepository);
    }

    @Test
    void getUseCase_unknownId_returns404() throws Exception {
        when(useCaseRepository.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/use-case/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Use case не найден"));
    }
}
