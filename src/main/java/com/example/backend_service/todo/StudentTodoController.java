package com.example.backend_service.todo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages To-Do list requests for students.
 */
@RestController
@RequestMapping("/api/students/{studentId}/todos")
public class StudentTodoController {

    private final StudentTodoStateRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Initializes the controller with necessary tools.
     */
    public StudentTodoController(StudentTodoStateRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches the To-Do list for a specific student.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getTodos(@PathVariable Long studentId) {
        return ResponseEntity.ok(
                repository.findByStudentId(studentId)
                        .map(StudentTodoState::getPayloadJson)
                        .map(this::readPayload)
                        .orElse(Collections.emptyList())
        );
    }

    /**
     * Saves or updates a student's full To-Do list.
     */
    @PutMapping
    public ResponseEntity<List<Map<String, Object>>> saveTodos(
            @PathVariable Long studentId,
            @RequestBody List<Map<String, Object>> todos
    ) {
        StudentTodoState state = repository.findByStudentId(studentId)
                .orElseGet(StudentTodoState::new);
        state.setStudentId(studentId);
        state.setPayloadJson(writePayload(todos));
        repository.save(state);
        return ResponseEntity.ok(todos == null ? Collections.emptyList() : todos);
    }

    /**
     * Converts a JSON string into a list of To-Do items.
     */
    private List<Map<String, Object>> readPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * Converts a list of To-Do items into a JSON string.
     */
    private String writePayload(List<Map<String, Object>> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Collections.emptyList() : payload);
        } catch (Exception ignored) {
            return "[]";
        }
    }
}