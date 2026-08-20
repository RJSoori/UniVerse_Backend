package com.example.backend_service.skills;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * "UniVerse Skill Matcher" and "View suggested skills" for Job Hub - sits alongside
 * {@link SkillsController} (same base path) but stays a separate file so that controller isn't
 * bloated with scoring logic; all the actual work lives in {@link SkillMatchService}.
 */
@RestController
@RequestMapping("/api/skills")
public class SkillMatchController {

    private final SkillMatchService skillMatchService;

    public SkillMatchController(SkillMatchService skillMatchService) {
        this.skillMatchService = skillMatchService;
    }

    @GetMapping("/match-scores")
    public List<JobMatchScore> getMatchScores(@AuthenticationPrincipal Long authStudentId) {
        return skillMatchService.computeMatchScores(authStudentId);
    }

    @GetMapping("/suggested-skills")
    public List<SuggestedSkill> getSuggestedSkills(@AuthenticationPrincipal Long authStudentId) {
        return skillMatchService.computeSuggestedSkills(authStudentId);
    }
}
