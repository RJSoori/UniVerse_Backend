package com.example.backend_service.skills;

/**
 * A job posting's skill-match percentage for the current student, as computed by
 * {@link SkillMatchService}. Only jobs with at least one parseable skill token are represented -
 * a job listing no skills has nothing to score against, so it's simply absent rather than 0%.
 */
public record JobMatchScore(Long jobId, int matchPercentage) {
}
