package com.example.backend_service.habits;

/*
 * Tests updated to reflect changes made to join/visibility behavior and
 * member name enrichment. These tests assert that joined groups are visible
 * to members and that placeholder member names are resolved to real names.
 */

import com.example.backend_service.Student;
import com.example.backend_service.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GroupHabitControllerTest {

    private GroupHabitRepository groupHabitRepository;
    private StudentRepository studentRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        groupHabitRepository = Mockito.mock(GroupHabitRepository.class);
        studentRepository = Mockito.mock(StudentRepository.class);
        GroupHabitController controller = new GroupHabitController(groupHabitRepository, studentRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getGroupHabits_returnsList() throws Exception {
        GroupHabit g = new GroupHabit();
        g.setId(5L);
        g.setStudentId(10L);
        g.setName("Study Buddies");

        when(groupHabitRepository.findVisibleToStudent(10L, "10")).thenReturn(List.of(g));

        mockMvc.perform(get("/api/students/10/group-habits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Study Buddies"));
    }

    @Test
    void getGroupHabits_resolvesPlaceholderMemberNames() throws Exception {
        GroupHabit g = new GroupHabit();
        g.setId(7L);
        g.setStudentId(10L);
        g.setName("Health Circle");
        g.setMembersJson("[{\"id\":\"16\",\"name\":\"Member 16\",\"role\":\"member\",\"joinedAt\":\"2026-01-01T00:00:00\"}]");

        Student s = new Student();
        s.setId(16L);
        s.setName("Nimal");

        when(groupHabitRepository.findVisibleToStudent(10L, "10")).thenReturn(List.of(g));
        when(studentRepository.findAllById(java.util.Set.of(16L))).thenReturn(List.of(s));

        mockMvc.perform(get("/api/students/10/group-habits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].members[0].name").value("Nimal"));
    }

    @Test
    void joinGroupHabit_matchesCodeIgnoringCase() throws Exception {
        GroupHabit g = new GroupHabit();
        g.setId(5L);
        g.setStudentId(10L);
        g.setName("Study Buddies");
        g.setCode("AB12CD");
        g.setMembersJson("[]");

        when(groupHabitRepository.findByCodeIgnoreCase("AB12CD")).thenReturn(java.util.Optional.of(g));
        when(groupHabitRepository.save(g)).thenReturn(g);
        Student student = new Student();
        student.setId(99L);
        student.setName("Alice");
        when(studentRepository.findById(99L)).thenReturn(java.util.Optional.of(student));

        mockMvc.perform(post("/api/students/99/group-habits/join").param("code", "ab12cd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AB12CD"));

        verify(groupHabitRepository, never()).findByCode("ab12cd");
    }
}
