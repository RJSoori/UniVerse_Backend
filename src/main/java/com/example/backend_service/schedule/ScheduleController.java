package com.example.backend_service.schedule;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
public class ScheduleController {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    private final ScheduleEventRepository repository;

    public ScheduleController(ScheduleEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/students/{studentId}/schedule")
    public List<ScheduleEvent> getSchedule(@AuthenticationPrincipal Long authStudentId) {
        logger.info("GET /api/students/{}/schedule", authStudentId);
        return repository.findByStudentIdOrderByDateAsc(authStudentId);
    }

    @PostMapping("/api/students/{studentId}/schedule")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleEvent createEvent(
            @AuthenticationPrincipal Long authStudentId,
            @RequestBody ScheduleEvent event) {
        logger.info("POST /api/students/{}/schedule - creating event: {}", authStudentId, event.getTitle());
        event.setStudentId(authStudentId);
        event.setId(null);
        ScheduleEvent saved = repository.save(event);
        logger.info("Event saved with id={}", saved.getId());
        return saved;
    }

    @PutMapping("/api/students/{studentId}/schedule/{id}")
    public ResponseEntity<ScheduleEvent> updateEvent(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable Long id,
            @RequestBody ScheduleEvent update) {
        logger.info("PUT /api/students/{}/schedule/{} - updating event", authStudentId, id);
        ScheduleEvent ev = repository.findById(id).orElse(null);
        if (ev == null) {
            logger.warn("Event not found: id={}", id);
            return ResponseEntity.notFound().build();
        }
        if (!authStudentId.equals(ev.getStudentId())) {
            logger.warn("Authorization check failed: studentId mismatch");
            return ResponseEntity.status(403).build();
        }
        ev.setTitle(update.getTitle());
        ev.setDate(update.getDate());
        ev.setStartTime(update.getStartTime());
        ev.setEndTime(update.getEndTime());
        ev.setDescription(update.getDescription());
        ev.setType(update.getType());
        ScheduleEvent saved = repository.save(ev);
        logger.info("Event updated: id={}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/api/students/{studentId}/schedule/{id}")
    public ResponseEntity<Void> deleteEvent(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable Long id) {
        logger.info("DELETE /api/students/{}/schedule/{}", authStudentId, id);
        ScheduleEvent ev = repository.findById(id).orElse(null);
        if (ev == null) {
            logger.warn("Event not found for deletion: id={}", id);
            return ResponseEntity.notFound().build();
        }
        if (!authStudentId.equals(ev.getStudentId())) {
            logger.warn("Authorization check failed for delete: studentId mismatch");
            return ResponseEntity.status(403).build();
        }
        repository.deleteById(id);
        logger.info("Event deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }
}
