/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import ru.beeline.projectbackend.dto.product.DiscoveredOperationUpsertDTO;
import ru.beeline.projectbackend.dto.usecase.OperationUseCasesResponseDTO;
import ru.beeline.projectbackend.dto.usecase.ReqOperationDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseCallDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseCardResponseDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseInfoDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertRequestDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseUpsertResponseDTO;
import ru.beeline.projectbackend.exception.ExternalServiceException;
import ru.beeline.projectbackend.exception.NotFoundException;
import ru.beeline.projectbackend.exception.ValidationException;
import ru.beeline.projectbackend.repository.ArtifactBranchRepository;
import ru.beeline.projectbackend.repository.ProjectRepository;
import ru.beeline.projectbackend.repository.UsOperationRelationRepository;
import ru.beeline.projectbackend.repository.UseCaseByOperationProjection;
import ru.beeline.projectbackend.repository.UseCaseRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UseCaseServiceTest {

    private static final Integer PROJECT_ID = 3;
    private static final Integer USE_CASE_BRANCH_ID = 42;
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
        when(useCaseRepository.findByProjectIdAndCodeIgnoreCase(anyInt(), anyString()))
                .thenReturn(Optional.empty());
        when(useCaseRepository.save(any(UseCase.class))).thenAnswer(invocation -> {
            UseCase saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(USE_CASE_ID);
            }
            return saved;
        });
        when(artifactBranchRepository.findByArtifactTypeAndArtifactIdAndNameIgnoreCase(anyString(), anyInt(), anyString()))
                .thenReturn(Optional.empty());
        when(artifactBranchRepository.save(any(ArtifactBranch.class))).thenAnswer(invocation -> {
            ArtifactBranch saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(USE_CASE_BRANCH_ID);
            }
            return saved;
        });
        when(usOperationRelationRepository.deleteAllByUseCaseBranchId(anyInt())).thenReturn(0);
        when(productClient.upsertUseCaseOperations(anyInt(), any())).thenReturn(publishedResponse());
    }

    @Test
    @DisplayName("Создание: карточка и ветка main, шаги на useCaseBranchId, ответ без projectBranchId")
    void createsUseCaseWithMainBranchAndSteps() {
        UseCaseUpsertResponseDTO response = service.upsert(PROJECT_ID, null, request());

        assertThat(response.getId()).isEqualTo(USE_CASE_ID);
        assertThat(response.getCode()).isEqualTo("uc-1");
        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(response.getUseCaseBranchId()).isEqualTo(USE_CASE_BRANCH_ID);
        assertThat(response.getBranch()).isEqualTo("main");

        ArgumentCaptor<UseCase> cardCaptor = ArgumentCaptor.forClass(UseCase.class);
        verify(useCaseRepository).save(cardCaptor.capture());
        UseCase card = cardCaptor.getValue();
        assertThat(card.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(card.getCode()).isEqualTo("uc-1");
        assertThat(card.getName()).isEqualTo("Оформление заказа");
        assertThat(card.getDescription()).isEqualTo("описание use case");
        assertThat(card.getBiStepCode()).isEqualTo("BI-1");

        ArgumentCaptor<ArtifactBranch> branchCaptor = ArgumentCaptor.forClass(ArtifactBranch.class);
        verify(artifactBranchRepository).save(branchCaptor.capture());
        assertThat(branchCaptor.getValue().getArtifactType()).isEqualTo("use_case");
        assertThat(branchCaptor.getValue().getArtifactId()).isEqualTo(USE_CASE_ID);
        assertThat(branchCaptor.getValue().getName()).isEqualTo("main");

        List<UsOperationRelation> relations = savedRelations();
        assertThat(relations).hasSize(2);
        assertThat(relations.get(0).getUseCaseBranchId()).isEqualTo(USE_CASE_BRANCH_ID);
        assertThat(relations.get(0).getOrder()).isEqualTo(1);
        assertThat(relations.get(0).getReqOperationId()).isNull();
        assertThat(relations.get(0).getRelatedReqOperationId()).isEqualTo(901);
        assertThat(relations.get(1).getOrder()).isEqualTo(2);
        assertThat(relations.get(1).getReqOperationId()).isEqualTo(901);
        assertThat(relations.get(1).getRelatedReqOperationId()).isEqualTo(903);
    }

    @Test
    @DisplayName("Ветка не передана — main; регистр имени ветки не важен")
    void defaultsBranchToMainAndNormalizesCase() {
        service.upsert(PROJECT_ID, null, request());
        verify(artifactBranchRepository)
                .findByArtifactTypeAndArtifactIdAndNameIgnoreCase(
                        eq("use_case"), eq(USE_CASE_ID), eq("main"));

        when(artifactBranchRepository.findByArtifactTypeAndArtifactIdAndNameIgnoreCase(
                "use_case", USE_CASE_ID, "develop"))
                .thenReturn(Optional.of(ArtifactBranch.builder()
                        .id(99)
                        .artifactType("use_case")
                        .artifactId(USE_CASE_ID)
                        .name("develop")
                        .build()));

        UseCaseUpsertResponseDTO response = service.upsert(PROJECT_ID, "  DeVeLoP ", request());
        assertThat(response.getBranch()).isEqualTo("develop");
        assertThat(response.getUseCaseBranchId()).isEqualTo(99);
        verify(artifactBranchRepository)
                .findByArtifactTypeAndArtifactIdAndNameIgnoreCase(
                        eq("use_case"), eq(USE_CASE_ID), eq("develop"));
    }

    @Test
    @DisplayName("Вторая ветка не затирает шаги первой: delete только по useCaseBranchId текущей ветки")
    void secondBranchDoesNotWipeFirstBranchSteps() {
        Integer mainBranchId = 10;
        Integer designBranchId = 20;
        AtomicInteger branchSeq = new AtomicInteger(0);

        when(useCaseRepository.findByProjectIdAndCodeIgnoreCase(eq(PROJECT_ID), anyString()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(UseCase.builder()
                        .id(USE_CASE_ID)
                        .projectId(PROJECT_ID)
                        .code("uc-1")
                        .name("прежнее")
                        .build()));

        when(artifactBranchRepository.findByArtifactTypeAndArtifactIdAndNameIgnoreCase(
                eq("use_case"), eq(USE_CASE_ID), eq("main")))
                .thenReturn(Optional.empty());
        when(artifactBranchRepository.findByArtifactTypeAndArtifactIdAndNameIgnoreCase(
                eq("use_case"), eq(USE_CASE_ID), eq("design")))
                .thenReturn(Optional.empty());
        when(artifactBranchRepository.save(any(ArtifactBranch.class))).thenAnswer(invocation -> {
            ArtifactBranch saved = invocation.getArgument(0);
            int n = branchSeq.getAndIncrement();
            saved.setId(n == 0 ? mainBranchId : designBranchId);
            return saved;
        });

        service.upsert(PROJECT_ID, "main", request());
        service.upsert(PROJECT_ID, "design", request());

        verify(usOperationRelationRepository).deleteAllByUseCaseBranchId(mainBranchId);
        verify(usOperationRelationRepository).deleteAllByUseCaseBranchId(designBranchId);
        verify(usOperationRelationRepository, never()).deleteAllByUseCaseBranchId(eq(USE_CASE_ID));
        verify(useCaseRepository, times(2)).save(any(UseCase.class));
        verify(artifactBranchRepository, times(2)).save(any(ArtifactBranch.class));
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
        assertThat(grouped.getProductPlanned()).isEqualTo("MYAPP-PLAN");
        assertThat(grouped.getOperations()).extracting(op -> op.getName())
                .containsExactly("getOrder", "putOrder");
        assertThat(grouped.getOperations().get(0).getTcCode()).isEqualTo("TC-1");
        assertThat(grouped.getOperations().get(0).getDescriptionTc()).isEqualTo("описание ТС");
        assertThat(grouped.getOperations().get(0).getDescriptionOperation()).isEqualTo("описание операции");
        assertThat(grouped.getOperations().get(0).getIsPlanned()).isTrue();

        DiscoveredInterfaceUpsertDTO standalone = payload.get(1);
        assertThat(standalone.getInterfaceCode()).isEqualTo(CRC32_CODE);
        assertThat(standalone.getProduct()).isEqualTo(ALIAS);
        assertThat(standalone.getProductPlanned()).isEqualTo("MYAPP-PLAN2");
        assertThat(standalone.getOperations()).extracting(op -> op.getName()).containsExactly("listOrders");
        assertThat(standalone.getOperations().get(0).getIsPlanned()).isFalse();
    }

    @Test
    @DisplayName("connectionOperationId уходит числом в operations тела продуктов, guid не включается")
    void sendsConnectionOperationIdToProductService() throws Exception {
        UseCaseUpsertRequestDTO withId = request();
        withId.getReqOperations().get(0).setConnectionOperationId(15);
        service.upsert(PROJECT_ID, "main", withId);

        DiscoveredOperationUpsertDTO operation = capturedPayload().get(0).getOperations().get(0);
        assertThat(operation.getName()).isEqualTo("getOrder");
        assertThat(operation.getConnectionOperationId()).isEqualTo(15);
        JsonNode json = new ObjectMapper().valueToTree(operation);
        assertThat(json.get("connectionOperationId").isIntegralNumber()).isTrue();
        assertThat(json.get("connectionOperationId").intValue()).isEqualTo(15);
        assertThat(json.has("guid")).isFalse();
        assertThat(capturedPayload().get(0).getOperations().get(1).getConnectionOperationId()).isNull();
    }

    @Test
    @DisplayName("Нет connectionOperationId или null — ключ в теле продуктов отсутствует")
    void omitsConnectionOperationIdWhenAbsentOrNull() throws Exception {
        UseCaseUpsertRequestDTO explicitNull = request();
        explicitNull.getReqOperations().forEach(operation -> operation.setConnectionOperationId(null));

        service.upsert(PROJECT_ID, "main", request());
        service.upsert(PROJECT_ID, "main", explicitNull);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiscoveredInterfaceUpsertDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(productClient, times(2)).upsertUseCaseOperations(eq(USE_CASE_ID), captor.capture());
        ObjectMapper mapper = new ObjectMapper();
        for (List<DiscoveredInterfaceUpsertDTO> payload : captor.getAllValues()) {
            for (DiscoveredInterfaceUpsertDTO iface : payload) {
                for (DiscoveredOperationUpsertDTO operation : iface.getOperations()) {
                    assertThat(mapper.valueToTree(operation).has("connectionOperationId")).isFalse();
                }
            }
        }
    }

    @Test
    @DisplayName("Обновление: карточка обновляется, прежний состав ветки удаляется и пересоздаётся")
    void rebuildsStepsOnUpdate() {
        when(useCaseRepository.findByProjectIdAndCodeIgnoreCase(anyInt(), anyString()))
                .thenReturn(Optional.of(UseCase.builder()
                        .id(USE_CASE_ID)
                        .projectId(PROJECT_ID)
                        .code("uc-1")
                        .name("прежнее имя")
                        .description("прежнее описание")
                        .build()));
        when(artifactBranchRepository.findByArtifactTypeAndArtifactIdAndNameIgnoreCase(
                "use_case", USE_CASE_ID, "main"))
                .thenReturn(Optional.of(ArtifactBranch.builder()
                        .id(USE_CASE_BRANCH_ID)
                        .artifactType("use_case")
                        .artifactId(USE_CASE_ID)
                        .name("main")
                        .build()));
        when(usOperationRelationRepository.deleteAllByUseCaseBranchId(USE_CASE_BRANCH_ID)).thenReturn(5);

        UseCaseUpsertResponseDTO response = service.upsert(PROJECT_ID, "main", request());

        assertThat(response.getId()).isEqualTo(USE_CASE_ID);
        assertThat(response.getUseCaseBranchId()).isEqualTo(USE_CASE_BRANCH_ID);
        ArgumentCaptor<UseCase> cardCaptor = ArgumentCaptor.forClass(UseCase.class);
        verify(useCaseRepository).save(cardCaptor.capture());
        assertThat(cardCaptor.getValue().getName()).isEqualTo("Оформление заказа");
        assertThat(cardCaptor.getValue().getDescription()).isEqualTo("описание use case");
        verify(usOperationRelationRepository).deleteAllByUseCaseBranchId(USE_CASE_BRANCH_ID);
        verify(artifactBranchRepository, never()).save(any(ArtifactBranch.class));
        assertThat(savedRelations()).hasSize(2);
    }

    @Test
    @DisplayName("biStepCode не задан или пустой — в карточку пишется NULL")
    void writesNullBiStepCodeWhenBlankOrAbsent() {
        UseCaseUpsertRequestDTO blank = request();
        blank.getUseCase().setBiStepCode("   ");
        service.upsert(PROJECT_ID, "main", blank);

        UseCaseUpsertRequestDTO absent = request();
        absent.getUseCase().setBiStepCode(null);
        service.upsert(PROJECT_ID, "main", absent);

        ArgumentCaptor<UseCase> captor = ArgumentCaptor.forClass(UseCase.class);
        verify(useCaseRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(UseCase::getBiStepCode).containsOnlyNulls();
    }

    @Test
    @DisplayName("productPlanned внутри группы должен совпадать — иначе 400 до вызова сервиса продуктов")
    void failsWhenProductPlannedDiffersInGroup() {
        UseCaseUpsertRequestDTO mismatch = request();
        mismatch.getReqOperations().get(1).setProductPlanned("OTHER");

        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", mismatch))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("productPlanned");
        verifyNoInteractions(productClient);
        verify(usOperationRelationRepository, never()).deleteAllByUseCaseBranchId(anyInt());
    }

    @Test
    @DisplayName("Шаги удаляются и пересоздаются только после успешного ответа сервиса продуктов")
    void recreatesStepsAfterProductCall() {
        service.upsert(PROJECT_ID, "main", request());

        InOrder inOrder = inOrder(productClient, usOperationRelationRepository);
        inOrder.verify(productClient).upsertUseCaseOperations(eq(USE_CASE_ID), any());
        inOrder.verify(usOperationRelationRepository).deleteAllByUseCaseBranchId(USE_CASE_BRANCH_ID);
        inOrder.verify(usOperationRelationRepository).saveAll(any());
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
    @DisplayName("Удалённый проект (delete_date) — 404")
    void failsOnDeletedProject() {
        when(projectRepository.findByIdAndDeleteDateIsNull(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", request()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Проекта с таким ID не существует");
        verifyNoInteractions(productClient);
        verify(useCaseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Пустое или blank имя ветки — 400")
    void failsOnBlankBranchName() {
        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "", request()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Имя ветки не может быть пустым");
        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "   ", request()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Имя ветки не может быть пустым");
        verifyNoInteractions(productClient);
        verify(useCaseRepository, never()).save(any());
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
        when(productClient.upsertUseCaseOperations(anyInt(), any()))
                .thenThrow(new ExternalServiceException(HttpStatus.BAD_REQUEST, "Продукт не найден"));

        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", request()))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Продукт не найден");
        verify(usOperationRelationRepository, never()).deleteAllByUseCaseBranchId(anyInt());
        verify(usOperationRelationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Поиск по операциям: тело, пустой список и невалидный элемент — 400 без запроса в БД")
    void failsOnInvalidOperationsBody() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        assertThatThrownBy(() -> service.findUseCasesByOperations(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Отсутствует тело запроса");

        assertThatThrownBy(() -> service.findUseCasesByOperations(array()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Список идентификаторов методов не может быть пустым");

        ArrayNode withNull = array(20);
        withNull.add(NullNode.getInstance());
        assertThatThrownBy(() -> service.findUseCasesByOperations(withNull))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Идентификатор метода не может быть пустым");

        assertThatThrownBy(() -> service.findUseCasesByOperations(mapper.readTree("[\"5\"]")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Идентификатор метода не может быть пустым");

        assertThatThrownBy(() -> service.findUseCasesByOperations(mapper.readTree("[5.7]")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Идентификатор метода не может быть пустым");

        verify(usOperationRelationRepository, never()).findUseCasesByOperationIds(any());
    }

    private ArrayNode array(Integer... values) {
        ArrayNode node = JsonNodeFactory.instance.arrayNode();
        if (values != null) {
            for (Integer value : values) {
                node.add(value);
            }
        }
        return node;
    }

    @Test
    @DisplayName("fdm-products не вернул id для операции справочника — 502, состав не записывается")
    void failsWhenOperationIdMissingInResponse() {
        when(productClient.upsertUseCaseOperations(anyInt(), any()))
                .thenReturn(List.of(DiscoveredInterfaceResultDTO.builder()
                        .interfaceCode("orders-api")
                        .product(ALIAS)
                        .operations(List.of(operationResult(901, "getOrder", "REST")))
                        .build()));

        assertThatThrownBy(() -> service.upsert(PROJECT_ID, "main", request()))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("op-2");
        verify(usOperationRelationRepository, never()).deleteAllByUseCaseBranchId(anyInt());
        verify(usOperationRelationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Поиск по операциям: дедубликация id, порядок первого вхождения, use case по возрастанию id")
    void findsUseCasesByOperations() {
        List<UseCaseByOperationProjection> rows = List.of(
                row(20, 30, 8, "Возврат"),
                row(20, 10, 8, "Возврат"),
                row(null, 20, 7, "Оформление заказа"),
                row(500, 600, 5, "Не попадёт"));
        when(usOperationRelationRepository.findUseCasesByOperationIds(any()))
                .thenReturn(rows);

        List<OperationUseCasesResponseDTO> response =
                service.findUseCasesByOperations(array(20, 99, 20));

        assertThat(response).extracting(OperationUseCasesResponseDTO::getOperationId)
                .containsExactly(20, 99);
        assertThat(response.get(0).getUseCases()).extracting(uc -> uc.getId()).containsExactly(7, 8);
        assertThat(response.get(0).getUseCases().get(0).getName()).isEqualTo("Оформление заказа");
        assertThat(response.get(0).getUseCases().get(1).getName()).isEqualTo("Возврат");
        assertThat(response.get(1).getUseCases()).isEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(usOperationRelationRepository).findUseCasesByOperationIds(captor.capture());
        assertThat(captor.getValue()).containsExactly(20, 99);
    }

    @Test
    @DisplayName("Поиск по операциям: одна карточка в двух ветках не дублируется в useCases")
    void findsUseCasesByOperations_sameUseCaseOnTwoBranches_deduped() {
        List<UseCaseByOperationProjection> rows = List.of(
                row(20, 30, 7, "Оформление заказа"),
                row(null, 20, 7, "Оформление заказа"),
                row(40, 20, 7, "Оформление заказа"));
        when(usOperationRelationRepository.findUseCasesByOperationIds(any()))
                .thenReturn(rows);

        List<OperationUseCasesResponseDTO> response =
                service.findUseCasesByOperations(array(20));

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getOperationId()).isEqualTo(20);
        assertThat(response.get(0).getUseCases()).extracting(uc -> uc.getId()).containsExactly(7);
        assertThat(response.get(0).getUseCases().get(0).getName()).isEqualTo("Оформление заказа");
    }

    @Test
    @DisplayName("Поиск по операциям: смешанный набор — известные id с use case, неизвестный с пустым списком, порядок сохранён")
    void findsUseCasesByOperations_mixedKnownAndUnknownIds() {
        List<UseCaseByOperationProjection> rows = List.of(
                row(null, 20, 7, "Оформление заказа"),
                row(10, 15, 8, "Возврат"));
        when(usOperationRelationRepository.findUseCasesByOperationIds(any()))
                .thenReturn(rows);

        List<OperationUseCasesResponseDTO> response =
                service.findUseCasesByOperations(array(20, 99999, 10));

        assertThat(response).extracting(OperationUseCasesResponseDTO::getOperationId)
                .containsExactly(20, 99999, 10);
        assertThat(response.get(0).getUseCases()).extracting(uc -> uc.getId()).containsExactly(7);
        assertThat(response.get(1).getUseCases()).isEmpty();
        assertThat(response.get(2).getUseCases()).extracting(uc -> uc.getId()).containsExactly(8);
    }

    @Test
    @DisplayName("Поиск по операциям: шаги без карточки (неверный artifact_type / orphan) исключаются без ошибки")
    void findsUseCasesByOperations_invalidOrOrphanStepsExcludedWithoutError() {
        List<UseCaseByOperationProjection> rows = List.of(
                row(20, 30, null, null),
                row(null, 10, 7, "Оформление заказа"));
        when(usOperationRelationRepository.findUseCasesByOperationIds(any()))
                .thenReturn(rows);

        List<OperationUseCasesResponseDTO> response =
                service.findUseCasesByOperations(array(20, 10));

        assertThat(response).extracting(OperationUseCasesResponseDTO::getOperationId)
                .containsExactly(20, 10);
        assertThat(response.get(0).getUseCases()).isEmpty();
        assertThat(response.get(1).getUseCases()).extracting(uc -> uc.getId()).containsExactly(7);
    }

    @Test
    @DisplayName("Карточка с ветками: сортировка по name, затем по id; пустые biStepCode и updatedDate — null")
    void getCard_returnsBranchesSortedAndNullsBlankBiStepCodeAndUpdatedDate() {
        LocalDateTime created = LocalDateTime.of(2026, 9, 30, 12, 0, 0);
        when(useCaseRepository.findById(7)).thenReturn(Optional.of(UseCase.builder()
                .id(7)
                .projectId(1)
                .code("UC-ORDER")
                .name("Оформление заказа")
                .description("Клиент оформляет заказ")
                .biStepCode("   ")
                .createdDate(created)
                .updatedDate(null)
                .deletedDate(null)
                .build()));
        when(artifactBranchRepository.findByArtifactTypeAndArtifactId("use_case", 7)).thenReturn(List.of(
                ArtifactBranch.builder().id(4).artifactType("use_case").artifactId(7).name("main").build(),
                ArtifactBranch.builder().id(8).artifactType("use_case").artifactId(7).name("main").build(),
                ArtifactBranch.builder().id(5).artifactType("use_case").artifactId(7).name("design").build()));

        UseCaseCardResponseDTO card = service.getCard(7);

        assertThat(card.getId()).isEqualTo(7);
        assertThat(card.getProjectId()).isEqualTo(1);
        assertThat(card.getCode()).isEqualTo("UC-ORDER");
        assertThat(card.getName()).isEqualTo("Оформление заказа");
        assertThat(card.getDescription()).isEqualTo("Клиент оформляет заказ");
        assertThat(card.getBiStepCode()).isNull();
        assertThat(card.getCreatedDate()).isEqualTo(created);
        assertThat(card.getUpdatedDate()).isNull();
        assertThat(card.getDeletedDate()).isNull();
        assertThat(card.getBranches()).extracting(branch -> branch.getName()).containsExactly("design", "main", "main");
        assertThat(card.getBranches()).extracting(branch -> branch.getId()).containsExactly(5, 4, 8);
        verifyNoInteractions(productClient, usOperationRelationRepository);
    }

    @Test
    @DisplayName("Карточка с deletedDate возвращается вместе с датой, это не 404")
    void getCard_returnsDeletedCard() {
        LocalDateTime deleted = LocalDateTime.of(2026, 10, 1, 8, 30, 0);
        when(useCaseRepository.findById(7)).thenReturn(Optional.of(UseCase.builder()
                .id(7)
                .projectId(1)
                .code("UC-ORDER")
                .name("Оформление заказа")
                .biStepCode("BI-STEP-1")
                .createdDate(LocalDateTime.of(2026, 9, 30, 12, 0, 0))
                .updatedDate(LocalDateTime.of(2026, 9, 30, 15, 0, 0))
                .deletedDate(deleted)
                .build()));
        when(artifactBranchRepository.findByArtifactTypeAndArtifactId("use_case", 7)).thenReturn(List.of(
                ArtifactBranch.builder().id(4).artifactType("use_case").artifactId(7).name("main").build()));

        UseCaseCardResponseDTO card = service.getCard(7);

        assertThat(card.getDeletedDate()).isEqualTo(deleted);
        assertThat(card.getBiStepCode()).isEqualTo("BI-STEP-1");
        assertThat(card.getBranches()).extracting(branch -> branch.getId()).containsExactly(4);
        verifyNoInteractions(productClient, usOperationRelationRepository);
    }

    @Test
    @DisplayName("Нет веток — карточка возвращается с branches = []")
    void getCard_returnsEmptyBranches() {
        when(useCaseRepository.findById(7)).thenReturn(Optional.of(UseCase.builder()
                .id(7)
                .projectId(1)
                .code("UC-ORDER")
                .name("Оформление заказа")
                .createdDate(LocalDateTime.of(2026, 9, 30, 12, 0, 0))
                .build()));
        when(artifactBranchRepository.findByArtifactTypeAndArtifactId("use_case", 7)).thenReturn(List.of());

        UseCaseCardResponseDTO card = service.getCard(7);

        assertThat(card.getId()).isEqualTo(7);
        assertThat(card.getBranches()).isEmpty();
        verifyNoInteractions(productClient, usOperationRelationRepository);
    }

    @Test
    @DisplayName("Неизвестный id — 404 Use case не найден, ветки не читаются")
    void getCard_unknownId_throwsNotFound() {
        when(useCaseRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCard(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Use case не найден");
        verify(artifactBranchRepository, never()).findByArtifactTypeAndArtifactId(anyString(), anyInt());
        verifyNoInteractions(productClient, usOperationRelationRepository);
    }

    private UseCaseByOperationProjection row(Integer reqOperationId, Integer relatedOperationId,
                                             Integer ucId, String ucName) {
        UseCaseByOperationProjection row = mock(UseCaseByOperationProjection.class);
        when(row.getReqOperationId()).thenReturn(reqOperationId);
        when(row.getRelatedReqOperationId()).thenReturn(relatedOperationId);
        when(row.getUcId()).thenReturn(ucId);
        when(row.getUcName()).thenReturn(ucName);
        return row;
    }

    @SuppressWarnings("unchecked")
    private List<DiscoveredInterfaceUpsertDTO> capturedPayload() {
        ArgumentCaptor<List<DiscoveredInterfaceUpsertDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(productClient).upsertUseCaseOperations(eq(USE_CASE_ID), captor.capture());
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
                        .biStepCode("BI-1")
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
                                .productPlanned("MYAPP-PLAN")
                                .interfaceCode("orders-api")
                                .name("getOrder")
                                .type("REST")
                                .descriptionTc("описание ТС")
                                .tcCode("TC-1")
                                .descriptionOperation("описание операции")
                                .isPlanned(true)
                                .build(),
                        ReqOperationDTO.builder()
                                .guid("op-2")
                                .product(ALIAS)
                                .productPlanned("MYAPP-PLAN")
                                .interfaceCode("orders-api")
                                .name("putOrder")
                                .type("REST")
                                .build(),
                        ReqOperationDTO.builder()
                                .guid("op-3")
                                .product(ALIAS)
                                .productPlanned("MYAPP-PLAN2")
                                .name("listOrders")
                                .type("REST")
                                .isPlanned(false)
                                .build())))
                .build();
    }
}
