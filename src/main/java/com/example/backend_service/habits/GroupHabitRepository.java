package com.example.backend_service.habits;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing GroupHabit entities.
 */
@Repository
public interface GroupHabitRepository extends JpaRepository<GroupHabit, Long> {
    /**
     * Finds all group habits where the given student is a member or owner.
     */
    List<GroupHabit> findByStudentId(Long studentId);

    /**
     * Returns groups visible to a student: owner groups or groups where the student appears in
     * the stored `membersJson` array.
     *
     * Notes:
     * - Uses a SQL `LIKE` against `membersJson` to match the JSON fragment `"id":"<studentId>"`.
     * - Avoids applying LOWER()/function calls to the CLOB column for Hibernate compatibility.
     */
    @Query("SELECT g FROM GroupHabit g WHERE g.studentId = :studentId OR COALESCE(g.membersJson, '') LIKE CONCAT('%\"id\":\"', :studentIdText, '\"%')")
    List<GroupHabit> findVisibleToStudent(@Param("studentId") Long studentId, @Param("studentIdText") String studentIdText);

    /**
     * Finds a group habit by its invite code.
     */
    Optional<GroupHabit> findByCode(String code);

    /**
     * Finds a group habit by its invite code, ignoring case differences.
     */
    Optional<GroupHabit> findByCodeIgnoreCase(String code);

    /**
     * Deletes all group habits owned by a student.
     */
    void deleteByStudentId(Long studentId);
}