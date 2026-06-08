package com.example.backend_service.todo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Handles database access for to-do states.
 */
public interface StudentTodoStateRepository extends JpaRepository<StudentTodoState, Long> {
    
    /**
     * Finds the to-do list for one specific student.
     */
    Optional<StudentTodoState> findByStudentId(Long studentId);
}