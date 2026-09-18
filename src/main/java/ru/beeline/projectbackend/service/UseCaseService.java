/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.service;

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
import ru.beeline.projectbackend.dto.product.DiscoveredInterfaceResultDTO;
import ru.beeline.projectbackend.dto.product.DiscoveredInterfaceUpsertDTO;
import ru.beeline.projectbackend.dto.product.DiscoveredOperationResultDTO;
import ru.beeline.projectbackend.dto.product.DiscoveredOperationUpsertDTO;
import ru.beeline.projectbackend.dto.usecase.ReqOperationDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseCallDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseInfoDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertRequestDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertResponseDTO;
import ru.beeline.projectbackend.exception.ExternalServiceException;
import ru.beeline.projectbackend.exception.NotFoundException;
import ru.beeline.projectbackend.exception.ValidationException;
import ru.beeline.projectbackend.repository.ArtifactBranchRepository;
import ru.beeline.projectbackend.repository.ProjectRepository;
import ru.beeline.projectbackend.repository.UsOperationRelationRepository;
import ru.beeline.projectbackend.repository.UseCaseRepository;
import ru.beeline.projectbackend.utils.InterfaceCodeGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UseCaseService {

    private static final String DEFAULT_BRANCH = "main";
    private static final String UNKNOWN_GUID_MESSAGE = "useCaseCall ссылается на неизвестный guid reqOperation";

    private final ProjectRepository projectRepository;
    private final ArtifactBranchRepository artifactBranchRepository;
    private final UseCaseRepository useCaseRepository;
    private final UsOperationRelationRepository usOperationRelationRepository;
    private final ProductClient productClient;

    @Transactional
    public UseCaseUpsertResponseDTO upsert(Integer projectId, String branch, UseCaseUpsertRequestDTO request) {
        String branchName = normalizeBranch(branch);
        log.info("UseCase upsert: обработка, projectId={}, branch={}, useCase.uid={}", projectId, branchName,
                request != null && request.getUseCase() != null ? request.getUseCase().getUid() : null);

        validateRequest(request);
        ArtifactBranch projectBranch = resolveBranch(projectId, branchName);

        Map<String, ReqOperationDTO> operationsByGuid = indexReqOperations(request.getReqOperations());
        List<UseCaseCallDTO> calls = request.getUseCaseCall();
        validateCalls(calls, operationsByGuid);

        UseCase useCase = upsertCard(projectBranch.getId(), request.getUseCase());
        int removed = usOperationRelationRepository.deleteAllByUcId(useCase.getId());
        if (removed > 0) {
            log.info("UseCase upsert: удалён прежний состав, ucId={}, шагов={}", useCase.getId(), removed);
        }

        List<DiscoveredInterfaceUpsertDTO> payload = buildDiscoveredInterfaces(operationsByGuid.values());
        List<DiscoveredInterfaceResultDTO> published =
                productClient.upsertProjectDiscoveredInterfaces(projectId, payload);
        Map<String, Integer> operationIdByGuid = mapOperationIds(operationsByGuid, published);

        saveRelations(useCase.getId(), calls, operationIdByGuid);

        log.info("UseCase upsert: завершён, id={}, code={}, projectBranchId={}, шагов={}",
                useCase.getId(), useCase.getCode(), useCase.getProjectBranchId(), calls.size());
        return UseCaseUpsertResponseDTO.builder()
                .id(useCase.getId())
                .code(useCase.getCode())
                .projectBranchId(useCase.getProjectBranchId())
                .build();
    }

    private String normalizeBranch(String branch) {
        return (branch == null || branch.isBlank()) ? DEFAULT_BRANCH : branch.trim().toLowerCase(Locale.ROOT);
    }

    private ArtifactBranch resolveBranch(Integer projectId, String branchName) {
        Project project = projectRepository.findByIdAndDeleteDateIsNull(projectId)
                .orElseThrow(() -> new NotFoundException("Проекта с таким ID не существует"));
        return artifactBranchRepository
                .findByArtifactTypeAndArtifactIdAndNameIgnoreCase(ArtifactBranch.TYPE_PROJECT, project.getId(),
                        branchName)
                .orElseThrow(() -> new NotFoundException("Ветка проекта не найдена: " + branchName));
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
            requireNonBlank(reqOperation.getProduct(), "reqOperations.product");
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

    private UseCase upsertCard(Integer projectBranchId, UseCaseInfoDTO info) {
        String code = info.getUid().trim();
        UseCase useCase = useCaseRepository.findByProjectBranchIdAndCodeIgnoreCase(projectBranchId, code)
                .orElseGet(() -> UseCase.builder()
                        .projectBranchId(projectBranchId)
                        .build());
        useCase.setCode(code);
        useCase.setName(info.getName().trim());
        useCase.setDescription(info.getDescription());
        return useCaseRepository.save(useCase);
    }

    private List<DiscoveredInterfaceUpsertDTO> buildDiscoveredInterfaces(Collection<ReqOperationDTO> reqOperations) {
        Map<String, DiscoveredInterfaceUpsertDTO> grouped = new LinkedHashMap<>();
        List<DiscoveredInterfaceUpsertDTO> standalone = new ArrayList<>();

        for (ReqOperationDTO reqOperation : reqOperations) {
            String interfaceCode = trimToNull(reqOperation.getInterfaceCode());
            DiscoveredOperationUpsertDTO operation = toOperation(reqOperation);
            if (interfaceCode == null) {
                standalone.add(DiscoveredInterfaceUpsertDTO.builder()
                        .interfaceCode(InterfaceCodeGenerator.crc32(
                                reqOperation.getName(),
                                reqOperation.getType(),
                                reqOperation.getDescriptionTc(),
                                reqOperation.getTcCode(),
                                reqOperation.getDescriptionOperation()))
                        .product(reqOperation.getProduct().trim())
                        .operations(new ArrayList<>(List.of(operation)))
                        .build());
                continue;
            }
            String product = reqOperation.getProduct().trim();
            String key = interfaceCode.toLowerCase(Locale.ROOT) + " " + product.toLowerCase(Locale.ROOT);
            grouped.computeIfAbsent(key, ignored -> DiscoveredInterfaceUpsertDTO.builder()
                            .interfaceCode(interfaceCode)
                            .product(product)
                            .operations(new ArrayList<>())
                            .build())
                    .getOperations().add(operation);
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
        return String.join(" ",
                normalizeKey(interfaceCode), normalizeKey(product), normalizeKey(name), normalizeKey(type));
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void saveRelations(Integer ucId, List<UseCaseCallDTO> calls, Map<String, Integer> operationIdByGuid) {
        List<UsOperationRelation> relations = new ArrayList<>();
        for (UseCaseCallDTO call : calls) {
            String operationGuid = trimToNull(call.getOperationGuid());
            relations.add(UsOperationRelation.builder()
                    .ucId(ucId)
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
