package com.example.backend_service.habits;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PersonalHabitControllerTest {

    private PersonalHabitRepository repository;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        repository = Mockito.mock(PersonalHabitRepository.class);
        PersonalHabitController controller = new PersonalHabitController();
        // inject repository via reflection since controller autowires in production
        try {
            java.lang.reflect.Field f = PersonalHabitController.class.getDeclaredField("habitRepository");
            f.setAccessible(true);
            f.set(controller, repository);
        } catch (Exception ignored) {}
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getHabits_returnsSaved() throws Exception {
        PersonalHabit h = new PersonalHabit();
        h.setId(1L);
        h.setStudentId(3L);
        h.setName("Meditate");

        when(repository.findByStudentId(3L)).thenReturn(List.of(h));

        mockMvc.perform(get("/api/students/3/habits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Meditate"));
    }

    @Test
    void createHabit_returnsCreated() throws Exception {
        PersonalHabitDto dto = new PersonalHabitDto();
        dto.setName("Run");

        PersonalHabit saved = new PersonalHabit();
        saved.setId(7L);
        saved.setName("Run");
        when(repository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/students/4/habits")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }
}
