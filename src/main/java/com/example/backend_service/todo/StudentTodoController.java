package com.example.backend_service.todo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students/{studentId}/todos")
public class StudentTodoController {

    private final StudentTodoStateRepository repository;
    private final ObjectMapper objectMapper;

    public StudentTodoController(StudentTodoStateRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getTodos(@AuthenticationPrincipal Long authStudentId) {
        return ResponseEntity.ok(
                repository.findByStudentId(authStudentId)
                        .map(StudentTodoState::getPayloadJson)
                        .map(this::readPayload)
                        .orElse(Collections.emptyList())
        );
    }

    @PutMapping
    public ResponseEntity<List<Map<String, Object>>> saveTodos(
            @AuthenticationPrincipal Long authStudentId,
            @RequestBody List<Map<String, Object>> todos
    ) {
        StudentTodoState state = repository.findByStudentId(authStudentId)
                .orElseGet(StudentTodoState::new);
        state.setStudentId(authStudentId);
        state.setPayloadJson(writePayload(todos));
        repository.save(state);
        return ResponseEntity.ok(todos == null ? Collections.emptyList() : todos);
    }

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

    private String writePayload(List<Map<String, Object>> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Collections.emptyList() : payload);
        } catch (Exception ignored) {
            return "[]";
        }
    }
}
