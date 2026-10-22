package com.example.backend_service.todo;

import com.example.backend_service.common.exception.ForbiddenException;
import com.example.backend_service.common.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoRepository todoRepository;

    public TodoController(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @GetMapping("/student/{studentId}")
    public List<Todo> getTodosByStudent(@AuthenticationPrincipal Long authStudentId) {
        return todoRepository.findByStudentId(authStudentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Todo createTodo(@AuthenticationPrincipal Long authStudentId, @RequestBody Todo todo) {
        if (todo.getTitle() == null || todo.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        validateDueDate(todo.getDueDate());
        todo.setId(null);
        todo.setStudentId(authStudentId);
        return todoRepository.save(todo);
    }

    @PutMapping("/{id}")
    public Todo updateTodo(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable Long id,
            @RequestBody Todo updateData) {
        Todo todo = todoRepository.findById(id).orElseThrow(NotFoundException::new);
        if (!authStudentId.equals(todo.getStudentId())) {
            throw new ForbiddenException();
        }
        validateDueDate(updateData.getDueDate());
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
        return todoRepository.save(todo);
    }

    private void validateDueDate(LocalDate dueDate) {
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("dueDate cannot be in the past");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTodo(@AuthenticationPrincipal Long authStudentId, @PathVariable Long id) {
        Todo todo = todoRepository.findById(id).orElseThrow(NotFoundException::new);
        if (!authStudentId.equals(todo.getStudentId())) {
            throw new ForbiddenException();
        }
        todoRepository.deleteById(id);
    }

    @Transactional
    @PutMapping("/student/{studentId}/bulk")
    public List<Todo> bulkSaveTodos(
            @AuthenticationPrincipal Long authStudentId,
            @RequestBody List<Todo> todos) {
        todoRepository.deleteByStudentId(authStudentId);
        todos.forEach(todo -> todo.setStudentId(authStudentId));
        return todoRepository.saveAll(todos);
    }
}
