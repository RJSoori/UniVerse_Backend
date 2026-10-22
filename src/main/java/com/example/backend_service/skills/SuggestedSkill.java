package com.example.backend_service.skills;

import java.util.List;

/**
 * A skill (or, when no single skill would do it, a pair of skills that only work together)
 * the student doesn't currently have, ranked by how many otherwise-sub-80% job postings would
 * cross the "suitable" threshold if the student picked it/them up. {@code skills} has size 1
 * for an ordinary single-skill suggestion, or size 2 for a combo suggestion - see
 * {@link SkillMatchService#computeSuggestedSkills}.
 */
public record SuggestedSkill(List<String> skills, int jobsUnlocked) {
}
