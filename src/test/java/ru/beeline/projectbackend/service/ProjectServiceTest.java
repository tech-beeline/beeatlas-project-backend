/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.beeline.projectbackend.client.UserClient;
import ru.beeline.projectbackend.domain.ArtifactBranch;
import ru.beeline.projectbackend.domain.Assessment;
import ru.beeline.projectbackend.domain.AssessmentStatusEnum;
import ru.beeline.projectbackend.domain.Project;
import ru.beeline.projectbackend.domain.ProjectStatusEnum;
import ru.beeline.projectbackend.dto.CreateProjectRequestDTO;
import ru.beeline.projectbackend.dto.ProjectDetailsResponseDTO;
import ru.beeline.projectbackend.dto.ProjectListResponseDTO;
import ru.beeline.projectbackend.dto.ProjectResponseDTO;
import ru.beeline.projectbackend.dto.UserProfileShortDTO;
import ru.beeline.projectbackend.exception.AuthServiceException;
import ru.beeline.projectbackend.exception.ConflictException;
import ru.beeline.projectbackend.exception.NotFoundException;
import ru.beeline.projectbackend.repository.ArtifactBranchRepository;
import ru.beeline.projectbackend.repository.AssessmentRepository;
import ru.beeline.projectbackend.repository.AssessmentStatusEnumRepository;
import ru.beeline.projectbackend.repository.ProjectRepository;
import ru.beeline.projectbackend.repository.ProjectStatusEnumRepository;
import ru.beeline.projectbackend.repository.ProjectUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectUserRepository projectUserRepository;
    @Mock
    private ProjectStatusEnumRepository projectStatusEnumRepository;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private AssessmentStatusEnumRepository assessmentStatusEnumRepository;
    @Mock
    private UserClient userClient;
    @Mock
    private ArtifactBranchRepository artifactBranchRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProject_success() {
        CreateProjectRequestDTO request = new CreateProjectRequestDTO();
        request.setName("Мой проект");
        request.setDescription("Описание");
        request.setDocLink("https://confluence/page");
        request.setSource("BeeAtlas");

        when(projectRepository.existsByName("Мой проект")).thenReturn(false);
        when(projectStatusEnumRepository.findByName("Backlog"))
                .thenReturn(Optional.of(new ProjectStatusEnum(7, "Backlog", null)));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(1);
            return project;
        });

        ProjectResponseDTO response = projectService.createProject(request, 42);

        assertEquals(1, response.getId());
        assertEquals("Мой проект", response.getName());
        assertEquals("Описание", response.getDescription());
        assertEquals("https://confluence/page", response.getDocLink());
        assertEquals("BeeAtlas", response.getSource());
        assertEquals("PROJ.00.00.01", response.getUniqueIdent());
        assertEquals(7, response.getStatusId());
        assertEquals(42, response.getOwnerId());
        assertNotNull(response.getCreatedDate());

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertEquals("BeeAtlas", captor.getValue().getSource());
        assertEquals(7, captor.getValue().getStatusId());
        assertEquals(42, captor.getValue().getOwnerId());

        ArgumentCaptor<ArtifactBranch> branchCaptor = ArgumentCaptor.forClass(ArtifactBranch.class);
        verify(artifactBranchRepository).save(branchCaptor.capture());
        assertEquals("project", branchCaptor.getValue().getArtifactType());
        assertEquals(1, branchCaptor.getValue().getArtifactId());
        assertEquals("main", branchCaptor.getValue().getName());
    }

    @Test
    void createProject_optionalFieldsMayBeNull() {
        CreateProjectRequestDTO request = new CreateProjectRequestDTO();
        request.setName("Только имя");
        request.setSource("Jira");

        when(projectRepository.existsByName("Только имя")).thenReturn(false);
        when(projectStatusEnumRepository.findByName("Backlog"))
                .thenReturn(Optional.of(new ProjectStatusEnum(1, "Backlog", null)));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(123);
            return project;
        });

        ProjectResponseDTO response = projectService.createProject(request, null);

        assertNull(response.getDescription());
        assertNull(response.getDocLink());
        assertNull(response.getOwnerId());
        assertEquals("PROJ.00.01.23", response.getUniqueIdent());
        assertEquals("Jira", response.getSource());
    }

    @Test
    void createProject_duplicateName_throwsConflict() {
        CreateProjectRequestDTO request = new CreateProjectRequestDTO();
        request.setName("Существующий");

        when(projectRepository.existsByName("Существующий")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> projectService.createProject(request, 1));

        assertEquals("Проект с таким названием уже существует", exception.getMessage());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void createProject_backlogNotFound_throwsNotFound() {
        CreateProjectRequestDTO request = new CreateProjectRequestDTO();
        request.setName("Проект");

        when(projectRepository.existsByName("Проект")).thenReturn(false);
        when(projectStatusEnumRepository.findByName("Backlog")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> projectService.createProject(request, 1));

        assertEquals("Статус проекта Backlog не найден", exception.getMessage());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void getProjectById_returnsProjectWithLatestDoneAssessment() {
        Project project = Project.builder()
                .id(123)
                .name("Мой проект")
                .source("BeeAtlas")
                .uniqueIdent("PROJ.00.01.23")
                .ownerId(42)
                .statusId(2)
                .createdDate(LocalDateTime.of(2026, 8, 19, 12, 0))
                .updateDate(LocalDateTime.of(2026, 8, 19, 14, 0))
                .build();
        Assessment doneAssessment = Assessment.builder()
                .id(10)
                .projectId(123)
                .statusId(5)
                .ownerId(42)
                .source("text")
                .impactLevel("M")
                .rawText("Бизнес-постановка")
                .taskDescription("Описание задачи")
                .createdDate(LocalDateTime.of(2026, 8, 18, 10, 0))
                .updateDate(LocalDateTime.of(2026, 8, 18, 11, 0))
                .build();

        when(projectRepository.findByIdAndDeleteDateIsNull(123)).thenReturn(Optional.of(project));
        when(projectStatusEnumRepository.findById(2))
                .thenReturn(Optional.of(new ProjectStatusEnum(2, "InWork", null)));
        when(assessmentStatusEnumRepository.findByName("Done"))
                .thenReturn(Optional.of(new AssessmentStatusEnum(5, "Done", null)));
        when(assessmentRepository.findFirstByProjectIdAndStatusIdAndDeleteDateIsNullOrderByCreatedDateDescUpdateDateDesc(123, 5))
                .thenReturn(Optional.of(doneAssessment));
        when(userClient.findUserProfiles(List.of(42))).thenReturn(List.of(
                UserProfileShortDTO.builder().id(42).fullName("Иван Иванов").build()));

        ProjectDetailsResponseDTO response = projectService.getProjectById(123);

        assertEquals(123, response.getId());
        assertEquals("PROJ.00.01.23", response.getUniqueIdent());
        assertEquals("Мой проект", response.getName());
        assertEquals("BeeAtlas", response.getSource());
        assertEquals(42, response.getOwnerId());
        assertEquals("Иван Иванов", response.getOwnerName());
        assertEquals(2, response.getStatusId());
        assertEquals("InWork", response.getStatusName());
        assertEquals("M", response.getImpactLevel());
        assertEquals("Бизнес-постановка", response.getRawText());
        assertEquals("Описание задачи", response.getTaskDescription());
        assertEquals(LocalDateTime.of(2026, 8, 19, 12, 0), response.getCreatedDate());
        assertEquals(LocalDateTime.of(2026, 8, 19, 14, 0), response.getUpdatedDate());
    }

    @Test
    void getProjectById_withoutDoneAssessment_returnsNullAssessmentFields() {
        Project project = Project.builder()
                .id(1)
                .name("Проект")
                .source("BeeAtlas")
                .uniqueIdent("PROJ.00.00.01")
                .ownerId(7)
                .statusId(1)
                .createdDate(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        when(projectRepository.findByIdAndDeleteDateIsNull(1)).thenReturn(Optional.of(project));
        when(projectStatusEnumRepository.findById(1))
                .thenReturn(Optional.of(new ProjectStatusEnum(1, "Backlog", null)));
        when(assessmentStatusEnumRepository.findByName("Done"))
                .thenReturn(Optional.of(new AssessmentStatusEnum(5, "Done", null)));
        when(assessmentRepository.findFirstByProjectIdAndStatusIdAndDeleteDateIsNullOrderByCreatedDateDescUpdateDateDesc(1, 5))
                .thenReturn(Optional.empty());
        when(userClient.findUserProfiles(List.of(7))).thenReturn(List.of());

        ProjectDetailsResponseDTO response = projectService.getProjectById(1);

        assertNull(response.getImpactLevel());
        assertNull(response.getRawText());
        assertNull(response.getTaskDescription());
        assertNull(response.getOwnerName());
        assertEquals("Backlog", response.getStatusName());
    }

    @Test
    void getProjectById_notFound_throws404() {
        when(projectRepository.findByIdAndDeleteDateIsNull(99)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> projectService.getProjectById(99));

        assertEquals("Проект не найден или удален", exception.getMessage());
        verify(userClient, never()).findUserProfiles(any());
    }

    @Test
    void getProjects_returnsListWithOwnerAndLatestDoneAssessment() {
        Project newer = Project.builder()
                .id(2)
                .name("Новый")
                .source("BeeAtlas")
                .uniqueIdent("PROJ.00.00.02")
                .docLink("https://conf/new")
                .ownerId(42)
                .statusId(2)
                .createdDate(LocalDateTime.of(2026, 8, 19, 12, 0))
                .updateDate(LocalDateTime.of(2026, 8, 19, 14, 0))
                .build();
        Project older = Project.builder()
                .id(1)
                .name("Старый")
                .source("BeeAtlas")
                .uniqueIdent("PROJ.00.00.01")
                .ownerId(7)
                .statusId(1)
                .createdDate(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updateDate(LocalDateTime.of(2026, 1, 2, 10, 0))
                .build();
        Assessment doneForNewer = Assessment.builder()
                .id(10)
                .projectId(2)
                .statusId(5)
                .ownerId(42)
                .source("text")
                .impactLevel("H")
                .rawText("Актуальная постановка")
                .taskDescription("Задача")
                .createdDate(LocalDateTime.of(2026, 8, 18, 12, 0))
                .build();
        Assessment olderDoneForNewer = Assessment.builder()
                .id(9)
                .projectId(2)
                .statusId(5)
                .ownerId(42)
                .source("text")
                .impactLevel("L")
                .rawText("Старая постановка")
                .createdDate(LocalDateTime.of(2026, 8, 1, 12, 0))
                .build();

        when(projectRepository.findAllFiltered(null, null)).thenReturn(List.of(newer, older));
        when(projectStatusEnumRepository.findAllById(any())).thenReturn(List.of(
                new ProjectStatusEnum(1, "Backlog", null),
                new ProjectStatusEnum(2, "InWork", null)));
        when(assessmentStatusEnumRepository.findByName("Done"))
                .thenReturn(Optional.of(new AssessmentStatusEnum(5, "Done", null)));
        when(assessmentRepository.findByProjectIdInAndStatusIdAndDeleteDateIsNullOrderByCreatedDateDescUpdateDateDesc(
                List.of(2, 1), 5)).thenReturn(List.of(doneForNewer, olderDoneForNewer));
        when(userClient.findUserProfiles(anyList())).thenReturn(List.of(
                UserProfileShortDTO.builder().id(42).fullName("Иван Иванов").build(),
                UserProfileShortDTO.builder().id(7).fullName("Пётр Петров").build()));

        List<ProjectListResponseDTO> response = projectService.getProjects(null, null);

        assertEquals(2, response.size());
        assertEquals(2, response.getFirst().getId());
        assertEquals("PROJ.00.00.02", response.getFirst().getUniqueIdent());
        assertEquals("https://conf/new", response.getFirst().getDocLink());
        assertEquals("Иван Иванов", response.getFirst().getOwnerName());
        assertEquals("InWork", response.getFirst().getStatusName());
        assertEquals("H", response.getFirst().getImpactLevel());
        assertEquals("Актуальная постановка", response.getFirst().getRawText());
        assertEquals("Задача", response.getFirst().getTaskDescription());
        assertEquals(1, response.get(1).getId());
        assertEquals("Пётр Петров", response.get(1).getOwnerName());
        assertEquals("Backlog", response.get(1).getStatusName());
        assertNull(response.get(1).getImpactLevel());
    }

    @Test
    void getProjects_passesFilters() {
        when(projectRepository.findAllFiltered(42, 2)).thenReturn(List.of());

        List<ProjectListResponseDTO> response = projectService.getProjects(2, 42);

        assertTrue(response.isEmpty());
        verify(projectRepository).findAllFiltered(42, 2);
        verify(userClient, never()).findUserProfiles(any());
    }

    @Test
    void getProjects_authUnavailable_throws() {
        Project project = Project.builder()
                .id(1)
                .name("Проект")
                .source("BeeAtlas")
                .uniqueIdent("PROJ.00.00.01")
                .ownerId(42)
                .statusId(1)
                .createdDate(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        when(projectRepository.findAllFiltered(null, null)).thenReturn(List.of(project));
        when(projectStatusEnumRepository.findAllById(any()))
                .thenReturn(List.of(new ProjectStatusEnum(1, "Backlog", null)));
        when(assessmentStatusEnumRepository.findByName("Done"))
                .thenReturn(Optional.of(new AssessmentStatusEnum(5, "Done", null)));
        when(assessmentRepository.findByProjectIdInAndStatusIdAndDeleteDateIsNullOrderByCreatedDateDescUpdateDateDesc(
                List.of(1), 5)).thenReturn(List.of());
        when(userClient.findUserProfiles(anyList()))
                .thenThrow(new AuthServiceException("Сервис авторизации недоступен"));

        AuthServiceException exception = assertThrows(AuthServiceException.class,
                () -> projectService.getProjects(null, null));

        assertEquals("Сервис авторизации недоступен", exception.getMessage());
    }
}
