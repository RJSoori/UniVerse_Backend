package com.example.backend_service.todo;

import com.example.backend_service.todo.Todo;
import com.example.backend_service.todo.TodoController;
import com.example.backend_service.todo.TodoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TodoControllerTest {

    private TodoRepository repository;
    private TodoController controller;
    private MockMvc mockMvc;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        repository = Mockito.mock(TodoRepository.class);
        controller = new TodoController(repository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createTodo_success() throws Exception {
        Todo t = new Todo();
        t.setStudentId(1L);
        t.setTitle("Test");
        when(repository.save(Mockito.any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/todos")
                .contentType("application/json")
                .content(mapper.writeValueAsString(t)))
                .andExpect(status().isOk());
    }

    @Test
    void getTodosByStudent_returnsOk() throws Exception {
        when(repository.findByStudentId(2L)).thenReturn(List.of(new Todo()));
        mockMvc.perform(get("/api/todos/student/2")).andExpect(status().isOk());
    }
}
