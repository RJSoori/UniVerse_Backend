package com.example.backend_service.skills;

import com.example.backend_service.jobhub.enums.JobStatus;
import com.example.backend_service.jobhub.model.Job;
import com.example.backend_service.jobhub.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillMatchServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private StudentSkillProfileRepository profileRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SkillMatchService service;

    @BeforeEach
    void setup() {
        service = new SkillMatchService(jobRepository, profileRepository, objectMapper);
    }

    // ---- computeMatchScores ----

    @Test
    void computeMatchScores_roundsDownUsingDoubleDivision() {
        when(jobRepository.findByStatus(JobStatus.APPROVED))
                .thenReturn(List.of(job(1L, "Java, SQL, React")));
        when(profileRepository.findByStudentId(42L))
                .thenReturn(Optional.of(profileWithSkills(42L, "Java")));

        assertThat(service.computeMatchScores(42L)).containsExactly(new JobMatchScore(1L, 33));
    }

    @Test
    void computeMatchScores_roundsUpUsingDoubleDivision() {
        when(jobRepository.findByStatus(JobStatus.APPROVED))
                .thenReturn(List.of(job(1L, "Java, SQL, React")));
        when(profileRepository.findByStudentId(42L))
                .thenReturn(Optional.of(profileWithSkills(42L, "Java", "SQL")));

        assertThat(service.computeMatchScores(42L)).containsExactly(new JobMatchScore(1L, 67));
    }

    @Test
    void computeMatchScores_excludesJobWithNoSkillTokens() {
        when(jobRepository.findByStatus(JobStatus.APPROVED)).thenReturn(List.of(job(1L, "   ")));
        when(profileRepository.findByStudentId(42L)).thenReturn(Optional.empty());

        assertThat(service.computeMatchScores(42L)).isEmpty();
    }

    @Test
    void computeMatchScores_matchesCaseInsensitively() {
        when(jobRepository.findByStatus(JobStatus.APPROVED)).thenReturn(List.of(job(1L, "python")));
        when(profileRepository.findByStudentId(42L))
                .thenReturn(Optional.of(profileWithSkills(42L, "PYTHON")));

        assertThat(service.computeMatchScores(42L)).containsExactly(new JobMatchScore(1L, 100));
    }

    @Test
    void computeMatchScores_dedupesDuplicateTokensWithinJob() {
        when(jobRepository.findByStatus(JobStatus.APPROVED))
                .thenReturn(List.of(job(1L, "Java, java, JAVA")));
        when(profileRepository.findByStudentId(42L))
                .thenReturn(Optional.of(profileWithSkills(42L, "Java")));

        // 1/1, not 1/3 - duplicate tokens collapse into a single requirement
        assertThat(service.computeMatchScores(42L)).containsExactly(new JobMatchScore(1L, 100));
    }

    @Test
    void computeMatchScores_filtersBlankTokens() {
        when(jobRepository.findByStatus(JobStatus.APPROVED))
                .thenReturn(List.of(job(1L, "Java,, SQL ,")));
        when(profileRepository.findByStudentId(42L))
                .thenReturn(Optional.of(profileWithSkills(42L, "Java", "SQL")));

        assertThat(service.computeMatchScores(42L)).containsExactly(new JobMatchScore(1L, 100));
    }

    @Test
    void computeMatchScores_noProfileRow_defaultsToZeroPercentWithoutCreatingOne() {
        when(jobRepository.findByStatus(JobStatus.APPROVED)).thenReturn(List.of(job(1L, "Java")));
        when(profileRepository.findByStudentId(42L)).thenReturn(Optional.empty());

        assertThat(service.computeMatchScores(42L)).containsExactly(new JobMatchScore(1L, 0));
        verify(profileRepository, never()).save(any());
    }

    // ---- computeSuggestedSkills ----

    @Test
    void computeSuggestedSkills_singleMissingSkillThatCrossesThreshold_isSuggested() {
        when(jobRepository.findByStatus(JobStatus.APPROVED)).thenReturn(List.of(job(1L, "Java")));
        when(profileRepository.findByStudentId(42L)).thenReturn(Optional.empty());

        assertThat(service.computeSuggestedSkills(42L))
                .containsExactly(new SuggestedSkill("Java", 1));
    }

    @Test
    void computeSuggestedSkills_jobNeedingMultipleNewSkills_doesNotSuggestAnySingleOne() {
        when(jobRepository.findByStatus(JobStatus.APPROVED))
                .thenReturn(List.of(job(1L, "Java, SQL, React, Docker, AWS")));
        when(profileRepository.findByStudentId(42L)).thenReturn(Optional.empty());

        // Adding any one skill only reaches 1/5 = 20% - never crosses 80%
        assertThat(service.computeSuggestedSkills(42L)).isEmpty();
    }

    @Test
    void computeSuggestedSkills_jobAlreadyAtThreshold_excluded() {
        when(jobRepository.findByStatus(JobStatus.APPROVED))
                .thenReturn(List.of(job(1L, "Java, SQL, React, Docker, AWS")));
        when(profileRepository.findByStudentId(42L))
                .thenReturn(Optional.of(profileWithSkills(42L, "Java", "SQL", "React", "Docker")));

        // Already 4/5 = 80% (inclusive threshold) - AWS must not be suggested from this job
        assertThat(service.computeSuggestedSkills(42L)).isEmpty();
    }

    @Test
    void computeSuggestedSkills_tiesBrokenAlphabetically() {
        when(jobRepository.findByStatus(JobStatus.APPROVED))
                .thenReturn(List.of(job(1L, "SQL"), job(2L, "Docker")));
        when(profileRepository.findByStudentId(42L)).thenReturn(Optional.empty());

        assertThat(service.computeSuggestedSkills(42L))
                .extracting(SuggestedSkill::skill)
                .containsExactly("Docker", "SQL");
    }

    @Test
    void computeSuggestedSkills_mergesSameSkillAcrossJobsCaseInsensitively() {
        when(jobRepository.findByStatus(JobStatus.APPROVED))
                .thenReturn(List.of(job(1L, "SQL"), job(2L, "Sql")));
        when(profileRepository.findByStudentId(42L)).thenReturn(Optional.empty());

        assertThat(service.computeSuggestedSkills(42L))
                .containsExactly(new SuggestedSkill("SQL", 2));
    }

    @Test
    void computeSuggestedSkills_capsAtFiveResults() {
        List<Job> jobs = new ArrayList<>();
        String[] skills = {"A", "B", "C", "D", "E", "F"};
        for (int i = 0; i < skills.length; i++) {
            jobs.add(job((long) (i + 1), skills[i]));
        }
        when(jobRepository.findByStatus(JobStatus.APPROVED)).thenReturn(jobs);
        when(profileRepository.findByStudentId(42L)).thenReturn(Optional.empty());

        assertThat(service.computeSuggestedSkills(42L)).hasSize(5);
    }

    private Job job(Long id, String skills) {
        Job job = new Job();
        job.setId(id);
        job.setSkills(skills);
        job.setStatus(JobStatus.APPROVED);
        return job;
    }

    private StudentSkillProfile profileWithSkills(Long studentId, String... skills) {
        StudentSkillProfile profile = new StudentSkillProfile();
        profile.setStudentId(studentId);
        profile.setSkillsJson(SkillsJsonUtil.writeSkills(objectMapper, List.of(skills)));
        return profile;
    }
}
