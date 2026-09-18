/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.projectbackend.client.UserClient;
import ru.beeline.projectbackend.domain.Assessment;
import ru.beeline.projectbackend.domain.AssessmentStatusEnum;
import ru.beeline.projectbackend.domain.AssessmentTc;
import ru.beeline.projectbackend.domain.AssessmentTcDesign;
import ru.beeline.projectbackend.domain.OpenQuestion;
import ru.beeline.projectbackend.domain.RequirementFunc;
import ru.beeline.projectbackend.domain.RequirementNonFunc;
import ru.beeline.projectbackend.dto.AssessmentDesignTcDTO;
import ru.beeline.projectbackend.dto.AssessmentDetailsResponseDTO;
import ru.beeline.projectbackend.dto.AssessmentOpenQuestionDTO;
import ru.beeline.projectbackend.dto.AssessmentReqFuncDTO;
import ru.beeline.projectbackend.dto.AssessmentReqNonFuncDTO;
import ru.beeline.projectbackend.dto.AssessmentResponseDTO;
import ru.beeline.projectbackend.dto.AssessmentTcItemDTO;
import ru.beeline.projectbackend.dto.CreateAssessmentRequestDTO;
import ru.beeline.projectbackend.dto.CreateAssessmentResponseDTO;
import ru.beeline.projectbackend.dto.UserProfileShortDTO;
import ru.beeline.projectbackend.exception.NotFoundException;
import ru.beeline.projectbackend.exception.ValidationException;
import ru.beeline.projectbackend.repository.AssessmentRepository;
import ru.beeline.projectbackend.repository.AssessmentStatusEnumRepository;
import ru.beeline.projectbackend.repository.AssessmentTcDesignRepository;
import ru.beeline.projectbackend.repository.AssessmentTcRepository;
import ru.beeline.projectbackend.repository.OpenQuestionRepository;
import ru.beeline.projectbackend.repository.ProjectRepository;
import ru.beeline.projectbackend.repository.RequirementFuncRepository;
import ru.beeline.projectbackend.repository.RequirementNonFuncRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private static final String STATUS_DONE = "Done";
    private static final String SOURCE_TEXT = "text";
    private static final String SOURCE_CONFLUENCE = "confluence";
    private static final Set<String> IMPACT_LEVELS = Set.of("S", "M", "L", "XL");

    private final AssessmentRepository assessmentRepository;
    private final AssessmentStatusEnumRepository assessmentStatusEnumRepository;
    private final RequirementFuncRepository requirementFuncRepository;
    private final RequirementNonFuncRepository requirementNonFuncRepository;
    private final AssessmentTcRepository assessmentTcRepository;
    private final AssessmentTcDesignRepository assessmentTcDesignRepository;
    private final OpenQuestionRepository openQuestionRepository;
    private final ProjectRepository projectRepository;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public List<AssessmentResponseDTO> getAssessments(Integer projectId, Integer statusId, Integer ownerId) {
        List<Assessment> assessments = assessmentRepository.findAllFiltered(projectId, ownerId, statusId);
        if (assessments.isEmpty()) {
            return List.of();
        }

        List<Integer> assessmentIds = assessments.stream().map(Assessment::getId).toList();
        Set<Integer> ownerIds = assessments.stream()
                .map(Assessment::getOwnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Integer> statusIds = assessments.stream()
                .map(Assessment::getStatusId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, String> ownerNames = fetchOwnerNames(ownerIds);
        Map<Integer, String> statusNames = assessmentStatusEnumRepository.findAllById(statusIds).stream()
                .collect(Collectors.toMap(AssessmentStatusEnum::getId, AssessmentStatusEnum::getName));

        Map<Integer, Long> reqFuncCounts = toCountMap(requirementFuncRepository.countGroupedByAssessmentId(assessmentIds));
        Map<Integer, Long> reqNonFuncCounts = toCountMap(requirementNonFuncRepository.countGroupedByAssessmentId(assessmentIds));
        Map<Integer, Long> tcCounts = toCountMap(assessmentTcRepository.countGroupedByAssessmentId(assessmentIds));
        Map<Integer, Long> tcDesignCounts = toCountMap(assessmentTcDesignRepository.countGroupedByAssessmentId(assessmentIds));
        Map<Integer, Long> oqCounts = toCountMap(openQuestionRepository.countGroupedByAssessmentId(assessmentIds));
        Map<Integer, Integer> productCounts = countUniqueProductAliases(
                assessmentTcRepository.findProductAliasesByAssessmentIds(assessmentIds),
                assessmentTcDesignRepository.findProductAliasesByAssessmentIds(assessmentIds));

        return assessments.stream()
                .map(assessment -> AssessmentResponseDTO.builder()
                        .id(assessment.getId())
                        .projectId(assessment.getProjectId())
                        .ownerId(assessment.getOwnerId())
                        .ownerName(ownerNames.get(assessment.getOwnerId()))
                        .statusId(assessment.getStatusId())
                        .statusName(statusNames.get(assessment.getStatusId()))
                        .impactLevel(assessment.getImpactLevel())
                        .taskDescription(assessment.getTaskDescription())
                        .reqFuncCount(toInt(reqFuncCounts.get(assessment.getId())))
                        .reqNonFuncCount(toInt(reqNonFuncCounts.get(assessment.getId())))
                        .tcCount(toInt(tcCounts.get(assessment.getId())) + toInt(tcDesignCounts.get(assessment.getId())))
                        .productCount(productCounts.getOrDefault(assessment.getId(), 0))
                        .oqCount(toInt(oqCounts.get(assessment.getId())))
                        .createdDate(assessment.getCreatedDate())
                        .updatedDate(assessment.getUpdateDate())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public AssessmentDetailsResponseDTO getAssessmentById(Integer id) {
        Assessment assessment = assessmentRepository.findByIdAndDeleteDateIsNull(id)
                .orElseThrow(() -> new NotFoundException("Оценка не найдена или удалена"));

        String ownerName = null;
        if (assessment.getOwnerId() != null) {
            ownerName = fetchOwnerNames(Set.of(assessment.getOwnerId())).get(assessment.getOwnerId());
        }

        String statusName = assessmentStatusEnumRepository.findById(assessment.getStatusId())
                .map(AssessmentStatusEnum::getName)
                .orElse(null);

        String projectName = projectRepository.findById(assessment.getProjectId())
                .map(project -> project.getName())
                .orElse(null);

        return AssessmentDetailsResponseDTO.builder()
                .id(assessment.getId())
                .projectId(assessment.getProjectId())
                .projectName(projectName)
                .ownerId(assessment.getOwnerId())
                .ownerName(ownerName)
                .statusId(assessment.getStatusId())
                .statusName(statusName)
                .source(assessment.getSource())
                .sourceUrl(assessment.getSourceUrl())
                .rawText(assessment.getRawText())
                .taskDescription(assessment.getTaskDescription())
                .impactLevel(assessment.getImpactLevel())
                .reqFunc(requirementFuncRepository.findByAssessmentId(id).stream()
                        .map(this::toReqFuncDto)
                        .toList())
                .reqNonFunc(requirementNonFuncRepository.findByAssessmentId(id).stream()
                        .map(this::toReqNonFuncDto)
                        .toList())
                .openQuestions(openQuestionRepository.findByAssessmentId(id).stream()
                        .map(this::toOpenQuestionDto)
                        .toList())
                .tc(assessmentTcRepository.findByAssessmentId(id).stream()
                        .map(this::toTcDto)
                        .toList())
                .designTc(assessmentTcDesignRepository.findByAssessmentId(id).stream()
                        .map(this::toDesignTcDto)
                        .toList())
                .createdDate(assessment.getCreatedDate())
                .updatedDate(assessment.getUpdateDate())
                .build();
    }

    @Transactional
    public CreateAssessmentResponseDTO createAssessment(CreateAssessmentRequestDTO request, Integer userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Заголовок user-id обязателен");
        }
        if (!SOURCE_TEXT.equals(request.getSource()) && !SOURCE_CONFLUENCE.equals(request.getSource())) {
            throw new ValidationException("source должен быть text или confluence");
        }
        if (SOURCE_CONFLUENCE.equals(request.getSource())
                && (request.getSourceUrl() == null || request.getSourceUrl().isBlank())) {
            throw new ValidationException("sourceURL обязателен при source = confluence");
        }
        if (request.getImpactLevel() == null || !IMPACT_LEVELS.contains(request.getImpactLevel())) {
            throw new ValidationException("impactLevel должен быть одним из: S, M, L, XL");
        }

        projectRepository.findByIdAndDeleteDateIsNull(request.getProjectId())
                .orElseThrow(() -> new NotFoundException("Проект не найден или удален"));

        Integer statusId = assessmentStatusEnumRepository.findByName(STATUS_DONE)
                .orElseThrow(() -> new NotFoundException("Статус оценки Done не найден"))
                .getId();

        LocalDateTime now = LocalDateTime.now();
        Assessment assessment = assessmentRepository.save(Assessment.builder()
                .projectId(request.getProjectId())
                .ownerId(userId)
                .statusId(statusId)
                .source(request.getSource())
                .sourceUrl(request.getSourceUrl())
                .rawText(request.getRawText())
                .taskDescription(request.getTaskDescription())
                .impactLevel(request.getImpactLevel())
                .createdDate(now)
                .updateDate(now)
                .build());

        saveRequirementsFunc(assessment.getId(), request.getRequirementsFunc());
        saveRequirementsNonFunc(assessment.getId(), request.getRequirementsNonFunc());
        saveOpenQuestions(assessment.getId(), request.getOpenQuestions());
        saveAssessmentTc(assessment.getId(), request.getTc());
        saveDesignTc(assessment.getId(), request.getDesignTc());

        return CreateAssessmentResponseDTO.builder()
                .id(assessment.getId())
                .projectId(assessment.getProjectId())
                .status(STATUS_DONE)
                .impactLevel(assessment.getImpactLevel())
                .createdDate(assessment.getCreatedDate())
                .build();
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

    private void saveRequirementsFunc(Integer assessmentId, List<CreateAssessmentRequestDTO.RequirementItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        requirementFuncRepository.saveAll(items.stream()
                .map(item -> RequirementFunc.builder()
                        .assessmentId(assessmentId)
                        .uniqueIdent(item.getUniqueIdent())
                        .title(item.getTitle())
                        .description(item.getDescription())
                        .build())
                .toList());
    }

    private void saveRequirementsNonFunc(Integer assessmentId, List<CreateAssessmentRequestDTO.RequirementItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        requirementNonFuncRepository.saveAll(items.stream()
                .map(item -> RequirementNonFunc.builder()
                        .assessmentId(assessmentId)
                        .uniqueIdent(item.getUniqueIdent())
                        .title(item.getTitle())
                        .description(item.getDescription())
                        .build())
                .toList());
    }

    private void saveOpenQuestions(Integer assessmentId, List<CreateAssessmentRequestDTO.OpenQuestionItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        openQuestionRepository.saveAll(items.stream()
                .map(item -> OpenQuestion.builder()
                        .assessmentId(assessmentId)
                        .uniqueIdent(item.getUniqueIdent())
                        .questionText(item.getQuestionText())
                        .build())
                .toList());
    }

    private void saveAssessmentTc(Integer assessmentId, List<CreateAssessmentRequestDTO.TcItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<String, AssessmentTc> byCode = new LinkedHashMap<>();
        List<AssessmentTc> entities = new ArrayList<>();
        for (CreateAssessmentRequestDTO.TcItem item : items) {
            AssessmentTc existing = item.getTcCode() == null ? null : byCode.get(item.getTcCode());
            if (existing != null) {
                mergeFrIds(existing, item.getFrIds());
                continue;
            }
            AssessmentTc entity = AssessmentTc.builder()
                    .assessmentId(assessmentId)
                    .tcCode(item.getTcCode())
                    .productAlias(item.getProductAlias())
                    .productName(item.getProductName())
                    .parentBcCode(item.getParentBcCode())
                    .frIds(item.getFrIds() == null ? null : new ArrayList<>(item.getFrIds()))
                    .build();
            if (item.getTcCode() != null) {
                byCode.put(item.getTcCode(), entity);
            }
            entities.add(entity);
        }
        assessmentTcRepository.saveAll(entities);
    }

    private void mergeFrIds(AssessmentTc target, List<String> frIds) {
        if (frIds == null || frIds.isEmpty()) {
            return;
        }
        if (target.getFrIds() == null) {
            target.setFrIds(new ArrayList<>());
        }
        frIds.stream()
                .filter(frId -> !target.getFrIds().contains(frId))
                .forEach(target.getFrIds()::add);
    }

    private void saveDesignTc(Integer assessmentId, List<CreateAssessmentRequestDTO.DesignTcItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        assessmentTcDesignRepository.saveAll(items.stream()
                .map(item -> AssessmentTcDesign.builder()
                        .assessmentId(assessmentId)
                        .name(item.getName())
                        .description(item.getDescription())
                        .productAlias(item.getProductAlias())
                        .productName(item.getProductName())
                        .parentBcCode(item.getParentBcCode())
                        .frIds(item.getFrIds())
                        .build())
                .toList());
    }

    private Map<Integer, Long> toCountMap(List<Object[]> rows) {
        Map<Integer, Long> counts = new HashMap<>();
        if (rows == null) {
            return counts;
        }
        for (Object[] row : rows) {
            counts.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    private Map<Integer, Integer> countUniqueProductAliases(List<Object[]> tcAliases, List<Object[]> designAliases) {
        Map<Integer, Set<String>> aliasesByAssessment = new HashMap<>();
        collectProductAliases(aliasesByAssessment, tcAliases);
        collectProductAliases(aliasesByAssessment, designAliases);
        Map<Integer, Integer> counts = new HashMap<>();
        aliasesByAssessment.forEach((assessmentId, aliases) -> counts.put(assessmentId, aliases.size()));
        return counts;
    }

    private void collectProductAliases(Map<Integer, Set<String>> aliasesByAssessment, List<Object[]> rows) {
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            Integer assessmentId = ((Number) row[0]).intValue();
            String alias = (String) row[1];
            if (alias == null || alias.isBlank()) {
                continue;
            }
            aliasesByAssessment.computeIfAbsent(assessmentId, ignored -> new HashSet<>()).add(alias);
        }
    }

    private int toInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private AssessmentReqFuncDTO toReqFuncDto(RequirementFunc requirement) {
        return AssessmentReqFuncDTO.builder()
                .id(requirement.getId())
                .uniqueIdent(requirement.getUniqueIdent())
                .title(requirement.getTitle())
                .description(requirement.getDescription())
                .build();
    }

    private AssessmentReqNonFuncDTO toReqNonFuncDto(RequirementNonFunc requirement) {
        return AssessmentReqNonFuncDTO.builder()
                .id(requirement.getId())
                .uniqueIdent(requirement.getUniqueIdent())
                .title(requirement.getTitle())
                .description(requirement.getDescription())
                .build();
    }

    private AssessmentOpenQuestionDTO toOpenQuestionDto(OpenQuestion question) {
        return AssessmentOpenQuestionDTO.builder()
                .id(question.getId())
                .uniqueIdent(question.getUniqueIdent())
                .questionText(question.getQuestionText())
                .build();
    }

    private AssessmentTcItemDTO toTcDto(AssessmentTc assessmentTc) {
        return AssessmentTcItemDTO.builder()
                .id(assessmentTc.getId())
                .tcCode(assessmentTc.getTcCode())
                .productAlias(assessmentTc.getProductAlias())
                .productName(assessmentTc.getProductName())
                .parentBcCode(assessmentTc.getParentBcCode())
                .frIds(assessmentTc.getFrIds())
                .build();
    }

    private AssessmentDesignTcDTO toDesignTcDto(AssessmentTcDesign design) {
        return AssessmentDesignTcDTO.builder()
                .id(design.getId())
                .name(design.getName())
                .description(design.getDescription())
                .productAlias(design.getProductAlias())
                .productName(design.getProductName())
                .parentBcCode(design.getParentBcCode())
                .frIds(design.getFrIds())
                .build();
    }
}
