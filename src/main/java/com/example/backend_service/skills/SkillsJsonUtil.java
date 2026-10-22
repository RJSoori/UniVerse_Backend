package com.example.backend_service.skills;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Shared convention for the {@code skillsJson} column on {@link StudentSkillProfile}: a JSON
 * array of skill names, read/written via Jackson. Extracted from {@link SkillsController} so the
 * skill-matching feature doesn't grow a third ad hoc copy of the same try/catch.
 */
final class SkillsJsonUtil {

    private SkillsJsonUtil() {}

    static List<String> readSkills(ObjectMapper objectMapper, String skillsJson) {
        try {
            return objectMapper.readValue(skillsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    static String writeSkills(ObjectMapper objectMapper, List<String> skills) {
        try {
            return objectMapper.writeValueAsString(skills);
        } catch (Exception e) {
            return "[]";
        }
    }
}
