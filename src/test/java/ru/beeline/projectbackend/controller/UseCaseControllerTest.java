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
