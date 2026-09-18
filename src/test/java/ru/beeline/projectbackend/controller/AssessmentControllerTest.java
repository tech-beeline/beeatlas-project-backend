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
import ru.beeline.projectbackend.dto.AssessmentDesignTcDTO;
import ru.beeline.projectbackend.dto.AssessmentDetailsResponseDTO;
import ru.beeline.projectbackend.dto.AssessmentOpenQuestionDTO;
import ru.beeline.projectbackend.dto.AssessmentReqFuncDTO;
import ru.beeline.projectbackend.dto.AssessmentReqNonFuncDTO;
import ru.beeline.projectbackend.dto.AssessmentResponseDTO;
import ru.beeline.projectbackend.dto.AssessmentTcItemDTO;
import ru.beeline.projectbackend.dto.CreateAssessmentRequestDTO;
import ru.beeline.projectbackend.dto.CreateAssessmentResponseDTO;
import ru.beeline.projectbackend.exception.AuthServiceException;
import ru.beeline.projectbackend.exception.NotFoundException;
import ru.beeline.projectbackend.exception.ValidationException;
import ru.beeline.projectbackend.service.AssessmentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.beeline.projectbackend.utils.Constant.USER_ID_HEADER;

@ExtendWith(MockitoExtension.class)
class AssessmentControllerTest {

    @Mock
    private AssessmentService assessmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(new AssessmentController(assessmentService))
                .setControllerAdvice(new CustomExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getAssessments_returnsListWithoutAuthHeader() throws Exception {
        when(assessmentService.getAssessments(null, null, null)).thenReturn(List.of(
                AssessmentResponseDTO.builder()
                        .id(123).projectId(10).ownerId(42).ownerName("Иван Иванов")
                        .statusId(7).statusName("Draft").impactLevel("M")
                        .taskDescription("Описание задачи")
                        .reqFuncCount(2).reqNonFuncCount(1).tcCount(4).productCount(1).oqCount(1)
                        .createdDate(LocalDateTime.of(2026, 8, 19, 12, 0, 0))
                        .updatedDate(LocalDateTime.of(2026, 8, 19, 14, 0, 0))
                        .build()));

        mockMvc.perform(get("/api/v1/assessments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tcCount").value(4))
                .andExpect(jsonPath("$[0].productCount").value(1))
                .andExpect(jsonPath("$[0].oqCount").value(1));
    }

    @Test
    void getAssessments_passesQueryFilters() throws Exception {
        when(assessmentService.getAssessments(eq(10), eq(7), eq(42))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/assessments")
                        .param("project-id", "10")
                        .param("status-id", "7")
                        .param("owner-id", "42"))
                .andExpect(status().isOk());

        verify(assessmentService).getAssessments(10, 7, 42);
    }

    @Test
    void getAssessments_authUnavailable_returns503() throws Exception {
        when(assessmentService.getAssessments(null, null, null))
                .thenThrow(new AuthServiceException("Сервис авторизации недоступен"));

        mockMvc.perform(get("/api/v1/assessments"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorMessage").value("Сервис авторизации недоступен"));
    }

    @Test
    void getAssessmentById_returnsDetails() throws Exception {
        when(assessmentService.getAssessmentById(123)).thenReturn(AssessmentDetailsResponseDTO.builder()
                .id(123).projectId(10).projectName("Мой проект").ownerId(42).ownerName("Иван Иванов")
                .statusId(7).statusName("Draft").source("text").sourceUrl("https://confluence/page")
                .rawText("Исходный текст").taskDescription("Описание задачи").impactLevel("M")
                .reqFunc(List.of(AssessmentReqFuncDTO.builder().id(1).uniqueIdent("FR-1").title("string").description("string").build()))
                .reqNonFunc(List.of(AssessmentReqNonFuncDTO.builder().id(1).uniqueIdent("NFR-1").title("string").description("string").build()))
                .openQuestions(List.of(AssessmentOpenQuestionDTO.builder().id(1).uniqueIdent("OQ-1").questionText("string").build()))
                .tc(List.of(AssessmentTcItemDTO.builder().id(1).tcCode("TC-001").productAlias("sys-a")
                        .productName("System A").parentBcCode("BC-001").frIds(List.of("FR-1")).build()))
                .designTc(List.of(AssessmentDesignTcDTO.builder().id(1).name("string").description("string")
                        .productAlias("sys-b").productName("System B").parentBcCode("BC-002").frIds(List.of("FR-3")).build()))
                .createdDate(LocalDateTime.of(2026, 8, 19, 12, 0, 0))
                .updatedDate(LocalDateTime.of(2026, 8, 19, 14, 0, 0))
                .build());

        mockMvc.perform(get("/api/v1/assessments/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").value("https://confluence/page"))
                .andExpect(jsonPath("$.tc[0].tcCode").value("TC-001"))
                .andExpect(jsonPath("$.designTc[0].name").value("string"));
    }

    @Test
    void getAssessmentById_notFound_returns404() throws Exception {
        when(assessmentService.getAssessmentById(99))
                .thenThrow(new NotFoundException("Оценка не найдена"));

        mockMvc.perform(get("/api/v1/assessments/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createAssessment_returns201() throws Exception {
        when(assessmentService.createAssessment(any(CreateAssessmentRequestDTO.class), eq(42)))
                .thenReturn(CreateAssessmentResponseDTO.builder()
                        .id(123).projectId(45).status("Done").impactLevel("M")
                        .createdDate(LocalDateTime.of(2026, 8, 19, 12, 0, 0))
                        .build());

        mockMvc.perform(post("/api/v1/assessments")
                        .header(USER_ID_HEADER, 42)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": 45,
                                  "source": "text",
                                  "rawText": "постановка",
                                  "impactLevel": "M",
                                  "requirementsFunc": [],
                                  "requirementsNonFunc": [],
                                  "openQuestions": [],
                                  "tc": [],
                                  "designTc": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(123));
    }

    @Test
    void createAssessment_missingRequired_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAssessment_confluenceWithoutUrl_returns400() throws Exception {
        when(assessmentService.createAssessment(any(CreateAssessmentRequestDTO.class), isNull()))
                .thenThrow(new ValidationException("sourceURL обязателен при source = confluence"));

        mockMvc.perform(post("/api/v1/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": 45,
                                  "source": "confluence",
                                  "rawText": "постановка",
                                  "impactLevel": "M",
                                  "requirementsFunc": [],
                                  "requirementsNonFunc": [],
                                  "openQuestions": [],
                                  "tc": [],
                                  "designTc": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
