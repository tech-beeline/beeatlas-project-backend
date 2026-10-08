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
import ru.beeline.projectbackend.dto.product.ConnectedContainerDTO;
import ru.beeline.projectbackend.dto.product.ConnectedInterfaceDTO;
import ru.beeline.projectbackend.dto.product.ConnectedOperationSlaDTO;
import ru.beeline.projectbackend.dto.product.DiscoveredInterfaceByIdsDTO;
import ru.beeline.projectbackend.dto.product.DiscoveredOperationByIdsDTO;
import ru.beeline.projectbackend.dto.product.OperationWithTechCapabilityDTO;
import ru.beeline.projectbackend.dto.product.ProductDTO;
import ru.beeline.projectbackend.dto.product.TcDTO;
import ru.beeline.projectbackend.dto.usecase.UseCaseOperationNodeDTO;
import ru.beeline.projectbackend.exception.ExternalServiceException;
import ru.beeline.projectbackend.exception.NotFoundException;
import ru.beeline.projectbackend.exception.ValidationException;
import ru.beeline.projectbackend.repository.ArtifactBranchRepository;
import ru.beeline.projectbackend.repository.ProjectRepository;
import ru.beeline.projectbackend.repository.UsOperationRelationRepository;
import ru.beeline.projectbackend.repository.UseCaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UseCaseOperationsTreeServiceTest {

    private static final Integer PROJECT_ID = 3;
    private static final Integer USE_CASE_ID = 7;
    private static final Integer BRANCH_ID = 42;

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
        when(projectRepository.findByIdAndDeleteDateIsNull(PROJECT_ID))
                .thenReturn(Optional.of(Project.builder().id(PROJECT_ID).name("проект").build()));
        when(useCaseRepository.findByIdAndProjectIdAndDeletedDateIsNull(USE_CASE_ID, PROJECT_ID))
                .thenReturn(Optional.of(UseCase.builder().id(USE_CASE_ID).projectId(PROJECT_ID).build()));
        when(artifactBranchRepository.findByArtifactTypeAndArtifactIdAndNameIgnoreCase(
                eq("use_case"), eq(USE_CASE_ID), anyString()))
                .thenReturn(Optional.of(ArtifactBranch.builder()
                        .id(BRANCH_ID).artifactType("use_case").artifactId(USE_CASE_ID).name("main").build()));
        when(productClient.getDiscoveredOperationsByIds(any())).thenAnswer(invocation -> {
            List<Integer> ids = invocation.getArgument(0);
            List<DiscoveredOperationByIdsDTO> result = new ArrayList<>();
            for (Integer id : ids) {
                result.add(operation(id));
            }
            return result;
        });
    }

    @Test
    @DisplayName("Дерево: корень, дети по req_operation_id, соседи по order затем id шага, узел заполнен целиком")
    void buildsTreeSortedByOrderThenId() {
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of(
                step(4, 2, 20, 23),
                step(3, 1, 20, 22),
                step(2, 1, 20, 21),
                step(1, 0, null, 20),
                step(5, 3, 21, 24)));

        List<UseCaseOperationNodeDTO> tree = service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null);

        assertThat(tree).hasSize(1);
        UseCaseOperationNodeDTO root = tree.get(0);
        assertThat(root.getOrder()).isZero();
        assertThat(root.getDiscoveredOperationId()).isEqualTo(20);
        assertThat(root.getDiscoveredOperation()).isEqualTo(operation(20));
        assertThat(root.getChildren()).extracting(UseCaseOperationNodeDTO::getDiscoveredOperationId)
                .containsExactly(21, 22, 23);
        assertThat(root.getChildren().get(0).getChildren())
                .extracting(UseCaseOperationNodeDTO::getDiscoveredOperationId).containsExactly(24);
        assertThat(root.getChildren().get(1).getChildren()).isEmpty();

        assertThat(capturedIds()).containsExactly(20, 21, 24, 22, 23);
    }

    @Test
    @DisplayName("Узел дерева сохраняет product интерфейса из ответа продуктов")
    void keepsInterfaceProductOnNode() {
        ProductDTO product = ProductDTO.builder()
                .id(3)
                .alias("MYAPP")
                .name("Заказы")
                .description("Продукт заказов")
                .build();
        DiscoveredOperationByIdsDTO fromProducts = DiscoveredOperationByIdsDTO.builder()
                .id(20)
                .isPlanned(false)
                .discoveredInterface(DiscoveredInterfaceByIdsDTO.builder()
                        .id(5)
                        .name("orders-api")
                        .productId(3)
                        .productPlanned(null)
                        .product(product)
                        .build())
                .build();
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of(
                step(1, 0, null, 20)));
        when(productClient.getDiscoveredOperationsByIds(List.of(20))).thenReturn(List.of(fromProducts));

        List<UseCaseOperationNodeDTO> tree = service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null);

        assertThat(tree.get(0).getDiscoveredOperation()).isSameAs(fromProducts);
        assertThat(tree.get(0).getDiscoveredOperation().getDiscoveredInterface().getProduct())
                .isEqualTo(product);
        verify(productClient).getDiscoveredOperationsByIds(List.of(20));
        verify(productClient, never()).upsertUseCaseOperations(any(), any());
    }

    @Test
    @DisplayName("Узел сохраняет techCapability, sla, interface и container из ответа продуктов")
    void keepsTechCapabilitySlaInterfaceAndContainerOnNode() {
        TcDTO techCapability = TcDTO.builder()
                .id(3)
                .code("TC.01")
                .name("Оформление заказа")
                .build();
        ConnectedOperationSlaDTO sla = ConnectedOperationSlaDTO.builder()
                .id(15)
                .operationId(100)
                .rps(100.0)
                .latency(50.0)
                .errorRate(0.1)
                .build();
        ConnectedContainerDTO container = ConnectedContainerDTO.builder()
                .id(4)
                .code("orders-api")
                .build();
        ConnectedInterfaceDTO connectedInterface = ConnectedInterfaceDTO.builder()
                .id(8)
                .code("orders-rest")
                .container(container)
                .build();
        OperationWithTechCapabilityDTO operation = OperationWithTechCapabilityDTO.builder()
                .id(100)
                .name("createOrder")
                .type("POST")
                .tcId(3)
                .techCapability(techCapability)
                .sla(sla)
                .connectedInterface(connectedInterface)
                .build();
        ProductDTO product = ProductDTO.builder()
                .id(3)
                .alias("MYAPP")
                .name("Заказы")
                .description("Продукт заказов")
                .build();
        DiscoveredOperationByIdsDTO fromProducts = DiscoveredOperationByIdsDTO.builder()
                .id(20)
                .name("createOrder")
                .type("POST")
                .isPlanned(false)
                .discoveredInterface(DiscoveredInterfaceByIdsDTO.builder()
                        .id(5)
                        .name("orders-api")
                        .productId(3)
                        .product(product)
                        .build())
                .operation(operation)
                .build();
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of(
                step(1, 0, null, 20)));
        when(productClient.getDiscoveredOperationsByIds(List.of(20))).thenReturn(List.of(fromProducts));

        UseCaseOperationNodeDTO node = service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null).get(0);

        assertThat(node.getDiscoveredOperation()).isSameAs(fromProducts);
        assertThat(node.getDiscoveredOperation().getOperation()).isSameAs(operation);
        assertThat(node.getDiscoveredOperation().getOperation().getTechCapability()).isEqualTo(techCapability);
        assertThat(node.getDiscoveredOperation().getOperation().getSla()).isEqualTo(sla);
        assertThat(node.getDiscoveredOperation().getOperation().getConnectedInterface()).isEqualTo(connectedInterface);
        assertThat(node.getDiscoveredOperation().getOperation().getConnectedInterface().getContainer())
                .isEqualTo(container);
        assertThat(node.getDiscoveredOperation().getDiscoveredInterface().getProduct()).isEqualTo(product);
        verify(productClient).getDiscoveredOperationsByIds(List.of(20));
        verify(productClient, never()).upsertUseCaseOperations(any(), any());
    }

    @Test
    @DisplayName("Дочерний узел с operation = null из ответа продуктов остаётся null")
    void keepsNullOperationFromProductsOnChildNode() {
        DiscoveredOperationByIdsDTO root = DiscoveredOperationByIdsDTO.builder()
                .id(20)
                .name("createOrder")
                .operation(OperationWithTechCapabilityDTO.builder()
                        .id(100)
                        .name("createOrder")
                        .type("POST")
                        .tcId(3)
                        .build())
                .build();
        DiscoveredOperationByIdsDTO child = DiscoveredOperationByIdsDTO.builder()
                .id(21)
                .name("reserveStock")
                .isPlanned(true)
                .discoveredInterface(DiscoveredInterfaceByIdsDTO.builder()
                        .id(6)
                        .name("stock-api")
                        .productPlanned("STOCK")
                        .product(null)
                        .build())
                .operation(null)
                .build();
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of(
                step(1, 0, null, 20),
                step(2, 1, 20, 21)));
        when(productClient.getDiscoveredOperationsByIds(List.of(20, 21))).thenReturn(List.of(root, child));

        UseCaseOperationNodeDTO treeRoot = service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null).get(0);

        assertThat(treeRoot.getDiscoveredOperation().getOperation()).isNotNull();
        assertThat(treeRoot.getChildren().get(0).getDiscoveredOperation()).isSameAs(child);
        assertThat(treeRoot.getChildren().get(0).getDiscoveredOperation().getOperation()).isNull();
        assertThat(treeRoot.getChildren().get(0).getDiscoveredOperation().getDiscoveredInterface().getProduct())
                .isNull();
        assertThat(treeRoot.getChildren().get(0).getDiscoveredOperation().getDiscoveredInterface().getProductPlanned())
                .isEqualTo("STOCK");
    }

    @Test
    @DisplayName("Пустой product в ответе продуктов остаётся null, productPlanned не теряется")
    void keepsNullProductAndProductPlanned() {
        DiscoveredOperationByIdsDTO fromProducts = DiscoveredOperationByIdsDTO.builder()
                .id(21)
                .isPlanned(true)
                .discoveredInterface(DiscoveredInterfaceByIdsDTO.builder()
                        .id(6)
                        .name("stock-api")
                        .productId(null)
                        .productPlanned("STOCK")
                        .product(null)
                        .build())
                .build();
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of(
                step(1, 0, null, 21)));
        when(productClient.getDiscoveredOperationsByIds(List.of(21))).thenReturn(List.of(fromProducts));

        UseCaseOperationNodeDTO node = service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null).get(0);

        assertThat(node.getDiscoveredOperation().getIsPlanned()).isTrue();
        assertThat(node.getDiscoveredOperation().getDiscoveredInterface().getProductId()).isNull();
        assertThat(node.getDiscoveredOperation().getDiscoveredInterface().getProduct()).isNull();
        assertThat(node.getDiscoveredOperation().getDiscoveredInterface().getProductPlanned()).isEqualTo("STOCK");
    }

    @Test
    @DisplayName("Дерево: метод под двумя родителями — два узла, id в продукты без дубликатов")
    void sameOperationUnderTwoParentsGivesTwoNodes() {
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of(
                step(1, 0, null, 20),
                step(2, 1, 20, 21),
                step(3, 2, 20, 22),
                step(4, 3, 21, 30),
                step(5, 4, 22, 30)));

        List<UseCaseOperationNodeDTO> tree = service.getOperationsTree(PROJECT_ID, USE_CASE_ID, "main");

        UseCaseOperationNodeDTO root = tree.get(0);
        assertThat(root.getChildren().get(0).getChildren())
                .extracting(UseCaseOperationNodeDTO::getDiscoveredOperationId).containsExactly(30);
        assertThat(root.getChildren().get(1).getChildren())
                .extracting(UseCaseOperationNodeDTO::getDiscoveredOperationId).containsExactly(30);
        assertThat(root.getChildren().get(1).getChildren().get(0).getDiscoveredOperation())
                .isEqualTo(operation(30));
        assertThat(capturedIds()).containsExactly(20, 21, 30, 22);
    }

    @Test
    @DisplayName("Дерево: цикл — повтор метода на пути от корня без детей")
    void cycleIsNotExpanded() {
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of(
                step(1, 0, null, 20),
                step(2, 1, 20, 21),
                step(3, 2, 21, 20)));

        List<UseCaseOperationNodeDTO> tree = service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null);

        UseCaseOperationNodeDTO repeat = tree.get(0).getChildren().get(0).getChildren().get(0);
        assertThat(repeat.getDiscoveredOperationId()).isEqualTo(20);
        assertThat(repeat.getChildren()).isEmpty();
        assertThat(capturedIds()).containsExactly(20, 21);
    }

    @Test
    @DisplayName("Дерево: несколько корней в порядке order")
    void severalRootsInOrder() {
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of(
                step(2, 5, null, 50),
                step(1, 1, null, 10)));

        List<UseCaseOperationNodeDTO> tree = service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null);

        assertThat(tree).extracting(UseCaseOperationNodeDTO::getDiscoveredOperationId).containsExactly(10, 50);
    }

    @Test
    @DisplayName("Дерево: id нет в ответе продуктов — discoveredOperation = null, узел остаётся")
    void missingOperationInProductResponseGivesNull() {
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of(
                step(1, 0, null, 20),
                step(2, 1, 20, 21)));
        doReturn(List.of(operation(20))).when(productClient).getDiscoveredOperationsByIds(any());

        List<UseCaseOperationNodeDTO> tree = service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null);

        UseCaseOperationNodeDTO child = tree.get(0).getChildren().get(0);
        assertThat(child.getDiscoveredOperationId()).isEqualTo(21);
        assertThat(child.getDiscoveredOperation()).isNull();
    }

    @Test
    @DisplayName("Шагов нет — [] без вызова продуктов")
    void emptyBranchReturnsEmptyWithoutProducts() {
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of());

        assertThat(service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null)).isEmpty();
        verifyNoInteractions(productClient);
    }

    @Test
    @DisplayName("Ветка: не передана — main, имя сравнивается без учёта регистра")
    void branchDefaultsToMainAndIsCaseInsensitive() {
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of());

        service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null);
        service.getOperationsTree(PROJECT_ID, USE_CASE_ID, " Design ");

        verify(artifactBranchRepository).findByArtifactTypeAndArtifactIdAndNameIgnoreCase("use_case", USE_CASE_ID, "main");
        verify(artifactBranchRepository).findByArtifactTypeAndArtifactIdAndNameIgnoreCase("use_case", USE_CASE_ID, "design");
    }

    @Test
    @DisplayName("Пустой branch — 400")
    void emptyBranchIs400() {
        assertThatThrownBy(() -> service.getOperationsTree(PROJECT_ID, USE_CASE_ID, ""))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Имя ветки не может быть пустым");
        verifyNoInteractions(projectRepository);
    }

    @Test
    @DisplayName("Проект не найден или удалён — 404")
    void projectNotFound() {
        when(projectRepository.findByIdAndDeleteDateIsNull(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Проект не найден или удален");
    }

    @Test
    @DisplayName("Use case нет, другого проекта или удалён — 404")
    void useCaseNotFound() {
        when(useCaseRepository.findByIdAndProjectIdAndDeletedDateIsNull(USE_CASE_ID, PROJECT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Use case не найден");
    }

    @Test
    @DisplayName("Ветки нет — 404, ветка не создаётся")
    void branchNotFound() {
        when(artifactBranchRepository.findByArtifactTypeAndArtifactIdAndNameIgnoreCase(anyString(), anyInt(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOperationsTree(PROJECT_ID, USE_CASE_ID, "design"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Ветка use case не найдена");
        verify(artifactBranchRepository, never()).save(any());
        verifyNoInteractions(usOperationRelationRepository);
    }

    @Test
    @DisplayName("Ошибка продуктов пробрасывается")
    void productErrorIsPropagated() {
        when(usOperationRelationRepository.findAllByUseCaseBranchId(BRANCH_ID)).thenReturn(List.of(
                step(1, 0, null, 20)));
        doThrow(new ExternalServiceException(HttpStatus.BAD_GATEWAY, "Сервис продуктов недоступен"))
                .when(productClient).getDiscoveredOperationsByIds(any());

        assertThatThrownBy(() -> service.getOperationsTree(PROJECT_ID, USE_CASE_ID, null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Сервис продуктов недоступен");
    }

    private UsOperationRelation step(Integer id, Integer order, Integer caller, Integer callee) {
        return UsOperationRelation.builder()
                .id(id)
                .useCaseBranchId(BRANCH_ID)
                .order(order)
                .reqOperationId(caller)
                .relatedReqOperationId(callee)
                .build();
    }

    private DiscoveredOperationByIdsDTO operation(Integer id) {
        return DiscoveredOperationByIdsDTO.builder()
                .id(id)
                .name("op-" + id)
                .discoveredInterface(DiscoveredInterfaceByIdsDTO.builder().id(5).name("orders-api").build())
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Integer> capturedIds() {
        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(productClient).getDiscoveredOperationsByIds(captor.capture());
        return captor.getValue();
    }
}
