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
import ru.beeline.projectbackend.dto.CreateProjectRequestDTO;
import ru.beeline.projectbackend.dto.ProjectDetailsResponseDTO;
import ru.beeline.projectbackend.dto.ProjectListResponseDTO;
import ru.beeline.projectbackend.dto.ProjectResponseDTO;
import ru.beeline.projectbackend.exception.AuthServiceException;
import ru.beeline.projectbackend.exception.ConflictException;
import ru.beeline.projectbackend.exception.NotFoundException;
import ru.beeline.projectbackend.service.ProjectService;

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
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectController(projectService))
                .setControllerAdvice(new CustomExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createProject_returns201() throws Exception {
        when(projectService.createProject(any(CreateProjectRequestDTO.class), eq(42)))
                .thenReturn(ProjectResponseDTO.builder()
                        .id(1)
                        .name("Мой проект")
                        .description("Описание")
                        .source("BeeAtlas")
                        .docLink("https://confluence/page")
                        .createdDate(LocalDateTime.of(2026, 2, 25, 15, 30, 0))
                        .uniqueIdent("PROJ.00.00.01")
                        .statusId(7)
                        .ownerId(42)
                        .build());

        mockMvc.perform(post("/api/v1/project")
                        .header(USER_ID_HEADER, 42)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Мой проект",
                                  "description": "Описание",
                                  "docLink": "https://confluence/page",
                                  "source": "BeeAtlas"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Мой проект"))
                .andExpect(jsonPath("$.description").value("Описание"))
                .andExpect(jsonPath("$.source").value("BeeAtlas"))
                .andExpect(jsonPath("$.docLink").value("https://confluence/page"))
                .andExpect(jsonPath("$.createdDate").value("2026-02-25T15:30:00"))
                .andExpect(jsonPath("$.uniqueIdent").value("PROJ.00.00.01"))
                .andExpect(jsonPath("$.statusId").value(7))
                .andExpect(jsonPath("$.ownerId").value(42));
    }

    @Test
    void createProject_withoutUserIdHeader_stillCallsService() throws Exception {
        when(projectService.createProject(any(CreateProjectRequestDTO.class), isNull()))
                .thenReturn(ProjectResponseDTO.builder()
                        .id(1)
                        .name("Проект")
                        .source("BeeAtlas")
                        .uniqueIdent("PROJ.00.00.01")
                        .statusId(1)
                        .build());

        mockMvc.perform(post("/api/v1/project")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Проект\", \"source\": \"BeeAtlas\"}"))
                .andExpect(status().isCreated());

        verify(projectService).createProject(any(CreateProjectRequestDTO.class), isNull());
    }

    @Test
    void createProject_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("name/source обязательные параметры"));
    }

    @Test
    void createProject_missingSource_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Проект\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("name/source обязательные параметры"));
    }

    @Test
    void createProject_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/project")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"source\": \"BeeAtlas\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("name/source обязательные параметры"));
    }

    @Test
    void createProject_duplicateName_returns409() throws Exception {
        when(projectService.createProject(any(CreateProjectRequestDTO.class), any()))
                .thenThrow(new ConflictException("Проект с таким названием уже существует"));

        mockMvc.perform(post("/api/v1/project")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Существующий\", \"source\": \"BeeAtlas\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorMessage").value("Проект с таким названием уже существует"));
    }

    @Test
    void getProjectById_returnsDetailsWithoutAuthHeader() throws Exception {
        when(projectService.getProjectById(123)).thenReturn(ProjectDetailsResponseDTO.builder()
                .id(123)
                .uniqueIdent("PROJ.00.01.23")
                .name("Мой проект")
                .source("BeeAtlas")
                .ownerId(42)
                .ownerName("Иван Иванов")
                .statusId(2)
                .statusName("InWork")
                .impactLevel("M")
                .rawText("Бизнес-постановка")
                .taskDescription("Описание задачи")
                .createdDate(LocalDateTime.of(2026, 8, 19, 12, 0, 0))
                .updatedDate(LocalDateTime.of(2026, 8, 19, 14, 0, 0))
                .build());

        mockMvc.perform(get("/api/v1/project/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.uniqueIdent").value("PROJ.00.01.23"))
                .andExpect(jsonPath("$.name").value("Мой проект"))
                .andExpect(jsonPath("$.ownerName").value("Иван Иванов"))
                .andExpect(jsonPath("$.statusName").value("InWork"))
                .andExpect(jsonPath("$.impactLevel").value("M"))
                .andExpect(jsonPath("$.rawText").value("Бизнес-постановка"))
                .andExpect(jsonPath("$.taskDescription").value("Описание задачи"))
                .andExpect(jsonPath("$.createdDate").value("2026-08-19T12:00:00"))
                .andExpect(jsonPath("$.updatedDate").value("2026-08-19T14:00:00"));
    }

    @Test
    void getProjectById_notFound_returns404() throws Exception {
        when(projectService.getProjectById(99)).thenThrow(new NotFoundException("Проект не найден или удален"));

        mockMvc.perform(get("/api/v1/project/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Проект не найден или удален"));
    }

    @Test
    void getProjectById_authUnavailable_returns503() throws Exception {
        when(projectService.getProjectById(123))
                .thenThrow(new AuthServiceException("Сервис авторизации недоступен"));

        mockMvc.perform(get("/api/v1/project/123"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorMessage").value("Сервис авторизации недоступен"));
    }

    @Test
    void getProjects_returnsListWithoutAuthHeader() throws Exception {
        when(projectService.getProjects(null, null)).thenReturn(List.of(
                ProjectListResponseDTO.builder()
                        .id(123)
                        .uniqueIdent("PROJ.00.01.23")
                        .name("Мой проект")
                        .docLink("https://conf/page")
                        .ownerId(42)
                        .ownerName("Иван Иванов")
                        .statusId(2)
                        .statusName("InWork")
                        .impactLevel("M")
                        .rawText("Бизнес-постановка")
                        .taskDescription("Описание задачи")
                        .createdDate(LocalDateTime.of(2026, 8, 19, 12, 0, 0))
                        .updatedDate(LocalDateTime.of(2026, 8, 19, 14, 0, 0))
                        .build()));

        mockMvc.perform(get("/api/v1/project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(123))
                .andExpect(jsonPath("$[0].uniqueIdent").value("PROJ.00.01.23"))
                .andExpect(jsonPath("$[0].name").value("Мой проект"))
                .andExpect(jsonPath("$[0].docLink").value("https://conf/page"))
                .andExpect(jsonPath("$[0].ownerName").value("Иван Иванов"))
                .andExpect(jsonPath("$[0].statusName").value("InWork"))
                .andExpect(jsonPath("$[0].impactLevel").value("M"))
                .andExpect(jsonPath("$[0].rawText").value("Бизнес-постановка"))
                .andExpect(jsonPath("$[0].taskDescription").value("Описание задачи"))
                .andExpect(jsonPath("$[0].createdDate").value("2026-08-19T12:00:00"))
                .andExpect(jsonPath("$[0].updatedDate").value("2026-08-19T14:00:00"));
    }

    @Test
    void getProjects_passesQueryFilters() throws Exception {
        when(projectService.getProjects(eq(2), eq(42))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/project")
                        .param("status-id", "2")
                        .param("owner-id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(projectService).getProjects(2, 42);
    }

    @Test
    void getProjects_authUnavailable_returns503() throws Exception {
        when(projectService.getProjects(null, null))
                .thenThrow(new AuthServiceException("Сервис авторизации недоступен"));

        mockMvc.perform(get("/api/v1/project"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorMessage").value("Сервис авторизации недоступен"));
    }
}
