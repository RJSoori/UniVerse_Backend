package com.example.backend_service.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, Long> {
    List<ScheduleEvent> findByStudentIdOrderByDateAsc(Long studentId);
}