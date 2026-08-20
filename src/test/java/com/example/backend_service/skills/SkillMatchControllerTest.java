package com.example.backend_service.skills;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillMatchControllerTest {

    @Mock private SkillMatchService skillMatchService;

    @InjectMocks
    private SkillMatchController controller;

    @Test
    void getMatchScores_delegatesToService() {
        List<JobMatchScore> scores = List.of(new JobMatchScore(1L, 75));
        when(skillMatchService.computeMatchScores(42L)).thenReturn(scores);

        assertThat(controller.getMatchScores(42L)).isEqualTo(scores);
    }

    @Test
    void getSuggestedSkills_delegatesToService() {
        List<SuggestedSkill> suggestions = List.of(new SuggestedSkill("Docker", 3));
        when(skillMatchService.computeSuggestedSkills(42L)).thenReturn(suggestions);

        assertThat(controller.getSuggestedSkills(42L)).isEqualTo(suggestions);
    }
}
