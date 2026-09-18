/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.projectbackend.client.UserClient;
import ru.beeline.projectbackend.domain.ArtifactBranch;
import ru.beeline.projectbackend.domain.Assessment;
import ru.beeline.projectbackend.domain.AssessmentStatusEnum;
import ru.beeline.projectbackend.domain.Project;
import ru.beeline.projectbackend.domain.ProjectStatusEnum;
import ru.beeline.projectbackend.domain.ProjectUser;
import ru.beeline.projectbackend.dto.CreateProjectRequestDTO;
import ru.beeline.projectbackend.dto.ProjectDetailsResponseDTO;
import ru.beeline.projectbackend.dto.ProjectListResponseDTO;
import ru.beeline.projectbackend.dto.ProjectResponseDTO;
import ru.beeline.projectbackend.dto.ProjectUserResponseDTO;
import ru.beeline.projectbackend.dto.UserProfileShortDTO;
import ru.beeline.projectbackend.exception.ConflictException;
import ru.beeline.projectbackend.exception.NotFoundException;
import ru.beeline.projectbackend.repository.ArtifactBranchRepository;
import ru.beeline.projectbackend.repository.AssessmentRepository;
import ru.beeline.projectbackend.repository.AssessmentStatusEnumRepository;
import ru.beeline.projectbackend.repository.ProjectRepository;
import ru.beeline.projectbackend.repository.ProjectStatusEnumRepository;
import ru.beeline.projectbackend.repository.ProjectUserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String STATUS_BACKLOG = "Backlog";
    private static final String STATUS_DONE = "Done";
    private static final String TEMPORARY_UNIQUE_IDENT = "temporary";
    private static final String DEFAULT_BRANCH = "main";

    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;
    private final ArtifactBranchRepository artifactBranchRepository;
    private final ProjectStatusEnumRepository projectStatusEnumRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentStatusEnumRepository assessmentStatusEnumRepository;
    private final UserClient userClient;

    @Transactional
    public ProjectResponseDTO createProject(CreateProjectRequestDTO request, Integer userId) {
        if (projectRepository.existsByName(request.getName())) {
            throw new ConflictException("Проект с таким названием уже существует");
        }
        Integer statusId = projectStatusEnumRepository.findByName(STATUS_BACKLOG)
                .orElseThrow(() -> new NotFoundException("Статус проекта Backlog не найден"))
                .getId();
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .docLink(request.getDocLink())
                .source(request.getSource())
                .uniqueIdent(TEMPORARY_UNIQUE_IDENT)
                .createdDate(LocalDateTime.now())
                .ownerId(userId)
                .statusId(statusId)
                .build();
        project = projectRepository.save(project);
        project.setUniqueIdent(toUniqueIdent(project.getId()));

        artifactBranchRepository.save(ArtifactBranch.builder()
                .artifactType(ArtifactBranch.TYPE_PROJECT)
                .artifactId(project.getId())
                .name(DEFAULT_BRANCH)
                .build());

        return toResponse(project);
    }

    public ProjectUserResponseDTO addUserToProject(Integer projectId, Integer userId) {
        projectRepository.findByIdAndDeleteDateIsNull(projectId)
                .orElseThrow(() -> new NotFoundException("Проекта с таким ID не существует"));
        if (projectUserRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ConflictException("Пользователь уже добавлен к проекту");
        }

        ProjectUser projectUser = ProjectUser.builder()
                .projectId(projectId)
                .userId(userId)
                .build();

        projectUser = projectUserRepository.save(projectUser);

        return ProjectUserResponseDTO.builder()
                .id(projectUser.getId())
                .projectId(projectUser.getProjectId())
                .userId(projectUser.getUserId())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProjectListResponseDTO> getProjects(Integer statusId, Integer ownerId) {
        List<Project> projects = projectRepository.findAllFiltered(ownerId, statusId);
        if (projects.isEmpty()) {
            return List.of();
        }
        Set<Integer> statusIds = projects.stream()
                .map(Project::getStatusId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, String> statusNames = projectStatusEnumRepository.findAllById(statusIds).stream()
                .collect(Collectors.toMap(ProjectStatusEnum::getId, ProjectStatusEnum::getName));
        Map<Integer, Assessment> latestDoneAssessments = latestDoneAssessmentsByProjectId(
                projects.stream().map(Project::getId).toList());
        Set<Integer> ownerIds = projects.stream()
                .map(Project::getOwnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, String> ownerNames = fetchOwnerNames(ownerIds);
        return projects.stream()
                .map(project -> toListResponse(
                        project,
                        statusNames.get(project.getStatusId()),
                        ownerNames.get(project.getOwnerId()),
                        latestDoneAssessments.get(project.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailsResponseDTO getProjectById(Integer id) {
        Project project = projectRepository.findByIdAndDeleteDateIsNull(id)
                .orElseThrow(() -> new NotFoundException("Проект не найден или удален"));

        String statusName = projectStatusEnumRepository.findById(project.getStatusId())
                .map(ProjectStatusEnum::getName)
                .orElse(null);
        Assessment latestDoneAssessment = assessmentStatusEnumRepository.findByName(STATUS_DONE)
                .flatMap(status -> assessmentRepository
                        .findFirstByProjectIdAndStatusIdAndDeleteDateIsNullOrderByCreatedDateDescUpdateDateDesc(
                                project.getId(), status.getId()))
                .orElse(null);
        String ownerName = null;
        if (project.getOwnerId() != null) {
            List<UserProfileShortDTO> users = userClient.findUserProfiles(List.of(project.getOwnerId()));
            if (users != null) {
                ownerName = users.stream()
                        .filter(user -> project.getOwnerId().equals(user.getId()))
                        .map(UserProfileShortDTO::getFullName)
                        .findFirst()
                        .orElse(null);
            }
        }

        return toDetailsResponse(project, statusName, ownerName, latestDoneAssessment);
    }

    private Map<Integer, Assessment> latestDoneAssessmentsByProjectId(List<Integer> projectIds) {
        Integer doneStatusId = assessmentStatusEnumRepository.findByName(STATUS_DONE)
                .map(AssessmentStatusEnum::getId)
                .orElse(null);
        if (doneStatusId == null || projectIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Assessment> latestByProjectId = new LinkedHashMap<>();
        assessmentRepository
                .findByProjectIdInAndStatusIdAndDeleteDateIsNullOrderByCreatedDateDescUpdateDateDesc(
                        projectIds, doneStatusId)
                .forEach(assessment -> latestByProjectId.putIfAbsent(assessment.getProjectId(), assessment));
        return latestByProjectId;
    }

    private Map<Integer, String> fetchOwnerNames(Set<Integer> ownerIds) {
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        List<UserProfileShortDTO> users = userClient.findUserProfiles(new ArrayList<>(ownerIds));
        if (users == null || users.isEmpty()) {
            return Map.of();
        }
        return users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(
                        UserProfileShortDTO::getId,
                        UserProfileShortDTO::getFullName,
                        (left, right) -> left));
    }

    private String toUniqueIdent(int id) {
        String digits = String.format("%06d", id);
        return String.format("PROJ.%s.%s.%s",
                digits.substring(0, 2),
                digits.substring(2, 4),
                digits.substring(4, 6));
    }

    private ProjectListResponseDTO toListResponse(Project project, String statusName,
                                                  String ownerName, Assessment latestDoneAssessment) {
        return ProjectListResponseDTO.builder()
                .id(project.getId())
                .uniqueIdent(project.getUniqueIdent())
                .name(project.getName())
                .docLink(project.getDocLink())
                .ownerId(project.getOwnerId())
                .ownerName(ownerName)
                .statusId(project.getStatusId())
                .statusName(statusName)
                .impactLevel(latestDoneAssessment != null ? latestDoneAssessment.getImpactLevel() : null)
                .rawText(latestDoneAssessment != null ? latestDoneAssessment.getRawText() : null)
                .taskDescription(latestDoneAssessment != null ? latestDoneAssessment.getTaskDescription() : null)
                .createdDate(project.getCreatedDate())
                .updatedDate(project.getUpdateDate())
                .build();
    }

    private ProjectDetailsResponseDTO toDetailsResponse(Project project, String statusName,
                                                        String ownerName, Assessment latestDoneAssessment) {
        return ProjectDetailsResponseDTO.builder()
                .id(project.getId())
                .uniqueIdent(project.getUniqueIdent())
                .name(project.getName())
                .source(project.getSource())
                .ownerId(project.getOwnerId())
                .ownerName(ownerName)
                .statusId(project.getStatusId())
                .statusName(statusName)
                .impactLevel(latestDoneAssessment != null ? latestDoneAssessment.getImpactLevel() : null)
                .rawText(latestDoneAssessment != null ? latestDoneAssessment.getRawText() : null)
                .taskDescription(latestDoneAssessment != null ? latestDoneAssessment.getTaskDescription() : null)
                .createdDate(project.getCreatedDate())
                .updatedDate(project.getUpdateDate())
                .build();
    }

    private ProjectResponseDTO toResponse(Project project) {
        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .source(project.getSource())
                .docLink(project.getDocLink())
                .createdDate(project.getCreatedDate())
                .uniqueIdent(project.getUniqueIdent())
                .statusId(project.getStatusId())
                .ownerId(project.getOwnerId())
                .build();
    }
}
