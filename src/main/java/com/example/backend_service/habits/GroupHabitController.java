package com.example.backend_service.habits;

import com.example.backend_service.Student;
import com.example.backend_service.StudentRepository;
import com.example.backend_service.common.exception.ForbiddenException;
import com.example.backend_service.common.exception.NotFoundException;
import com.example.backend_service.notifications.PushNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for group habits shared by multiple students.
 *
 * Changes implemented recently:
 * - Join-by-code: supports case-insensitive/trimmed invite codes and persists new members
 *   and their memberProgress reliably.
 * - Visibility: group listing returns groups visible to a student (owner OR listed member),
 *   preventing a joined group from disappearing after a refresh.
 * - Member names: placeholder names like "Member 16" are enriched by resolving student
 *   records on read so UIs show real names when available.
 *
 * The controller uses {@link GroupHabitRepository#findVisibleToStudent} for visibility
 * and {@link StudentRepository} to resolve member names.
 */
@RestController
@RequestMapping("/api/students/{studentId}/group-habits")
public class GroupHabitController {
    private static final Logger logger = LoggerFactory.getLogger(GroupHabitController.class);

    private final GroupHabitRepository groupHabitRepository;
    private final StudentRepository studentRepository;
    private final PushNotificationService pushNotificationService;
    private final GroupHabitInviteService groupHabitInviteService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroupHabitController(GroupHabitRepository groupHabitRepository, StudentRepository studentRepository,
            PushNotificationService pushNotificationService, GroupHabitInviteService groupHabitInviteService) {
        this.groupHabitRepository = groupHabitRepository;
        this.studentRepository = studentRepository;
        this.pushNotificationService = pushNotificationService;
        this.groupHabitInviteService = groupHabitInviteService;
    }

    /**
     * Creates a new group habit.
     */
    @PostMapping
    public ResponseEntity<GroupHabitDto> createGroupHabit(
            @AuthenticationPrincipal Long authStudentId,
            @RequestBody GroupHabitDto dto) {
        logger.info("POST /api/students/{}/group-habits - creating: {}", authStudentId, dto.getName());
        GroupHabit entity = convertToEntity(dto);
        entity.setStudentId(authStudentId);
        GroupHabit saved = groupHabitRepository.save(entity);
        logger.info("Group created with id={}", saved.getId());
        return ResponseEntity.ok(convertToDto(saved, authStudentId));
    }

    /**
     * Gets all group habits for a student.
     */
    @GetMapping
    public ResponseEntity<List<GroupHabitDto>> getGroupHabits(@AuthenticationPrincipal Long authStudentId) {
        logger.info("GET /api/students/{}/group-habits", authStudentId);
        List<GroupHabit> entities = groupHabitRepository.findVisibleToStudent(authStudentId, String.valueOf(authStudentId));
        logger.info("Found {} groups for student {}", entities.size(), authStudentId);
        List<GroupHabitDto> dtos = entities.stream()
                .map(entity -> convertToDto(entity, authStudentId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Gets a specific group habit by ID.
     */
    @GetMapping("/{groupHabitId}")
    public ResponseEntity<GroupHabitDto> getGroupHabit(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable @NonNull Long groupHabitId) {
        GroupHabit existing = groupHabitRepository.findById(groupHabitId)
                .orElseThrow(NotFoundException::new);
        if (!isVisibleToStudent(existing, authStudentId)) {
            throw new ForbiddenException();
        }
        return ResponseEntity.ok(convertToDto(existing, authStudentId));
    }

    /** Mirrors {@link GroupHabitRepository#findVisibleToStudent}: visible to the owner or any listed member. */
    private boolean isVisibleToStudent(GroupHabit entity, Long studentId) {
        if (studentId.equals(entity.getStudentId())) {
            return true;
        }
        String membersJson = entity.getMembersJson();
        return membersJson != null && membersJson.contains("\"id\":\"" + studentId + "\"");
    }

    /**
     * Updates an existing group habit.
     *
     * <p>The owner (the student the group was created by) can edit everything — name, habit,
     * description, invite code/link, icon, and the full member list — and handing off
     * ownership when they remove themselves from that list (see
     * {@link #transferOwnershipIfNeeded}) is how a member gets promoted after the owner
     * leaves.
     *
     * <p>Any other member may only do two things through this endpoint: record their own
     * completion dates, or remove themselves from the group (leave). Everything else in
     * their request body — group details, the invite code/link, or other members' data — is
     * silently ignored rather than trusted from the client.
     */
    @PutMapping("/{groupHabitId}")
    public ResponseEntity<GroupHabitDto> updateGroupHabit(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable @NonNull Long groupHabitId,
            @RequestBody GroupHabitDto dto) {
        GroupHabit existing = groupHabitRepository.findById(groupHabitId)
                .orElseThrow(NotFoundException::new);

        List<GroupHabitDto.MemberDto> currentMembers = readMembers(existing.getMembersJson());
        Map<String, List<String>> currentProgress = readMemberProgress(existing.getMemberProgressJson());
        String selfId = String.valueOf(authStudentId);
        boolean isOwner = authStudentId.equals(existing.getStudentId());

        if (!isOwner && currentMembers.stream().noneMatch(m -> selfId.equals(m.getId()))) {
            throw new ForbiddenException();
        }

        Map<String, List<String>> updatedProgress;

        if (isOwner) {
            existing.setName(dto.getName());
            existing.setHabitName(dto.getHabitName());
            existing.setDescription(dto.getDescription());
            existing.setCode(dto.getCode());
            existing.setInviteLink(dto.getInviteLink());
            existing.setIconId(dto.getIconId());

            List<GroupHabitDto.MemberDto> newMembers = dto.getMembers() != null ? dto.getMembers() : currentMembers;
            writeMembers(existing, newMembers);
            transferOwnershipIfNeeded(existing, authStudentId, dto.getOwnerId(), newMembers);

            updatedProgress = normalizeMemberProgress(
                    dto.getMemberProgress() != null ? dto.getMemberProgress() : currentProgress);
        } else {
            List<GroupHabitDto.MemberDto> newMembers = applySelfLeaveOnly(currentMembers, dto.getMembers(), selfId);
            writeMembers(existing, newMembers);

            Map<String, List<String>> merged = new LinkedHashMap<>(currentProgress);
            if (dto.getMemberProgress() != null && dto.getMemberProgress().containsKey(selfId)) {
                merged.put(selfId, sanitizeDates(dto.getMemberProgress().get(selfId)));
            }
            // Drop progress for anyone no longer listed as a member (covers self, once they leave).
            merged.keySet().removeIf(id -> newMembers.stream().noneMatch(m -> id.equals(m.getId())));
            updatedProgress = normalizeMemberProgress(merged);
        }

        persistProgress(existing, updatedProgress);
        return ResponseEntity.ok(convertToDto(groupHabitRepository.save(existing), authStudentId));
    }

    /**
     * Hands ownership to a remaining member when the current owner removes themselves from
     * the member list — this is how the group survives the owner leaving. Only fires when the
     * owner is gone from {@code newMembers} and the client-requested new owner is actually one
     * of the members left behind; an owner who stays in the group keeps ownership regardless
     * of what {@code requestedOwnerId} says.
     */
    private void transferOwnershipIfNeeded(GroupHabit existing, Long currentOwnerId, String requestedOwnerId,
            List<GroupHabitDto.MemberDto> newMembers) {
        String ownerIdStr = String.valueOf(currentOwnerId);
        boolean ownerStillMember = newMembers.stream().anyMatch(m -> ownerIdStr.equals(m.getId()));
        if (ownerStillMember || requestedOwnerId == null || requestedOwnerId.isBlank()) {
            return;
        }

        boolean requestedIsRemainingMember = newMembers.stream().anyMatch(m -> requestedOwnerId.equals(m.getId()));
        if (!requestedIsRemainingMember) {
            return;
        }

        try {
            existing.setStudentId(Long.parseLong(requestedOwnerId));
        } catch (NumberFormatException ignored) {
            // Keep the current owner if the requested id isn't a valid student id.
        }
    }

    /**
     * A non-owner member's only allowed membership change is removing themselves. Any other
     * shape of member-list edit from a non-owner is discarded and the current list is kept.
     */
    private List<GroupHabitDto.MemberDto> applySelfLeaveOnly(
            List<GroupHabitDto.MemberDto> currentMembers, List<GroupHabitDto.MemberDto> requestedMembers, String selfId) {
        if (requestedMembers == null) {
            return currentMembers;
        }

        boolean selfRemoved = requestedMembers.stream().noneMatch(m -> selfId.equals(m.getId()));
        boolean othersUnchanged = requestedMembers.size() == currentMembers.size() - 1
                && currentMembers.stream()
                        .filter(m -> !selfId.equals(m.getId()))
                        .allMatch(cm -> requestedMembers.stream().anyMatch(rm -> cm.getId() != null && cm.getId().equals(rm.getId())));

        return selfRemoved && othersUnchanged ? requestedMembers : currentMembers;
    }

    private List<String> sanitizeDates(List<String> dates) {
        if (dates == null) {
            return new ArrayList<>();
        }
        return dates.stream().filter(d -> d != null && !d.isBlank()).distinct().collect(Collectors.toList());
    }

    private void writeMembers(GroupHabit existing, List<GroupHabitDto.MemberDto> members) {
        try {
            existing.setMembersJson(objectMapper.writeValueAsString(members));
        } catch (JsonProcessingException e) {
            existing.setMembersJson("[]");
        }
    }

    private void persistProgress(GroupHabit existing, Map<String, List<String>> memberProgress) {
        try {
            existing.setMemberProgressJson(objectMapper.writeValueAsString(memberProgress));
            existing.setCompletedDatesJson(objectMapper.writeValueAsString(flattenMemberProgress(memberProgress)));
        } catch (JsonProcessingException e) {
            existing.setCompletedDatesJson("[]");
            existing.setMemberProgressJson("{}");
        }
    }

    private List<GroupHabitDto.MemberDto> readMembers(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            List<GroupHabitDto.MemberDto> members = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, GroupHabitDto.MemberDto.class));
            return members != null ? members : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Deletes a group habit.
     */
    @DeleteMapping("/{groupHabitId}")
    public ResponseEntity<Void> deleteGroupHabit(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable @NonNull Long groupHabitId) {
        GroupHabit existing = groupHabitRepository.findById(groupHabitId)
                .orElseThrow(NotFoundException::new);
        if (!authStudentId.equals(existing.getStudentId())) {
            throw new ForbiddenException();
        }
        groupHabitRepository.deleteById(groupHabitId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Joins a group habit using an invite code.
     */
    @PostMapping("/join")
    public ResponseEntity<GroupHabitDto> joinGroupHabit(
            @AuthenticationPrincipal Long authStudentId,
            @RequestParam String code) {
        Long studentId = authStudentId;
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        logger.info("POST /api/students/{}/group-habits/join?code={}", studentId, normalizedCode);
        var optional = groupHabitRepository.findByCodeIgnoreCase(normalizedCode);
        if (optional.isEmpty()) {
            logger.warn("Group not found for code: {}", normalizedCode);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        GroupHabit existing = optional.get();
        logger.info("Found group: {} (id={})", existing.getName(), existing.getId());
        
        try {
            List<GroupHabitDto.MemberDto> members = objectMapper.readValue(
                    existing.getMembersJson() != null ? existing.getMembersJson() : "[]",
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, GroupHabitDto.MemberDto.class));

            Map<String, List<String>> memberProgress = readMemberProgress(existing.getMemberProgressJson());
            if (memberProgress.isEmpty()) {
                try {
                    List<String> legacyCompletedDates = objectMapper.readValue(
                            existing.getCompletedDatesJson() != null ? existing.getCompletedDatesJson() : "[]",
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                    if (legacyCompletedDates != null && !legacyCompletedDates.isEmpty()) {
                        memberProgress.put(String.valueOf(existing.getStudentId()), new ArrayList<>(legacyCompletedDates));
                    }
                } catch (Exception ignored) {
                    // Keep the map empty if legacy data cannot be parsed.
                }
            }

            boolean isMember = members.stream()
                    .anyMatch(m -> m.getId().equals(String.valueOf(studentId)));

            if (!isMember) {
                logger.info("Adding new member: studentId={}", studentId);
                String memberName = studentRepository.findById(studentId)
                        .map(Student::getName)
                        .filter(name -> name != null && !name.isBlank())
                        .orElse("Member " + studentId);
                GroupHabitDto.MemberDto newMember = new GroupHabitDto.MemberDto();
                newMember.setId(String.valueOf(studentId));
                newMember.setName(memberName);
                newMember.setRole("member");
                newMember.setJoinedAt(java.time.LocalDateTime.now().toString());
                members.add(newMember);

                memberProgress.putIfAbsent(String.valueOf(studentId), new ArrayList<>());

                existing.setMembersJson(objectMapper.writeValueAsString(members));
                existing.setMemberProgressJson(objectMapper.writeValueAsString(normalizeMemberProgress(memberProgress)));
                existing.setCompletedDatesJson(objectMapper.writeValueAsString(flattenMemberProgress(memberProgress)));
                groupHabitRepository.save(existing);
                logger.info("Member added successfully. Total members: {}", members.size());
                pushNotificationService.sendToStudent(existing.getStudentId(), "New group member",
                        memberName + " joined " + existing.getName() + ".");
            } else {
                logger.info("User {} already a member of group", studentId);
            }

            return ResponseEntity.ok(convertToDto(existing, studentId));
        } catch (Exception e) {
            logger.error("Error joining group: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Emails a group-habit invite to the given address. The invite message (group name,
     * habit name, sender's name, invite link/code) is generated entirely server-side —
     * the caller only supplies the recipient's email.
     */
    @PostMapping("/{groupHabitId}/invite")
    public ResponseEntity<Void> inviteByEmail(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable @NonNull Long groupHabitId,
            @RequestBody GroupHabitInviteRequest request) {
        logger.info("POST /api/students/{}/group-habits/{}/invite", authStudentId, groupHabitId);
        groupHabitInviteService.sendInvite(authStudentId, groupHabitId, request == null ? null : request.getEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * Converts a DTO to a database entity.
     */
    private GroupHabit convertToEntity(GroupHabitDto dto) {
        GroupHabit entity = new GroupHabit();
        entity.setName(dto.getName());
        entity.setHabitName(dto.getHabitName());
        entity.setDescription(dto.getDescription());
        entity.setCode(dto.getCode() == null ? null : dto.getCode().trim().toUpperCase());
        entity.setInviteLink(dto.getInviteLink());
        entity.setIconId(dto.getIconId());

        try {
            entity.setMembersJson(objectMapper.writeValueAsString(dto.getMembers()));
        } catch (Exception e) {
            entity.setMembersJson("[]");
        }

        try {
            Map<String, List<String>> memberProgress = normalizeMemberProgress(dto.getMemberProgress());
            if (memberProgress.isEmpty()) {
                List<String> completedDates = dto.getCompletedDates() != null ? dto.getCompletedDates() : List.of();
                memberProgress = new LinkedHashMap<>();
                memberProgress.put(String.valueOf(entity.getStudentId() != null ? entity.getStudentId() : 0L), new ArrayList<>(completedDates));
            }
            entity.setMemberProgressJson(objectMapper.writeValueAsString(memberProgress));
            entity.setCompletedDatesJson(objectMapper.writeValueAsString(flattenMemberProgress(memberProgress)));
        } catch (Exception e) {
            entity.setCompletedDatesJson("[]");
            entity.setMemberProgressJson("{}");
        }

        return entity;
    }

    /**
     * Converts a database entity to a DTO, hiding the invite link and code from anyone but the
     * owner — members shouldn't be able to see or re-share them.
     */
    private GroupHabitDto convertToDto(GroupHabit entity, Long requestingStudentId) {
        GroupHabitDto dto = convertToDto(entity);
        boolean isOwner = requestingStudentId != null && requestingStudentId.equals(entity.getStudentId());
        if (!isOwner) {
            dto.setInviteLink(null);
            dto.setCode(null);
        }
        return dto;
    }

    /**
     * Converts a database entity to a DTO.
     */
    private GroupHabitDto convertToDto(GroupHabit entity) {
        GroupHabitDto dto = new GroupHabitDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setHabitName(entity.getHabitName());
        dto.setDescription(entity.getDescription());
        dto.setCode(entity.getCode());
        dto.setInviteLink(entity.getInviteLink());
        dto.setIconId(entity.getIconId());
        dto.setOwnerId(String.valueOf(entity.getStudentId()));
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);

        try {
            List<GroupHabitDto.MemberDto> members = objectMapper.readValue(
                    entity.getMembersJson() != null ? entity.getMembersJson() : "[]",
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, GroupHabitDto.MemberDto.class));
            dto.setMembers(enrichMemberNames(members));
        } catch (Exception e) {
            dto.setMembers(List.of());
        }

        try {
            dto.setMemberProgress(readMemberProgress(entity.getMemberProgressJson()));
            List<String> completedDates = objectMapper.readValue(
                    entity.getCompletedDatesJson() != null ? entity.getCompletedDatesJson() : "[]",
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            if (completedDates == null || completedDates.isEmpty()) {
                completedDates = flattenMemberProgress(dto.getMemberProgress());
            }
            dto.setCompletedDates(completedDates);
        } catch (Exception e) {
            dto.setCompletedDates(List.of());
            dto.setMemberProgress(Map.of());
        }

        return dto;
    }

    private Map<String, List<String>> readMemberProgress(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, List<String>>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, List<String>> normalizeMemberProgress(Map<String, List<String>> memberProgress) {
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        if (memberProgress == null) {
            return normalized;
        }

        memberProgress.forEach((memberId, dates) -> {
            if (memberId == null || memberId.isBlank()) {
                return;
            }
            List<String> safeDates = dates == null ? new ArrayList<>() : dates.stream().filter(date -> date != null && !date.isBlank()).distinct().toList();
            normalized.put(memberId, new ArrayList<>(safeDates));
        });

        return normalized;
    }

    private List<String> flattenMemberProgress(Map<String, List<String>> memberProgress) {
        if (memberProgress == null || memberProgress.isEmpty()) {
            return List.of();
        }

        return memberProgress.values().stream()
                .flatMap(List::stream)
                .filter(date -> date != null && !date.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Resolves placeholder member names like "Member 16" to real student names from DB.
     */
    private List<GroupHabitDto.MemberDto> enrichMemberNames(List<GroupHabitDto.MemberDto> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }

        Set<Long> idsToResolve = new HashSet<>();
        for (GroupHabitDto.MemberDto member : members) {
            if (member == null || member.getId() == null || member.getId().isBlank()) {
                continue;
            }

            String name = member.getName() == null ? "" : member.getName().trim();
            boolean needsResolution = name.isBlank() || name.matches("(?i)^member\\s+\\d+$");
            if (!needsResolution) {
                continue;
            }

            try {
                idsToResolve.add(Long.parseLong(member.getId().trim()));
            } catch (NumberFormatException ignored) {
                // Keep existing name when id is not numeric.
            }
        }

        if (idsToResolve.isEmpty()) {
            return members;
        }

        Map<Long, String> namesById = new HashMap<>();
        studentRepository.findAllById(idsToResolve).forEach(student -> {
            if (student != null && student.getId() != null && student.getName() != null && !student.getName().isBlank()) {
                namesById.put(student.getId(), student.getName());
            }
        });

        for (GroupHabitDto.MemberDto member : members) {
            if (member == null || member.getId() == null || member.getId().isBlank()) {
                continue;
            }

            String name = member.getName() == null ? "" : member.getName().trim();
            boolean needsResolution = name.isBlank() || name.matches("(?i)^member\\s+\\d+$");
            if (!needsResolution) {
                continue;
            }

            try {
                Long parsedId = Long.parseLong(member.getId().trim());
                String resolved = namesById.get(parsedId);
                if (resolved != null && !resolved.isBlank()) {
                    member.setName(resolved);
                }
            } catch (NumberFormatException ignored) {
                // Keep existing name when id is not numeric.
            }
        }

        return members;
    }
}