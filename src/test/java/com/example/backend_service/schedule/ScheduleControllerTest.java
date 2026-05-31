package com.example.backend_service.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ScheduleControllerTest {

    private ScheduleEventRepository repository;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        repository = Mockito.mock(ScheduleEventRepository.class);
        ScheduleController controller = new ScheduleController(repository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // enable Java time module for LocalDate serialization
        try {
            objectMapper.findAndRegisterModules();
        } catch (Exception ignored) {}
    }

    @Test
    void getSchedule_returnsEvents() throws Exception {
        ScheduleEvent e = new ScheduleEvent();
        e.setId(2L);
        e.setStudentId(5L);
        e.setTitle("Exam");
        e.setDate(LocalDate.now());

        when(repository.findByStudentIdOrderByDateAsc(5L)).thenReturn(List.of(e));

        mockMvc.perform(get("/api/students/5/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Exam"));
    }

    @Test
    void createEvent_savesAndReturns() throws Exception {
        ScheduleEvent input = new ScheduleEvent();
        input.setTitle("Meeting");
        input.setDate(LocalDate.now().plusDays(1));

        ScheduleEvent saved = new ScheduleEvent();
        saved.setId(9L);
        saved.setTitle("Meeting");
        saved.setStudentId(8L);

        when(repository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/students/8/schedule")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.title").value("Meeting"));
    }
}
