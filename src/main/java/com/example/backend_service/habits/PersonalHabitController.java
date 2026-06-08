package com.example.backend_service.habits;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles requests for personal habits that one student tracks alone.
 * Students can add, view, update, and delete their own habits.
 */
@RestController
@RequestMapping("/api/students/{studentId}/habits")
@CrossOrigin(origins = "*")
public class PersonalHabitController {

    @Autowired
    private PersonalHabitRepository habitRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Saves a new habit for a student.
     */
    @PostMapping
    public ResponseEntity<PersonalHabitDto> createHabit(@PathVariable @NonNull Long studentId, @RequestBody @NonNull PersonalHabitDto dto) {
        final PersonalHabit entity = convertToEntity(dto);
        entity.setStudentId(studentId);
        PersonalHabit saved = habitRepository.save(entity);
        return ResponseEntity.ok(convertToDto(saved));
    }

    /**
     * Gets a list of all habits for a student.
     */
    @GetMapping
    public ResponseEntity<List<PersonalHabitDto>> getHabits(@PathVariable @NonNull Long studentId) {
        List<PersonalHabit> entities = habitRepository.findByStudentId(studentId);
        List<PersonalHabitDto> dtos = entities.stream().map(this::convertToDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Updates an existing habit's details.
     */
    @PutMapping("/{habitId}")
    public ResponseEntity<PersonalHabitDto> updateHabit(@PathVariable @NonNull Long studentId, @PathVariable @NonNull Long habitId, @RequestBody @NonNull PersonalHabitDto dto) {
        return habitRepository.findById(habitId).map(existing -> {
            existing.setName(dto.getName());
            existing.setDescription(dto.getDescription());
            existing.setColor(dto.getColor());
            existing.setIconId(dto.getIconId());
            existing.setCategory(dto.getCategory());
            existing.setFocusArea(dto.getFocusArea());
            
            try {
                existing.setCompletedDatesJson(objectMapper.writeValueAsString(dto.getCompletedDates()));
            } catch (JsonProcessingException e) {
                existing.setCompletedDatesJson("[]");
            }
            
            PersonalHabit updated = habitRepository.save(existing);
            return ResponseEntity.ok(convertToDto(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Removes a habit from the database.
     */
    @DeleteMapping("/{habitId}")
    public ResponseEntity<Void> deleteHabit(@PathVariable @NonNull Long habitId) {
        habitRepository.deleteById(habitId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Converts a data object into a database entity.
     */
    private PersonalHabit convertToEntity(PersonalHabitDto dto) {
        PersonalHabit entity = new PersonalHabit();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setColor(dto.getColor());
        entity.setIconId(dto.getIconId());
        entity.setCategory(dto.getCategory());
        entity.setFocusArea(dto.getFocusArea());
        try {
            entity.setCompletedDatesJson(objectMapper.writeValueAsString(dto.getCompletedDates()));
        } catch (Exception e) {
            entity.setCompletedDatesJson("[]");
        }
        return entity;
    }

    /**
     * Converts a database entity back into a data object.
     */
    private PersonalHabitDto convertToDto(PersonalHabit entity) {
        PersonalHabitDto dto = new PersonalHabitDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setColor(entity.getColor());
        dto.setIconId(entity.getIconId());
        dto.setCategory(entity.getCategory());
        dto.setFocusArea(entity.getFocusArea());
        try {
            dto.setCompletedDates(objectMapper.readValue(
                    entity.getCompletedDatesJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        } catch (Exception e) {
            dto.setCompletedDates(List.of());
        }
        return dto;
    }
}