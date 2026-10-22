package com.example.backend_service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FocusRepository extends JpaRepository<Focus, Long> {

    //Find a record for a specific user on a specific day
    Optional<Focus> findByUserIdAndFocusDate(String userId, LocalDate focusDate);

    //Filters the sum of minutes for analytics charts
    @Query("SELECT f.focusDate, SUM(f.totalMinutes) FROM Focus f WHERE f.userId = :userId GROUP BY f.focusDate")
    List<Object[]> findTotalMinutesByDate(@Param("userId") String userId);
}