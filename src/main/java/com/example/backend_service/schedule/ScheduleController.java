package com.example.backend_service.schedule;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * Handles all schedule requests for students.
 * Lets students create, view, update, and delete calendar events.
 */
@RestController
public class ScheduleController {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    private final ScheduleEventRepository repository;

    public ScheduleController(ScheduleEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Gets all events for a student.
     * Returns events sorted by date from earliest to latest.
     */
    @GetMapping("/api/students/{studentId}/schedule")
    public List<ScheduleEvent> getSchedule(@PathVariable Long studentId) {
        logger.info("GET /api/students/{}/schedule", studentId);
        List<ScheduleEvent> events = repository.findByStudentIdOrderByDateAsc(studentId);
        logger.info("Returning {} events for studentId {}", events.size(), studentId);
        return events;
    }

    /**
     * Creates a new event for a student.
     * The event is saved with the student ID and current timestamp.
     */
    @PostMapping("/api/students/{studentId}/schedule")
    public ScheduleEvent createEvent(@PathVariable Long studentId, @RequestBody ScheduleEvent event) {
        logger.info("POST /api/students/{}/schedule - creating event: {}", studentId, event.getTitle());
        event.setStudentId(studentId);
        event.setId(null);
        ScheduleEvent saved = repository.save(event);
        logger.info("Event saved with id={}, studentId={}", saved.getId(), saved.getStudentId());
        return saved;
    }

    /**
     * Updates an event for a student.
     * Only the student who created the event can update it.
     */
    @PutMapping("/api/students/{studentId}/schedule/{id}")
    public ResponseEntity<ScheduleEvent> updateEvent(@PathVariable @NonNull Long studentId, @PathVariable @NonNull Long id, @RequestBody ScheduleEvent update) {
        logger.info("PUT /api/students/{}/schedule/{} - updating event", studentId, id);
        Optional<ScheduleEvent> existing = repository.findById(id);
        if (existing.isEmpty()) {
            logger.warn("Event not found: id={}", id);
            return ResponseEntity.notFound().build();
        }

        ScheduleEvent ev = existing.get();
        if (!studentId.equals(ev.getStudentId())) {
            logger.warn("Authorization check failed: studentId mismatch");
            return ResponseEntity.status(403).build();
        }

        ev.setTitle(update.getTitle());
        ev.setDate(update.getDate());
        ev.setStartTime(update.getStartTime());
        ev.setEndTime(update.getEndTime());
        ev.setDescription(update.getDescription());

        ScheduleEvent saved = repository.save(ev);
        logger.info("Event updated: id={}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * Deletes an event for a student.
     * Only the student who created the event can delete it.
     */
    @DeleteMapping("/api/students/{studentId}/schedule/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable @NonNull Long studentId, @PathVariable @NonNull Long id) {
        logger.info("DELETE /api/students/{}/schedule/{}", studentId, id);
        Optional<ScheduleEvent> existing = repository.findById(id);
        if (existing.isEmpty()) {
            logger.warn("Event not found for deletion: id={}", id);
            return ResponseEntity.notFound().build();
        }
        ScheduleEvent ev = existing.get();
        if (!studentId.equals(ev.getStudentId())) {
            logger.warn("Authorization check failed for delete: studentId mismatch");
            return ResponseEntity.status(403).build();
        }
        repository.deleteById(id);
        logger.info("Event deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }
}