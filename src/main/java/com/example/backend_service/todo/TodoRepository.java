package com.example.backend_service.todo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for managing Todo entities.
 * Provides database operations for individual todo items.
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    /**
     * Finds all todos for a specific student.
     */
    List<Todo> findByStudentId(Long studentId);

    /**
     * Deletes all todos for a specific student.
     */
    void deleteByStudentId(Long studentId);
}
