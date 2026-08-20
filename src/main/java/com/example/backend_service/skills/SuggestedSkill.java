package com.example.backend_service.skills;

/**
 * A skill the student doesn't currently have, ranked by how many otherwise-sub-80% job postings
 * would cross the "suitable" threshold if the student picked it up. See
 * {@link SkillMatchService#computeSuggestedSkills}.
 */
public record SuggestedSkill(String skill, int jobsUnlocked) {
}
