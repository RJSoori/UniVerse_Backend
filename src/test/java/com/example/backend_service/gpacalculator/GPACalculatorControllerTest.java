package com.example.backend_service.gpacalculator;

import com.example.backend_service.common.exception.GlobalExceptionHandler;
import com.example.backend_service.gpacalculator.model.GPASemester;
import com.example.backend_service.gpacalculator.model.GPASubject;
import com.example.backend_service.gpacalculator.model.GradeEnum;
import com.example.backend_service.gpacalculator.repository.GPASemesterRepository;
import com.example.backend_service.gpacalculator.repository.GPASettingsRepository;
import com.example.backend_service.gpacalculator.repository.GPASubjectRepository;
import com.example.backend_service.gpacalculator.service.GPACalculatorService;
import com.example.backend_service.gpacalculator.web.GPACalculatorController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers {@link GPACalculatorController}'s subject create/delete endpoints - in particular a
 * regression guard for the bug where {@code deleteSubject} appeared to succeed (200/204, and a
 * "deleted subject id=..." log line) but never actually removed the row: {@code
 * GPASemester.subjects} is a {@code fetch = EAGER} collection, so the ownership-check {@code
 * findById} call in {@code deleteSubject} also eagerly loads the parent semester's whole subjects
 * collection into the request-scoped persistence context (open-in-view), and the plain inherited
 * {@code subjectRepository.deleteById(...)} silently lost the race against that still-attached,
 * cascade-ALL parent collection at flush time - confirmed live against the real Aiven MySQL
 * instance: the same subject id could be "deleted" twice and successfully updated afterward. The
 * fix routes the delete through {@link GPASubjectRepository#deleteByIdImmediate}, a bulk JPQL
 * DELETE that bypasses that entity-graph reconciliation entirely.
 */
class GPACalculatorControllerTest {

    private GPASemesterRepository semesterRepository;
    private GPASubjectRepository subjectRepository;
    private GPASettingsRepository settingsRepository;
    private GPACalculatorController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        semesterRepository = Mockito.mock(GPASemesterRepository.class);
        subjectRepository = Mockito.mock(GPASubjectRepository.class);
        settingsRepository = Mockito.mock(GPASettingsRepository.class);
        GPACalculatorService service = new GPACalculatorService(semesterRepository, subjectRepository, settingsRepository);
        controller = new GPACalculatorController(service, semesterRepository, subjectRepository, settingsRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsStudent(long studentId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        studentId, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }

    private GPASemester semester(String id, Long studentId) {
        GPASemester semester = new GPASemester("Year 1", "Semester 1", studentId);
        semester.setId(id);
        return semester;
    }

    private GPASubject subject(String id, GPASemester semester, Long studentId) {
        GPASubject subject = new GPASubject("Maths", 3.0, GradeEnum.A, true, studentId, semester);
        subject.setId(id);
        return subject;
    }

    @Test
    void deleteSubject_ownedSubject_usesBulkDeleteNotDeleteById() throws Exception {
        authenticateAsStudent(6L);
        GPASemester sem = semester("sem-1", 6L);
        GPASubject subj = subject("subj-1", sem, 6L);
        when(subjectRepository.findById("subj-1")).thenReturn(Optional.of(subj));

        mockMvc.perform(delete("/api/gpa/subjects/subj-1"))
                .andExpect(status().isNoContent());

        // The actual regression: deleteById() silently fails to persist here because the
        // ownership check above eagerly loads the parent semester's whole (cascade-ALL,
        // orphanRemoval) subjects collection into the same persistence context. Only the bulk
        // delete reliably removes the row.
        verify(subjectRepository).deleteByIdImmediate("subj-1");
        verify(subjectRepository, never()).deleteById(any());
        verify(subjectRepository, never()).delete(any());
    }

    @Test
    void deleteSubject_notOwnedByRequester_returnsNotFoundAndNeverDeletes() throws Exception {
        authenticateAsStudent(99L); // requester is not the subject's owner
        GPASemester sem = semester("sem-1", 6L);
        GPASubject subj = subject("subj-1", sem, 6L);
        when(subjectRepository.findById("subj-1")).thenReturn(Optional.of(subj));

        mockMvc.perform(delete("/api/gpa/subjects/subj-1"))
                .andExpect(status().is4xxClientError());

        verify(subjectRepository, never()).deleteByIdImmediate(any());
        verify(subjectRepository, never()).deleteById(any());
    }

    @Test
    void createSubject_stillSavesThroughTheRepositoryAsBefore() throws Exception {
        authenticateAsStudent(6L);
        GPASemester sem = semester("sem-1", 6L);
        when(semesterRepository.findById("sem-1")).thenReturn(Optional.of(sem));
        when(subjectRepository.save(any(GPASubject.class))).thenAnswer(inv -> {
            GPASubject s = inv.getArgument(0);
            s.setId("new-subj-id");
            return s;
        });

        String body = "{\"name\":\"Physics\",\"credits\":3,\"grade\":\"A\",\"isGpa\":true,\"semester\":{\"id\":\"sem-1\"}}";

        mockMvc.perform(post("/api/gpa/subjects")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        verify(subjectRepository, times(1)).save(any(GPASubject.class));
    }
}
