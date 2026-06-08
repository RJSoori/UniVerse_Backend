package com.example.backend_service.todo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing To-Do items.
 * Handles CRUD operations for student todo lists.
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoRepository todoRepository;

    public TodoController(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    /**
     * Fetches all todos for a specific student.
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Todo>> getTodosByStudent(@PathVariable Long studentId) {
        List<Todo> todos = todoRepository.findByStudentId(studentId);
        return ResponseEntity.ok(todos);
    }

    /**
     * Creates a new todo item.
     */
    @PostMapping
    public ResponseEntity<Todo> createTodo(@RequestBody Todo todo) {
        if (todo.getStudentId() == null || todo.getStudentId() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        if (todo.getTitle() == null || todo.getTitle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        Todo savedTodo = todoRepository.save(todo);
        return ResponseEntity.ok(savedTodo);
    }

    /**
     * Updates an existing todo item.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Todo> updateTodo(@PathVariable Long id, @RequestBody Todo updateData) {
        Optional<Todo> optional = todoRepository.findById(id);
        
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Todo todo = optional.get();
        
        // Update allowed fields
        if (updateData.getTitle() != null && !updateData.getTitle().isBlank()) {
            todo.setTitle(updateData.getTitle());
        }
        if (updateData.getDescription() != null) {
            todo.setDescription(updateData.getDescription());
        }
        if (updateData.getDueDate() != null) {
            todo.setDueDate(updateData.getDueDate());
        }
        if (updateData.getDueTime() != null) {
            todo.setDueTime(updateData.getDueTime());
        }
        if (updateData.getDurationMinutes() != null) {
            todo.setDurationMinutes(updateData.getDurationMinutes());
        }
        if (updateData.getPriority() != null) {
            todo.setPriority(updateData.getPriority());
        }
        if (updateData.getCompleted() != null) {
            todo.setCompleted(updateData.getCompleted());
        }
        if (updateData.getReminderEnabled() != null) {
            todo.setReminderEnabled(updateData.getReminderEnabled());
        }

        Todo savedTodo = todoRepository.save(todo);
        return ResponseEntity.ok(savedTodo);
    }

    /**
     * Deletes a specific todo item.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTodo(@PathVariable Long id) {
        if (!todoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        todoRepository.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Todo deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk saves/replaces all todos for a student.
     * Deletes old todos and creates new ones.
     */
    @PutMapping("/student/{studentId}/bulk")
    public ResponseEntity<List<Todo>> bulkSaveTodos(@PathVariable Long studentId, @RequestBody List<Todo> todos) {
        // Delete all existing todos for this student
        todoRepository.deleteByStudentId(studentId);

        // Set student ID for all new todos
        for (Todo todo : todos) {
            todo.setStudentId(studentId);
        }

        // Save all new todos
        List<Todo> savedTodos = todoRepository.saveAll(todos);
        return ResponseEntity.ok(savedTodos);
    }
}
