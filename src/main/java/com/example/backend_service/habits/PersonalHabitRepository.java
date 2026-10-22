package com.example.backend_service.habits;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Handles database operations for habits.
 */
@Repository
public interface PersonalHabitRepository extends JpaRepository<PersonalHabit, Long> {
    
    /**
     * Finds all habits that belong to a specific student.
     */
    List<PersonalHabit> findByStudentId(Long studentId);
}