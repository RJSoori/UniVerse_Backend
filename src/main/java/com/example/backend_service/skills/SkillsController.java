package com.example.backend_service.skills;

import com.example.backend_service.AzureBlobService;
import com.example.backend_service.common.exception.BadRequestException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A student's Job Hub skills profile: the manually-managed skill list, plus CV upload ->
 * Gemini extraction that merges newly identified skills into it. Backs job-hub/SkillsManager.tsx,
 * which previously stored everything in localStorage with a mocked "AI parsing" step.
 */
@RestController
@RequestMapping("/api/skills")
public class SkillsController {

    private static final Logger log = LoggerFactory.getLogger(SkillsController.class);
    private static final long MAX_CV_SIZE_BYTES = 10L * 1024 * 1024; // matches AzureBlobService's own cap

    private final StudentSkillProfileRepository repository;
    private final AzureBlobService azureBlobService;
    private final GeminiSkillExtractionService extractionService;
    private final ObjectMapper objectMapper;

    public SkillsController(
            StudentSkillProfileRepository repository,
            AzureBlobService azureBlobService,
            GeminiSkillExtractionService extractionService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.azureBlobService = azureBlobService;
        this.extractionService = extractionService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public Map<String, Object> getSkills(@AuthenticationPrincipal Long authStudentId) {
        StudentSkillProfile profile = findOrCreate(authStudentId);
        return toResponse(profile);
    }

    /** Full replace — used by the manual add/remove UI. */
    @PutMapping
    public Map<String, Object> replaceSkills(
            @AuthenticationPrincipal Long authStudentId,
            @RequestBody List<String> skills) {
        StudentSkillProfile profile = findOrCreate(authStudentId);
        writeSkills(profile, normalize(skills));
        repository.save(profile);
        log.info("student={} updated skills manually, count={}", authStudentId, readSkills(profile).size());
        return toResponse(profile);
    }

    @PostMapping("/cv")
    public Map<String, Object> uploadCv(
            @AuthenticationPrincipal Long authStudentId,
            @RequestParam("file") MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a CV file to upload.");
        }
        if (file.getSize() > MAX_CV_SIZE_BYTES) {
            throw new BadRequestException("File exceeds the maximum allowed size of 10MB.");
        }

        String resumeText = PdfTextExtractor.extractText(file);
        List<String> extracted = extractionService.extractSkills(resumeText);

        // Upload after extraction succeeds - no point keeping a CV we couldn't analyze.
        String cvUrl = azureBlobService.uploadFile(file);

        StudentSkillProfile profile = findOrCreate(authStudentId);
        List<String> existing = readSkills(profile);
        // Only the subset Gemini found that the student didn't already have - "extracted" is
        // everything Gemini found in the CV this time, which usually overlaps heavily with
        // what's already on file.
        List<String> newlyAdded = extracted.stream()
                .filter(skill -> existing.stream().noneMatch(s -> s.equalsIgnoreCase(skill)))
                .distinct()
                .toList();
        List<String> merged = mergeSkills(existing, newlyAdded);
        writeSkills(profile, merged);
        profile.setCvUrl(cvUrl);
        profile.setCvUploadedAt(Instant.now());
        repository.save(profile);

        log.info("student={} uploaded CV, extracted={} newSkills, totalSkills={}",
                authStudentId, newlyAdded.size(), merged.size());

        Map<String, Object> response = new LinkedHashMap<>(toResponse(profile));
        response.put("newlyExtracted", newlyAdded);
        return response;
    }

    private StudentSkillProfile findOrCreate(Long studentId) {
        return repository.findByStudentId(studentId).orElseGet(() -> {
            StudentSkillProfile profile = new StudentSkillProfile();
            profile.setStudentId(studentId);
            return repository.save(profile);
        });
    }

    private List<String> mergeSkills(List<String> existing, List<String> extracted) {
        List<String> merged = new ArrayList<>(existing);
        for (String skill : extracted) {
            boolean alreadyPresent = merged.stream().anyMatch(s -> s.equalsIgnoreCase(skill));
            if (!alreadyPresent) {
                merged.add(skill);
            }
        }
        return merged;
    }

    private List<String> normalize(List<String> skills) {
        if (skills == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String skill : skills) {
            if (skill == null) {
                continue;
            }
            String trimmed = skill.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            boolean alreadyPresent = result.stream().anyMatch(s -> s.equalsIgnoreCase(trimmed));
            if (!alreadyPresent) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private List<String> readSkills(StudentSkillProfile profile) {
        try {
            return objectMapper.readValue(profile.getSkillsJson(), new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private void writeSkills(StudentSkillProfile profile, List<String> skills) {
        try {
            profile.setSkillsJson(objectMapper.writeValueAsString(skills));
        } catch (Exception e) {
            profile.setSkillsJson("[]");
        }
    }

    private Map<String, Object> toResponse(StudentSkillProfile profile) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("skills", readSkills(profile));
        response.put("cvUrl", profile.getCvUrl());
        response.put("cvUploadedAt", profile.getCvUploadedAt());
        return response;
    }
}
