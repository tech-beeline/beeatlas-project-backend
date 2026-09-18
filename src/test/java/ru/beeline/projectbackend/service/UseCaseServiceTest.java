/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import ru.beeline.projectbackend.client.ProductClient;
import ru.beeline.projectbackend.domain.ArtifactBranch;
import ru.beeline.projectbackend.domain.Project;
import ru.beeline.projectbackend.domain.UsOperationRelation;
import ru.beeline.projectbackend.domain.UseCase;
import ru.beeline.projectbackend.dto.product.DiscoveredInterfaceResultDTO;
import ru.beeline.projectbackend.dto.product.DiscoveredInterfaceUpsertDTO;
import ru.beeline.projectbackend.dto.product.DiscoveredOperationResultDTO;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UseCaseServiceTest {

    private static final Integer PROJECT_ID = 3;
    private static final Integer BRANCH_ID = 42;
    private static final Integer USE_CASE_ID = 7;
    private static final String ALIAS = "my-product";

    private static final String CRC32_CODE = "cd4c9ad0";

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

    @InjectMocks
    private UseCaseService service;

    @BeforeEach
    void setUp() {
        when(projectRepository.findByIdAndDeleteDateIsNull(anyInt()))
                .thenReturn(Optional.of(Project.builder().id(PROJECT_ID).name("проект").build()));
        when(artifactBranchRepository.findByArtifactTypeAndArtifactIdAndNameIgnoreCase(anyString(), anyInt(), anyString()))
                .thenReturn(Optional.of(ArtifactBranch.builder()
                        .id(BRANCH_ID)
                        .artifactType(ArtifactBranch.TYPE_PROJECT)
                        .artifactId(PROJECT_ID)
                        .name("main")
                        .build()));
        when(useCaseRepository.findByProjectBranchIdAndCodeIgnoreCase(anyInt(), anyString()))
                .thenReturn(Optional.empty());
        when(useCaseRepository.save(any(UseCase.class))).thenAnswer(invocation -> {
            UseCase saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(USE_CASE_ID);
            }
            return saved;
        });
        when(usOperationRelationRepository.deleteAllByUcId(anyInt())).thenReturn(0);
        when(productClient.upsertProjectDiscoveredInterfaces(anyInt(), any())).thenReturn(publishedResponse());
    }

    @Test
    @DisplayName("Создание: карточка сохраняется, шаги ссылаются на id операций из ответа fdm-products")
    void createsUseCaseWithSteps() {
        UseCaseUpsertResponseDTO response = service.upsert(PROJECT_ID, "main", request());

        assertThat(response.getId()).isEqualTo(USE_CASE_ID);
        assertThat(response.getCode()).isEqualTo("uc-1");
        assertThat(response.getProjectBranchId()).isEqualTo(BRANCH_ID);

        ArgumentCaptor<UseCase> cardCaptor = ArgumentCaptor.forClass(UseCase.class);
        verify(useCaseRepository).save(cardCaptor.capture());
        UseCase card = cardCaptor.getValue();
        assertThat(card.getProjectBranchId()).isEqualTo(BRANCH_ID);
        assertThat(card.getCode()).isEqualTo("uc-1");
        assertThat(card.getName()).isEqualTo("Оформление заказа");
        assertThat(card.getDescription()).isEqualTo("описание use case");

        List<UsOperationRelation> relations = savedRelations();
        assertThat(relations).hasSize(2);
        assertThat(relations.get(0).getUcId()).isEqualTo(USE_CASE_ID);
        assertThat(relations.get(0).getOrder()).isEqualTo(1);
        assertThat(relations.get(0).getReqOperationId()).isNull();
        assertThat(relations.get(0).getRelatedReqOperationId()).isEqualTo(901);
        assertThat(relations.get(1).getOrder()).isEqualTo(2);
        assertThat(relations.get(1).getReqOperationId()).isEqualTo(901);
        assertThat(relations.get(1).getRelatedReqOperationId()).isEqualTo(903);
    }

    @Test
    @DisplayName("Ветка не передана — используется main, регистр не важен")
    void defaultsBranchToMain() {
        service.upsert(PROJECT_ID, null, request());
        verify(artifactBranchRepository)
                .findByArtifactTypeAndArtifactIdAndNameIgnoreCase(eq("project"), eq(PROJECT_ID), eq("main"));

        service.upsert(PROJECT_ID, "  DeVeLoP ", request());
        verify(artifactBranchRepository)
                .findByArtifactTypeAndArtifactIdAndNameIgnoreCase(eq("project"), eq(PROJECT_ID), eq("develop"));
    }

    @Test
    @DisplayName("Агрегация: операции с одним interfaceCode+product в одной группе, без кода — отдельным интерфейсом с CRC32")
    void groupsReqOperationsForProductService() {
        service.upsert(PROJECT_ID, "main", request());

        List<DiscoveredInterfaceUpsertDTO> payload = capturedPayload();
        assertThat(payload).hasSize(2);

        DiscoveredInterfaceUpsertDTO grouped = payload.get(0);
        assertThat(grouped.getInterfaceCode()).isEqualTo("orders-api");
        assertThat(grouped.getProduct()).isEqualTo(ALIAS);
        assertThat(grouped.getOperations()).extracting(op -> op.getName())
                .containsExactly("getOrder", "putOrder");
        assertThat(grouped.getOperations().get(0).getTcCode()).isEqualTo("TC-1");
        assertThat(grouped.getOperations().get(0).getDescriptionTc()).isEqualTo("описание ТС");
        assertThat(grouped.getOperations().get(0).getDescriptionOperation()).isEqualTo("описание операции");

        DiscoveredInterfaceUpsertDTO standalone = payload.get(1);
        assertThat(standalone.getInterfaceCode()).isEqualTo(CRC32_CODE);
        assertThat(standalone.getProduct()).isEqualTo(ALIAS);
        assertThat(standalone.getOperations()).extracting(op -> op.getName()).containsExactly("listOrders");
    }

    @Test
    @DisplayName("Обновление: карточка обновляется, прежний состав удаляется и пересоздаётся")
    void rebuildsStepsOnUpdate() {
        when(useCaseRepository.findByProjectBranchIdAndCodeIgnoreCase(anyInt(), anyString()))
                .thenReturn(Optional.of(UseCase.builder()
                        .id(USE_CASE_ID)
                        .projectBranchId(BRANCH_ID)
                        .code("uc-1")
                        .name("прежнее имя")
                        .description("прежнее описание")
                        .build()));
        when(usOperationRelationRepository.deleteAllByUcId(USE_CASE_ID)).thenReturn(5);

        UseCaseUpsertResponseDTO response = service.upsert(PROJECT_ID, "main", request());

        assertThat(response.getId()).isEqualTo(USE_CASE_ID);
        ArgumentCaptor<UseCase> cardCaptor = ArgumentCaptor.forClass(UseCase.class);
        verify(useCaseRepository).save(cardCaptor.capture());
        assertThat(cardCaptor.getValue().getName()).isEqualTo("Оформление заказа");
        assertThat(cardCaptor.getValue().getDescription()).isEqualTo("описание use case");
        verify(usOperationRelationRepository).deleteAllByUcId(USE_CASE_ID);
        assertThat(savedRelations()).hasSize(2);
    }

    @Test
    @DisplayName("Проект не найден — 404, вызова fdm-products нет")
    void failsOnUnknownProject() {
        when(projectRepository.findByIdAndDeleteDateIsNull(anyInt())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", request()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Проекта с таким ID не существует");
        verifyNoInteractions(productClient);
    }

    @Test
    @DisplayName("Ветка проекта не найдена — 404, вызова fdm-products нет")
    void failsOnUnknownBranch() {
        when(artifactBranchRepository.findByArtifactTypeAndArtifactIdAndNameIgnoreCase(anyString(), anyInt(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "design", request()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("design");
        verifyNoInteractions(productClient);
    }

    @Test
    @DisplayName("Тело, обязательные поля, дубликат guid и неизвестный guid — 400 до вызова fdm-products")
    void failsOnInvalidRequest() {
        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("тело запроса");

        UseCaseUpsertRequestDTO noUid = request();
        noUid.getUseCase().setUid("  ");
        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", noUid))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("useCase.uid");

        UseCaseUpsertRequestDTO noName = request();
        noName.getUseCase().setName(null);
        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", noName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("useCase.name");

        UseCaseUpsertRequestDTO noReqOperations = request();
        noReqOperations.setReqOperations(List.of());
        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", noReqOperations))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("reqOperations");

        UseCaseUpsertRequestDTO noOrder = request();
        noOrder.getUseCaseCall().get(0).setOrder(null);
        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", noOrder))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("useCaseCall.order");

        UseCaseUpsertRequestDTO duplicateGuid = request();
        duplicateGuid.getReqOperations().get(1).setGuid("op-1");
        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", duplicateGuid))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Дубликат guid");

        UseCaseUpsertRequestDTO unknownGuid = request();
        unknownGuid.getUseCaseCall().get(1).setRelatedOperationGuid("op-missing");
        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", unknownGuid))
                .isInstanceOf(ValidationException.class)
                .hasMessage("useCaseCall ссылается на неизвестный guid reqOperation");

        verifyNoInteractions(productClient);
        verify(usOperationRelationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Ошибка fdm-products пробрасывается, состав use case не записывается")
    void propagatesProductServiceFailure() {
        when(productClient.upsertProjectDiscoveredInterfaces(anyInt(), any()))
                .thenThrow(new ExternalServiceException(HttpStatus.BAD_REQUEST, "Продукт не найден"));

        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", request()))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Продукт не найден");
        verify(usOperationRelationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("fdm-products не вернул id для операции справочника — 502, состав не записывается")
    void failsWhenOperationIdMissingInResponse() {
        when(productClient.upsertProjectDiscoveredInterfaces(anyInt(), any()))
                .thenReturn(List.of(DiscoveredInterfaceResultDTO.builder()
                        .interfaceCode("orders-api")
                        .product(ALIAS)
                        .operations(List.of(operationResult(901, "getOrder", "REST")))
                        .build()));

        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", request()))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("op-2");
        verify(usOperationRelationRepository, never()).saveAll(any());
    }

    @SuppressWarnings("unchecked")
    private List<DiscoveredInterfaceUpsertDTO> capturedPayload() {
        ArgumentCaptor<List<DiscoveredInterfaceUpsertDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(productClient).upsertProjectDiscoveredInterfaces(eq(PROJECT_ID), captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<UsOperationRelation> savedRelations() {
        ArgumentCaptor<List<UsOperationRelation>> captor = ArgumentCaptor.forClass(List.class);
        verify(usOperationRelationRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private List<DiscoveredInterfaceResultDTO> publishedResponse() {
        return List.of(
                DiscoveredInterfaceResultDTO.builder()
                        .interfaceCode("orders-api")
                        .product(ALIAS)
                        .operations(List.of(
                                operationResult(901, "getOrder", "REST"),
                                operationResult(902, "putOrder", "REST")))
                        .build(),
                DiscoveredInterfaceResultDTO.builder()
                        .interfaceCode(CRC32_CODE)
                        .product(ALIAS)
                        .operations(List.of(operationResult(903, "listOrders", "REST")))
                        .build());
    }

    private DiscoveredOperationResultDTO operationResult(Integer id, String name, String type) {
        return DiscoveredOperationResultDTO.builder().discoveredOperationId(id).name(name).type(type).build();
    }

    private UseCaseUpsertRequestDTO request() {
        return UseCaseUpsertRequestDTO.builder()
                .useCase(UseCaseInfoDTO.builder()
                        .uid("uc-1")
                        .name("Оформление заказа")
                        .description("описание use case")
                        .build())
                .useCaseCall(new ArrayList<>(Arrays.asList(
                        UseCaseCallDTO.builder()
                                .relatedOperationGuid("op-1")
                                .order(1)
                                .build(),
                        UseCaseCallDTO.builder()
                                .operationGuid("op-1")
                                .relatedOperationGuid("op-3")
                                .order(2)
                                .build())))
                .reqOperations(new ArrayList<>(Arrays.asList(
                        ReqOperationDTO.builder()
                                .guid("op-1")
                                .product(ALIAS)
                                .interfaceCode("orders-api")
                                .name("getOrder")
                                .type("REST")
                                .descriptionTc("описание ТС")
                                .tcCode("TC-1")
                                .descriptionOperation("описание операции")
                                .build(),
                        ReqOperationDTO.builder()
                                .guid("op-2")
                                .product(ALIAS)
                                .interfaceCode("orders-api")
                                .name("putOrder")
                                .type("REST")
                                .build(),
                        ReqOperationDTO.builder()
                                .guid("op-3")
                                .product(ALIAS)
                                .name("listOrders")
                                .type("REST")
                                .build())))
                .build();
    }
}
