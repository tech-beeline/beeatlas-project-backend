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
import ru.beeline.projectbackend.domain.Assessment;
import ru.beeline.projectbackend.domain.AssessmentStatusEnum;
import ru.beeline.projectbackend.domain.AssessmentTc;
import ru.beeline.projectbackend.domain.AssessmentTcDesign;
import ru.beeline.projectbackend.domain.OpenQuestion;
import ru.beeline.projectbackend.domain.Project;
import ru.beeline.projectbackend.domain.RequirementFunc;
import ru.beeline.projectbackend.domain.RequirementNonFunc;
import ru.beeline.projectbackend.dto.AssessmentDetailsResponseDTO;
import ru.beeline.projectbackend.dto.AssessmentResponseDTO;
import ru.beeline.projectbackend.dto.CreateAssessmentRequestDTO;
import ru.beeline.projectbackend.dto.CreateAssessmentResponseDTO;
import ru.beeline.projectbackend.dto.UserProfileShortDTO;
import ru.beeline.projectbackend.exception.AuthServiceException;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private AssessmentStatusEnumRepository assessmentStatusEnumRepository;
    @Mock
    private RequirementFuncRepository requirementFuncRepository;
    @Mock
    private RequirementNonFuncRepository requirementNonFuncRepository;
    @Mock
    private AssessmentTcRepository assessmentTcRepository;
    @Mock
    private AssessmentTcDesignRepository assessmentTcDesignRepository;
    @Mock
    private OpenQuestionRepository openQuestionRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserClient userClient;

    @InjectMocks
    private AssessmentService assessmentService;

    @Test
    void getAssessments_enrichesOwnerStatusAndCounts() {
        Assessment assessment = assessment(123, 10, 42, 7,
                LocalDateTime.of(2026, 8, 19, 12, 0),
                LocalDateTime.of(2026, 8, 19, 14, 0));

        when(assessmentRepository.findAllFiltered(10, 42, 7)).thenReturn(List.of(assessment));
        when(userClient.findUserProfiles(anyList())).thenReturn(List.of(
                UserProfileShortDTO.builder().id(42).fullName("Иван Иванов").build()));
        when(assessmentStatusEnumRepository.findAllById(Set.of(7)))
                .thenReturn(List.of(new AssessmentStatusEnum(7, "Draft", "Черновик")));
        when(requirementFuncRepository.countGroupedByAssessmentId(List.of(123)))
                .thenReturn(counts(123, 2L));
        when(requirementNonFuncRepository.countGroupedByAssessmentId(List.of(123)))
                .thenReturn(counts(123, 1L));
        when(assessmentTcRepository.countGroupedByAssessmentId(List.of(123)))
                .thenReturn(counts(123, 3L));
        when(assessmentTcDesignRepository.countGroupedByAssessmentId(List.of(123)))
                .thenReturn(counts(123, 1L));
        when(openQuestionRepository.countGroupedByAssessmentId(List.of(123)))
                .thenReturn(counts(123, 2L));
        when(assessmentTcRepository.findProductAliasesByAssessmentIds(List.of(123)))
                .thenReturn(List.<Object[]>of(new Object[]{123, "SYS-1"}, new Object[]{123, "SYS-2"}));
        when(assessmentTcDesignRepository.findProductAliasesByAssessmentIds(List.of(123)))
                .thenReturn(List.<Object[]>of(new Object[]{123, "SYS-1"}, new Object[]{123, "SYS-3"}));

        List<AssessmentResponseDTO> result = assessmentService.getAssessments(10, 7, 42);

        assertEquals(1, result.size());
        AssessmentResponseDTO dto = result.getFirst();
        assertEquals(123, dto.getId());
        assertEquals(4, dto.getTcCount());
        assertEquals(3, dto.getProductCount());
        assertEquals(2, dto.getOqCount());
        assertEquals("Иван Иванов", dto.getOwnerName());
    }

    @Test
    void getAssessments_emptyList_doesNotCallAuth() {
        when(assessmentRepository.findAllFiltered(null, null, null)).thenReturn(List.of());

        List<AssessmentResponseDTO> result = assessmentService.getAssessments(null, null, null);

        assertTrue(result.isEmpty());
        verify(userClient, never()).findUserProfiles(anyList());
    }

    @Test
    void getAssessments_missingRelatedRecords_returnsZeroCounts() {
        Assessment assessment = assessment(1, 1, 9, 2,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 2, 11, 0));

        when(assessmentRepository.findAllFiltered(null, null, null)).thenReturn(List.of(assessment));
        when(userClient.findUserProfiles(anyList())).thenReturn(List.of());
        when(assessmentStatusEnumRepository.findAllById(Set.of(2)))
                .thenReturn(List.of(new AssessmentStatusEnum(2, "REQ", null)));
        when(requirementFuncRepository.countGroupedByAssessmentId(List.of(1))).thenReturn(List.of());
        when(requirementNonFuncRepository.countGroupedByAssessmentId(List.of(1))).thenReturn(List.of());
        when(assessmentTcRepository.countGroupedByAssessmentId(List.of(1))).thenReturn(List.of());
        when(assessmentTcDesignRepository.countGroupedByAssessmentId(List.of(1))).thenReturn(List.of());
        when(openQuestionRepository.countGroupedByAssessmentId(List.of(1))).thenReturn(List.of());
        when(assessmentTcRepository.findProductAliasesByAssessmentIds(List.of(1))).thenReturn(List.of());
        when(assessmentTcDesignRepository.findProductAliasesByAssessmentIds(List.of(1))).thenReturn(List.of());

        AssessmentResponseDTO dto = assessmentService.getAssessments(null, null, null).getFirst();

        assertEquals(0, dto.getTcCount());
        assertEquals(0, dto.getProductCount());
        assertEquals(0, dto.getOqCount());
    }

    @Test
    void getAssessments_authUnavailable_throwsError() {
        Assessment assessment = assessment(1, 1, 9, 2,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 2, 11, 0));

        when(assessmentRepository.findAllFiltered(null, null, null)).thenReturn(List.of(assessment));
        when(userClient.findUserProfiles(anyList()))
                .thenThrow(new AuthServiceException("Сервис авторизации недоступен"));

        AuthServiceException exception = assertThrows(AuthServiceException.class,
                () -> assessmentService.getAssessments(null, null, null));

        assertEquals("Сервис авторизации недоступен", exception.getMessage());
    }

    @Test
    void getAssessmentById_returnsDetailsWithRelatedData() {
        Assessment assessment = assessment(123, 10, 42, 7,
                LocalDateTime.of(2026, 8, 19, 12, 0),
                LocalDateTime.of(2026, 8, 19, 14, 0));
        assessment.setSource("text");
        assessment.setSourceUrl("https://confluence/page");
        assessment.setRawText("Исходный текст");

        when(assessmentRepository.findByIdAndDeleteDateIsNull(123)).thenReturn(Optional.of(assessment));
        when(userClient.findUserProfiles(anyList())).thenReturn(List.of(
                UserProfileShortDTO.builder().id(42).fullName("Иван Иванов").build()));
        when(assessmentStatusEnumRepository.findById(7))
                .thenReturn(Optional.of(new AssessmentStatusEnum(7, "Draft", "Черновик")));
        when(projectRepository.findById(10)).thenReturn(Optional.of(Project.builder()
                .id(10).name("Мой проект").source("BeeAtlas").uniqueIdent("PROJ.00.00.10")
                .ownerId(1).statusId(1).createdDate(LocalDateTime.of(2026, 1, 1, 0, 0)).build()));
        when(requirementFuncRepository.findByAssessmentId(123)).thenReturn(List.of(
                RequirementFunc.builder().id(1).assessmentId(123).uniqueIdent("FR-1").title("FR title").description("FR desc").build()));
        when(requirementNonFuncRepository.findByAssessmentId(123)).thenReturn(List.of(
                RequirementNonFunc.builder().id(2).assessmentId(123).uniqueIdent("NFR-1").title("NFR title").description("NFR desc").build()));
        when(openQuestionRepository.findByAssessmentId(123)).thenReturn(List.of(
                OpenQuestion.builder().id(3).assessmentId(123).uniqueIdent("OQ-1").questionText("Вопрос?").build()));
        when(assessmentTcRepository.findByAssessmentId(123)).thenReturn(List.of(
                AssessmentTc.builder().id(5).assessmentId(123).tcCode("TC-001")
                        .productAlias("sys-a").productName("System A").parentBcCode("BC-001")
                        .frIds(List.of("FR-1", "FR-2")).build()));
        when(assessmentTcDesignRepository.findByAssessmentId(123)).thenReturn(List.of(
                AssessmentTcDesign.builder().id(6).assessmentId(123).name("Новая TC").description("описание")
                        .productAlias("sys-b").productName("System B").parentBcCode("BC-002")
                        .frIds(List.of("FR-3")).build()));

        AssessmentDetailsResponseDTO dto = assessmentService.getAssessmentById(123);

        assertEquals("TC-001", dto.getTc().getFirst().getTcCode());
        assertEquals("sys-a", dto.getTc().getFirst().getProductAlias());
        assertEquals("Новая TC", dto.getDesignTc().getFirst().getName());
        assertEquals("sys-b", dto.getDesignTc().getFirst().getProductAlias());
    }

    @Test
    void getAssessmentById_notFound_throws404() {
        when(assessmentRepository.findByIdAndDeleteDateIsNull(99)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> assessmentService.getAssessmentById(99));

        assertEquals("Оценка не найдена или удалена", exception.getMessage());
    }

    @Test
    void createAssessment_savesAssessmentAndChildren() {
        CreateAssessmentRequestDTO request = emptyCreateRequest();
        CreateAssessmentRequestDTO.TcItem tc = new CreateAssessmentRequestDTO.TcItem();
        tc.setTcCode("TC-001");
        tc.setProductAlias("sys-a");
        tc.setProductName("System A");
        tc.setParentBcCode("BC-001");
        tc.setFrIds(List.of("FR-1"));
        request.setTc(List.of(tc));

        CreateAssessmentRequestDTO.DesignTcItem designTc = new CreateAssessmentRequestDTO.DesignTcItem();
        designTc.setName("Новая TC");
        designTc.setProductAlias("sys-b");
        request.setDesignTc(List.of(designTc));

        when(projectRepository.findByIdAndDeleteDateIsNull(45))
                .thenReturn(Optional.of(Project.builder().id(45).name("Проект").source("BeeAtlas")
                        .uniqueIdent("PROJ.00.00.45").ownerId(1).statusId(1).createdDate(LocalDateTime.now()).build()));
        when(assessmentStatusEnumRepository.findByName("Done"))
                .thenReturn(Optional.of(new AssessmentStatusEnum(9, "Done", null)));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> {
            Assessment assessment = invocation.getArgument(0);
            assessment.setId(123);
            return assessment;
        });

        CreateAssessmentResponseDTO response = assessmentService.createAssessment(request, 42);

        assertEquals(123, response.getId());
        assertEquals("Done", response.getStatus());
        verify(assessmentTcRepository).saveAll(anyList());
        verify(assessmentTcDesignRepository).saveAll(anyList());
    }

    @Test
    void createAssessment_duplicateTcCodes_mergedWithFrIds() {
        CreateAssessmentRequestDTO request = emptyCreateRequest();
        request.setTc(List.of(
                tcItem("TC-001", List.of("FR-1", "FR-2")),
                tcItem("TC-002", List.of("FR-3")),
                tcItem("TC-001", List.of("FR-2", "NFR-1"))));

        when(projectRepository.findByIdAndDeleteDateIsNull(45))
                .thenReturn(Optional.of(Project.builder().id(45).name("Проект").source("BeeAtlas")
                        .uniqueIdent("PROJ.00.00.45").ownerId(1).statusId(1).createdDate(LocalDateTime.now()).build()));
        when(assessmentStatusEnumRepository.findByName("Done"))
                .thenReturn(Optional.of(new AssessmentStatusEnum(9, "Done", null)));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> {
            Assessment assessment = invocation.getArgument(0);
            assessment.setId(123);
            return assessment;
        });

        assessmentService.createAssessment(request, 42);

        ArgumentCaptor<List<AssessmentTc>> captor = ArgumentCaptor.forClass(List.class);
        verify(assessmentTcRepository).saveAll(captor.capture());
        List<AssessmentTc> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals("TC-001", saved.get(0).getTcCode());
        assertEquals(List.of("FR-1", "FR-2", "NFR-1"), saved.get(0).getFrIds());
        assertEquals("TC-002", saved.get(1).getTcCode());
        assertEquals(List.of("FR-3"), saved.get(1).getFrIds());
    }

    @Test
    void createAssessment_missingUserId_throws400() {
        CreateAssessmentRequestDTO request = emptyCreateRequest();

        ValidationException exception = assertThrows(ValidationException.class,
                () -> assessmentService.createAssessment(request, null));

        assertEquals("Заголовок user-id обязателен", exception.getMessage());
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void createAssessment_invalidImpactLevel_throws400() {
        CreateAssessmentRequestDTO request = emptyCreateRequest();
        request.setImpactLevel("XXL");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> assessmentService.createAssessment(request, 42));

        assertEquals("impactLevel должен быть одним из: S, M, L, XL", exception.getMessage());
    }

    private static CreateAssessmentRequestDTO.TcItem tcItem(String tcCode, List<String> frIds) {
        CreateAssessmentRequestDTO.TcItem item = new CreateAssessmentRequestDTO.TcItem();
        item.setTcCode(tcCode);
        item.setProductAlias("sys-a");
        item.setFrIds(frIds);
        return item;
    }

    private static Assessment assessment(int id, int projectId, int ownerId, int statusId,
                                         LocalDateTime created, LocalDateTime updated) {
        return Assessment.builder()
                .id(id).projectId(projectId).ownerId(ownerId).statusId(statusId)
                .source("text").rawText("raw").taskDescription("Описание задачи").impactLevel("M")
                .createdDate(created).updateDate(updated).build();
    }

    private static List<Object[]> counts(int assessmentId, long count) {
        return List.<Object[]>of(new Object[]{assessmentId, count});
    }

    private static CreateAssessmentRequestDTO emptyCreateRequest() {
        CreateAssessmentRequestDTO request = new CreateAssessmentRequestDTO();
        request.setProjectId(45);
        request.setSource("text");
        request.setRawText("постановка");
        request.setImpactLevel("M");
        request.setRequirementsFunc(List.of());
        request.setRequirementsNonFunc(List.of());
        request.setOpenQuestions(List.of());
        request.setTc(List.of());
        request.setDesignTc(List.of());
        return request;
    }
}
