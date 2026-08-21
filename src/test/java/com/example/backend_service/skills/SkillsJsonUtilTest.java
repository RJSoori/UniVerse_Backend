package com.example.backend_service.skills;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsJsonUtilTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readSkills_parsesJsonArray() {
        List<String> result = SkillsJsonUtil.readSkills(objectMapper, "[\"Java\",\"SQL\"]");
        assertThat(result).containsExactly("Java", "SQL");
    }

    @Test
    void readSkills_fallsBackToEmptyListOnMalformedJson() {
        assertThat(SkillsJsonUtil.readSkills(objectMapper, "not json")).isEmpty();
    }

    @Test
    void readSkills_fallsBackToEmptyListOnNull() {
        assertThat(SkillsJsonUtil.readSkills(objectMapper, null)).isEmpty();
    }

    @Test
    void writeSkills_roundTripsThroughReadSkills() {
        String json = SkillsJsonUtil.writeSkills(objectMapper, List.of("React", "Project Management"));
        assertThat(SkillsJsonUtil.readSkills(objectMapper, json))
                .containsExactly("React", "Project Management");
    }
}
