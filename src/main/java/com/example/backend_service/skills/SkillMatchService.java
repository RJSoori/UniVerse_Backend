package com.example.backend_service.skills;

import com.example.backend_service.jobhub.enums.JobStatus;
import com.example.backend_service.jobhub.model.Job;
import com.example.backend_service.jobhub.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Computes "UniVerse Skill Matcher" percentages between a student's skill set
 * ({@link StudentSkillProfile}) and approved job postings' required skills ({@link Job#getSkills()}),
 * and the top skills a student is missing that would unlock the most currently-unsuitable postings.
 *
 * <p>{@code Job.skills} is a single freeform, comma-separated string with no enforced structure
 * (recruiters type it into a plain textarea) - this service owns the only parsing convention for it.
 */
@Service
public class SkillMatchService {

    /** A job is "suitable" for suggested-skills purposes at this percentage or above. */
    private static final int SUITABLE_THRESHOLD = 80;

    /** Max entries returned by {@link #computeSuggestedSkills}. */
    private static final int MAX_SUGGESTED_SKILLS = 5;

    private final JobRepository jobRepository;
    private final StudentSkillProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    public SkillMatchService(
            JobRepository jobRepository,
            StudentSkillProfileRepository profileRepository,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
    }

    /** One match-percentage entry per APPROVED job that has at least one parseable skill token. */
    public List<JobMatchScore> computeMatchScores(Long studentId) {
        List<String> studentSkills = loadStudentSkills(studentId);
        return eligibleJobs().stream()
                .map(job -> new JobMatchScore(
                        job.jobId(),
                        percentage(countMatched(job.tokens(), studentSkills), job.tokens().size())))
                .toList();
    }

    /**
     * The top skills the student doesn't have, ranked by how many jobs currently scoring below
     * {@link #SUITABLE_THRESHOLD} would cross it if the student picked up that one skill.
     * A job needing several new skills at once never shows up under any single skill here -
     * that's expected: this ranks individual skills' marginal impact, not skill combinations.
     */
    public List<SuggestedSkill> computeSuggestedSkills(Long studentId) {
        List<String> studentSkills = loadStudentSkills(studentId);

        // Tally keyed case-insensitively so "SQL" (job A) and "Sql" (job B) accumulate together;
        // the first casing seen is kept for display.
        Map<String, Integer> tally = new LinkedHashMap<>();
        Map<String, String> displayName = new LinkedHashMap<>();

        for (JobTokens job : eligibleJobs()) {
            int matched = countMatched(job.tokens(), studentSkills);
            int currentScore = percentage(matched, job.tokens().size());
            if (currentScore >= SUITABLE_THRESHOLD) {
                continue; // already suitable - not a candidate to "unlock"
            }
            for (String token : job.tokens()) {
                boolean studentAlreadyHasIt =
                        studentSkills.stream().anyMatch(s -> s.equalsIgnoreCase(token));
                if (studentAlreadyHasIt) {
                    continue;
                }
                int simulatedScore = percentage(matched + 1, job.tokens().size());
                if (simulatedScore >= SUITABLE_THRESHOLD) {
                    String key = token.toLowerCase(Locale.ROOT);
                    tally.merge(key, 1, Integer::sum);
                    displayName.putIfAbsent(key, token);
                }
            }
        }

        return tally.entrySet().stream()
                .map(e -> new SuggestedSkill(displayName.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingInt(SuggestedSkill::jobsUnlocked).reversed()
                        .thenComparing(s -> s.skill().toLowerCase(Locale.ROOT)))
                .limit(MAX_SUGGESTED_SKILLS)
                .toList();
    }

    /** Read-only lookup - deliberately does NOT create a profile row as a side effect. */
    private List<String> loadStudentSkills(Long studentId) {
        return profileRepository.findByStudentId(studentId)
                .map(profile -> SkillsJsonUtil.readSkills(objectMapper, profile.getSkillsJson()))
                .orElse(List.of());
    }

    /** APPROVED jobs with their skill string parsed into tokens; jobs with none are excluded. */
    private List<JobTokens> eligibleJobs() {
        return jobRepository.findByStatus(JobStatus.APPROVED).stream()
                .map(job -> new JobTokens(job.getId(), parseJobSkills(job.getSkills())))
                .filter(job -> !job.tokens().isEmpty())
                .toList();
    }

    /** Splits on comma, trims, drops blanks, and dedupes case-insensitively (first casing kept). */
    private List<String> parseJobSkills(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            boolean alreadyPresent = tokens.stream().anyMatch(t -> t.equalsIgnoreCase(trimmed));
            if (!alreadyPresent) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    private static int countMatched(List<String> jobTokens, List<String> studentSkills) {
        return (int) jobTokens.stream()
                .filter(token -> studentSkills.stream().anyMatch(s -> s.equalsIgnoreCase(token)))
                .count();
    }

    /** Double division, then round - integer division here would truncate before rounding. */
    private static int percentage(int matched, int total) {
        return (int) Math.round((matched * 100.0) / total);
    }

    private record JobTokens(Long jobId, List<String> tokens) {
    }
}
