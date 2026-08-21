package com.example.backend_service.skills;

import com.example.backend_service.jobhub.enums.JobStatus;
import com.example.backend_service.jobhub.model.Job;
import com.example.backend_service.jobhub.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
     * The top skills (or, where needed, skill pairs) the student doesn't have, ranked by how
     * many jobs currently scoring below {@link #SUITABLE_THRESHOLD} would cross it. Two passes:
     * first, every job that a single missing skill alone would unlock (as before); second -
     * only for jobs that NO single skill could unlock on its own - every pair of missing skills
     * that would unlock it *together*, since recommending either skill alone there would be
     * misleading (neither one, by itself, gets the student to a suitable match).
     */
    public List<SuggestedSkill> computeSuggestedSkills(Long studentId) {
        List<String> studentSkills = loadStudentSkills(studentId);

        List<JobTokens> belowThreshold = eligibleJobs().stream()
                .filter(job -> percentage(countMatched(job.tokens(), studentSkills), job.tokens().size()) < SUITABLE_THRESHOLD)
                .toList();

        // ---- Pass 1: single skills ----
        // Tally keyed case-insensitively so "SQL" (job A) and "Sql" (job B) accumulate together;
        // the first casing seen is kept for display.
        Map<String, Integer> singleTally = new LinkedHashMap<>();
        Map<String, String> singleDisplay = new LinkedHashMap<>();
        Set<Long> singleUnlockableJobIds = new HashSet<>();

        for (JobTokens job : belowThreshold) {
            int matched = countMatched(job.tokens(), studentSkills);
            for (String token : missingTokens(job.tokens(), studentSkills)) {
                int simulatedScore = percentage(matched + 1, job.tokens().size());
                if (simulatedScore >= SUITABLE_THRESHOLD) {
                    String key = token.toLowerCase(Locale.ROOT);
                    singleTally.merge(key, 1, Integer::sum);
                    singleDisplay.putIfAbsent(key, token);
                    singleUnlockableJobIds.add(job.jobId());
                }
            }
        }

        // ---- Pass 2: pairs, only for jobs no single skill could unlock ----
        Map<String, Integer> pairTally = new LinkedHashMap<>();
        Map<String, List<String>> pairDisplay = new LinkedHashMap<>();

        for (JobTokens job : belowThreshold) {
            if (singleUnlockableJobIds.contains(job.jobId())) {
                continue;
            }
            int matched = countMatched(job.tokens(), studentSkills);
            List<String> missing = missingTokens(job.tokens(), studentSkills);
            for (int i = 0; i < missing.size(); i++) {
                for (int j = i + 1; j < missing.size(); j++) {
                    int simulatedScore = percentage(matched + 2, job.tokens().size());
                    if (simulatedScore >= SUITABLE_THRESHOLD) {
                        List<String> pair = Stream.of(missing.get(i), missing.get(j))
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList();
                        String key = pair.get(0).toLowerCase(Locale.ROOT) + "+" + pair.get(1).toLowerCase(Locale.ROOT);
                        pairTally.merge(key, 1, Integer::sum);
                        pairDisplay.putIfAbsent(key, pair);
                    }
                }
            }
        }

        Stream<SuggestedSkill> singleSuggestions = singleTally.entrySet().stream()
                .map(e -> new SuggestedSkill(List.of(singleDisplay.get(e.getKey())), e.getValue()));
        Stream<SuggestedSkill> pairSuggestions = pairTally.entrySet().stream()
                .map(e -> new SuggestedSkill(pairDisplay.get(e.getKey()), e.getValue()));

        return Stream.concat(singleSuggestions, pairSuggestions)
                // Ranked by impact first; ties prefer the simpler (single-skill) ask, then
                // alphabetically so repeated calls are stable.
                .sorted(Comparator.comparingInt(SuggestedSkill::jobsUnlocked).reversed()
                        .thenComparingInt(s -> s.skills().size())
                        .thenComparing(s -> String.join("+", s.skills()).toLowerCase(Locale.ROOT)))
                .limit(MAX_SUGGESTED_SKILLS)
                .toList();
    }

    /** Tokens from a job's skill list the student doesn't already have (case-insensitive). */
    private static List<String> missingTokens(List<String> jobTokens, List<String> studentSkills) {
        return jobTokens.stream()
                .filter(token -> studentSkills.stream().noneMatch(s -> s.equalsIgnoreCase(token)))
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
