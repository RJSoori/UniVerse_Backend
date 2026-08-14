package com.example.backend_service.habits;

import com.example.backend_service.common.exception.ForbiddenException;
import com.example.backend_service.common.exception.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students/{studentId}/habits")
public class PersonalHabitController {

    @Autowired
    private PersonalHabitRepository habitRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalHabitDto createHabit(
            @AuthenticationPrincipal Long authStudentId,
            @RequestBody PersonalHabitDto dto) {
        PersonalHabit entity = convertToEntity(dto);
        entity.setStudentId(authStudentId);
        return convertToDto(habitRepository.save(entity));
    }

    @GetMapping
    public ResponseEntity<List<PersonalHabitDto>> getHabits(@AuthenticationPrincipal Long authStudentId) {
        List<PersonalHabit> entities = habitRepository.findByStudentId(authStudentId);
        return ResponseEntity.ok(entities.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    @PutMapping("/{habitId}")
    public ResponseEntity<PersonalHabitDto> updateHabit(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable Long habitId,
            @RequestBody PersonalHabitDto dto) {
        PersonalHabit existing = habitRepository.findById(habitId)
                .orElseThrow(NotFoundException::new);
        if (!authStudentId.equals(existing.getStudentId())) {
            throw new ForbiddenException();
        }
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
        return ResponseEntity.ok(convertToDto(habitRepository.save(existing)));
    }

    @DeleteMapping("/{habitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHabit(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable Long habitId) {
        PersonalHabit existing = habitRepository.findById(habitId)
                .orElseThrow(NotFoundException::new);
        if (!authStudentId.equals(existing.getStudentId())) {
            throw new ForbiddenException();
        }
        habitRepository.deleteById(habitId);
    }

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
