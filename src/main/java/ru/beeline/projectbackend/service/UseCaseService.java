/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.projectbackend.client.ProductClient;
import ru.beeline.projectbackend.domain.ArtifactBranch;
import ru.beeline.projectbackend.domain.Project;
import ru.beeline.projectbackend.domain.UsOperationRelation;
import ru.beeline.projectbackend.domain.UseCase;
import ru.beeline.projectbackend.dto.product.*;
import ru.beeline.projectbackend.dto.usecase.*;
import ru.beeline.projectbackend.exception.ExternalServiceException;
import ru.beeline.projectbackend.exception.NotFoundException;
import ru.beeline.projectbackend.exception.ValidationException;
import ru.beeline.projectbackend.repository.*;
import ru.beeline.projectbackend.utils.InterfaceCodeGenerator;

import java.time.LocalDateTime;
import java.util.*;

import static ru.beeline.projectbackend.utils.Constant.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UseCaseService {

    private static final String DEFAULT_BRANCH = "main";
    private static final String ARTIFACT_TYPE_USE_CASE = "use_case";
    private static final String UNKNOWN_GUID_MESSAGE = "useCaseCall ссылается на неизвестный guid reqOperation";
    private static final String EMPTY_BRANCH_MESSAGE = "Имя ветки не может быть пустым";
    private static final String PROJECT_NOT_FOUND_OR_DELETED = "Проект не найден или удален";
    private static final String USE_CASE_NOT_FOUND = "Use case не найден";
    private static final String USE_CASE_BRANCH_NOT_FOUND = "Ветка use case не найдена";

    private final ProjectRepository projectRepository;
    private final ArtifactBranchRepository artifactBranchRepository;
    private final UseCaseRepository useCaseRepository;
    private final UsOperationRelationRepository usOperationRelationRepository;
    private final ProductClient productClient;

    @Transactional
    public UseCaseUpsertResponseDTO upsert(Integer projectId, String branch, UseCaseUpsertRequestDTO request) {
        String branchName = resolveBranchName(branch);
        log.info("UseCase upsert: обработка, projectId={}, branch={}, useCase.uid={}", projectId, branchName,
                request != null && request.getUseCase() != null ? request.getUseCase().getUid() : null);
        validateRequest(request);
        Project project = projectRepository.findByIdAndDeleteDateIsNull(projectId)
                .orElseThrow(() -> new NotFoundException("Проекта с таким ID не существует"));
        Map<String, ReqOperationDTO> operationsByGuid = indexReqOperations(request.getReqOperations());
        List<UseCaseCallDTO> calls = request.getUseCaseCall();
        validateCalls(calls, operationsByGuid);
        UseCase useCase = upsertCard(project.getId(), request.getUseCase());
        ArtifactBranch useCaseBranch = findOrCreateUseCaseBranch(useCase.getId(), branchName);
        List<DiscoveredInterfaceUpsertDTO> payload = buildDiscoveredInterfaces(operationsByGuid.values());
        List<DiscoveredInterfaceResultDTO> published =
                productClient.upsertUseCaseOperations(useCase.getId(), payload);
        Map<String, Integer> operationIdByGuid = mapOperationIds(operationsByGuid, published);
        int removed = usOperationRelationRepository.deleteAllByUseCaseBranchId(useCaseBranch.getId());
        if (removed > 0) {
            log.info("UseCase upsert: удалён прежний состав, useCaseBranchId={}, шагов={}",
                    useCaseBranch.getId(), removed);
        }
        saveRelations(useCaseBranch.getId(), calls, operationIdByGuid);
        log.info("UseCase upsert: завершён, id={}, code={}, projectId={}, useCaseBranchId={}, branch={}, шагов={}",
                useCase.getId(), useCase.getCode(), useCase.getProjectId(), useCaseBranch.getId(), branchName,
                calls.size());
        return UseCaseUpsertResponseDTO.builder()
                .id(useCase.getId())
                .code(useCase.getCode())
                .projectId(useCase.getProjectId())
                .useCaseBranchId(useCaseBranch.getId())
                .branch(branchName)
                .build();
    }

    @Transactional(readOnly = true)
    public List<OperationUseCasesResponseDTO> findUseCasesByOperations(JsonNode request) {
        List<Integer> operationIds = validateAndDedupe(request);
        log.info("UseCase by operations: обработка, методов={}", operationIds.size());

        List<UseCaseByOperationProjection> rows =
                usOperationRelationRepository.findUseCasesByOperationIds(operationIds);

        Set<Integer> requested = new HashSet<>(operationIds);
        Map<Integer, NavigableSet<Integer>> ucIdsByOperation = new LinkedHashMap<>();
        Map<Integer, String> nameByUcId = new HashMap<>();
        for (UseCaseByOperationProjection row : rows) {
            if (row.getUcId() == null) {
                continue;
            }
            nameByUcId.put(row.getUcId(), row.getUcName());
            collectUseCase(ucIdsByOperation, requested, row.getReqOperationId(), row.getUcId());
            collectUseCase(ucIdsByOperation, requested, row.getRelatedReqOperationId(), row.getUcId());
        }

        List<OperationUseCasesResponseDTO> response = new ArrayList<>(operationIds.size());
        for (Integer operationId : operationIds) {
            NavigableSet<Integer> ucIds = ucIdsByOperation.get(operationId);
            List<OperationUseCaseDTO> useCases = ucIds == null ? List.of() : ucIds.stream()
                    .map(ucId -> OperationUseCaseDTO.builder()
                            .id(ucId)
                            .name(nameByUcId.get(ucId))
                            .build())
                    .toList();
            response.add(OperationUseCasesResponseDTO.builder()
                    .operationId(operationId)
                    .useCases(useCases)
                    .build());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public UseCaseCardResponseDTO getCard(Integer useCaseId) {
        log.info("UseCase card: обработка, useCaseId={}", useCaseId);
        UseCase useCase = useCaseRepository.findById(useCaseId)
                .orElseThrow(() -> new NotFoundException(USE_CASE_NOT_FOUND));
        List<UseCaseBranchDTO> branches = artifactBranchRepository
                .findByArtifactTypeAndArtifactId(ARTIFACT_TYPE_USE_CASE, useCase.getId())
                .stream()
                .sorted(Comparator.comparing(ArtifactBranch::getName, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ArtifactBranch::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(branch -> UseCaseBranchDTO.builder()
                        .id(branch.getId())
                        .name(branch.getName())
                        .build())
                .toList();
        log.info("UseCase card: завершён, useCaseId={}, веток={}", useCase.getId(), branches.size());
        return UseCaseCardResponseDTO.builder()
                .id(useCase.getId())
                .projectId(useCase.getProjectId())
                .code(useCase.getCode())
                .name(useCase.getName())
                .description(useCase.getDescription())
                .biStepCode(trimToNull(useCase.getBiStepCode()))
                .createdDate(useCase.getCreatedDate())
                .updatedDate(useCase.getUpdatedDate())
                .deletedDate(useCase.getDeletedDate())
                .branches(branches)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProjectUseCaseResponseDTO> getLiveUseCases(Integer projectId) {
        log.info("UseCase list: обработка, projectId={}", projectId);
        Project project = projectRepository.findByIdAndDeleteDateIsNull(projectId)
                .orElseThrow(() -> new NotFoundException(PROJECT_NOT_FOUND_OR_DELETED));
        List<ProjectUseCaseResponseDTO> result = useCaseRepository
                .findByProjectIdAndDeletedDateIsNullOrderByIdAsc(project.getId())
                .stream()
                .filter(useCase -> project.getId().equals(useCase.getProjectId()))
                .filter(useCase -> useCase.getDeletedDate() == null)
                .sorted(Comparator.comparing(UseCase::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(useCase -> ProjectUseCaseResponseDTO.builder()
                        .id(useCase.getId())
                        .code(useCase.getCode())
                        .name(useCase.getName())
                        .description(useCase.getDescription())
                        .projectId(useCase.getProjectId())
                        .createdDate(useCase.getCreatedDate())
                        .updatedDate(useCase.getUpdatedDate())
                        .build())
                .toList();
        log.info("UseCase list: завершён, projectId={}, карточек={}", project.getId(), result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<UseCaseOperationNodeDTO> getOperationsTree(Integer projectId, Integer useCaseId, String branch) {
        String branchName = resolveBranchName(branch);
        log.info("UseCase operations tree: обработка, projectId={}, useCaseId={}, branch={}",
                projectId, useCaseId, branchName);
        projectRepository.findByIdAndDeleteDateIsNull(projectId)
                .orElseThrow(() -> new NotFoundException(PROJECT_NOT_FOUND_OR_DELETED));
        UseCase useCase = useCaseRepository.findByIdAndProjectIdAndDeletedDateIsNull(useCaseId, projectId)
                .orElseThrow(() -> new NotFoundException(USE_CASE_NOT_FOUND));
        ArtifactBranch useCaseBranch = artifactBranchRepository
                .findByArtifactTypeAndArtifactIdAndNameIgnoreCase(ARTIFACT_TYPE_USE_CASE, useCase.getId(), branchName)
                .orElseThrow(() -> new NotFoundException(USE_CASE_BRANCH_NOT_FOUND));
        List<UsOperationRelation> steps = usOperationRelationRepository.findAllByUseCaseBranchId(useCaseBranch.getId());
        if (steps.isEmpty()) {
            log.info("UseCase operations tree: шагов нет, useCaseBranchId={}", useCaseBranch.getId());
            return List.of();
        }
        Comparator<UsOperationRelation> byOrderThenId = Comparator
                .comparing(UsOperationRelation::getOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(UsOperationRelation::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        List<UsOperationRelation> rootSteps = new ArrayList<>();
        Map<Integer, List<UsOperationRelation>> stepsByCaller = new HashMap<>();
        for (UsOperationRelation step : steps) {
            if (step.getReqOperationId() == null) {
                rootSteps.add(step);
            } else {
                stepsByCaller.computeIfAbsent(step.getReqOperationId(), ignored -> new ArrayList<>()).add(step);
            }
        }
        rootSteps.sort(byOrderThenId);
        stepsByCaller.values().forEach(children -> children.sort(byOrderThenId));
        List<UseCaseOperationNodeDTO> roots = new ArrayList<>(rootSteps.size());
        Set<Integer> path = new HashSet<>();
        for (UsOperationRelation rootStep : rootSteps) {
            roots.add(buildNode(rootStep, stepsByCaller, path));
        }
        Set<Integer> operationIds = new LinkedHashSet<>();
        collectOperationIds(roots, operationIds);
        if (operationIds.isEmpty()) {
            return roots;
        }
        List<DiscoveredOperationByIdsDTO> operations =
                productClient.getDiscoveredOperationsByIds(new ArrayList<>(operationIds));
        Map<Integer, DiscoveredOperationByIdsDTO> operationById = new HashMap<>();
        for (DiscoveredOperationByIdsDTO operation : operations) {
            if (operation != null && operation.getId() != null) {
                operationById.putIfAbsent(operation.getId(), operation);
            }
        }
        fillDiscoveredOperations(roots, operationById);
        log.info("UseCase operations tree: завершён, useCaseBranchId={}, шагов={}, корней={}, методов={}",
                useCaseBranch.getId(), steps.size(), roots.size(), operationIds.size());
        return roots;
    }

    private UseCaseOperationNodeDTO buildNode(UsOperationRelation step,
                                              Map<Integer, List<UsOperationRelation>> stepsByCaller,
                                              Set<Integer> path) {
        Integer operationId = step.getRelatedReqOperationId();
        UseCaseOperationNodeDTO node = UseCaseOperationNodeDTO.builder()
                .order(step.getOrder())
                .discoveredOperationId(operationId)
                .build();
        if (operationId == null || !path.add(operationId)) {
            return node;
        }
        for (UsOperationRelation child : stepsByCaller.getOrDefault(operationId, List.of())) {
            node.getChildren().add(buildNode(child, stepsByCaller, path));
        }
        path.remove(operationId);
        return node;
    }

    private void collectOperationIds(List<UseCaseOperationNodeDTO> nodes, Set<Integer> operationIds) {
        for (UseCaseOperationNodeDTO node : nodes) {
            if (node.getDiscoveredOperationId() != null) {
                operationIds.add(node.getDiscoveredOperationId());
            }
            collectOperationIds(node.getChildren(), operationIds);
        }
    }

    private void fillDiscoveredOperations(List<UseCaseOperationNodeDTO> nodes,
                                          Map<Integer, DiscoveredOperationByIdsDTO> operationById) {
        for (UseCaseOperationNodeDTO node : nodes) {
            node.setDiscoveredOperation(operationById.get(node.getDiscoveredOperationId()));
            fillDiscoveredOperations(node.getChildren(), operationById);
        }
    }

    private void collectUseCase(Map<Integer, NavigableSet<Integer>> ucIdsByOperation, Set<Integer> requested,
                                Integer operationId, Integer ucId) {
        if (operationId != null && requested.contains(operationId)) {
            ucIdsByOperation.computeIfAbsent(operationId, ignored -> new TreeSet<>()).add(ucId);
        }
    }

    private List<Integer> validateAndDedupe(JsonNode request) {
        if (request == null || request.isNull() || request.isMissingNode()) {
            throw new ValidationException(MISSING_REQUEST_BODY);
        }
        if (!request.isArray()) {
            throw new ValidationException(MISSING_REQUEST_BODY);
        }
        if (request.isEmpty()) {
            throw new ValidationException(EMPTY_OPERATION_IDS);
        }
        List<Integer> operationIds = new ArrayList<>(request.size());
        Set<Integer> unique = new LinkedHashSet<>();
        for (JsonNode element : request) {
            if (element == null || element.isNull()) {
                throw new ValidationException(BLANK_OPERATION_ID);
            }
            if (!element.isIntegralNumber() || !element.canConvertToInt()) {
                throw new ValidationException(BLANK_OPERATION_ID);
            }
            Integer operationId = element.intValue();
            if (unique.add(operationId)) {
                operationIds.add(operationId);
            }
        }
        return operationIds;
    }

    private String resolveBranchName(String branch) {
        if (branch == null) {
            return DEFAULT_BRANCH;
        }
        String trimmed = branch.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(EMPTY_BRANCH_MESSAGE);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private ArtifactBranch findOrCreateUseCaseBranch(Integer useCaseId, String branchName) {
        return artifactBranchRepository
                .findByArtifactTypeAndArtifactIdAndNameIgnoreCase(ARTIFACT_TYPE_USE_CASE, useCaseId, branchName)
                .orElseGet(() -> artifactBranchRepository.save(ArtifactBranch.builder()
                        .artifactType(ARTIFACT_TYPE_USE_CASE)
                        .artifactId(useCaseId)
                        .name(branchName)
                        .build()));
    }

    private void validateRequest(UseCaseUpsertRequestDTO request) {
        if (request == null) {
            throw new ValidationException("Отсутствует тело запроса");
        }
        if (request.getUseCase() == null) {
            throw new ValidationException("Отсутствует обязательное поле useCase");
        }
        requireNonBlank(request.getUseCase().getUid(), "useCase.uid");
        requireNonBlank(request.getUseCase().getName(), "useCase.name");
        if (request.getUseCaseCall() == null) {
            throw new ValidationException("Отсутствует обязательное поле useCaseCall");
        }
        if (request.getReqOperations() == null || request.getReqOperations().isEmpty()) {
            throw new ValidationException("Отсутствует обязательное непустое поле reqOperations");
        }
    }

    private Map<String, ReqOperationDTO> indexReqOperations(List<ReqOperationDTO> reqOperations) {
        Map<String, ReqOperationDTO> byGuid = new LinkedHashMap<>();
        for (ReqOperationDTO reqOperation : reqOperations) {
            if (reqOperation == null) {
                throw new ValidationException("Пустой элемент в reqOperations");
            }
            requireNonBlank(reqOperation.getGuid(), "reqOperations.guid");
            requireNonBlank(reqOperation.getName(), "reqOperations.name");
            requireNonBlank(reqOperation.getType(), "reqOperations.type");
            String guid = reqOperation.getGuid().trim();
            if (byGuid.put(guid, reqOperation) != null) {
                throw new ValidationException("Дубликат guid в reqOperations: " + guid);
            }
        }
        return byGuid;
    }

    private void validateCalls(List<UseCaseCallDTO> calls, Map<String, ReqOperationDTO> operationsByGuid) {
        for (UseCaseCallDTO call : calls) {
            if (call == null) {
                throw new ValidationException("Пустой элемент в useCaseCall");
            }
            requireNonBlank(call.getRelatedOperationGuid(), "useCaseCall.relatedOperationGuid");
            if (call.getOrder() == null) {
                throw new ValidationException("Отсутствует обязательное поле useCaseCall.order");
            }
            if (!operationsByGuid.containsKey(call.getRelatedOperationGuid().trim())) {
                throw new ValidationException(UNKNOWN_GUID_MESSAGE);
            }
            String operationGuid = trimToNull(call.getOperationGuid());
            if (operationGuid != null && !operationsByGuid.containsKey(operationGuid)) {
                throw new ValidationException(UNKNOWN_GUID_MESSAGE);
            }
        }
    }

    private UseCase upsertCard(Integer projectId, UseCaseInfoDTO info) {
        String code = info.getUid().trim();
        UseCase useCase = useCaseRepository.findByProjectIdAndCodeIgnoreCase(projectId, code)
                .orElseGet(() -> UseCase.builder()
                        .projectId(projectId)
                        .build());
        useCase.setProjectId(projectId);
        useCase.setCode(code);
        useCase.setName(info.getName().trim());
        useCase.setDescription(info.getDescription());
        useCase.setBiStepCode(trimToNull(info.getBiStepCode()));
        if (useCase.getId() == null) {
            useCase.setCreatedDate(LocalDateTime.now());
        } else {
            useCase.setUpdatedDate(LocalDateTime.now());
            useCase.setDeletedDate(null);
        }
        return useCaseRepository.save(useCase);
    }

    private List<DiscoveredInterfaceUpsertDTO> buildDiscoveredInterfaces(Collection<ReqOperationDTO> reqOperations) {
        Map<String, DiscoveredInterfaceUpsertDTO> grouped = new LinkedHashMap<>();
        List<DiscoveredInterfaceUpsertDTO> standalone = new ArrayList<>();

        for (ReqOperationDTO reqOperation : reqOperations) {
            String interfaceCode = trimToNull(reqOperation.getInterfaceCode());
            DiscoveredOperationUpsertDTO operation = toOperation(reqOperation);
            String productPlanned = trimToNull(reqOperation.getProductPlanned());
            if (interfaceCode == null) {
                DiscoveredInterfaceUpsertDTO.DiscoveredInterfaceUpsertDTOBuilder builder =
                        DiscoveredInterfaceUpsertDTO.builder()
                                .interfaceCode(InterfaceCodeGenerator.crc32(
                                        reqOperation.getName(),
                                        reqOperation.getType(),
                                        reqOperation.getDescriptionTc(),
                                        reqOperation.getTcCode(),
                                        reqOperation.getDescriptionOperation()))
                                .operations(new ArrayList<>(List.of(operation)));

                String product = reqOperation.getProduct();
                if (product != null && !product.trim().isEmpty()) {
                    builder.product(product.trim());
                }

                if (productPlanned != null) {
                    builder.productPlanned(productPlanned);
                }

                standalone.add(builder.build());
                continue;
            }
            String product = reqOperation.getProduct().trim();
            String key = interfaceCode.toLowerCase(Locale.ROOT) + " " + product.toLowerCase(Locale.ROOT);
            DiscoveredInterfaceUpsertDTO group = grouped.get(key);
            if (group == null) {
                group = DiscoveredInterfaceUpsertDTO.builder()
                        .interfaceCode(interfaceCode)
                        .product(product)
                        .productPlanned(productPlanned)
                        .operations(new ArrayList<>())
                        .build();
                grouped.put(key, group);
            } else if (!normalizeKey(group.getProductPlanned()).equals(normalizeKey(productPlanned))) {
                throw new ValidationException("productPlanned должен совпадать для интерфейса "
                        + interfaceCode + " и продукта " + product);
            }
            group.getOperations().add(operation);
        }

        List<DiscoveredInterfaceUpsertDTO> payload = new ArrayList<>(grouped.values());
        payload.addAll(standalone);
        return payload;
    }

    private DiscoveredOperationUpsertDTO toOperation(ReqOperationDTO reqOperation) {
        return DiscoveredOperationUpsertDTO.builder()
                .name(reqOperation.getName().trim())
                .type(reqOperation.getType().trim())
                .descriptionTc(reqOperation.getDescriptionTc())
                .tcCode(reqOperation.getTcCode())
                .descriptionOperation(reqOperation.getDescriptionOperation())
                .isPlanned(reqOperation.getIsPlanned())
                .connectionOperationId(reqOperation.getConnectionOperationId())
                .build();
    }

    private Map<String, Integer> mapOperationIds(Map<String, ReqOperationDTO> operationsByGuid,
                                                 List<DiscoveredInterfaceResultDTO> published) {
        Map<String, Integer> idByKey = new LinkedHashMap<>();
        for (DiscoveredInterfaceResultDTO iface : published) {
            if (iface == null || iface.getOperations() == null) {
                continue;
            }
            for (DiscoveredOperationResultDTO operation : iface.getOperations()) {
                if (operation == null || operation.getDiscoveredOperationId() == null) {
                    continue;
                }
                idByKey.put(operationKey(iface.getInterfaceCode(), iface.getProduct(),
                        operation.getName(), operation.getType()), operation.getDiscoveredOperationId());
            }
        }

        Map<String, Integer> idByGuid = new LinkedHashMap<>();
        for (Map.Entry<String, ReqOperationDTO> entry : operationsByGuid.entrySet()) {
            ReqOperationDTO reqOperation = entry.getValue();
            String interfaceCode = trimToNull(reqOperation.getInterfaceCode());
            if (interfaceCode == null) {
                interfaceCode = InterfaceCodeGenerator.crc32(
                        reqOperation.getName(),
                        reqOperation.getType(),
                        reqOperation.getDescriptionTc(),
                        reqOperation.getTcCode(),
                        reqOperation.getDescriptionOperation());
            }
            String key = operationKey(interfaceCode, reqOperation.getProduct(),
                    reqOperation.getName(), reqOperation.getType());
            Integer operationId = idByKey.get(key);
            if (operationId == null) {
                throw new ExternalServiceException(HttpStatus.BAD_GATEWAY,
                        "Сервис продуктов не вернул идентификатор операции для guid " + entry.getKey());
            }
            idByGuid.put(entry.getKey(), operationId);
        }
        return idByGuid;
    }

    private String operationKey(String interfaceCode, String product, String name, String type) {
        return String.join("\0",
                normalizeKey(interfaceCode), normalizeKey(product), normalizeKey(name), normalizeKey(type));
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void saveRelations(Integer useCaseBranchId, List<UseCaseCallDTO> calls,
                               Map<String, Integer> operationIdByGuid) {
        List<UsOperationRelation> relations = new ArrayList<>();
        for (UseCaseCallDTO call : calls) {
            String operationGuid = trimToNull(call.getOperationGuid());
            relations.add(UsOperationRelation.builder()
                    .useCaseBranchId(useCaseBranchId)
                    .order(call.getOrder())
                    .reqOperationId(operationGuid == null ? null : operationIdByGuid.get(operationGuid))
                    .relatedReqOperationId(operationIdByGuid.get(call.getRelatedOperationGuid().trim()))
                    .build());
        }
        usOperationRelationRepository.saveAll(relations);
    }

    private void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Отсутствует обязательное поле " + field);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
